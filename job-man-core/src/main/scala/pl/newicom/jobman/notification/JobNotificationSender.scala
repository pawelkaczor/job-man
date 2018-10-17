package pl.newicom.jobman.notification

import java.util.concurrent.CompletionStage

trait JobNotificationSender {
  def apply(msgFuture: CompletionStage[NotificationMsg]): CompletionStage[Void]
}
