package pl.newicom.jobman.notification
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{setup, withTimers}
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman.execution.JobExecution.jobExecutionReportSource
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.notification.Notification.EventHandler
import pl.newicom.jobman.notification.command.{NotificationCommand, SendAwaitingNotifications}
import pl.newicom.jobman.shared.command._
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged
import pl.newicom.jobman.{EventSourcedCommandHandler, JobMan}

import scala.concurrent.duration._

object Notification {
  val NotificationJournalId = "Notification"

  val checkoutInterval: FiniteDuration = 1.minutes
  val cacheAskTimeout: FiniteDuration  = 3.seconds

  def behavior(implicit jm: JobMan): Behavior[NotificationCommand] =
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
            ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination.apply)
          }(jm.actorMaterializer("Notification service failure"))
        }

        PersistentBehaviors
          .receive(
            persistenceId = NotificationJournalId,
            emptyState = NotificationState(),
            commandHandler = new NotificationCommandHandler(ctx, eventHandler),
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

class NotificationCommandHandler(ctx: ActorContext[NotificationCommand], eventHandler: EventHandler)(implicit jm: JobMan)
    extends EventSourcedCommandHandler[NotificationCommand, NotificationEvent, NotificationState](ctx, eventHandler) {

  def apply(state: State, command: Command): Effect[Event, State] = command match {
    case cmd @ JobExecutionReport(result, _) =>
      Effect.none
    //persist(withOffsetChanged(cmd, NotificationRequested(cmd.jobId, result)))
  }

  def withOffsetChanged(cmd: HasExecutionJournalOffset, event: NotificationEvent): List[NotificationEvent] =
    List(ExecutionJournalOffsetChanged(cmd.executionJournalOffset + 1), event)
}
