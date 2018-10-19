package pl.newicom.jobman.schedule
import pl.newicom.jobman.{Job, JobParameters}

object TestJob {
  val TestJobType = "Test"

  def testJob(jobId: String): Job =
    Job(jobId, TestJobType, new AnyRef with JobParameters {
      override def toString: String = "params"
    })
}
