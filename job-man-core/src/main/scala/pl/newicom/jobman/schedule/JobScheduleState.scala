package pl.newicom.jobman.schedule

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobSchedule.Entry
import pl.newicom.jobman.schedule.event._
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged
import pl.newicom.jobman.utils._

case class JobScheduleState(override val queues: Map[Int, List[Entry]] = Map.empty,
                            override val waitingList: List[Job] = List.empty,
                            executionJournalOffset: Long = 1L)
    extends JobSchedule.State
    with JobSchedule.StateQueries {

  def state: JobSchedule.State = this

  def apply(event: JobScheduleEvent): JobScheduleState = event match {
    case JobScheduleEntryAdded(job, queueId, pos) =>
      copy(
        queues = queues.plus(pos, Entry(job.id, job.params, queueId)),
        waitingList = waitingList.filterNot(_ == job)
      )

    case JobAddedToWaitingList(job, _) =>
      copy(waitingList = waitingList.:+(job))

    case JobCanceled(_, queueIdOpt, pos) =>
      queueIdOpt
        .map(queueId => copy(queues = queues.minus(queueId, pos)))
        .getOrElse {
          copy(waitingList = waitingList.drop(pos))
        }

    case JobEnded(jobId) =>
      dispatchedJobs
        .find(_.jobId == jobId)
        .map(e => {
          copy(queues = queues.minus(e.queueId, e))
        })
        .getOrElse(this)

    case ExecutionJournalOffsetChanged(newOffsetValue) =>
      copy(executionJournalOffset = newOffsetValue)

    case _ => this
  }

  def successorJob(jobId: String): Option[Entry] =
    queueId(jobId).flatMap(queueId => {
      val es    = entries(queueId)
      val pairs = es.:+(null).zip(null :: es.tail)
      pairs
        .find { case (l, _) => Option(l).exists(_.jobId == jobId) }
        .map(_._2)
    })

  def contains(jobId: String): Boolean =
    entry(jobId).isDefined

  def jobsDispatchedForExecution(predicate: Entry => Boolean): Iterable[Entry] =
    dispatchedJobs.filter(predicate)

  def position(jobId: String): Int =
    queueId(jobId)
      .map(qId => entries(qId).indexWhere(_.jobId == jobId))
      .getOrElse(waitingList.indexWhere(_.id == jobId))

  private def dispatchedJobs =
    queues.mapValues(_.headOption.toList).values.flatten

  private def queueId(jobId: String): Option[Int] =
    queues.find { case (_, es) => es.exists(_.jobId == jobId) } map (_._1)

}
