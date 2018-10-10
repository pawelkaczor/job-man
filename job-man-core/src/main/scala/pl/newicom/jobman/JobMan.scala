package pl.newicom.jobman

import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.ActorMaterializer

trait JobMan {

  def config: JobManConfig

  def readJournal: EventsByPersistenceIdQuery

  def actorMaterializer(errorMsg: String): ActorMaterializer
}
