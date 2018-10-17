package pl.newicom.jobman.healthcheck.event

import java.time.ZonedDateTime

trait HealthCheckEvent

case class WorkerStopped(queueId: Int, dateTime: ZonedDateTime) extends HealthCheckEvent
