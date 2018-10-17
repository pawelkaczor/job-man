package pl.newicom.jobman.notification

import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent

trait NotificationEvent

case class NotificationRequested(jobId: String, result: JobExecutionTerminalEvent) extends NotificationEvent
case class NotificationAcknowledged(jobId: String)                                 extends NotificationEvent
