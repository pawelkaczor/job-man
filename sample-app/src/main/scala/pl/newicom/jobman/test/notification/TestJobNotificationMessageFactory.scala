package pl.newicom.jobman.test.notification

import pl.newicom.jobman.{JobParameters, JobType}
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.notification.{NotificationMsg, ScalaJobNotificationMessageFactory}
import pl.newicom.jobman.test.job.TestJobParameters
import pl.newicom.jobman.test.job.TestJobParameters.TestJobType

import scala.concurrent.{ExecutionContext, Future}

case class TestJobNotification(jobId: String, params: Option[TestJobParameters], report: String, success: Option[Boolean])
    extends NotificationMsg {
  def jobType: JobType = TestJobType

  def subject: String = success match {
    case Some(true)  => "Test job completed successfully"
    case Some(false) => "Test job failed"
    case None        => "Test job expired or terminated"
  }

  def content: String = s"Report: $report. Job params: $params"
}

class TestJobNotificationMessageFactory(implicit ec: ExecutionContext) extends ScalaJobNotificationMessageFactory {

  def apply(result: JobExecutionTerminalEvent, jobParams: () => Future[Option[JobParameters]]): List[Future[NotificationMsg]] = {
    List(jobParams().mapTo[Option[TestJobParameters]].map { params =>
      result match {
        case JobCompleted(_, jobResult, _) =>
          TestJobNotification(result.jobId, params, jobResult.report, success = Some(true))

        case JobFailed(_, jobFailure, _) =>
          TestJobNotification(result.jobId, params, jobFailure.report, success = Some(false))

        case _: JobExpired =>
          TestJobNotification(result.jobId, params, "Job expired", None)

        case _: JobTerminated =>
          TestJobNotification(result.jobId, params, "Job terminated", None)
      }
    })
  }
}
