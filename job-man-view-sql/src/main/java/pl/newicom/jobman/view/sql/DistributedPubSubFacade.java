package pl.newicom.jobman.view.sql;

import static akka.actor.ActorRef.noSender;
import static akka.actor.typed.javadsl.Adapter.toUntyped;

import akka.actor.ActorRef;
import akka.cluster.pubsub.DistributedPubSubMediator;

public class DistributedPubSubFacade {
	private final ActorRef mediator;

	DistributedPubSubFacade(ActorRef mediator) {
		this.mediator = mediator;
	}

	public void subscribe(String topic, akka.actor.typed.ActorRef<?> ref) {
		subscribe(topic, toUntyped(ref));
	}

	public void subscribe(String topic, ActorRef ref) {
		mediator.tell(new DistributedPubSubMediator.Subscribe(topic, ref), noSender());
	}

	public void publish(String topic, Object msg) {
		mediator.tell(new DistributedPubSubMediator.Publish(topic, msg), noSender());
	}
}
