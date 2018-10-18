package pl.newicom.jobman.execution

import pl.newicom.jobman.{JobConfigRegistry, JobType}

case class JobExecutionConfig(maxWorkers: Int, jobConfigRegistry: JobConfigRegistry) {
  def parallelism(jobType: JobType, defaultValue: Int): Int =
    jobConfigRegistry.jobConfig(jobType).parallelism.getOrElse(defaultValue)
}
