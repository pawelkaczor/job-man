package pl.newicom.jobman

import java.time.Clock

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.typed.scaladsl
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.Config
import com.typesafe.config.ConfigUtil.joinPath
import org.slf4j.{Logger, LoggerFactory}
import pl.newicom.jobman.JobManInfraServices._
import pl.newicom.jobman.cache.{JobCache, ReplicatedCache}
import pl.newicom.jobman.execution.JobExecutionConfig
import pl.newicom.jobman.schedule.JobSchedulingConfig
import scala.collection.JavaConverters._

object JobManInfraServices {
  lazy val log: Logger = LoggerFactory.getLogger(classOf[JobManInfraServices])

  val rootPath = "jm"

  def path(path: String): String = joinPath(rootPath, path)

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def getOptionalBoolean(path: String): Option[Boolean] =
      if (underlying.hasPath(path)) {
        Some(underlying.getBoolean(path))
      } else {
        None
      }

    def getOptionalString(path: String): Option[String] =
      if (underlying.hasPath(path)) {
        Some(underlying.getString(path))
      } else {
        None
      }

    def getOptionalInt(path: String): Option[Int] =
      if (underlying.hasPath(path)) {
        Some(underlying.getInt(path))
      } else {
        None
      }

  }
}

class JobManInfraServices(cluster: Cluster, _config: Config, val readJournal: EventsByPersistenceIdQuery)(implicit as: ActorSystem)
    extends JobMan {

  lazy val jobConfigRegistry: JobConfigRegistry = {
    def jobConfig(c: Config): JobConfig =
      JobConfig(
        Class.forName(c.getString("paramsClass")).asInstanceOf[Class[JobParameters]],
        c.getOptionalInt("parallelism"),
        c.getDuration("maxDuration"),
        c.getDuration("maxTaskDuration"),
        c.getOptionalString("onJobExpiredAction"),
        c.getOptionalString("onJobTerminatedAction"),
        c.getOptionalBoolean("notifyOnSuccess").getOrElse(false)
      )

    val jobTypesConfig = _config.getConfig(path("job-types"))
    JobConfigRegistry(jobTypesConfig.root().entrySet().asScala.map(e => e.getKey -> jobConfig(jobTypesConfig.getConfig(e.getKey))).toMap)
  }

  lazy val schedulingConfig: JobSchedulingConfig = {
    val c = _config.getConfig(path("scheduling"))
    JobSchedulingConfig(c.getInt("minQueues"), c.getInt("maxQueues"), c.getInt("queueCapacity"), jobConfigRegistry.jobType2ParamMap)
  }

  lazy val executionConfig: JobExecutionConfig = {
    JobExecutionConfig(jobConfigRegistry)
  }

  def actorMaterializer(errorMsg: String): ActorMaterializer = {
    scaladsl.ActorMaterializer(
      Some(ActorMaterializerSettings(as)
        .withSupervisionStrategy(ex => {
          log.error(errorMsg, ex)
          Supervision.stop
        })))(as.toTyped)
  }

  lazy val config: JobManConfig = {
    val c = _config.getConfig(rootPath)
    JobManConfig(
      jobConfigRegistry,
      schedulingConfig,
      executionConfig,
      c.getInt("maxShards"),
      c.getInt("journalSnapshotInterval"),
      c.getBoolean("healthCheckNotificationsEnabled")
    )
  }

  val clock: Clock =
    Clock.systemDefaultZone

  lazy val clusterSharding: ClusterSharding =
    ClusterSharding(as.toTyped)

  lazy val clusterShardingSettingsForWorkers: ClusterShardingSettings =
    ClusterShardingSettings(as.toTyped).withRole(JobMan.Role.worker)

  lazy val distributedPubSub: DistributedPubSubFacade =
    new DistributedPubSubFacade(DistributedPubSub(as).mediator)

  lazy val jobCache: ActorRef = {
    val replicatedCache = as.actorOf(ReplicatedCache.props, "ReplicatedCache")
    as.actorOf(JobCache.props(replicatedCache), "JobCache")
  }

}
