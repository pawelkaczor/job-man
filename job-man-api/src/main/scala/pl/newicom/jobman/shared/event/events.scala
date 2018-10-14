package pl.newicom.jobman.shared.event

import pl.newicom.jobman.notification.NotificationEvent
import pl.newicom.jobman.schedule.event.JobScheduleEvent

trait SubscriptionOffsetChangedEvent

case class ExecutionJournalOffsetChanged(newOffsetValue: Long)
    extends JobScheduleEvent
    with NotificationEvent
    with SubscriptionOffsetChangedEvent
