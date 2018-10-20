package pl.newicom.jobman.schedule
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.JobSchedulingResult

class DefaultJobSchedulingPolicy extends JobSchedulingPolicy {

  def apply(job: Job, schedule: JobSchedule)(implicit config: JobSchedulingConfig): JobSchedulingResult =
    schedule
      .rejectIfEquivalentJobEnqueued(job)
      .getOrElse(scheduleJob(job, schedule))

  private def scheduleJob(job: Job, schedule: JobSchedule)(implicit config: JobSchedulingConfig): JobSchedulingResult = {
    (if (schedule.notEmptyQueuesNumber < config.minQueues)
       schedule.enqueueInNewQueue(job, config.maxQueues)
     else {
       schedule.queueWithTheLowestNrOfEnqueuedJobs
         .filter(qId => schedule.queueLength(qId) < config.queueCapacity)
         .map(schedule.enqueueLast(job, _))
         .orElse(schedule.enqueueInNewQueue(job, config.maxQueues))
     }).getOrElse(scheduleJob(job, schedule)(config.withQueueCapacityBumped))
  }

}
