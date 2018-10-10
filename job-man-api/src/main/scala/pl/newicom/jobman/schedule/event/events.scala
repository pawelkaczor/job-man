package pl.newicom.jobman.schedule.event

import pl.newicom.jobman.Job

trait JobScheduleEvent

sealed trait JobSchedulingResult extends JobScheduleEvent

sealed trait JobCancellationResult

case class JobAlreadyDispatchedForExecution(jobId: String)                 extends JobCancellationResult
case class JobCanceled(jobId: String, queueId: Option[Int], position: Int) extends JobCancellationResult with JobSchedulingResult
case class EquivalentJobFound(jobId: String, equivalentJobId: String)      extends JobSchedulingResult

case class JobDispatchedForExecution(job: Job, queueId: Int) extends JobScheduleEvent

case class JobNotFound(jobId: String) extends JobCancellationResult

case class JobScheduleEntryAdded(job: Job, queueId: Int, position: Int) extends JobSchedulingResult {
  def jobId: String = job.id
}

case class JobAddedToWaitingList(job: Job, position: Int) extends JobSchedulingResult {
  def jobId: String = job.id
}

case class JobEnded(jobId: String) extends JobScheduleEvent
