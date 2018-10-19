package pl.newicom.jobman.notification

import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import pl.newicom.jobman.JobParameters
import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent

import scala.concurrent.Future

sealed trait JobNotificationMessageFactory

trait JavaJobNotificationMessageFactory extends JobNotificationMessageFactory {
  def apply(result: JobExecutionTerminalEvent, jobParamsSupplier: Supplier[CompletionStage[Optional[JobParameters]]]): List[CompletionStage[NotificationMsg]]
}

trait ScalaJobNotificationMessageFactory extends JobNotificationMessageFactory {
  def apply(result: JobExecutionTerminalEvent, jobParamsSupplier: () => Future[Option[JobParameters]]): List[Future[NotificationMsg]]
}