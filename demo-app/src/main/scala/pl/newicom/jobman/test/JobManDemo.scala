package pl.newicom.jobman.test

import java.io.{File, FileInputStream}
import java.lang.String.format

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.cluster.Cluster
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.javadsl.{LeveldbReadJournal => LeveldbReadJournalJava}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseFileAnySyntax
import org.apache.log4j.PropertyConfigurator
import org.slf4j.LoggerFactory
import pl.newicom.jobman.execution.cluster.WorkerOffice.workerOffice
import pl.newicom.jobman.execution.{JobExecution, JobExecutionJournalId}
import pl.newicom.jobman.notification.{Notification, NotificationJournalId}
import pl.newicom.jobman.progress.JobExecutionStatisticsPublisher
import pl.newicom.jobman.progress.event.{JobExecutionStatisticsEvent, JobRemainingTimeUpdated}
import pl.newicom.jobman.schedule.{DefaultJobSchedulingPolicy, JobScheduling, JobSchedulingJournalId}
import pl.newicom.jobman.test.ClusterSingletonFactory.clusterSingleton
import pl.newicom.jobman.test.notification.{TestJobNotificationHandler, TestJobNotificationMessageFactory}
import pl.newicom.jobman.view.sql.ViewUpdateService.viewUpdateService
import pl.newicom.jobman.{JobMan, JobManInfraServices}

import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object JobManDemo {

  def main(args: Array[String]): Unit = {
    setupEnvironment()
    configureLog4j()

    val config  = ConfigFactory.load
    val system  = ActorSystem("jm", config)
    val cluster = Cluster(system)
    val port    = config.getString("akka.remote.netty.tcp.port")

    startupSharedJournal(system, startStore = port == "2551", path = ActorPath.fromString("akka.tcp://jm@127.0.0.1:2551/user/store"))

    implicit val jm: JobMan = new JobManInfraServices(cluster, config, readJournal(system))(system)

    cluster.registerOnMemberUp {
      implicit val ec: ExecutionContext = system.dispatcher
      val as                            = system.toTyped
      clusterSingleton(as, JobSchedulingJournalId, JobScheduling.behavior(new DefaultJobSchedulingPolicy))
      clusterSingleton(as,
                       NotificationJournalId,
                       Notification.behavior(new TestJobNotificationMessageFactory(), new TestJobNotificationHandler()))
      clusterSingleton(as, JobExecutionJournalId, JobExecution.behavior)

      JobExecutionStatisticsPublisher.start(
        jm,
        Set(classOf[JobRemainingTimeUpdated]).asInstanceOf[Set[Class[_ <: JobExecutionStatisticsEvent]]].asJava)

      clusterSingleton(as,
                       "ViewUpdateService",
                       viewUpdateService(jm.distributedPubSub.mediator, jm.actorMaterializer, readJournalJavaApi(system)))

      workerOffice(new TestJobHandlerProvider, jm.config.maxShards)

    }

    getLogger.info("OK")
  }

  def setupEnvironment(): Unit = {
    val envFile = getPropertiesFile("environment-%s")
    val config  = parseFileAnySyntax(envFile)
    if (config.isEmpty)
      throw new RuntimeException(format("Missing or empty %s", envFile.getAbsolutePath + ".conf"))

    config.resolve.entrySet.forEach { entry =>
      {
        System.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
      }
    }
  }

  def configureLog4j(): Unit = {
    PropertyConfigurator.configure(new FileInputStream(getPropertiesFile("log4j-%s.properties")))
  }

  def getPropertiesFile(fileNamePattern: String): File = {
    new File(System.getProperty("jm.conf"), fileNamePattern.format(System.getProperty("jm.env")))
  }

  private def getLogger = LoggerFactory.getLogger(JobManDemo.getClass)

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    import akka.pattern.ask
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")

    // register the shared journal
    import system.dispatcher
    implicit val timeout: Timeout = Timeout(15.seconds)
    val f: Future[Any]            = system.actorSelection(path) ? Identify(None)

    f.onComplete {

      case Success(ActorIdentity(_, Some(ref))) =>
        SharedLeveldbJournal.setStore(ref, system)

      case Success(_) =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()

      case Failure(_) =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()

    }
  }

  def readJournal(as: ActorSystem): EventsByPersistenceIdQuery =
    PersistenceQuery(as).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

  def readJournalJavaApi(as: ActorSystem): akka.persistence.query.javadsl.EventsByPersistenceIdQuery =
    PersistenceQuery.get(as).getReadJournalFor(classOf[LeveldbReadJournalJava], LeveldbReadJournalJava.Identifier)

}
