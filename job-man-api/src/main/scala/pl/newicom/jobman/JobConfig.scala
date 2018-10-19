package pl.newicom.jobman

import java.time.Duration

case class JobConfig(jobParamsClass: Class[JobParameters],
                     parallelism: Option[Int],
                     maxDuration: Duration,
                     maxTaskDuration: Duration,
                     onJobExpiredAction: Option[String] = None,
                     onJobTerminatedAction: Option[String] = None,
                     notifyOnSuccess: Boolean = false)
