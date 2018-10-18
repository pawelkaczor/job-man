package pl.newicom.jobman.schedule
import org.scalatest.WordSpec
import pl.newicom.jobman.{Job, JobParameters}
import pl.newicom.jobman.schedule.TestJob.{TestJobType, testJob}
import pl.newicom.jobman.schedule.event.{EquivalentJobFound, JobScheduleEntryAdded}

class DefaultJobSchedulingPolicyTest extends WordSpec {

  val policy = new DefaultJobSchedulingPolicy
  val conf = JobSchedulingConfig(minQueues = 1, maxQueues = 10, queueCapacity = 10)
  val empty = TestJobSchedule()

  "Default job scheduling policy" when {

    "no active queues" should {

      val config = conf.copy(minQueues = 1, maxQueues = 1)
      val schedule = empty

      "assign first queue, first position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 1, 0))
      }
    }

    "single active queue, max queues reached" should {

      val config = conf.copy(minQueues = 1, maxQueues = 1)
      val schedule = empty.withJob(1, "j1")

      "assign the active queue, last position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 1, 1))
      }
    }

    "min queues not reached" should {

      val config = conf.copy(minQueues = 2)
      val schedule = empty.withJob(1, "j1")

      "assign new queue, first position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 2, 0))
      }
    }

    "multiple active queues" should {

      val config = conf
      val schedule = empty
        .withJob(1, "j1", "j2")
        .withJob(2, "j3")
        .withJob(3, "j4", "j5")

      "assign the smallest queue, last position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 2, 1))
      }
    }

    "all queues full" should {

      val config = conf.copy(queueCapacity = 1)
      val schedule = empty.withJob(1, "j1").withJob(2, "j2")

      "assign new queue, first position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 3, 0))
      }
    }

    "all queues full, max queues reached" should {

      val config = conf.copy(maxQueues = 1, queueCapacity = 1)
      val schedule = empty.withJob(1, "j1").withJob(2, "j2")

      "assign existing queue, last position" in {
        val result = policy(testJob("j"), schedule, config)
        assert(result == JobScheduleEntryAdded(testJob("j"), 1, 1))
      }
    }

    "single active queue" should {

      val params = new AnyRef with JobParameters
      val config = conf
      val schedule = empty.withJobs(1, Job("j1", TestJobType, params))

      "reject equivalent job" in {
        val result = policy(Job("j2", TestJobType, params), schedule, config)
        assert(result == EquivalentJobFound("j2", "j1"))
      }
    }

  }

  implicit def state2schedule(state: JobSchedule.State): JobSchedule =
    JobSchedule(state)
}
