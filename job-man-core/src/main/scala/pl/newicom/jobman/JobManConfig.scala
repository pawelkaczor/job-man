package pl.newicom.jobman
import pl.newicom.jobman.execution.JobExecutionConfig
import pl.newicom.jobman.schedule.JobSchedulingConfig

case class JobManConfig(jobConfigRegistry: JobConfigRegistry,
                        schedulingConfig: JobSchedulingConfig,
                        executionConfig: JobExecutionConfig,
                        maxShards: Int,
                        journalSnapshotInterval: Int,
                        healthCheckNotificationsEnabled: Boolean)
