package pl.newicom.jobman.shared.command

import pl.newicom.jobman.JobType
import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent
import pl.newicom.jobman.notification.command.NotificationCommand
import pl.newicom.jobman.schedule.command.JobScheduleCommand

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}

case class JobExecutionReport(result: JobExecutionTerminalEvent, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset {

  def jobId: String    = result.jobId
  def jobType: JobType = result.jobType
}

case class QueueTerminationReport(queueId: Int) extends JobExecutionCommand with NotificationCommand

case object Stop extends JobScheduleCommand with JobExecutionCommand with NotificationCommand

case class StopDueToEventSubsriptionTermination(ex: Throwable) extends JobScheduleCommand with JobExecutionCommand with NotificationCommand
