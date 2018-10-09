package pl.newicom.jobman

trait JobConfig {
  def jobParamsClass: Class[JobParameters]
}
