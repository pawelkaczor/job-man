package pl.newicom.jobman.schedule

import pl.newicom.jobman.schedule.JobSchedule.ParamsPredicate
import pl.newicom.jobman.{Job, JobParameters}
import pl.newicom.jobman.schedule.event.JobSchedulingResult

trait JobSchedulingPolicy {
  def apply(job: Job, schedule: JobSchedule)(implicit config: JobSchedulingConfig): JobSchedulingResult
}

trait JobTypeSpecificSchedulingPolicy[T <: JobParameters] {

  def matchingJobType(predicate: ParamsPredicate[T])(implicit config: JobSchedulingConfig): ParamsPredicate[JobParameters] = {
    case (p1, p2) if config.jobType(p1.getClass) == config.jobType(p2.getClass) =>
      predicate(p1.asInstanceOf[T], p2.asInstanceOf[T])
    case _ =>
      false
  }

}
