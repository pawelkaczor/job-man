package pl.newicom.jobman

case class Job(id: String, params: JobParameters) {

  def canEqual(a: Any): Boolean = a.isInstanceOf[Job]

  override def equals(that: Any): Boolean =
    that match {
      case that: Job => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }


  override def hashCode:Int = {
    31  + id.hashCode
  }
}
