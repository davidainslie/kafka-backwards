package com.backwards.kafka.avro

import java.util
import scala.reflect.ClassTag
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import com.sksamuel.avro4s._
import com.twitter.bijection.Injection
import com.twitter.bijection.avro.GenericAvroCodecs

object GenericAvroSerde {
  def apply[T: ClassTag: SchemaFor: ToRecord: FromRecord]: GenericAvroSerde[T] = new GenericAvroSerde
}

class GenericAvroSerde[T: ClassTag: SchemaFor: ToRecord: FromRecord] extends Serde[T] {
  val schema: Schema = AvroSchema[T]

  val format: RecordFormat[T] = RecordFormat[T]

  val injection: Injection[GenericRecord, Array[Byte]] =
    GenericAvroCodecs toBinary schema

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serializer(): Serializer[T] = new Serializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def serialize(topic: String, data: T): Array[Byte] =
      injection(format to data)

    override def close(): Unit = {}
  }

  override def deserializer(): Deserializer[T] = new Deserializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def deserialize(topic: String, data: Array[Byte]): T =
      getInverted(data)

    override def close(): Unit = {}
  }

  override def close(): Unit = {}

  private def getInverted(data: Array[Byte]): T =
    if (data == null) null.asInstanceOf[T]
    else format.from(injection.invert(data).get)
}