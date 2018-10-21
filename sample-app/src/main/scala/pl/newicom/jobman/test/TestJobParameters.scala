package pl.newicom.jobman.test

import pl.newicom.jobman.JobParameters

object TestJobParameters {
  val TestJobType = "Test"
}

case class TestJobParameters(taskExecutionTimeSecs: Int = 1,
                             nrOfTasks: Int = 1,
                             simulateJobFailure: Option[Boolean] = None,
                             locks: Set[String] = Set.empty)
    extends JobParameters {}
