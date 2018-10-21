package pl.newicom.jobman.test
import pl.newicom.jobman.Job
import pl.newicom.jobman.execution.result.JobResult
import pl.newicom.jobman.handler.VanillaScalaJobHandler
import pl.newicom.jobman.progress.JobProgressListener

import scala.concurrent.{ExecutionContext, Future}

class TestJobHandler(implicit ec: ExecutionContext) extends VanillaScalaJobHandler {

  def execute(job: Job, pl: JobProgressListener): Future[JobResult] =
    job.params match {
      case TestJobParameters(taskExecutionTimeSecs, nrOfTasks, simulateJobFailure, _) =>
        Future {
          pl.tasksStarted(nrOfTasks)
          simulateJobFailure.map(_ => TestJobFailure(job.id, "Simulated exception")).getOrElse {
            for (_ <- 1 to nrOfTasks) {
              Thread.sleep(1000 * taskExecutionTimeSecs)
              pl.tasksCompleted(1)
            }
            TestJobResult(job.id, "OK")
          }
        }

      case _ =>
        Future.failed(new RuntimeException("Invalid job parameters"))
    }
}
