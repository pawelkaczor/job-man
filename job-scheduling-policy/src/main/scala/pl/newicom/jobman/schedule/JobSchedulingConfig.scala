package pl.newicom.jobman.schedule
import pl.newicom.jobman.{JobParameters, JobType}

case class JobSchedulingConfig(minQueues: Int, maxQueues: Int, queueCapacity: Int, jobTypes: Map[JobType, Class[_ <: JobParameters]]) {

  def withQueueCapacityBumped: JobSchedulingConfig =
    copy(queueCapacity = queueCapacity + 1)

  def jobType(paramsClass: Class[_ <: JobParameters]): JobType =
    jobTypes.find { case (_, p) => p == paramsClass }.map(_._1).get
}
