package pl.newicom.jobman.test
import pl.newicom.jobman.Job
import pl.newicom.jobman.execution.result.{JobResult, JobResultMessage}
import pl.newicom.jobman.handler.VanillaScalaJobHandler
import pl.newicom.jobman.progress.JobProgressListener

import scala.concurrent.{ExecutionContext, Future}

class TestJobHandler(implicit ec: ExecutionContext) extends VanillaScalaJobHandler {

  def execute(job: Job, pl: JobProgressListener): Future[JobResult] =
    job.params match {
      case TestJobParameters(taskExecutionTimeSecs, nrOfTasks, simulateJobFailure, _) =>
        Future {
          pl.tasksStarted(nrOfTasks)

          simulateJobFailure.foreach(throw new RuntimeException("Simulated exception"))

          for (_ <- 1 to nrOfTasks) {
            Thread.sleep(1000 * taskExecutionTimeSecs)
            pl.tasksCompleted(1)
          }

          JobResultMessage(job.id, job.jobType, "OK")
        }

      case _ =>
        Future.failed(new RuntimeException("Invalid job parameters"))
    }
}
