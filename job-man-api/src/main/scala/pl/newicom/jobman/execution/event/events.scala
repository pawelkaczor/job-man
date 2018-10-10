package pl.newicom.jobman.execution.event

import java.time.ZonedDateTime

import pl.newicom.jobman.{JobResult, JobType}

trait JobExecutorEvent

trait JobExecutionEvent extends JobExecutorEvent {
  def jobId: String
}

trait JobExecutionTerminalEvent extends JobExecutionEvent {
  def dateTime: ZonedDateTime
}

trait JobEndedOrExpired extends JobExecutionTerminalEvent

case class JobExpired(jobId: String, followUp: String, dateTime: ZonedDateTime) extends JobEndedOrExpired

case class JobTerminated(jobId: String, jobType: JobType, queueId: Int, followUp: String, dateTime: ZonedDateTime)
    extends JobExecutionTerminalEvent

trait JobEnded extends JobEndedOrExpired {
  def result: JobResult
}
