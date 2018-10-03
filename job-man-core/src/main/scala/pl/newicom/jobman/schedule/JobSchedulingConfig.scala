package pl.newicom.jobman.schedule

trait JobSchedulingConfig {
  def getMinQueues: Int

  def getMaxQueues: Int

  def getQueueCapacity: Int

  def withQueueCapacityBumped: JobSchedulingConfig

}
