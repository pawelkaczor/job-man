package pl.newicom.jobman.notification
import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{setup, withTimers}
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import pl.newicom.jobman.execution.JobExecution.JobExecutionJournalId
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

        PersistentBehaviors
          .receive(
            persistenceId = NotificationJournalId,
            emptyState = NotificationState(),
            commandHandler = new NotificationCommandHandler(ctx, eventHandler),
            eventHandler = eventHandler
          )
          .onRecoveryCompleted(recoveryHandler(ctx))
          .snapshotEvery(jm.config.journalSnapshotInterval)
      })
    })

  type EventHandler = (NotificationState, NotificationEvent) => NotificationState

  private val eventHandler: EventHandler = {
    case (s, e) => s.apply(e)
  }

  private def recoveryHandler(ctx: ActorContext[NotificationCommand])(implicit jm: JobMan): NotificationState => Unit = { state =>
    ctx.log.info("Notification Service resumed from executionJournalOffset: {}", state.executionJournalOffset)

    def isNotificationRequired(event: JobExecutionTerminalEvent): Boolean =
      event match {
        case JobFailed(_, _, _) | JobExpired(_, _, _, _) | JobTerminated(_, _, _, _, _) =>
          true
        case e: JobCompleted =>
          jm.jobConfigRegistry.jobConfig(e.jobType).notifyOnSuccess
      }

    def reactToExecutionTerminalEvent(reaction: (JobExecutionTerminalEvent, Long) => NotificationCommand) = {
      val source: Source[NotificationCommand, NotUsed] = jm.readJournal
        .eventsByPersistenceId(JobExecutionJournalId, state.executionJournalOffset, Long.MaxValue)
        .filter(envelope => envelope.event.isInstanceOf[JobExecutionTerminalEvent])
        .filter(envelope => isNotificationRequired(envelope.event.asInstanceOf[JobExecutionTerminalEvent]))
        .map(envelope => reaction(envelope.event.asInstanceOf[JobExecutionTerminalEvent], envelope.sequenceNr))

      val sink: Sink[NotificationCommand, NotUsed] = ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination.apply)

      source.runWith(sink)(jm.actorMaterializer("Notification service failure"))
    }

    reactToExecutionTerminalEvent((event, offset) =>
      event match {
        case JobExpired(jobId, jobType, compensation, _) =>
          JobExpirationReport(jobId, jobType, compensation, offset)

        case e: JobEnded =>
          JobExecutionReport(e.jobId, offset)

        case e: JobTerminated =>
          JobTerminationReport(e.jobId, e.compensation, offset)

    })

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
