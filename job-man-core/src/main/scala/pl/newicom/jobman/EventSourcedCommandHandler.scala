package pl.newicom.jobman

import akka.actor.typed.Logger
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.ActorContext
import akka.event.Logging
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import pl.newicom.jobman.shared.event.SubscriptionOffsetChangedEvent

abstract class EventSourcedCommandHandler[C, E, S](ctx: ActorContext[C], eventHandler: (S, E) => S) extends CommandHandler[C, E, S] {
  type Command = C
  type Event   = E
  type State   = S

  type EventHandler   = (State, Event) ⇒ State
  type CommandHandler = (State, Command) ⇒ Effect[Event, State]

  case class Event2Persist[EE <: E](event: EE, callback: EE => Unit = (_: EE) => ()) {
    def thenRun(c: EE => Unit): Effect[E, S] = toEffect(copy(callback = c))
  }

  case class Events2Persist[EE <: E](events: List[EE], callback: EE => Unit = (_: EE) => ()) {
    def thenRun(c: PartialFunction[EE, Unit]): Effect[E, S] =
      toEffect(copy(callback = (e: EE) => c.applyOrElse(e, callback)))
  }

  implicit def toEffect[EE <: E](event: Event2Persist[EE]): Effect[E, S] =
    persistEffect(List(event.event))(event.callback)

  implicit def toEffect[EE <: E](events: Events2Persist[EE]): Effect[E, S] =
    persistEffect(events.events)(events.callback)

  protected def persist[EE <: E](event: EE): Event2Persist[EE] =
    Event2Persist(event)

  protected def persist[EE <: E](events: List[EE]): Events2Persist[EE] =
    Events2Persist(events)

  private def persistEffect[EE <: E](events: List[EE])(callback: EE => Unit): Effect[E, State] =
    Effect
      .persist(events)
      .thenRun(_ =>
        events.foreach(e => {
          callback(e)
          logEvent(e)
        }))

  protected def noStateChanges: EventHandler =
    (s: State, _: Event) => s

  protected def after(state: State, events: List[Event]): State =
    events.foldLeft(state)(eventHandler)

  private def logEvent(event: Event): Unit = {
    val message = domain.toUpperCase.padTo(15, ' ') + event.toString
    if (event.isInstanceOf[SubscriptionOffsetChangedEvent])
      journalLogger.debug(message)
    else
      journalLogger.info(message)
  }

  private def journalLogger = Logging.getLogger(ctx.system.toUntyped, "journal")

  def logger: Logger = ctx.log

  private def domain = {
    val packageName = getClass.getPackage.getName
    packageName.substring(packageName.lastIndexOf(".") + 1)
  }

}
