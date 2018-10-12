package pl.newicom.jobman

import java.time.Clock

import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.ActorMaterializer

trait JobMan {

  def clock: Clock

  def config: JobManConfig

  def jobConfigRegistry: JobConfigRegistry

  def clusterSharding: ClusterSharding

  def clusterShardingSettingsForWorkers: ClusterShardingSettings

  def distributedPubSub: DistributedPubSubFacade

  def readJournal: EventsByPersistenceIdQuery

  def actorMaterializer(errorMsg: String): ActorMaterializer
}
