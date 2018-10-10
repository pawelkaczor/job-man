package pl.newicom.jobman.schedule

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.JobSchedulingResult

class JobSchedulingPolicyFacade extends JobSchedulingPolicy {
  def scheduleJob(job: Job, schedule: JobSchedule): JobSchedulingResult = ???
}
