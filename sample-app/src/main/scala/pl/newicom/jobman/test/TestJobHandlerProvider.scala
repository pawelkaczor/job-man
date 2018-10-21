package pl.newicom.jobman.test
import pl.newicom.jobman.JobType
import pl.newicom.jobman.handler.{AbstractJobHandler, JobHandlerProvider}
import pl.newicom.jobman.test.ThreadPoolExecutorFactory.threadPoolExecutor

import scala.concurrent.ExecutionContext

class TestJobHandlerProvider extends JobHandlerProvider {

  val ec: ExecutionContext = ExecutionContext.fromExecutor(threadPoolExecutor(1, "w-%d"))

  def jobHandler(jobType: JobType): AbstractJobHandler =
    new TestJobHandler()(ec)
}
