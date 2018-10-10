package pl.newicom.jobman.schedule.command

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.adapter._
import pl.newicom.jobman.Job
import pl.newicom.jobman.schedule.event.{JobCancellationResult, JobSchedulingResult}

trait JobScheduleCommand

case class ScheduleJob(job: Job, replyToUntyped: ActorRef) extends JobScheduleCommand {

  def replyTo: akka.actor.typed.ActorRef[JobSchedulingResult] =
    replyToUntyped.toTyped
}

case class CancelJob(jobId: String, replyToUntyped: ActorRef) extends JobScheduleCommand {

  def replyTo: akka.actor.typed.ActorRef[JobCancellationResult] =
    replyToUntyped.toTyped
}