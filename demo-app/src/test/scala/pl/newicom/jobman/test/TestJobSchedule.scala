package pl.newicom.jobman.test

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.JobSchedule
import pl.newicom.jobman.schedule.JobSchedule.Entry
import pl.newicom.jobman.utils._

case class TestJobSchedule(queues: Map[Int, List[Entry]] = Map.empty, waitingList: List[Job] = List.empty) extends JobSchedule.State {

  def withJobs(queueId: Int, jobs: Job*): TestJobSchedule =
    jobs.toList match {
      case Nil =>
        this
      case job :: Nil =>
        copy(queues.plus(queueId, Entry(job, queueId)))
      case job :: tail =>
        withJobs(queueId, job).withJobs(queueId, tail: _*)
    }

}
