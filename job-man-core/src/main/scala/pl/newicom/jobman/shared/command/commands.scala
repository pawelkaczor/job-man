package pl.newicom.jobman.shared.command

import java.time.ZonedDateTime

import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.execution.worker.command.WorkerCommand
import pl.newicom.jobman.notification.command.NotificationCommand
import pl.newicom.jobman.schedule.command.JobScheduleCommand

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}

case class JobExecutionReport(result: JobResult, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset {
  def jobId: String = result.jobId
}

case class JobExecutionResult(queueId: Int, jobResult: JobResult, dateTime: ZonedDateTime) extends JobExecutionCommand with WorkerCommand

case class JobExpirationReport(jobId: String, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with NotificationCommand
    with HasExecutionJournalOffset

case class JobTerminationReport(jobId: String, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with HasExecutionJournalOffset

case class QueueTerminationReport(queueId: Int, terminatedJob: Option[String]) extends JobExecutionCommand with NotificationCommand

case object Stop extends JobScheduleCommand with JobExecutionCommand with NotificationCommand

case class StopDueToEventSubsriptionTermination(ex: Throwable) extends JobScheduleCommand with JobExecutionCommand with NotificationCommand
