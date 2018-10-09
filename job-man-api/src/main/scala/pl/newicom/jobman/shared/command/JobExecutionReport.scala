package pl.newicom.jobman.shared.command

import pl.newicom.jobman.JobResult
import pl.newicom.jobman.schedule.command.JobScheduleCommand

case class JobExecutionReport(result: JobResult, executionJournalOffset: Long) extends JobScheduleCommand with HasExecutionJournalOffset {

  def jobId: String = result.jobId

}
