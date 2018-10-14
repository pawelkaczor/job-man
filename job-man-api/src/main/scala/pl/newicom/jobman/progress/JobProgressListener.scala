package pl.newicom.jobman.progress

import java.util.function.Consumer

import pl.newicom.jobman.progress.JobProgressListener.Task

object JobProgressListener {
  type Task = AnyRef
}

trait JobProgressListener extends Consumer[Task] {
  def tasksStarted(nrOfTasks: Int): Unit
  def tasksCompleted(nrOfTasks: Int): Unit

  override def accept(t: Task): Unit = tasksCompleted(1)
}
