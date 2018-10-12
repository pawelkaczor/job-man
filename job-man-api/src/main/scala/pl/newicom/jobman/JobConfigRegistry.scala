package pl.newicom.jobman

trait JobConfigRegistry {
  def jobConfigs: Map[JobType, JobConfig]

  def jobConfig(jobType: JobType): JobConfig =
    jobConfigs(jobType)
}
