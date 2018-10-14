package pl.newicom.jobman.healthcheck.event

import java.time.ZonedDateTime

trait HealthCheckEvent

case class WorkerStopped(queueId: Int, runningJob: Option[String], dateTime: ZonedDateTime) extends HealthCheckEvent
