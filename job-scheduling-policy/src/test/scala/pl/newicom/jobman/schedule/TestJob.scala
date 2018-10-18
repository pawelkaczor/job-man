package pl.newicom.jobman.schedule
import pl.newicom.jobman.{Job, JobParameters, JobType}

object TestJob {
  object TestJobType extends JobType {
    override def toString: String = "Test"
  }

  def testJob(jobId: String): Job =
    Job(jobId, TestJobType, new AnyRef with JobParameters {
      override def toString: String = "params"
    })
}
