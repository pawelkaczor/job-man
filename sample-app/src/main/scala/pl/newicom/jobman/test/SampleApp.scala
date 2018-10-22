package pl.newicom.jobman.test
import java.io.{File, FileInputStream}
import java.lang.String.format

import com.typesafe.config.ConfigFactory.parseFileAnySyntax
import org.apache.log4j.PropertyConfigurator
import org.slf4j.{Logger, LoggerFactory}

object SampleApp extends App {
  override def main(args: Array[String]): Unit = {
    setupEnvironment()
    configureLog4j()

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

}
