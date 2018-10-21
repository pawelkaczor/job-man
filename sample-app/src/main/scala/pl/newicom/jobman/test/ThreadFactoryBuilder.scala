package pl.newicom.jobman.test

import java.util.Locale
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicLong

object ThreadFactoryBuilder {

  def build(builder: ThreadFactoryBuilder): ThreadFactory = {
    val nameFormat                                                        = builder.nameFormat
    val daemon: Option[Boolean]                                           = builder.daemon
    val priority: Option[Int]                                             = builder.priority
    val uncaughtExceptionHandler: Option[Thread.UncaughtExceptionHandler] = builder.uncaughtExceptionHandler
    val backingThreadFactory =
      if (builder.backingThreadFactory != null) builder.backingThreadFactory else Executors.defaultThreadFactory

    val count = if (nameFormat != null) new AtomicLong(0) else null

    runnable: Runnable =>
      {
        val thread = backingThreadFactory.newThread(runnable)
        if (nameFormat != null) thread.setName(format(nameFormat, count.getAndIncrement))
        daemon.foreach(thread.setDaemon)
        priority.foreach(thread.setPriority)
        uncaughtExceptionHandler.foreach(thread.setUncaughtExceptionHandler)
        thread
      }
  }
  private def format(format: String, args: Any*) = String.format(Locale.ROOT, format, args)
}

final class ThreadFactoryBuilder() {
  private var nameFormat: String                                                = _
  private var daemon: Option[Boolean]                                           = None
  private var priority: Option[Int]                                             = None
  private var uncaughtExceptionHandler: Option[Thread.UncaughtExceptionHandler] = None
  private var backingThreadFactory: ThreadFactory                               = _

  def setNameFormat(nameFormat: String): ThreadFactoryBuilder = {
    val unused = ThreadFactoryBuilder.format(nameFormat, 0)
    this.nameFormat = nameFormat
    this
  }

  def setDaemon(daemon: Boolean): ThreadFactoryBuilder = {
    this.daemon = Option(daemon)
    this
  }

  def setPriority(priority: Int): ThreadFactoryBuilder = {
    this.priority = Option(priority)
    this
  }

  def setUncaughtExceptionHandler(uncaughtExceptionHandler: Thread.UncaughtExceptionHandler): ThreadFactoryBuilder = {
    this.uncaughtExceptionHandler = Option(uncaughtExceptionHandler)
    this
  }

  def setThreadFactory(backingThreadFactory: ThreadFactory): ThreadFactoryBuilder = {
    this.backingThreadFactory = backingThreadFactory
    this
  }

  def build: ThreadFactory = ThreadFactoryBuilder.build(this)
}
