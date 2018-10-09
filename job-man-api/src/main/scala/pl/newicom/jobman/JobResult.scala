package pl.newicom.jobman

trait JobResult {
  def jobId: String
  def jobType: JobType
  def report: String
  def isSuccess: Boolean
}
