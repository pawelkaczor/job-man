package pl.newicom.jobman

case class JobConfigRegistry(jobConfigs: Map[JobType, JobConfig]) {
  def jobConfig(jobType: JobType): JobConfig =
    jobConfigs(jobType)
}
