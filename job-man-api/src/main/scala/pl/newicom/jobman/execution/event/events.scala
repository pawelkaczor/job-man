package pl.newicom.jobman.execution.event

import java.time.ZonedDateTime

import pl.newicom.jobman.JobType
import pl.newicom.jobman.execution.result.{JobFailure, JobResult, SuccessfulJobResult}
import pl.newicom.jobman.shared.event.SubscriptionOffsetChangedEvent

trait JobExecutorEvent

trait JobExecutionEvent extends JobExecutorEvent {
  def jobId: String
  def jobType: JobType
}

case class JobStarted(jobId: String, jobType: JobType, queueId: Int, jobExecutionId: String, dateTime: ZonedDateTime)
    extends JobExecutionEvent

case class SchedulingJournalOffsetChanged(newOffsetValue: Long) extends JobExecutorEvent with SubscriptionOffsetChangedEvent

sealed trait JobExecutionTerminalEvent extends JobExecutionEvent {
  def dateTime: ZonedDateTime
}

case class JobExpired(jobId: String, jobType: JobType, compensation: Option[String], dateTime: ZonedDateTime)
    extends JobExecutionTerminalEvent

case class JobTerminated(jobId: String, jobType: JobType, queueId: Int, compensation: Option[String], dateTime: ZonedDateTime)
    extends JobExecutionTerminalEvent

sealed trait JobEnded extends JobExecutionTerminalEvent {
  def result: JobResult
  def jobType: JobType = result.jobType
}

case class JobCompleted(jobId: String, result: SuccessfulJobResult, dateTime: ZonedDateTime) extends JobEnded

case class JobFailed(jobId: String, result: JobFailure, dateTime: ZonedDateTime) extends JobEnded
