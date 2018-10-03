package pl.newicom.jobman

trait JobType {
  def jobParamsClass: Class[JobParameters]
}
