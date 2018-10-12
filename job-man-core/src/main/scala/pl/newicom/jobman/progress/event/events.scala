package pl.newicom.jobman.progress.event

import java.time.LocalTime

trait JobProgressEvent {
  def jobExecutionId: String
}

case class JobProgressUpdated(jobId: String, jobExecutionId: String, nrOfTasksDiff: Int, time: LocalTime) extends JobProgressEvent
