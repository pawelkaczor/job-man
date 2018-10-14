package pl.newicom.jobman.notification.command

trait NotificationCommand

case object SendAwaitingNotifications                 extends NotificationCommand
case class AcknowledgeNotificationSent(jobId: String) extends NotificationCommand
