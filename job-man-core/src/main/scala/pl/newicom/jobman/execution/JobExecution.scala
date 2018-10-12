package pl.newicom.jobman.execution

import java.time.ZonedDateTime.now
import java.util.UUID

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.SideEffect
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman._
import pl.newicom.jobman.execution.JobExecution._
import pl.newicom.jobman.execution.cluster.WorkerOffice
import pl.newicom.jobman.execution.command.{ConfirmJobActivity, ExpireOverrunningJobs, JobExecutionCommand, StartJob}
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.execution.result.{JobFailure, SuccessfulJobResult}
import pl.newicom.jobman.execution.worker.command.ExecuteJob
import pl.newicom.jobman.schedule.CompensatingAction.RemoveFromSchedule
import pl.newicom.jobman.schedule.JobScheduling.JobSchedulingJournalId
import pl.newicom.jobman.schedule.event.JobDispatchedForExecution
import pl.newicom.jobman.shared.command.{JobExecutionResult, QueueTerminationReport, Stop, StopDueToEventSubsriptionTermination}

import scala.concurrent.duration._

object JobExecution {
  val JobExecutionJournalId = "JobExecution"

  val overrunningJobsCheckoutInterval: FiniteDuration = 5.seconds

  def behavior(config: JobExecutionConfig)(implicit jm: JobMan): Behavior[JobExecutionCommand] =
    Behaviors.withTimers(scheduler => {
      scheduler.startPeriodicTimer("TickKey", ExpireOverrunningJobs, overrunningJobsCheckoutInterval)
      Behaviors.setup(ctx => {
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

      case cmd @ JobExecutionResult(queueId, result, dateTime) =>
        persist(jobEnded(cmd))

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

  def jobEnded(report: JobExecutionResult): JobEnded =
    report.result match {
      case r: SuccessfulJobResult =>
        JobCompleted(r.jobId, r, report.dateTime)
      case r: JobFailure =>
        JobFailed(r.jobId, r, report.dateTime)
      case r =>
        throw new RuntimeException(String.format("Unknown JobResult: %s", r.getClass))
    }

  def jobExpiryCheckRequested(state: JobExecutionState): List[Event] =
    state.overrunningJobs.map { jobId =>
      val compensation = state.jobType(jobId).map(onJobExpiredAction).getOrElse(RemoveFromSchedule)
      JobExpired(jobId, compensation, now())
    }.toList

  def queueTerminated(queueId: Int, state: JobExecutionState)(implicit jm: JobMan): List[Event] =
    state.jobsEnqueued(queueId).map {
      case (jobId, entry) =>
        JobTerminated(jobId, entry.jobType, queueId, onJobTerminatedAction(entry.jobType), now(jm.clock))
    }

  def onJobExpiredAction(jobType: JobType): String =
    jm.jobConfigRegistry.jobConfig(jobType).onJobExpiredAction

  def onJobTerminatedAction(jobType: JobType): String =
    jm.jobConfigRegistry.jobConfig(jobType).onJobTerminatedAction

}
