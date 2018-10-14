package pl.newicom.jobman.execution.worker

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{receivePartial, stopped}
import pl.newicom.jobman.execution.result.{JobHandlerException, JobResult, JobTimeout}
import pl.newicom.jobman.execution.worker.command.{ExecuteJob, StopWorker, WorkerCommand}
import pl.newicom.jobman.handler._
import pl.newicom.jobman.healthcheck.HealthCheckTopic
import pl.newicom.jobman.healthcheck.event.WorkerStopped
import pl.newicom.jobman.progress.JobProgressPublisher
import pl.newicom.jobman.shared.command.JobExecutionResult
import pl.newicom.jobman.{DistributedPubSubFacade, JobMan, JobType}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class WorkerContext(val queueId: Int, jm: JobMan, jhProvider: JobHandlerProvider) {
  def now: ZonedDateTime =
    ZonedDateTime.now(jm.clock)

  def jobHandler(jobType: JobType): AbstractJobHandler =
    jhProvider.jobHandler(jobType)

  def jobProgressListener(jobId: String, jobExecutionId: String): JobProgressPublisher =
    new JobProgressPublisher(jobId, jobExecutionId, jm.distributedPubSub)

  def pubSub: DistributedPubSubFacade =
    jm.distributedPubSub

  def maxJobDuration(jobType: JobType): FiniteDuration =
    FiniteDuration(jm.jobConfigRegistry.jobConfig(jobType).maxDuration.toNanos, TimeUnit.NANOSECONDS)

}

object WorkerBehavior {

  def workerBehavior(queueId: String, jobHandlerProvider: JobHandlerProvider)(implicit jm: JobMan): Behavior[WorkerCommand] =
    workerBehavior(new WorkerContext(queueId.toInt, jm, jobHandlerProvider), None)

  def workerBehavior(ctx: WorkerContext, runningJob: Option[String]): Behavior[WorkerCommand] =
    receivePartial {
      case (actorCtx, cmd @ ExecuteJob(queueId, jobExecutionId, job, _)) =>
        actorCtx.log.info("Execution of Job {} started on queue {}.", job, queueId)

        val pl = ctx.jobProgressListener(job.id, jobExecutionId)

        def log(jobResult: JobResult): Unit =
          if (jobResult.isSuccess) {
            actorCtx.log.info("Job {} completed.", job)
          } else {
            actorCtx.log.error("Job {} failed. Exception: {}", job, jobResult.report)
          }

        def handleJobResult(jobResult: JobResult): Unit = {
          val jobExecutionResult = JobExecutionResult(queueId, jobResult, ctx.now)
          log(jobExecutionResult.jobResult)
          cmd.reportCallback ! jobExecutionResult
          actorCtx.self ! jobExecutionResult
        }

        def executeJava(jh: VanillaJavaJobHandler) =
          jh.execute(job, pl)
            .exceptionally(ex => JobHandlerException(job.id, job.jobType, ex))
            .thenAccept(handleJobResult(_))

        def executeScala(jh: VanillaScalaJobHandler): Unit = {
          import actorCtx.executionContext
          import akka.pattern.after
          after(ctx.maxJobDuration(job.jobType), actorCtx.system.scheduler) {
            jh.execute(job, pl)
              .recover {
                case _: TimeoutException => JobTimeout(job.id, job.jobType)
                case NonFatal(ex)        => JobHandlerException(job.id, job.jobType, ex)
              }
          }.onComplete {
            case scala.util.Success(result) =>
              handleJobResult(result)
            case _ => // ignore
          }
        }

        ctx.jobHandler(job.jobType) match {
          case jh: VanillaJavaJobHandler =>
            executeJava(jh)
          case jh: VanillaScalaJobHandler =>
            executeScala(jh)
        }

        workerBehavior(ctx, Some(job.id))

      case (_, _: JobExecutionResult) =>
        workerBehavior(ctx, None)

      case (actorCtx, StopWorker) =>
        ctx.pubSub.publish(HealthCheckTopic, WorkerStopped(ctx.queueId, runningJob, ctx.now))
        actorCtx.log.info("Queue {} stopped.", ctx.queueId)
        stopped
    }

}
