package pl.newicom.jobman.notification

import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged

case class NotificationState(awaitingNotifications: Map[String, JobExecutionTerminalEvent] = Map.empty, executionJournalOffset: Long = 1L) {

  def apply(event: NotificationEvent): NotificationState = event match {

    case NotificationRequested(jobId, result) =>
      copy(awaitingNotifications + (jobId -> result))

    case NotificationAcknowledged(jobId) =>
      copy(awaitingNotifications - jobId)

    case ExecutionJournalOffsetChanged(newOffsetValue) =>
      copy(executionJournalOffset = newOffsetValue)
  }
}
