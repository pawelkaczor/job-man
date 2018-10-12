package pl.newicom.jobman.execution.event

import java.time.ZonedDateTime

import pl.newicom.jobman.JobType
import pl.newicom.jobman.execution.result.{JobFailure, JobResult, SuccessfulJobResult}
import pl.newicom.jobman.shared.event.SubscriptionOffsetChangedEvent

trait JobExecutorEvent

trait JobExecutionEvent extends JobExecutorEvent {
  def jobId: String
}

case class JobStarted(jobId: String, jobType: JobType, queueId: Int, jobExecutionId: String, dateTime: ZonedDateTime)
    extends JobExecutionEvent

trait JobExecutionTerminalEvent extends JobExecutionEvent {
  def dateTime: ZonedDateTime
}

trait JobEndedOrExpired extends JobExecutionTerminalEvent

case class JobExpired(jobId: String, compensation: String, dateTime: ZonedDateTime) extends JobEndedOrExpired

case class JobTerminated(jobId: String, jobType: JobType, queueId: Int, compensation: String, dateTime: ZonedDateTime)
    extends JobExecutionTerminalEvent

trait JobEnded extends JobEndedOrExpired {
  def result: JobResult
}

case class JobCompleted(jobId: String, result: SuccessfulJobResult, dateTime: ZonedDateTime) extends JobEnded

case class JobFailed(jobId: String, result: JobFailure, dateTime: ZonedDateTime) extends JobEnded

case class SchedulingJournalOffsetChanged(newOffsetValue: Long) extends JobExecutorEvent with SubscriptionOffsetChangedEvent
