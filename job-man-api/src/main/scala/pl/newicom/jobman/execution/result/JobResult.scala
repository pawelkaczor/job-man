package pl.newicom.jobman.execution.result

import pl.newicom.jobman.JobType

trait JobResult {
  def jobId: String
  def jobType: JobType
  def report: String
  def isSuccess: Boolean
}
