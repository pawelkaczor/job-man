package pl.newicom.jobman.execution.command

import pl.newicom.jobman.Job

trait JobExecutionCommand

case class StartJob(job: Job, queueId: Int, schedulingJournalOffset: Long) extends JobExecutionCommand
case class ConfirmJobActivity(jobId: String)                               extends JobExecutionCommand
case object ExpireOverrunningJobs                                          extends JobExecutionCommand
