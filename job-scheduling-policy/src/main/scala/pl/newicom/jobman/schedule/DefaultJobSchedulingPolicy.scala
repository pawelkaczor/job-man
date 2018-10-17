package pl.newicom.jobman.schedule
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.JobSchedulingResult

class DefaultJobSchedulingPolicy extends JobSchedulingPolicy {

  def scheduleJob(job: Job, schedule: JobSchedule): JobSchedulingResult =
    schedule
      .rejectIfEquivalentJobEnqueued(job)
      .getOrElse(schedule.enqueueDefault(job))

}
