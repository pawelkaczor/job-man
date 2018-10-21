package pl.newicom.jobman.test.schedule

import pl.newicom.jobman.schedule.JobSchedule.ParamsPredicate
import pl.newicom.jobman.schedule._
import pl.newicom.jobman.schedule.event.JobSchedulingResult
import pl.newicom.jobman.test.job.TestJobParameters
import pl.newicom.jobman.{Job, JobParameters}

class TestJobSchedulingPolicy extends JobSchedulingPolicy with JobTypeSpecificSchedulingPolicy[TestJobParameters] {

  def apply(job: Job, schedule: JobSchedule)(implicit config: JobSchedulingConfig): JobSchedulingResult = {
    schedule
      .rejectIfEquivalentJobEnqueued(job)
      .orElse {
        schedule.enqueueAfterAll(job)(
          matchingJobType((p1, p2) => (p1.locks intersect p2.locks).nonEmpty),
          someOtherPredicate
        )
      }
      .getOrElse(applyDefaultPolicy(job, schedule))
  }

  private def someOtherPredicate: ParamsPredicate[JobParameters] =
    (_, _) => false

  private def applyDefaultPolicy(job: Job, schedule: JobSchedule)(implicit config: JobSchedulingConfig): JobSchedulingResult =
    new DefaultJobSchedulingPolicy().apply(job, schedule)

}
