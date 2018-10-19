package pl.newicom.jobman.notification

import java.util.concurrent.CompletionStage

import scala.concurrent.Future

trait JobNotificationSender

trait JavaJobNotificationSender extends JobNotificationSender {
  def apply(msgFuture: CompletionStage[NotificationMsg]): CompletionStage[Void]
}

trait ScalaJobNotificationSender extends JobNotificationSender {
  def apply(msgFuture: Future[NotificationMsg]): Future[Unit]
}
