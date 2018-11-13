package pl.newicom.jobman.test.serialization
import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

object JacksonObjectMapperProvider extends ExtensionId[JacksonObjectMapperProviderImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): JacksonObjectMapperProviderImpl =
    new JacksonObjectMapperProviderImpl(system)

  override def lookup(): ExtensionId[_ <: Extension] =
    JacksonObjectMapperProvider

  override def get(system: ActorSystem): JacksonObjectMapperProviderImpl =
    super.get(system)
}
