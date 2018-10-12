package pl.newicom.jobman

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.pubsub.DistributedPubSubMediator

class DistributedPubSubFacade(mediator: ActorRef) {

  def subscribe(topic: String, ref: akka.actor.typed.ActorRef[_]): Unit =
    subscribe(topic, ref.toUntyped)

  def subscribe(topic: String, ref: ActorRef): Unit =
    mediator ! new DistributedPubSubMediator.Subscribe(topic, ref)

  def publish(topic: String, msg: Any): Unit =
    mediator ! DistributedPubSubMediator.Publish(topic, msg)

}
