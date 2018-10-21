package pl.newicom.jobman.notification

import java.util.concurrent.CompletionStage

import scala.concurrent.Future

trait JobNotificationHandler

trait JavaJobNotificationHandler extends JobNotificationHandler {
  def apply(msgFuture: CompletionStage[NotificationMsg]): CompletionStage[Void]
}

trait ScalaJobNotificationHandler extends JobNotificationHandler {
  def apply(msgFuture: Future[NotificationMsg]): Future[Unit]
}
