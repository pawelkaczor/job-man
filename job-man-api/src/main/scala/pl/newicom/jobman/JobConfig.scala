package pl.newicom.jobman

import java.time.Duration

trait JobConfig {
  def jobParamsClass: Class[JobParameters]
  def maxDuration: Duration
  def maxTaskDuration: Duration
  def onJobExpiredAction: String
  def onJobTerminatedAction: String
  def notifyOnSuccess: Boolean
}
