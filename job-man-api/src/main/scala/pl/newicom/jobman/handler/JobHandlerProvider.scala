package pl.newicom.jobman.handler

import pl.newicom.jobman.JobType

trait JobHandlerProvider {
  def jobHandler(jobType: JobType): AbstractJobHandler
}
