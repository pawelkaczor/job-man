package pl.newicom.jobman.schedule
import pl.newicom.jobman.schedule.JobSchedule.{Entry, ParamsPredicate}
import pl.newicom.jobman.schedule.event.{EquivalentJobFound, JobAddedToWaitingList, JobScheduleEntryAdded, JobSchedulingResult}
import pl.newicom.jobman.{Job, JobParameters}

trait JobScheduleOps {
  this: JobScheduleQueries =>

  def enqueueLast(job: Job, queueId: Int): JobScheduleEntryAdded =
    JobScheduleEntryAdded(job, queueId, queueLength(queueId))

  def enqueueBefore(job: Job, queueId: Int, dependentJobId: String): JobScheduleEntryAdded =
    entries(queueId).zipWithIndex
      .find(_._1.jobId == dependentJobId)
      .map(p => JobScheduleEntryAdded(job, queueId, p._2))
      .get

  def enqueueAfter[T <: JobParameters](job: Job, predicate: ParamsPredicate[T]): Option[JobSchedulingResult] =
    enqueueAfter(job, e => {
      e.jobType == job.jobType && predicate(job.params.asInstanceOf[T], e.jobParams.asInstanceOf[T])
    })

  @SafeVarargs
  def enqueueAfterAll(job: Job)(predicates: ParamsPredicate[JobParameters]*): Option[JobSchedulingResult] = {
    val entryPredicates =
      predicates.map(p => (e: Entry) => p(job.params, e.jobParams))

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

  def rejectIfEquivalentJobEnqueued(job: Job): Option[JobSchedulingResult] =
    entries(e => e.jobType == job.jobType && (e.jobId == job.id || e.jobParams == job.params)).headOption
      .map(e => EquivalentJobFound(job.id, e.jobId))

  def enqueueInNewQueue(job: Job, maxQueues: Int): Option[JobSchedulingResult] =
    nextQueueId(maxQueues).map(JobScheduleEntryAdded(job, _, 0))

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

  private def nextQueueId(maxQueues: Int): Option[Int] =
    emptyQueues.keys.headOption.orElse {
      val notEmptyNum = notEmptyQueuesNumber
      if (notEmptyNum < maxQueues) Some(notEmptyNum + 1) else None
    }

}
