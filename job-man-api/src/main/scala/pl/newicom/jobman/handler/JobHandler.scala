package pl.newicom.jobman.handler

import java.util.concurrent.CompletionStage

import pl.newicom.jobman.Job
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.progress.JobProgressListener

import scala.concurrent.Future

trait AbstractJobHandler

trait JobHandler[R <: JobResult, F[_]] extends AbstractJobHandler {
  def execute(job: Job, pl: JobProgressListener): F[R]
}

trait VanillaJavaJobHandler  extends JobHandler[JobResult, CompletionStage]
trait VanillaScalaJobHandler extends JobHandler[JobResult, Future]
