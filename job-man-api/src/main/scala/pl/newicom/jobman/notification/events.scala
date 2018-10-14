package pl.newicom.jobman.notification

import pl.newicom.jobman.execution.result.JobResult

trait NotificationEvent

object NotificationRequested {
  def apply(jobId: String, resultOpt: Option[JobResult], compensationOpt: Option[String]): NotificationRequested =
    new NotificationRequested(jobId, resultOpt, compensationOpt)

  def apply(jobId: String, result: JobResult): NotificationRequested =
    new NotificationRequested(jobId, Some(result), None)

  def apply(jobId: String, compensationOpt: String): NotificationRequested =
    new NotificationRequested(jobId, None, Some(compensationOpt))
}

case class NotificationRequested(jobId: String, resultOpt: Option[JobResult], compensationOpt: Option[String]) extends NotificationEvent {
  def result: Either[String, JobResult] =
    resultOpt.toRight(compensationOpt.get)
}

case class NotificationAcknowledged(jobId: String) extends NotificationEvent
