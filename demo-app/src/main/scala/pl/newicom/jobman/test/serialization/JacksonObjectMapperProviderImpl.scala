package pl.newicom.jobman.test.serialization

import akka.actor.{ActorRef, ExtendedActorSystem, Extension}
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import pl.newicom.jobman.test.serialization.JacksonObjectMapperProviderImpl.AkkaModule

object JacksonObjectMapperProviderImpl {

  class ActorRefSerializer(val as: ExtendedActorSystem) extends StdSerializer[ActorRef](classOf[ActorRef]) {
    override def serialize(value: ActorRef, gen: JsonGenerator, provider: SerializerProvider): Unit = {
      gen.writeString(value.path.toStringWithAddress(as.provider.getDefaultAddress))
    }
  }

  class ActorRefDeserializer(val as: ExtendedActorSystem) extends StdDeserializer[ActorRef](classOf[ActorRef]) {
    override def deserialize(p: JsonParser, ctx: DeserializationContext): ActorRef =
      as.provider.resolveActorRef(p.getValueAsString)
  }

  class AkkaModule(val _system: ExtendedActorSystem) extends SimpleModule {
    override def setupModule(context: Module.SetupContext): Unit = {
      addSerializer(classOf[ActorRef], new ActorRefSerializer(_system))
      addDeserializer(classOf[ActorRef], new ActorRefDeserializer(_system))
      super.setupModule(context)
    }
  }

}

class JacksonObjectMapperProviderImpl(actorSystem: ExtendedActorSystem) extends Extension {

  val mapper: ObjectMapper = new ObjectMapper()
    .registerModule(new ParameterNamesModule)
    .registerModule(new Jdk8Module)
    .registerModule(new JavaTimeModule)
    .registerModule(new AkkaModule(actorSystem))
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(Include.NON_NULL)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

  def objectMapper: ObjectMapper = mapper
}
