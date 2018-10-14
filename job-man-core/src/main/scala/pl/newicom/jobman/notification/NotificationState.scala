package pl.newicom.jobman.notification

import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged

case class NotificationState(awaitingNotifications: Map[String, Either[String, JobResult]] = Map.empty, executionJournalOffset: Long = 1L) {

  def apply(event: NotificationEvent): NotificationState = event match {

    case event @ NotificationRequested(jobId, _, _) =>
      copy(awaitingNotifications + (jobId -> event.result))

    case NotificationAcknowledged(jobId) =>
      copy(awaitingNotifications - jobId)

    case ExecutionJournalOffsetChanged(newOffsetValue) =>
      copy(executionJournalOffset = newOffsetValue)
  }
}
