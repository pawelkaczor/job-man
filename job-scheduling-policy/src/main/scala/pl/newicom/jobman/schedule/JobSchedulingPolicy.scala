package pl.newicom.jobman.schedule

import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.JobSchedulingResult

trait JobSchedulingPolicy {
  def apply(job: Job, schedule: JobSchedule, config: JobSchedulingConfig): JobSchedulingResult
}
