package pl.newicom.jobman.test.serialization

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.fasterxml.jackson.databind.ObjectMapper

class JacksonJsonSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  val objectMapper: ObjectMapper = JacksonObjectMapperProvider.get(system).objectMapper

  override def identifier: Int =
    234629837

  override def manifest(o: AnyRef): String =
    o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] =
    objectMapper.writeValueAsBytes(o)

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    objectMapper.readValue(bytes, Class.forName(manifest)).asInstanceOf[AnyRef]

}
