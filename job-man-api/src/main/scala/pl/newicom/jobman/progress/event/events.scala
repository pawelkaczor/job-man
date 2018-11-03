package pl.newicom.jobman.progress.event
import java.time.{Duration, LocalTime}

sealed trait JobProgressEvent {
  def jobExecutionId: String
}

case class JobProgressUpdated(jobId: String, jobExecutionId: String, nrOfTasksDiff: Int, time: LocalTime) extends JobProgressEvent

sealed trait JobExecutionStatisticsEvent extends JobProgressEvent

case class JobExecutionCompleted(jobExecutionId: String)                              extends JobExecutionStatisticsEvent
case class JobRemainingTimeUpdated(jobExecutionId: String, remainingTime: Duration)   extends JobExecutionStatisticsEvent
case class TaskAvgExecutionTimeUpdated(jobExecutionId: String, avgDuration: Duration) extends JobExecutionStatisticsEvent
