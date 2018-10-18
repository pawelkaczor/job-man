package pl.newicom.jobman.schedule
import org.scalatest.WordSpec
import pl.newicom.jobman.schedule.TestJob.testJob
import pl.newicom.jobman.schedule.event.JobScheduleEntryAdded

class DefaultJobSchedulingPolicyTest extends WordSpec {

  val policyUnderTest = new DefaultJobSchedulingPolicy

  "Default job scheduling policy" when {

    "empty schedule" should {

      val config = JobSchedulingConfig(1, 1, 10)
      val schedule = TestJobSchedule()

      val result = policyUnderTest(testJob("j1"), schedule, config)

      "assign new job to queue: 1, pos: 0" in {
        assert(result == JobScheduleEntryAdded(testJob("j1"), 1, 0))
      }
    }
  }

  implicit def state2schedule(state: JobSchedule.State): JobSchedule =
    JobSchedule(state)
}
