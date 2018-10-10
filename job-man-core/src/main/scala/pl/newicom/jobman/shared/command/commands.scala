package pl.newicom.jobman.shared.command

import pl.newicom.jobman.JobResult
import pl.newicom.jobman.schedule.command.JobScheduleCommand

trait HasExecutionJournalOffset {
  def executionJournalOffset: Long
  def jobId: String
}

case class JobExecutionReport(result: JobResult, executionJournalOffset: Long) extends JobScheduleCommand with HasExecutionJournalOffset {
  def jobId: String = result.jobId
}

case class JobExpirationReport(jobId: String, followUp: String, executionJournalOffset: Long) extends JobScheduleCommand with HasExecutionJournalOffset

case class JobTerminationReport(jobId: String, followUp: String, executionJournalOffset: Long) extends JobScheduleCommand with HasExecutionJournalOffset


case object Stop extends JobScheduleCommand

case class StopDueToEventSubsriptionTermination(ex: Throwable) extends JobScheduleCommand