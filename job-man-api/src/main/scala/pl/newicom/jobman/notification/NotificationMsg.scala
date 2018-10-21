package pl.newicom.jobman.notification

import pl.newicom.jobman.JobType

trait NotificationMsg {
  def jobId: String
  def jobType: JobType
  def subject: String
  def content: String
}
