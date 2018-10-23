package pl.newicom.jobman.test

import java.io.{File, FileInputStream}
import java.lang.String.format

import akka.actor.{ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.cluster.Cluster
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseFileAnySyntax
import org.apache.log4j.PropertyConfigurator
import org.slf4j.LoggerFactory
import pl.newicom.jobman.JobManInfraServices

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object SampleApp {

  def main(args: Array[String]): Unit = {
    setupEnvironment()
    configureLog4j()

    val config  = ConfigFactory.load
    val system  = ActorSystem("jm", config)
    val cluster = Cluster(system)
    val port    = config.getString("akka.remote.netty.tcp.port")

    startupSharedJournal(system, startStore = port == "2551", path = ActorPath.fromString("akka.tcp://jm@127.0.0.1:2551/user/store"))

    new JobManInfraServices(cluster, config)(system)

    cluster.registerOnMemberUp {}

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

  private def getLogger = LoggerFactory.getLogger(SampleApp.getClass)

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

}
