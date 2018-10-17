package pl.newicom.jobman.notification
import java.util.function.Supplier

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{setup, withTimers}
import akka.persistence.typed.SideEffect
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman.execution.JobExecution.jobExecutionReportSource
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.notification.Notification.EventHandler
import pl.newicom.jobman.notification.command.{AcknowledgeNotificationSent, NotificationCommand, SendAwaitingNotifications}
import pl.newicom.jobman.shared.command._
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged
import pl.newicom.jobman.{EventSourcedCommandHandler, JobMan, JobParameters}

import scala.concurrent.duration._

object Notification {
  val NotificationJournalId = "Notification"

  val checkoutInterval: FiniteDuration = 1.minutes
  val cacheAskTimeout: FiniteDuration  = 3.seconds

  def behavior(jobNotificationSender: JobNotificationSender, jobNotificationMessageFactory: JobNotificationMessageFactory)(
      implicit jm: JobMan): Behavior[NotificationCommand] =
    withTimers(scheduler => {
      scheduler.startPeriodicTimer("TickKey", SendAwaitingNotifications, checkoutInterval)
      setup(ctx => {

        def isNotificationRequired(event: JobExecutionTerminalEvent): Boolean =
          event match {
            case JobFailed(_, _, _) | JobExpired(_, _, _, _) | JobTerminated(_, _, _, _, _) =>
              true
            case e: JobCompleted =>
              jm.jobConfigRegistry.jobConfig(e.jobType).notifyOnSuccess
          }

        def recoveryHandler(state: NotificationState): Unit = {
          ctx.log.info("Notification Service resumed from executionJournalOffset: {}", state.executionJournalOffset)
          jobExecutionReportSource(state.executionJournalOffset, filter = isNotificationRequired).runWith {
            ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination)
          }(jm.actorMaterializer("Notification service failure"))
        }

        PersistentBehaviors
          .receive(
            persistenceId = NotificationJournalId,
            emptyState = NotificationState(),
            commandHandler = new NotificationCommandHandler(ctx, eventHandler, jobNotificationSender, jobNotificationMessageFactory),
            eventHandler = eventHandler
          )
          .onRecoveryCompleted(recoveryHandler)
          .snapshotEvery(jm.config.journalSnapshotInterval)
      })
    })

  type EventHandler = (NotificationState, NotificationEvent) => NotificationState

  private val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

}

class NotificationCommandHandler(ctx: ActorContext[NotificationCommand],
                                 eventHandler: EventHandler,
                                 notificationSender: JobNotificationSender,
                                 notificationFactory: JobNotificationMessageFactory)(implicit jm: JobMan)
    extends EventSourcedCommandHandler[NotificationCommand, NotificationEvent, NotificationState](ctx, eventHandler) {

  def apply(state: State, command: Command): Effect[Event, State] = command match {
    case cmd @ JobExecutionReport(result, _) =>
      persist(withOffsetChanged(cmd, NotificationRequested(cmd.jobId, result)))
        .andThen(SideEffect[State](_ => sendNotification(cmd.jobId, result)))

    case SendAwaitingNotifications =>
      Effect.none.andThen(SideEffect[State](_.awaitingNotifications.foreach {
        case (jobId, result) =>
          sendNotification(jobId, result)
      }))

    case AcknowledgeNotificationSent(jobId) =>
      persist(NotificationAcknowledged(jobId))

    case cmd @ (Stop | StopDueToEventSubsriptionTermination(_)) =>
      logger.info("{} received. Stopping Notification Service at executionJournalOffset: {}", cmd, state.executionJournalOffset)
      Effect.stop

  }

  def withOffsetChanged(cmd: HasExecutionJournalOffset, event: NotificationEvent): List[NotificationEvent] =
    List(ExecutionJournalOffsetChanged(cmd.executionJournalOffset + 1), event)

  private def sendNotification(jobId: String, result: JobExecutionTerminalEvent) =
    notificationFactory(result, () => ???).foreach(notificationSender(_).whenComplete((_, ex) => {
      if (ex == null)
        ctx.self ! AcknowledgeNotificationSent(jobId)
      else
        logger.error(ex, "Sending of notification failed!")
    }))

}
