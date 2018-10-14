package pl.newicom.jobman.notification
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, withTimers}
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import pl.newicom.jobman.{EventSourcedCommandHandler, JobMan}
import pl.newicom.jobman.healthcheck.HealthCheckTopic
import pl.newicom.jobman.healthcheck.event.HealthCheckEvent
import pl.newicom.jobman.notification.Notification.EventHandler
import pl.newicom.jobman.notification.command.{NotificationCommand, SendAwaitingNotifications}
import pl.newicom.jobman.shared.command.{HasExecutionJournalOffset, JobExecutionReport}
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged

import scala.concurrent.duration._

object Notification {
  val NotificationJournalId = "Notification"

  val checkoutInterval: FiniteDuration = 1.minutes
  val cacheAskTimeout: FiniteDuration  = 3.seconds

  def behavior(healthCheckNotificationSender: HealthCheckNotificationSender)(implicit jm: JobMan): Behavior[NotificationCommand] =
    withTimers(scheduler => {
      scheduler.startPeriodicTimer("TickKey", SendAwaitingNotifications, checkoutInterval)
      setup(ctx => {

        def healthCheckNotification: Behavior[HealthCheckEvent] =
          receiveMessage { event =>
            healthCheckNotificationSender.sendNotification(event)
            same
          }

        if (jm.config.healthCheckNotificationsEnabled) {
          jm.distributedPubSub.subscribe(HealthCheckTopic, ctx.spawn(healthCheckNotification, "HealthCheckNotifier"))
        }

        PersistentBehaviors.receive(
          persistenceId = NotificationJournalId,
          emptyState = NotificationState(),
          commandHandler = new NotificationCommandHandler(ctx, eventHandler),
          eventHandler = eventHandler
        )
      })
    })

  type EventHandler = (NotificationState, NotificationEvent) => NotificationState

  private val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

}

class NotificationCommandHandler(ctx: ActorContext[NotificationCommand], eventHandler: EventHandler)(implicit jm: JobMan)
    extends EventSourcedCommandHandler[NotificationCommand, NotificationEvent, NotificationState](ctx, eventHandler) {

  def apply(state: State, command: Command): Effect[Event, State] = command match {
    case cmd @ JobExecutionReport(result, _) =>
      persist(withOffsetChanged(cmd, NotificationRequested(cmd.jobId, result)))
  }

  def withOffsetChanged(cmd: HasExecutionJournalOffset, event: NotificationEvent): List[NotificationEvent] =
    List(ExecutionJournalOffsetChanged(cmd.executionJournalOffset + 1), event)
}
