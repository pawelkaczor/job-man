package pl.newicom.jobman.schedule

import pl.newicom.jobman.schedule.JobSchedule._
import pl.newicom.jobman.schedule.event.{EquivalentJobFound, JobAddedToWaitingList, JobScheduleEntryAdded, JobSchedulingResult}
import pl.newicom.jobman.{Job, JobParameters, JobType}

object JobSchedule {

  type ParamsPredicate[T <: JobParameters] = (T, T) => Boolean

  case class Entry(job: Job, queueId: Int) {
    def jobId: String            = job.id
    def jobParams: JobParameters = job.params
    def jobType: JobType         = job.jobType
  }

  trait State {
    def queues: Map[Int, List[Entry]]
    def waitingList: List[Job]
  }

}

case class JobSchedule(state: State) extends JobScheduleOps with JobScheduleQueries
