package pl.newicom.jobman.execution

import pl.newicom.jobman.JobType

trait JobExecutionConfig {
  def maxWorkers: Int
  def parallelism(jobType: JobType): Int
}
