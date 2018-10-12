package pl.newicom.jobman.execution.cluster

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.HashCodeNoEnvelopeMessageExtractor
import akka.cluster.sharding.typed.scaladsl.{EntityRef, EntityTypeKey, ShardedEntity}
import pl.newicom.jobman.JobMan
import pl.newicom.jobman.execution.worker.command.{ExecuteJob, StopWorker, WorkerCommand}

object WorkerOffice {

  private val typeKey: EntityTypeKey[WorkerCommand] = EntityTypeKey("Worker")

  def workerBehavior(queueId: String): Behavior[WorkerCommand] = ???

  def workerOffice(jobMan: JobMan, maxNumberOfShards: Int): ActorRef[ExecuteJob] =
    jobMan.clusterSharding.start(
      ShardedEntity(
        create = queueId => workerBehavior(queueId),
        typeKey = typeKey,
        stopMessage = StopWorker
      ).withMessageExtractor(new HashCodeNoEnvelopeMessageExtractor[WorkerCommand](maxNumberOfShards) {
        def entityId(message: WorkerCommand): String = message.queueId.toString
      }))

  def worker(queueId: Int)(implicit jm: JobMan): EntityRef[WorkerCommand] =
    jm.clusterSharding.entityRefFor(typeKey, queueId.toString)

}
