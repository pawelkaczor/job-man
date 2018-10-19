package pl.newicom.jobman.schedule.error
import pl.newicom.jobman.schedule.JobCancellationResult

case class JobAlreadyDispatchedForExecution(jobId: String) extends JobCancellationResult
case class JobNotFound(jobId: String)                      extends JobCancellationResult
