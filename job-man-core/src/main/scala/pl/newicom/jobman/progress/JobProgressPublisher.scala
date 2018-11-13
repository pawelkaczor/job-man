package pl.newicom.jobman.progress

import java.time.LocalTime.now

import pl.newicom.jobman.DistributedPubSubFacade
import pl.newicom.jobman.progress.event.JobProgressUpdated

class JobProgressPublisher(jobId: String, jobExecutionId: String, pubSub: DistributedPubSubFacade) extends JobProgressListener {

  def tasksStarted(nrOfTasks: Int): Unit =
    publishProgressUpdate(nrOfTasks)

  def tasksCompleted(nrOfTasks: Int): Unit =
    publishProgressUpdate(-nrOfTasks)

  private def publishProgressUpdate(nrOfTasksDiff: Int): Unit =
    publish(JobProgressUpdated(jobId, jobExecutionId, nrOfTasksDiff, now))

  private def publish(event: AnyRef): Unit =
    pubSub.publish(ProgressTopic.Name, event)

}
