package pl.newicom.jobman.notification

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{setup, withTimers}
import akka.pattern.ask
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.stream.typed.scaladsl.ActorSink
import akka.util.Timeout
import pl.newicom.jobman.cache.JobCache.{GetJob, JobFound, RemoveJob}
import pl.newicom.jobman.execution.JobExecution.jobExecutionReportSource
import pl.newicom.jobman.execution.event._
import pl.newicom.jobman.notification.Notification.{EventHandler, cacheAskTimeout}
import pl.newicom.jobman.notification.command.{AcknowledgeNotificationSent, NotificationCommand, SendAwaitingNotifications}
import pl.newicom.jobman.shared.command._
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged
import pl.newicom.jobman.{EventSourcedCommandHandler, JobMan, JobParameters}

import scala.Option.empty
import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Notification {
  val NotificationJournalId = "Notification"

  val checkoutInterval: FiniteDuration = 1.minutes
  val cacheAskTimeout: FiniteDuration  = 3.seconds

  def behavior(jobNotificationMessageFactory: JobNotificationMessageFactory, jobNotificationHandler: JobNotificationHandler)(
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
          jobExecutionReportSource(state.executionJournalOffset)
            .filter {
              case JobExecutionReport(event, _) =>
                if (isNotificationRequired(event)) {
                  true
                } else {
                  jm.jobCache ! RemoveJob(event.jobId)
                  false
                }
            }
            .runWith {
              ActorSink.actorRef(ctx.self, Stop, StopDueToEventSubsriptionTermination)
            }(jm.actorMaterializer("Notification service failure"))
        }

        PersistentBehaviors
          .receive(
            persistenceId = NotificationJournalId,
            emptyState = NotificationState(),
            commandHandler = new NotificationCommandHandler(ctx, eventHandler, jobNotificationHandler, jobNotificationMessageFactory),
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
                                 notificationHandler: JobNotificationHandler,
                                 notificationFactory: JobNotificationMessageFactory)(implicit jm: JobMan)
    extends EventSourcedCommandHandler[NotificationCommand, NotificationEvent, NotificationState](ctx, eventHandler) {

  def apply(state: State, command: Command): Effect[Event, State] = command match {
    case cmd @ JobExecutionReport(result, _) =>
      persist(withOffsetChanged(cmd, NotificationRequested(cmd.jobId, result)))
        .thenRun(_ => handleNotification(cmd.jobId, result))

    case SendAwaitingNotifications =>
      Effect.none.thenRun(_.awaitingNotifications.foreach {
        case (jobId, result) =>
          handleNotification(jobId, result)
      })

    case AcknowledgeNotificationSent(jobId) =>
      persist(NotificationAcknowledged(jobId)).thenRun { _ =>
        jm.jobCache ! RemoveJob(jobId)
      }

    case cmd @ (Stop | StopDueToEventSubsriptionTermination(_)) =>
      logger.info("{} received. Stopping Notification Service at executionJournalOffset: {}", cmd, state.executionJournalOffset)
      Effect.stop

  }

  def withOffsetChanged(cmd: HasExecutionJournalOffset, event: NotificationEvent): List[NotificationEvent] =
    List(ExecutionJournalOffsetChanged(cmd.executionJournalOffset + 1), event)

  def handleNotification(jobId: String, result: JobExecutionTerminalEvent): Unit = {
    import ctx.executionContext
    notificationMessages(jobId, result).foreach { msg =>
      handleNotification(msg).onComplete {
        case Success(_) =>
          ctx.self ! AcknowledgeNotificationSent(jobId)
        case Failure(ex) =>
          logger.error(ex, "Sending of notification failed!")
      }
    }
  }

  def handleNotification(msgFuture: Future[NotificationMsg]): Future[Any] =
    notificationHandler match {
      case ns: JavaJobNotificationHandler  => ns(msgFuture.toJava).toScala
      case ns: ScalaJobNotificationHandler => ns(msgFuture)
    }

  def notificationMessages(jobId: String, result: JobExecutionTerminalEvent): List[Future[NotificationMsg]] = {
    import ctx.executionContext
    notificationFactory match {
      case nf: ScalaJobNotificationMessageFactory =>
        nf(result, () => jobParams(jobId))
      case nf: JavaJobNotificationMessageFactory =>
        nf(result, () => jobParams(jobId).map(_.asJava).toJava).map(_.toScala)
    }
  }

  def jobParams(jobId: String): Future[Option[JobParameters]] = {
    import ctx.executionContext
    implicit val timeout: Timeout = cacheAskTimeout
    (jm.jobCache ? GetJob(jobId)).map {
      case JobFound(_, params) =>
        Some(params)
      case _ =>
        empty
    }
  }
}
