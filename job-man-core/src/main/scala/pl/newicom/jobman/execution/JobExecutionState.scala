package pl.newicom.jobman.execution

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

import pl.newicom.jobman.{JobConfig, JobConfigRegistry, JobType}
import pl.newicom.jobman.execution.JobExecutionState.Entry
import pl.newicom.jobman.execution.event.{JobExecutionTerminalEvent, JobExecutorEvent, JobStarted, SchedulingJournalOffsetChanged}

import scala.collection.mutable

object JobExecutionState {
  case class Entry(queueId: Int, jobType: JobType, jobStartTime: ZonedDateTime, jobDeadline: ZonedDateTime)
}

case class JobExecutionState(@transient jobConfigRegistry: JobConfigRegistry,
                             entries: Map[String, Entry] = Map.empty,
                             schedulingJournalOffset: Long = 1L) {

  @transient
  val recentActivityMap: mutable.Map[String, ZonedDateTime] = mutable.Map.empty // mutable, jobId -> timestamp of recent progress update

  def apply(event: JobExecutorEvent): JobExecutionState = event match {

    case SchedulingJournalOffsetChanged(newOffsetValue) =>
      copy(schedulingJournalOffset = newOffsetValue)

    case JobStarted(jobId, jobType, queueId, _, dateTime) =>
      val maxDuration = jobConfig(jobType).maxDuration
      val deadline    = dateTime.plus(maxDuration)
      copy(entries = entries + (jobId -> Entry(queueId, jobType, dateTime, deadline)))

    case event: JobExecutionTerminalEvent =>
      recentActivityMap.remove(event.jobId)
      copy(entries = entries - event.jobId)
  }

  def overrunningJobs: Set[String] =
    entries.filter { case (jobId, entry) => now().isAfter(jobDeadline(jobId, entry)) }.keySet

  def jobDeadline(jobId: String, entry: Entry): ZonedDateTime =
    recentActivityMap
      .get(jobId)
      .map(_.plus(jobConfig(entry.jobType).maxTaskDuration))
      .getOrElse(entry.jobDeadline)

  def jobType(jobId: String): Option[JobType] =
    entries.get(jobId).map(_.jobType)

  def updateJobActivityTimestamp(jobId: String): Unit =
    if (entries.contains(jobId))
      recentActivityMap.put(jobId, now)

  def jobsEnqueued(queueId: Integer): List[(String, Entry)] =
    entries.filter { case (_, entry) => entry.queueId == queueId }.toList

  def jobConfig(jobType: JobType): JobConfig =
    jobConfigRegistry.jobConfig(jobType)

}
