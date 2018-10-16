package pl.newicom.jobman.execution

import java.time.ZonedDateTime.now
import java.util.UUID

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, withTimers}
import akka.persistence.typed.SideEffect
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman._
import pl.newicom.jobman.execution.JobExecution._
import pl.newicom.jobman.execution.cluster.WorkerOffice
import pl.newicom.jobman.execution.command.{ConfirmJobActivity, ExpireOverrunningJobs, JobExecutionCommand, StartJob}
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.execution.result.{JobFailure, JobTimeout, SuccessfulJobResult}
import pl.newicom.jobman.execution.worker.command.ExecuteJob
import pl.newicom.jobman.healthcheck.HealthCheckTopic
import pl.newicom.jobman.healthcheck.event.WorkerStopped
import pl.newicom.jobman.progress.ProgressTopic
import pl.newicom.jobman.progress.event.JobProgressUpdated
import pl.newicom.jobman.schedule.JobScheduling.JobSchedulingJournalId
import pl.newicom.jobman.schedule.event.JobDispatchedForExecution
import pl.newicom.jobman.shared.command.{JobExecutionReport, QueueTerminationReport, Stop, StopDueToEventSubsriptionTermination}

import scala.concurrent.duration._

object JobExecution {
  val JobExecutionJournalId = "JobExecution"

  val overrunningJobsCheckoutInterval: FiniteDuration = 5.seconds

  def behavior(config: JobExecutionConfig)(implicit jm: JobMan): Behavior[JobExecutionCommand] =
    withTimers(scheduler => {
      scheduler.startPeriodicTimer("TickKey", ExpireOverrunningJobs, overrunningJobsCheckoutInterval)
      setup(ctx => {

        def queueTermination: Behavior[WorkerStopped] =
          receiveMessage { event =>
            ctx.self ! QueueTerminationReport(event.queueId, event.runningJob)
            same
          }

        def jobActivityConfirmation: Behavior[JobProgressUpdated] =
          receiveMessage { event =>
            ctx.self ! ConfirmJobActivity(event.jobId)
            same
          }

        jm.distributedPubSub.subscribe(HealthCheckTopic, ctx.spawn(queueTermination, "QueueTerminationReporter"))
        jm.distributedPubSub.subscribe(ProgressTopic, ctx.spawn(jobActivityConfirmation, "JobActivityConfirmer"))

        PersistentBehaviors
          .receive(
            persistenceId = JobExecutionJournalId,
            emptyState = JobExecutionState(jm.jobConfigRegistry),
            commandHandler = new JobExecutionCommandHandler(ctx, eventHandler),
            eventHandler
          )
          .onRecoveryCompleted(recoveryHandler(ctx))
          .snapshotEvery(jm.config.journalSnapshotInterval)
      })
    })

  private def recoveryHandler(ctx: ActorContext[JobExecutionCommand])(implicit jm: JobMan): JobExecutionState => Unit = { state =>
    ctx.log.info("Job Execution resumed from schedulingJournalOffset: {}", state.schedulingJournalOffset)

    def reactToJobDispatchedForExecution(reaction: (JobDispatchedForExecution, Long) => JobExecutionCommand) = {
      val source: Source[JobExecutionCommand, NotUsed] = jm.readJournal
        .eventsByPersistenceId(JobSchedulingJournalId, state.schedulingJournalOffset, Long.MaxValue)
        .filter(envelope => envelope.event.isInstanceOf[JobDispatchedForExecution])
        .map(envelope => reaction(envelope.event.asInstanceOf[JobDispatchedForExecution], envelope.sequenceNr))

      val sink: Sink[JobExecutionCommand, NotUsed] = ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination.apply)

      source.runWith(sink)(jm.actorMaterializer("Job Execution service failure"))
    }

    reactToJobDispatchedForExecution((event, offset) => StartJob(event.job, event.queueId, offset))
  }

  type EventHandler = (JobExecutionState, JobExecutorEvent) => JobExecutionState

  private val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

  def jobExecutionReportSource(journalOffset: Long, filter: JobExecutionTerminalEvent => Boolean = _ => true)(
      implicit jm: JobMan): Source[JobExecutionReport, NotUsed] =
    jm.readJournal
      .eventsByPersistenceId(JobExecutionJournalId, journalOffset, Long.MaxValue)
      .filter(envelope => envelope.event.isInstanceOf[JobExecutionTerminalEvent])
      .map(envelope => (envelope.event.asInstanceOf[JobExecutionTerminalEvent], envelope.sequenceNr))
      .filter { case (event, _) => filter(event) }
      .map { case (event, offset) => JobExecutionReport(event, offset) }

}

class JobExecutionCommandHandler(ctx: ActorContext[JobExecutionCommand], eventHandler: EventHandler)(implicit jm: JobMan)
    extends EventSourcedCommandHandler[JobExecutionCommand, JobExecutorEvent, JobExecutionState](ctx, eventHandler) {

  def apply(state: State, command: Command): Effect[Event, State] =
    command match {

      case cmd @ StartJob(job, queueId, _) =>
        val jobExecutionId = UUID.randomUUID().toString
        persist(jobStarted(cmd, queueId, jobExecutionId, job)).andThen(SideEffect[State](_ => {
          WorkerOffice.worker(queueId) ! ExecuteJob(queueId, jobExecutionId, job, ctx.self)
        }))

      case cmd: JobExecutionResult =>
        persist(jobEndedOrExpired(state, cmd))

      case ExpireOverrunningJobs =>
        persist(jobExpiryCheckRequested(state))

      case QueueTerminationReport(queueId, _) =>
        persist(queueTerminated(queueId, state))

      case ConfirmJobActivity(jobId) =>
        state.updateJobActivityTimestamp(jobId)
        Effect.none

      case cmd @ (Stop | StopDueToEventSubsriptionTermination(_)) =>
        logger.info("{} received. Stopping Job Execution at schedulingJournalOffset: {}", cmd, state.schedulingJournalOffset)
        Effect.stop

    }

  def jobStarted(cmd: StartJob, queueId: Int, jobExecutionId: String, job: Job): List[Event] =
    List(
      JobStarted(job.id, job.jobType, queueId, jobExecutionId, now()),
      SchedulingJournalOffsetChanged(cmd.schedulingJournalOffset + 1)
    )

  def jobEndedOrExpired(state: State, report: JobExecutionResult): JobEndedOrExpired =
    report.jobResult match {
      case r: SuccessfulJobResult =>
        JobCompleted(r.jobId, r, report.dateTime)

      case JobTimeout(jobId, jobType) =>
        jobExpired(jobId, jobType)

      case r: JobFailure =>
        JobFailed(r.jobId, r, report.dateTime)
    }

  def jobExpiryCheckRequested(state: State): List[Event] =
    state.overrunningJobs.map {
      case (jobId, entry) =>
        jobExpired(jobId, entry.jobType)
    }.toList

  def queueTerminated(queueId: Int, state: State): List[Event] =
    state
      .runningJob(queueId)
      .map {
        case (jobId, entry) =>
          val compensation = jm.jobConfigRegistry.jobConfig(entry.jobType).onJobTerminatedAction
          JobTerminated(jobId, entry.jobType, queueId, compensation, now(jm.clock))
      }
      .toList

  def jobExpired(jobId: String, jobType: JobType): JobExpired = {
    val compensation = jm.jobConfigRegistry.jobConfig(jobType).onJobExpiredAction
    JobExpired(jobId, jobType, compensation, now())
  }

}
