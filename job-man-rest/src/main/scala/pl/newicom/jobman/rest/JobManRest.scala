package pl.newicom.jobman.rest

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, RejectionHandler, Route}
import akka.pattern.Patterns.askWithReplyTo
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import pl.newicom.jobman.Job
import pl.newicom.jobman.rest.JobManRest.JobRejection
import pl.newicom.jobman.schedule.JobCancellationResult
import pl.newicom.jobman.schedule.command.{CancelJob, JobScheduleCommand, ScheduleJob}
import pl.newicom.jobman.schedule.error.{JobAlreadyDispatchedForExecution, JobNotFound}
import pl.newicom.jobman.schedule.event._

import scala.concurrent.{ExecutionContext, Future}

object JobManRest {
  case class JobRejection(equivalentId: String) extends Rejection
}

class JobManRest(jobScheduler: ActorRef[JobScheduleCommand])(implicit mat: Materializer, ex: ExecutionContext) {

  import JacksonSupport._

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case JobRejection(equivalentId) =>
          complete((BadRequest, EquivalentJobFoundResponse(equivalentId)))
      }
      .result()

  def route(): Route =
    pathPrefix("job-man")(scheduleRoute)

  def scheduleRoute: Route =
    pathPrefix("schedule") {
      put(entity(as[Job])(scheduleJobRoute)) ~ path(Segment)(cancelJobRoute)
    }

  def scheduleJobRoute(job: Job): Route =
    onSuccess(handleScheduleRequest(job)) {
      case EquivalentJobFound(_, equivalentJobId) =>
        reject(JobRejection(equivalentJobId))

      case JobAddedToWaitingList(_, position) =>
        complete(JobAddedToWaitingListResponse(job.id, position))

      case JobScheduleEntryAdded(_, queueId, position) =>
        complete(JobScheduledResponse(job.id, queueId, position))
    }

  def cancelJobRoute(jobId: String): Route =
    onSuccess(handleCancelRequest(jobId)) {
      case _: JobCanceled =>
        complete(JobCanceledResponse(jobId))

      case _: JobAlreadyDispatchedForExecution =>
        complete((Conflict, "Job already dispatched for execution"))

      case _: JobNotFound =>
        complete((NotFound, "Job does not exist in the current schedule"))
    }

  def handleScheduleRequest(job: Job): Future[JobSchedulingResult] =
    askWithReplyTo(jobScheduler.toUntyped, ScheduleJob(job, _), 5000).mapTo[JobSchedulingResult]

  def handleCancelRequest(jobId: String): Future[JobCancellationResult] =
    askWithReplyTo(jobScheduler.toUntyped, CancelJob(jobId, _), 5000).mapTo[JobCancellationResult]

}
