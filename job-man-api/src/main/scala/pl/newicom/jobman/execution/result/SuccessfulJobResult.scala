package pl.newicom.jobman.execution.result

trait SuccessfulJobResult extends JobResult {
  override def isSuccess: Boolean = true
}
