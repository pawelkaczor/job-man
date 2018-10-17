package pl.newicom.jobman.execution.worker.command

import java.time.ZonedDateTime

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.adapter._
import pl.newicom.jobman.execution.command.JobExecutionCommand
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.{Job, JobType}

import scala.util.Try

sealed trait WorkerCommand {
  def queueId: Int
}

object ExecuteJob {
  def apply(queueId: Int, jobExecutionId: String, job: Job, responseCallbackUntyped: ActorRef): ExecuteJob =
    new ExecuteJob(queueId, jobExecutionId, job, responseCallbackUntyped)

  def apply(queueId: Int, jobExecutionId: String, job: Job, responseCallback: akka.actor.typed.ActorRef[WorkerResponse]): ExecuteJob =
    apply(queueId, jobExecutionId, job, Try(responseCallback.toUntyped).getOrElse(null))
}

case class ExecuteJob(queueId: Int, jobExecutionId: String, job: Job, responseCallbackUntyped: ActorRef) extends WorkerCommand {

  @transient
  val responseCallback: akka.actor.typed.ActorRef[WorkerResponse] =
    responseCallbackUntyped.toTyped

  def jobType: JobType = job.jobType
}

sealed trait WorkerResponse extends JobExecutionCommand {
  def queueId: Int
}

case class JobExecutionResult(queueId: Int, jobResult: JobResult, dateTime: ZonedDateTime) extends WorkerResponse {
  def jobId: String = jobResult.jobId
}

case class JobTimeout(queueId: Int, jobId: String, jobType: JobType) extends WorkerResponse

case object StopWorker extends WorkerCommand {
  override def queueId: Int = throw new UnsupportedOperationException
}
