package pl.newicom.jobman

case class JobConfigRegistry(jobConfigs: Map[JobType, JobConfig]) {

  def jobConfig(jobType: JobType): JobConfig =
    jobConfigs(jobType)

  def jobType(paramsClass: Class[_ <: JobParameters]): JobType =
    jobConfigs
      .find { case (_, jobConfig) => jobConfig.jobParamsClass == paramsClass }
      .map(_._1)
      .get

  def jobType2ParamMap: Map[JobType, Class[_ <: JobParameters]] =
    jobConfigs.mapValues(_.jobParamsClass)
}
