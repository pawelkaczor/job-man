package pl.newicom.jobman.shared.command

import pl.newicom.jobman.JobType
import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.notification.command.NotificationCommand
import pl.newicom.jobman.schedule.command.JobScheduleCommand

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}

case class JobExecutionReport(jobId: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset

case class JobExpirationReport(jobId: String, jobType: JobType, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset

case class JobTerminationReport(jobId: String, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset

case class QueueTerminationReport(queueId: Int, terminatedJob: Option[String]) extends JobExecutionCommand with NotificationCommand

case object Stop extends JobScheduleCommand with JobExecutionCommand with NotificationCommand

case class StopDueToEventSubsriptionTermination(ex: Throwable) extends JobScheduleCommand with JobExecutionCommand with NotificationCommand
