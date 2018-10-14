package pl.newicom.jobman.notification

import pl.newicom.jobman.JobType

case class NotificationMessage(jobId: String,
                               jobType: JobType,
                               content: String,
                               jobCompletedSuccessfully: Boolean,
                               jobDescription: Option[String]) {}
