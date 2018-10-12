package pl.newicom.jobman.execution.result

import java.io.{PrintWriter, StringWriter}

import pl.newicom.jobman.JobType

object JobFailure {

  def apply(jobId: String, jobType: JobType, reason: Throwable): JobFailure =
    apply(jobId, jobType, reason.getMessage, stackTrace(reason))

  def apply(jobId: String, jobType: JobType, summary: String, report: String) =
    new JobFailure(jobId, jobType, summary, report)

  private def stackTrace(throwable: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw, true)
    throwable.printStackTrace(pw)
    sw.getBuffer.toString
  }

}

case class JobFailure(jobId: String, jobType: JobType, summary: String, report: String) extends JobResult {
  override def isSuccess: Boolean = false
}
