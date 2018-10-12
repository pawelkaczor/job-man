package pl.newicom.jobman.shared.command

import java.time.ZonedDateTime

import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.schedule.command.JobScheduleCommand

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}

case class JobExecutionReport(result: JobResult, executionJournalOffset: Long) extends JobScheduleCommand with HasExecutionJournalOffset {
  def jobId: String = result.jobId
}

case class JobExecutionResult(queueId: Int, result: JobResult, dateTime: ZonedDateTime) extends JobExecutionCommand

case class JobExpirationReport(jobId: String, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with HasExecutionJournalOffset

case class JobTerminationReport(jobId: String, compensation: String, executionJournalOffset: Long)
    extends JobScheduleCommand
    with HasExecutionJournalOffset

case class QueueTerminationReport(queueId: Int, terminatedJobs: List[String]) extends JobExecutionCommand

case object Stop extends JobScheduleCommand with JobExecutionCommand

case class StopDueToEventSubsriptionTermination(ex: Throwable) extends JobScheduleCommand with JobExecutionCommand
