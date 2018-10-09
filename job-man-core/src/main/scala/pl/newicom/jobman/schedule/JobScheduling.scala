package pl.newicom.jobman.schedule

import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.Effect
import pl.newicom.jobman.schedule.JobScheduling.EventHandler
import pl.newicom.jobman.schedule.command.{CancelJob, JobScheduleCommand, ScheduleJob}
import pl.newicom.jobman.schedule.event._
import pl.newicom.jobman.shared.command.JobExecutionReport
import pl.newicom.jobman.{CommandHandler, Job}

object JobScheduling {

  type EventHandler = (JobScheduleState, JobScheduleEvent) => JobScheduleState

  val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

}

class JobSchedulingCommandHandler(ctx: ActorContext[JobScheduleCommand],
                                   schedulingPolicy: JobSchedulingPolicy,
                                   config: JobSchedulingConfig,
                                   eventHandler: EventHandler)
  extends CommandHandler[JobScheduleCommand, JobScheduleEvent, JobScheduleState](ctx, eventHandler) {

  type ScheduleEntry = JobSchedule.Entry

  implicit def state2Schedule(state: JobScheduleState): JobSchedule =
    JobSchedule(state, config, null)

  def apply(schedule: State, command: Command): Effect[Event, State] = command match {

    case cmd @ ScheduleJob(job, _) =>
      persist(jobScheduled(schedule, job)).thenRun {
        case result: JobSchedulingResult => cmd.replyTo ! result
      }

    case cmd @ CancelJob(jobId, _) =>
      if (!schedule.contains(jobId)) {
        cmd.replyTo ! JobNotFound(jobId)
        Effect.none
      } else if (schedule.jobsDispatchedForExecution(_.jobId == jobId).nonEmpty) {
        cmd.replyTo ! JobAlreadyDispatchedForExecution(jobId)
        Effect.none
      } else {
        schedule
          .entry(jobId).map(e => (e.jobId, Some(e.queueId)))
          .orElse(schedule.awaitingJob(jobId).map(job => (job.id, None)))
          .map { t =>
            persist(JobCanceled(t._1, t._2, schedule.position(jobId))).thenRun { event =>
              cmd.replyTo ! event
            }
          }
          .getOrElse(Effect.none)
      }

    case JobExecutionReport(result, executionJournalOffset) =>
      Effect.none
  }

  def jobScheduled(schedule: State, job: Job): List[Event] =
    schedulingPolicy.scheduleJob(job, state2Schedule(schedule)) match {
      case event: JobScheduleEntryAdded if event.position == 0 =>
         List(event, JobDispatchedForExecution(job, event.queueId))
      case event =>
        List(event)
    }

  def jobEntryRemoved(schedule: State, jobId: String): List[Event] = {
    val events = List(JobEnded(jobId)) ++ jobDispatchedForExecution(schedule.successorJob(jobId))
    val (currEvts, newEvts, _) = schedule.waitingList.foldLeft(List.empty[Event], events, schedule)((t, awaitingJob) => {
      val (currentEvents, newEvents, currentState) = t

      val newState = after(currentState, newEvents)
      (currentEvents ++ newEvents, jobScheduled(newState, awaitingJob), newState)
    })
    currEvts ++ newEvts
  }

  def jobExpired(schedule: State, jobId: String, fUp: String): List[Event] =
    followUp(schedule, jobId, fUp)

  def jobTerminated(schedule: State, jobId: String, fUp: String): List[Event] =
    followUp(schedule, jobId, fUp)

  def followUp(schedule: State, jobId: String, followUp: String): List[Event] =
    schedule.entry(jobId).map(entry => {
      followUp match {
        case "Reschedule" =>
          val events = jobEntryRemoved(schedule, jobId)
          events ++ jobScheduled(after(schedule, events), entry.job)
        case "Retry" =>
          jobDispatchedForExecution(Some(entry))
        case _ =>
          jobEntryRemoved(schedule, jobId)
      }
    }).toList.flatten

  def jobDispatchedForExecution(entry: Option[ScheduleEntry]): List[Event] =
    entry.map(e => JobDispatchedForExecution(e.job, e.queueId)).toList


}