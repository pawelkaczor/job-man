package pl.newicom.jobman.cache
import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, LWWMap, LWWMapKey}

object ReplicatedCache {

  def props: Props = Props(new ReplicatedCache)

  case class PutInCache(cacheId: String, key: String, value: Any)
  case class GetFromCache(cacheId: String, key: String, context: Any)
  case class Cached(key: String, value: Option[Any], senderContext: Any)
  case class Evict(cacheId: String, key: String)
  case class Request(key: String, sender: ActorRef, senderContext: Any)
}

class ReplicatedCache extends Actor {
  import ReplicatedCache._
  import akka.cluster.ddata.Replicator._

  val replicator: ActorRef      = DistributedData(context.system).replicator
  implicit val cluster: Cluster = Cluster(context.system)

  def receive: Receive = {

    case PutInCache(cacheId, key, value) =>
      replicator ! Update(dataKey(cacheId), LWWMap(), WriteLocal)(_ + (key -> value))

    case Evict(cacheId, key) =>
      replicator ! Update(dataKey(cacheId), LWWMap(), WriteLocal)(_ - key)

    case GetFromCache(cacheId, key, ctx) =>
      replicator ! Get(dataKey(cacheId), ReadLocal, Some(Request(key, sender(), ctx)))

    case g @ GetSuccess(LWWMapKey(_), Some(Request(key, replyTo, senderContext))) =>
      g.dataValue match {
        case data: LWWMap[_, _] =>
          data.asInstanceOf[LWWMap[String, Any]].get(key) match {
            case Some(value) => replyTo ! Cached(key, Some(value), senderContext)
            case None        => replyTo ! Cached(key, None, senderContext)
          }
      }

    case NotFound(_, Some(Request(key, replyTo, senderContext))) =>
      replyTo ! Cached(key, None, senderContext)

    case _: UpdateResponse[_] => // ok

  }

  def dataKey(entryKey: String): LWWMapKey[String, Any] = LWWMapKey(entryKey)

}
