package pl.newicom.jobman

case class JobManConfig(minQueues: Int,
                        maxQueues: Int,
                        queueCapacity: Int,
                        maxWorkers: Int,
                        maxShards: Int,
                        journalSnapshotInterval: Int,
                        healthCheckNotificationsEnabled: Boolean) {}
