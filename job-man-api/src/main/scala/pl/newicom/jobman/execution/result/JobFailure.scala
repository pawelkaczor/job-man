package pl.newicom.jobman.execution.result

import java.io.{PrintWriter, StringWriter}

import pl.newicom.jobman.JobType

trait JobFailure extends JobResult {
  def jobId: String
  def jobType: JobType
  def summary: String
  def report: String

  override def isSuccess: Boolean = false
}

object JobHandlerException {

  def apply(jobId: String, jobType: JobType, reason: Throwable): JobHandlerException =
    apply(jobId, jobType, reason.getMessage, stackTrace(reason))

  def apply(jobId: String, jobType: JobType, summary: String, report: String): JobHandlerException =
    new JobHandlerException(jobId, jobType, summary, report)

  private def stackTrace(throwable: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw, true)
    throwable.printStackTrace(pw)
    sw.getBuffer.toString
  }

}

case class JobHandlerException(jobId: String, jobType: JobType, summary: String, report: String) extends JobFailure
