package pl.newicom.jobman.schedule
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobSchedule.{Entry, State}

trait JobScheduleQueries {
  def state: State

  def queues: Map[Int, List[Entry]] =
    state.queues.withDefaultValue(List.empty)

  def emptyQueues: Map[Int, List[Entry]] =
    queues.filter(_._2.isEmpty)

  def waitingList: List[Job] =
    state.waitingList

  def entry(jobId: String): Option[Entry] =
    entries(_.jobId == jobId).headOption

  def entries(queueId: Int): List[Entry] =
    queues(queueId)

  def entries(entryPredicate: Entry => Boolean): Iterable[Entry] =
    queues.values.flatten.filter(entryPredicate)

  def queueExists(queueId: Int): Boolean =
    queues.contains(queueId)

  def queueLength(queueId: Int): Int =
    queues(queueId).size

  def waitingListLength: Int =
    waitingList.size

  def notEmptyQueuesNumber: Int =
    queues.filterNot(_._2.isEmpty).size

  def queueWithTheLowestNrOfEnqueuedJobs: Option[Int] =
    queues
      .map { case (qId, es) => (qId, es.size) }
      .toSeq
      .sortBy(_._2)
      .map(_._1)
      .headOption

  def awaitingJob(jobId: String): Option[Job] =
    waitingList.find(_.id == jobId)

  def jobsNumber: Int =
    queues.values.flatten.size + waitingList.size

  def jobs: Set[Job] =
    (queues.values.flatten.map(_.job) ++ waitingList).toSet
}
