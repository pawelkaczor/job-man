package pl.newicom.jobman.execution.result

import pl.newicom.jobman.JobType

case class JobResultMessage(jobId: String, jobType: JobType, report: String) extends SuccessfulJobResult
