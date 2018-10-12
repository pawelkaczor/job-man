package pl.newicom.jobman.execution.worker.command

import akka.actor.ActorRef
import pl.newicom.jobman.{Job, JobType}
import pl.newicom.jobman.shared.command.JobExecutionResult
import akka.actor.typed.scaladsl.adapter._

import scala.util.Try

trait WorkerCommand {
  def queueId: Int
}

object ExecuteJob {
  def apply(queueId: Int, jobExecutionId: String, job: Job, reportCallbackUntyped: ActorRef): ExecuteJob =
    new ExecuteJob(queueId, jobExecutionId, job, reportCallbackUntyped)

  def apply(queueId: Int, jobExecutionId: String, job: Job, reportCallback: akka.actor.typed.ActorRef[JobExecutionResult]): ExecuteJob =
    apply(queueId, jobExecutionId, job, Try(reportCallback.toUntyped).getOrElse(null))
}

case class ExecuteJob(queueId: Int, jobExecutionId: String, job: Job, reportCallbackUntyped: ActorRef) extends WorkerCommand {

  @transient
  val reportCallback: akka.actor.typed.ActorRef[JobExecutionResult] =
    reportCallbackUntyped.toTyped

  def jobType: JobType = job.jobType
}

case object StopWorker extends WorkerCommand {
  override def queueId: Int = throw new UnsupportedOperationException
}
