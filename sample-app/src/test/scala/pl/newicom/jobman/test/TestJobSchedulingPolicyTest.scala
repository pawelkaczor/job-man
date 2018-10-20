package pl.newicom.jobman.test

import org.scalatest.WordSpec
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.JobAddedToWaitingList
import pl.newicom.jobman.schedule.{JobSchedule, JobSchedulingConfig}
import pl.newicom.jobman.test.TestJobParameters.TestJobType

import scala.collection.mutable

class TestJobSchedulingPolicyTest extends WordSpec {

  val policy = new TestJobSchedulingPolicy
  implicit val conf: JobSchedulingConfig =
    JobSchedulingConfig(minQueues = 1, maxQueues = 10, queueCapacity = 10, Map(TestJobType -> classOf[TestJobParameters]))
  val empty = TestJobSchedule()

  "Test job scheduling policy" should {

    val schedule = empty
      .withJobs(1, testJob("j1", Map("locks" -> "l1")))
      .withJobs(2, testJob("j2", Map("locks" -> "l2")))

    "add job to a waiting list if conflicting jobs detected on multiple queues" in {

      val j3 = testJob("j3", Map("locks" -> "l1,l2"))

      val result = policy(j3, schedule)

      assert(result == JobAddedToWaitingList(j3, 0))

    }
  }

  def testJob(jobId: String, params: Map[String, Any]): Job =
    Job(jobId, TestJobType, TestJobParameters(mutable.Map(params.toSeq: _*)))

  implicit def state2schedule(state: JobSchedule.State): JobSchedule =
    JobSchedule(state)

}
