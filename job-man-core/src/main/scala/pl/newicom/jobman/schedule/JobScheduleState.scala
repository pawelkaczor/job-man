package pl.newicom.jobman.schedule

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobSchedule.Entry
import pl.newicom.jobman.schedule.event.{JobAddedToWaitingList, JobCanceled, JobEnded, JobScheduleEntryAdded}
import pl.newicom.jobman.shared.event.ExecutionJournalOffsetChanged
import pl.newicom.jobman.utils._

case class JobScheduleState(
                             queues: Map[Int, List[Entry]] = Map.empty,
                             waitingList: List[Job] = List.empty,
                             executionJournalOffset: Long = 1L) {

  def withScheduleEntryAdded(event: JobScheduleEntryAdded): JobScheduleState = {
    val newEntry = Entry(event.jobId, event.job.params, event.queueId)
    copy(
      queues = queues.plus(event.position, newEntry),
      waitingList = waitingList.filterNot(_ == event.job)
    )
  }

  def withJobAddedToWaitingList(event: JobAddedToWaitingList): JobScheduleState =
    copy(waitingList = waitingList.:+(event.job))

  def withJobCanceled(event: JobCanceled): JobScheduleState =
    event.queueId.map(queueId =>
      copy(queues = queues.minus(queueId, event.position))
    ).getOrElse {
      copy(waitingList = waitingList.drop(event.position))
    }


  def withJobEnded(event: JobEnded): JobScheduleState =
    dispatchedJobs.find(_.jobId == event.jobId).map(e => {
      copy(queues = queues.minus(e.queueId, e))
    }).getOrElse(this)


  def withExecutionJournalOffsetChanged(event: ExecutionJournalOffsetChanged): JobScheduleState =
    copy(executionJournalOffset = event.newOffsetValue)

  private def dispatchedJobs =
    queues.mapValues(_.headOption).values.filter(_.isDefined).map(_.get)

  private def jobs(queueId: Int): List[Entry] =
    queues.getOrElse(queueId, List.empty)

  def successorJob(jobId: String): Option[Entry] =
    queueId(jobId).flatMap(queueId => {
      val js = jobs(queueId)
      val pairs = js.:+(null).zip(null :: js.tail)
      pairs
        .find { case (l, _) => Option(l).exists(_.jobId == jobId) }
        .map(_._2)
    })

  private def queueId(jobId: String): Option[Int] =
    queues.find { case (_, es) => es.exists(_.jobId == jobId) } map (_._1)
}
