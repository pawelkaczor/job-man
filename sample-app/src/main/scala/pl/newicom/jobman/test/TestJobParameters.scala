package pl.newicom.jobman.test

import com.fasterxml.jackson.annotation.{JsonAnyGetter, JsonAnySetter, JsonIgnore}
import pl.newicom.jobman.JobParameters

import scala.collection.mutable

object TestJobParameters {
  val TestJobType = "Test"
}

case class TestJobParameters(params: mutable.Map[String, Any]) extends JobParameters {

  @JsonAnyGetter
  def any: mutable.Map[String, Any] =
    params

  @JsonAnySetter
  def set(name: String, value: Any): Unit =
    params.put(name, value)

  @JsonIgnore
  def get[T](name: String): Option[T] =
    params.get(name).map(_.asInstanceOf[T])

  def locks: Set[String] =
    get[String]("locks").map(_.split(',').toSet).getOrElse(Set.empty)

}
