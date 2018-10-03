package pl.newicom.jobman

trait JobTypeRegistry {
  def jobType(jobParams: JobParameters): JobType
}
