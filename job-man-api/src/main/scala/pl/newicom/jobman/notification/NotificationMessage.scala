package pl.newicom.jobman.notification

import pl.newicom.jobman.JobType

trait NotificationMsg {
  def jobId: String
  def jobType: JobType
  def subject: String
  def content: String
}

case class MailNotification(jobId: String,
                            jobType: JobType,
                            subject: String,
                            content: String,
                            to: List[String],
                            cc: List[String] = List.empty)
    extends NotificationMsg
