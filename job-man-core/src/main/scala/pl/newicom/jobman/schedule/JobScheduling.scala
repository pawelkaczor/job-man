package pl.newicom.jobman.schedule

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman._
import pl.newicom.jobman.execution.JobExecution.JobExecutionJournalId
import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent
import pl.newicom.jobman.schedule.CompensatingAction.{Reschedule, Retry}
import pl.newicom.jobman.schedule.JobScheduling.EventHandler
import pl.newicom.jobman.schedule.command.{CancelJob, JobScheduleCommand, ScheduleJob}
import pl.newicom.jobman.schedule.event._
import pl.newicom.jobman.shared.command._
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged

object JobScheduling {

  val JobSchedulingJournalId = "JobSchedule"

  def behavior(policy: JobSchedulingPolicy, config: JobSchedulingConfig)(implicit jm: JobMan): Behavior[JobScheduleCommand] =
    Behaviors.setup(ctx => {
      PersistentBehaviors
        .receive(
          persistenceId = JobSchedulingJournalId,
          emptyState = JobScheduleState(),
          commandHandler = new JobSchedulingCommandHandler(ctx, policy, config, eventHandler),
          eventHandler
        )
        .onRecoveryCompleted(recoveryHandler(ctx))
        .snapshotEvery(jm.config.journalSnapshotInterval)
    })

  private def recoveryHandler(ctx: ActorContext[JobScheduleCommand])(implicit jm: JobMan): JobScheduleState => Unit = { schedule =>
    ctx.log.info("Job Scheduling resumed from executionJournalOffset: {}", schedule.executionJournalOffset)

    def reactToJobTerminalEvent(reaction: (JobExecutionTerminalEvent, Long) => JobScheduleCommand) = {
      val source: Source[JobScheduleCommand, NotUsed] = jm.readJournal
        .eventsByPersistenceId(JobExecutionJournalId, schedule.executionJournalOffset, Long.MaxValue)
        .filter(envelope => envelope.event.isInstanceOf[JobExecutionTerminalEvent])
        .map(envelope => reaction(envelope.event.asInstanceOf[JobExecutionTerminalEvent], envelope.sequenceNr))

      val sink: Sink[JobScheduleCommand, NotUsed] = ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination.apply)

      source.runWith(sink)(jm.actorMaterializer("Job Scheduling service failure"))
    }

    reactToJobTerminalEvent((event, offset) =>
      event match {

        case execution.event.JobExpired(jobId, jobType, compensation, _) =>
          JobExpirationReport(jobId, jobType, compensation, offset)

        case e: execution.event.JobEnded =>
          JobExecutionReport(e.jobId, offset)

        case e: execution.event.JobTerminated =>
          JobTerminationReport(e.jobId, e.compensation, offset)

    })
  }

  type EventHandler = (JobScheduleState, JobScheduleEvent) => JobScheduleState

  private val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

}

class JobSchedulingCommandHandler(ctx: ActorContext[JobScheduleCommand],
                                  schedulingPolicy: JobSchedulingPolicy,
                                  config: JobSchedulingConfig,
                                  eventHandler: EventHandler)
    extends EventSourcedCommandHandler[JobScheduleCommand, JobScheduleEvent, JobScheduleState](ctx, eventHandler) {

  def apply(schedule: State, command: Command): Effect[Event, State] =
    command match {

      case cmd @ ScheduleJob(job, _) =>
        persist(jobScheduled(schedule, job)).thenRun {
          case result: JobSchedulingResult => cmd.replyTo ! result
        }

      case cmd @ CancelJob(jobId, _) =>
        if (!schedule.contains(jobId)) {
          cmd.replyTo ! JobNotFound(jobId)
          Effect.none
        } else if (schedule
                     .jobsDispatchedForExecution(_.jobId == jobId)
                     .nonEmpty) {
          cmd.replyTo ! JobAlreadyDispatchedForExecution(jobId)
          Effect.none
        } else {
          schedule
            .entry(jobId)
            .map(e => (e.jobId, Some(e.queueId)))
            .orElse(schedule.awaitingJob(jobId).map(job => (job.id, None)))
            .map { t =>
              persist(JobCanceled(t._1, t._2, schedule.position(jobId)))
                .thenRun { event =>
                  cmd.replyTo ! event
                }
            }
            .getOrElse(Effect.none)
        }

      case cmd: JobExecutionReport =>
        persist(withOffsetChanged(cmd, jobEntryRemoved(schedule, cmd.jobId)))

      case cmd @ JobExpirationReport(jobId, _, compensation, _) =>
        persist(withOffsetChanged(cmd, jobExpiredOrTerminated(schedule, jobId, compensation)))

      case cmd @ JobTerminationReport(jobId, compensation, _) =>
        persist(withOffsetChanged(cmd, jobExpiredOrTerminated(schedule, jobId, compensation)))

      case cmd @ (Stop | StopDueToEventSubsriptionTermination(_)) =>
        logger.info("{} received. Stopping Job Scheduling. Number of jobs in the schedule {}", cmd, schedule.jobsNumber)
        Effect.stop
    }

  def jobScheduled(schedule: State, job: Job): List[Event] =
    schedulingPolicy.scheduleJob(job, schedule) match {
      case event: JobScheduleEntryAdded if event.position == 0 =>
        List(event, JobDispatchedForExecution(job, event.queueId))
      case event =>
        List(event)
    }

  def jobEntryRemoved(schedule: State, jobId: String): List[Event] = {
    val events = List(JobEnded(jobId)) ++ jobDispatchedForExecution(schedule.successorJob(jobId))
    val (currEvts, newEvts, _) =
      schedule.waitingList.foldLeft(List.empty[Event], events, schedule)((t, awaitingJob) => {
        val (currentEvents, newEvents, currentState) = t

        val newState = after(currentState, newEvents)
        (currentEvents ++ newEvents, jobScheduled(newState, awaitingJob), newState)
      })
    currEvts ++ newEvts
  }

  def jobExpiredOrTerminated(schedule: State, jobId: String, compensation: String): List[Event] =
    schedule
      .entry(jobId)
      .map(entry => {
        compensation match {
          case Reschedule =>
            val events = jobEntryRemoved(schedule, jobId)
            events ++ jobScheduled(after(schedule, events), entry.job)
          case Retry =>
            jobDispatchedForExecution(Some(entry))
          case _ =>
            jobEntryRemoved(schedule, jobId)
        }
      })
      .toList
      .flatten

  def jobDispatchedForExecution(entry: Option[JobSchedule.Entry]): List[Event] =
    entry.map(e => JobDispatchedForExecution(e.job, e.queueId)).toList

  def withOffsetChanged(cmd: HasExecutionJournalOffset, events: List[Event]): List[Event] =
    events.+:(ExecutionJournalOffsetChanged(cmd.executionJournalOffset + 1))

  private implicit def state2Schedule(state: JobScheduleState): JobSchedule =
    JobSchedule(state, config, null)

}
