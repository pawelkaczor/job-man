package pl.newicom.jobman.execution.cluster

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.HashCodeNoEnvelopeMessageExtractor
import akka.cluster.sharding.typed.scaladsl.{EntityRef, EntityTypeKey, ShardedEntity}
import pl.newicom.jobman.JobMan
import pl.newicom.jobman.execution.worker.WorkerBehavior.workerBehavior
import pl.newicom.jobman.execution.worker.command.{ExecuteJob, StopWorker, WorkerCommand}
import pl.newicom.jobman.handler.JobHandlerProvider

object WorkerOffice {

  private val typeKey: EntityTypeKey[WorkerCommand] = EntityTypeKey("Worker")

  def workerOffice(jobHandlerProvider: JobHandlerProvider, maxNumberOfShards: Int)(implicit jm: JobMan): ActorRef[ExecuteJob] =
    jm.clusterSharding.start(
      ShardedEntity(
        create = queueId => workerBehavior(queueId, jobHandlerProvider),
        typeKey = typeKey,
        stopMessage = StopWorker
      ).withMessageExtractor(new HashCodeNoEnvelopeMessageExtractor[WorkerCommand](maxNumberOfShards) {
          def entityId(message: WorkerCommand): String = message.queueId.toString
        })
        .withSettings(jm.clusterShardingSettingsForWorkers)
    )

  def worker(queueId: Int)(implicit jm: JobMan): EntityRef[WorkerCommand] =
    jm.clusterSharding.entityRefFor(typeKey, queueId.toString)

}
