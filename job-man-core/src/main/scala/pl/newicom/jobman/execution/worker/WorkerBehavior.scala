package pl.newicom.jobman.execution.worker

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import akka.actor.typed.Behavior
import akka.actor.typed.Behavior.same
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.scaladsl.Behaviors.stopped
import akka.pattern.after
import pl.newicom.jobman.execution.result.{JobHandlerException, JobResult}
import pl.newicom.jobman.execution.worker.command._
import pl.newicom.jobman.handler._
import pl.newicom.jobman.healthcheck.HealthCheckTopic
import pl.newicom.jobman.healthcheck.event.WorkerStopped
import pl.newicom.jobman.progress.JobProgressPublisher
import pl.newicom.jobman.{DistributedPubSubFacade, JobMan, JobType}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, TimeoutException}
import scala.util.control.NonFatal
import scala.compat.java8.FutureConverters._
import scala.util.{Failure, Success}

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
    workerBehavior(new WorkerContext(queueId.toInt, jm, jobHandlerProvider))

  def workerBehavior(ctx: WorkerContext): Behavior[WorkerCommand] =
    Behaviors.receive {
      case (actorCtx, cmd @ ExecuteJob(queueId, jobExecutionId, job, _)) =>
        import actorCtx.executionContext
        val progressListener = ctx.jobProgressListener(job.id, jobExecutionId)
        val scheduler        = actorCtx.system.scheduler
        val maxJobDuration   = ctx.maxJobDuration(job.jobType)

        def executeJobHandler: Future[JobResult] =
          ctx.jobHandler(job.jobType) match {
            case jh: VanillaJavaJobHandler =>
              jh.execute(job, progressListener).toScala
            case jh: VanillaScalaJobHandler =>
              jh.execute(job, progressListener)
          }

        actorCtx.log.info("Execution of Job {} started on queue {}.", job, queueId)

        after(maxJobDuration, scheduler)(executeJobHandler)
          .map(JobExecutionResult(queueId, _, ctx.now))
          .recover {
            case _: TimeoutException => JobTimeout(queueId, job.id, job.jobType)
            case NonFatal(ex)        => JobExecutionResult(queueId, JobHandlerException(job.id, job.jobType, ex), ctx.now)
          }
          .onComplete {
            case Success(response) =>
              log(actorCtx, response)
              cmd.responseCallback ! response
            case Failure(ex) =>
              actorCtx.log.error(ex, "Fatal exception")
          }

        same

      case (actorCtx, StopWorker) =>
        ctx.pubSub.publish(HealthCheckTopic, WorkerStopped(ctx.queueId, ctx.now))
        actorCtx.log.info("Queue {} stopped.", ctx.queueId)
        stopped
    }

  def log(ctx: ActorContext[_], response: WorkerResponse): Unit = response match {
    case r: JobExecutionResult if r.jobResult.isSuccess =>
      ctx.log.info("Job {} completed.", r.jobId)

    case r: JobExecutionResult =>
      ctx.log.error("Job {} failed. Exception: {}", r.jobId, r.jobResult.report)

    case r: JobTimeout =>
      ctx.log.error("Job {} expired", r.jobId)
  }

}
