package pl.newicom.jobman.test
import pl.newicom.jobman.JobType
import pl.newicom.jobman.execution.result.{JobFailure, SuccessfulJobResult}
import pl.newicom.jobman.test.TestJobParameters.TestJobType

case class TestJobResult(jobId: String, report: String) extends SuccessfulJobResult {
  def jobType: JobType = TestJobType
}

case class TestJobFailure(jobId: String, report: String) extends JobFailure {
  def jobType: JobType = TestJobType
}
