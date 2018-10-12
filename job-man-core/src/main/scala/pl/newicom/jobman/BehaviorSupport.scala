package pl.newicom.jobman

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.receive

trait BehaviorSupport {

  /*
  def receiveOnly[W, N <: W](narrowBehaviorType: Class[N], narrowBehavior: Behavior[N]): Behavior[W] =
    Behaviors.widened(narrowBehavior, (pf: PFBuilder[W, N]) => pf.`match`(narrowBehaviorType, (m: N) => m))
   */

  /*
  def receiveOnly[W, N <: W](narrowBehaviorType: Class[N], onMessage: N => Behavior[N]): Behavior[W] =
    receiveOnly(narrowBehaviorType, receive((ctx: ActorContext[N], msg: N) => onMessage(msg)))
 */

}
