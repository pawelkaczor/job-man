package pl.newicom.jobman.notification

import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import pl.newicom.jobman.JobParameters
import pl.newicom.jobman.execution.event.JobExecutionTerminalEvent

trait JobNotificationMessageFactory {

  def apply(result: JobExecutionTerminalEvent, jobParamsSupplier: Supplier[JobParameters]): List[CompletionStage[NotificationMsg]]

}
