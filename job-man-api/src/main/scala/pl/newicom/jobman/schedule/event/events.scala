package pl.newicom.jobman.schedule.event

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobCancellationResult

trait JobScheduleEvent

sealed trait JobSchedulingResult extends JobScheduleEvent
sealed trait JobAccepted         extends JobSchedulingResult

case class JobCanceled(jobId: String, queueId: Option[Int], position: Int) extends JobScheduleEvent with JobCancellationResult
case class EquivalentJobFound(jobId: String, equivalentJobId: String)      extends JobSchedulingResult

case class JobDispatchedForExecution(job: Job, queueId: Int) extends JobScheduleEvent

case class JobScheduleEntryAdded(job: Job, queueId: Int, position: Int) extends JobAccepted {
  def jobId: String = job.id
}

case class JobAddedToWaitingList(job: Job, position: Int) extends JobAccepted {
  def jobId: String = job.id
}

case class JobEnded(jobId: String) extends JobScheduleEvent
