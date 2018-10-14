package pl.newicom.jobman.notification

import java.util.concurrent.CompletionStage

import pl.newicom.jobman.healthcheck.event.HealthCheckEvent

trait HealthCheckNotificationSender {
  def sendNotification(healthCheckEvent: HealthCheckEvent): CompletionStage[Void]
}
