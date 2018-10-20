package pl.newicom.jobman.rest

object JobScheduledResponse {
  def apply(jobId: String, queueId: Int, position: Int, message: String): JobScheduledResponse =
    new JobScheduledResponse(jobId, queueId, position, message)

  def apply(jobId: String, queueId: Int, position: Int): JobScheduledResponse =
    apply(jobId, queueId, position, s"Job scheduled on queue $queueId, position $position")
}

case class JobScheduledResponse(jobId: String, queueId: Int, position: Int, message: String)

object JobAddedToWaitingListResponse {
  def apply(jobId: String, position: Int, message: String): JobAddedToWaitingListResponse =
    new JobAddedToWaitingListResponse(jobId, position, message)

  def apply(jobId: String, position: Int): JobAddedToWaitingListResponse =
    apply(jobId, position, s"Job added to waiting list with position number $position")
}

case class JobAddedToWaitingListResponse(jobId: String, position: Int, message: String)

object EquivalentJobFoundResponse {
  def apply(equivalentId: String, message: String): EquivalentJobFoundResponse =
    new EquivalentJobFoundResponse(equivalentId, message)

  def apply(equivalentId: String): EquivalentJobFoundResponse =
    apply(equivalentId, s"Job already scheduled: $equivalentId")
}

case class EquivalentJobFoundResponse(equivalentId: String, message: String)

object JobCanceledResponse {
  def apply(jobId: String, message: String): JobCanceledResponse =
    new JobCanceledResponse(jobId, message)

  def apply(jobId: String): JobCanceledResponse =
    apply(jobId, "Job removed from the schedule")
}

case class JobCanceledResponse(jobId: String, message: String)
