package pl.newicom.jobman.schedule

case class JobSchedulingConfig(minQueues: Int, maxQueues: Int, queueCapacity: Int) {

  def withQueueCapacityBumped: JobSchedulingConfig =
    copy(queueCapacity = queueCapacity + 1)

}
