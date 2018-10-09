package pl.newicom.jobman

trait JobConfigRegistry {
  def jobConfigs: Map[JobType, JobConfig]

  def jobType(jobParams: JobParameters): JobType =
    jobConfigs
      .find(_._2.jobParamsClass.isAssignableFrom(jobParams.getClass))
      .map(_._1)
      .get
}
