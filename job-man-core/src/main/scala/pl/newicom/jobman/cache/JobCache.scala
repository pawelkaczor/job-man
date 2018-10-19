package pl.newicom.jobman.cache

import akka.actor.{Actor, ActorRef, Props}
import pl.newicom.jobman.JobParameters
import pl.newicom.jobman.cache.JobCache._
import pl.newicom.jobman.cache.ReplicatedCache.{Cached, Evict, GetFromCache, PutInCache}

object JobCache {

  private val JobCacheId = "JobCache"

  def props(replicatedCache: ActorRef): Props =
    Props(new JobCache(replicatedCache))

  sealed trait JobCacheRequest {
    def jobId: String
  }

  case class AddJob(jobId: String, params: JobParameters) extends JobCacheRequest
  case class GetJob(jobId: String)                        extends JobCacheRequest
  case class RemoveJob(jobId: String)                     extends JobCacheRequest

  sealed trait JobCacheResponse {
    def jobId: String
  }

  case class JobFound(jobId: String, params: JobParameters) extends JobCacheResponse
  case class JobNotFound(jobId: String)                     extends JobCacheResponse
}

class JobCache(cache: ActorRef) extends Actor {
  def receive: Receive = {
    case AddJob(jobId, params) =>
      cache ! PutInCache(JobCacheId, jobId, params)

    case RemoveJob(jobId) =>
      cache ! Evict(JobCacheId, jobId)

    case GetJob(jobId) =>
      cache ! GetFromCache(JobCacheId, jobId, sender())

    case Cached(key, value, sender: ActorRef) =>
      sender ! value.map(v => JobFound(key, v.asInstanceOf[JobParameters])).getOrElse(JobNotFound(key))
  }
}
