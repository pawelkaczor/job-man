package pl.newicom.jobman

import java.time.Clock

import akka.actor.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.typed.scaladsl
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import org.slf4j.{Logger, LoggerFactory}
import pl.newicom.jobman.JobManInfraServices.log

object JobManInfraServices {
  lazy val log: Logger = LoggerFactory.getLogger(classOf[JobManInfraServices])
}

class JobManInfraServices(implicit actorSystem: ActorSystem[Void]) extends JobMan {

  def actorMaterializer(errorMsg: String): ActorMaterializer = {
    scaladsl.ActorMaterializer(
      Some(
        ActorMaterializerSettings(actorSystem.toUntyped)
          .withSupervisionStrategy(ex => {
            log.error(errorMsg, ex)
            Supervision.stop
          })))
  }

  def readJournal: EventsByPersistenceIdQuery = ???

  def config: JobManConfig = ???

  def clock: Clock = ???

  def clusterSharding: ClusterSharding = ???

  def clusterShardingSettingsForWorkers: ClusterShardingSettings = ???

  def jobConfigRegistry: JobConfigRegistry = ???

  def distributedPubSub: DistributedPubSubFacade = ???

  def jobCache: ActorRef = ???
}
