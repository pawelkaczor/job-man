package pl.newicom.jobman.schedule

import pl.newicom.jobman.schedule.JobSchedule._
import pl.newicom.jobman.schedule.event.{EquivalentJobFound, JobAddedToWaitingList, JobScheduleEntryAdded, JobSchedulingResult}
import pl.newicom.jobman.{Job, JobParameters, JobType, JobConfigRegistry}

import scala.util.Try

object JobSchedule {

  type ParamsPredicate[T <: JobParameters] = (T, T) => Boolean

  case class Entry(jobId: String, params: JobParameters, queueId: Int) {
    def job: Job = Job(jobId, params)
  }

  trait State {
    def queues: Map[Int, List[Entry]]
    def waitingList: List[Job]
  }

  trait StateQueries {
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
  }

  trait JobTypeRegistryQueries {
    def jobConfigRegistry: JobConfigRegistry

    def jobType(entry: Entry): JobType =
      jobConfigRegistry.jobType(entry.params)

    def jobType(job: Job): JobType =
      jobConfigRegistry.jobType(job.params)
  }
}

case class JobSchedule(state: State, config: JobSchedulingConfig, jobConfigRegistry: JobConfigRegistry)
    extends StateQueries
    with JobTypeRegistryQueries {

  def enqueueDefault(job: Job): JobSchedulingResult =
    Try {
      if (notEmptyQueuesNumber < config.getMinQueues)
        enqueueInNewQueue(job)
      else {
        queueWithTheLowestNrOfEnqueuedJobs
          .filter(qId => queueLength(qId) < config.getQueueCapacity)
          .map(enqueueLast(job, _))
          .getOrElse(enqueueInNewQueue(job))
      }
    }.recover {
      case _: JobSchedulingException =>
        copy(config = config.withQueueCapacityBumped).enqueueDefault(job)
    }.get

  private def enqueueInNewQueue(job: Job): JobSchedulingResult =
    JobScheduleEntryAdded(job, this.nextQueueId, 0)

  def enqueueLast(job: Job, queueId: Int): JobSchedulingResult =
    JobScheduleEntryAdded(job, queueId, queueLength(queueId))

  def enqueueBefore(job: Job, queueId: Int, dependentJobId: String): JobScheduleEntryAdded =
    entries(queueId).zipWithIndex
      .find(_._1.jobId == dependentJobId)
      .map(p => JobScheduleEntryAdded(job, queueId, p._2))
      .get

  def enqueueAfter[T <: JobParameters](job: Job, predicate: ParamsPredicate[T]): Option[JobSchedulingResult] =
    enqueueAfter(job, e => {
      jobType(e) == jobType(job) && predicate(job.params.asInstanceOf[T], e.params.asInstanceOf[T])
    })

  @SafeVarargs
  def enqueueAfter(job: Job, predicates: ParamsPredicate[JobParameters]*): Option[JobSchedulingResult] = {
    val entryPredicates =
      predicates.map(p => (e: Entry) => p(job.params, e.params))

    val results = entryPredicates.flatMap(p => enqueueAfter(job, p).toList)
    if (results.isEmpty)
      None
    else if (results.size == 1)
      Option(results.head)
    else
      results.find(_.isInstanceOf[JobAddedToWaitingList]).orElse {
        val queues: Set[Int] = results
          .filter(_.isInstanceOf[JobScheduleEntryAdded])
          .map(_.asInstanceOf[JobScheduleEntryAdded])
          .groupBy(_.queueId)
          .keySet

        enqueueLast(job, queues)
      }
  }

  private def enqueueLast(job: Job, queues: Set[Int]): Option[JobSchedulingResult] =
    if (queues.isEmpty)
      None
    else if (queues.size == 1)
      Option(enqueueLast(job, queues.head))
    else if (awaitingJob(job.id).isDefined)
      None
    else
      Option(JobAddedToWaitingList(job, waitingListLength))

  private def enqueueAfter(job: Job, entryPredicate: Entry => Boolean): Option[JobSchedulingResult] =
    enqueueLast(job, entries(entryPredicate).groupBy(_.queueId).keySet)

  def rejectIfEquivalentJobEnqueued(job: Job): Option[JobSchedulingResult] =
    entries(e => jobType(e) == jobType(job) && (e.jobId == job.id || e.params == job.params)).headOption
      .map(e => EquivalentJobFound(job.id, e.jobId))

  private def nextQueueId: Int =
    emptyQueues.keys.headOption.getOrElse {
      val qNum = notEmptyQueuesNumber
      if (qNum < config.getMaxQueues)
        qNum + 1
      else
        throw new JobSchedulingException("Maximum number of queues reached.")
    }

}
