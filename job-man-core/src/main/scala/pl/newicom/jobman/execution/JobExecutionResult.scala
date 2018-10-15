package pl.newicom.jobman.execution

import java.time.ZonedDateTime

import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.execution.worker.command.WorkerCommand

case class JobExecutionResult(queueId: Int, jobResult: JobResult, dateTime: ZonedDateTime) extends JobExecutionCommand with WorkerCommand
