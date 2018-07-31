package com.backwards.kafka.avro

import java.util
import scala.language.implicitConversions
import scala.reflect.ClassTag
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serdes, Serializer}
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.{Consumed, KeyValue}
import com.lightbend.kafka.scala.streams._
import com.sksamuel.avro4s._
import com.twitter.bijection.Injection
import com.twitter.bijection.avro.GenericAvroCodecs

object Implicits extends ImplicitConversions with DefaultSerdes {
  implicit def serde[T: ClassTag: SchemaFor: ToRecord: FromRecord]: Serde[T] = new Implicits[T]

  class Implicits[T: ClassTag: SchemaFor: ToRecord: FromRecord] extends Serde[T] {
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
}

trait ImplicitConversions {
  implicit def wrapKStream[K, V](inner: KStream[K, V]): KStreamS[K, V] =
    new KStreamS[K, V](inner)

  implicit def wrapKGroupedStream[K, V](inner: KGroupedStream[K, V]): KGroupedStreamS[K, V] =
    new KGroupedStreamS[K, V](inner)

  implicit def wrapSessionWindowedKStream[K, V](inner: SessionWindowedKStream[K, V]): SessionWindowedKStreamS[K, V] =
    new SessionWindowedKStreamS[K, V](inner)

  implicit def wrapTimeWindowedKStream[K, V](inner: TimeWindowedKStream[K, V]): TimeWindowedKStreamS[K, V] =
    new TimeWindowedKStreamS[K, V](inner)

  implicit def wrapKTable[K, V](inner: KTable[K, V]): KTableS[K, V] =
    new KTableS[K, V](inner)

  implicit def wrapKGroupedTable[K, V](inner: KGroupedTable[K, V]): KGroupedTableS[K, V] =
    new KGroupedTableS[K, V](inner)

  implicit def tuple2ToKeyValue[K, V](tuple: (K, V)): KeyValue[K, V] = new KeyValue(tuple._1, tuple._2)

  //scalastyle:on null
  // we would also like to allow users implicit serdes
  // and these implicits will convert them to `Serialized`, `Produced` or `Consumed`

  implicit def serializedFromSerde[K, V](implicit keySerde: Serde[K], valueSerde: Serde[V]): Serialized[K, V] =
    Serialized.`with`(keySerde, valueSerde)

  implicit def consumedFromSerde[K, V](implicit keySerde: Serde[K], valueSerde: Serde[V]): Consumed[K, V] =
    Consumed.`with`(keySerde, valueSerde)

  implicit def producedFromSerde[K, V](implicit keySerde: Serde[K], valueSerde: Serde[V]): Produced[K, V] =
    Produced.`with`(keySerde, valueSerde)

  implicit def joinedFromKVOSerde[K, V, VO](implicit keySerde: Serde[K], valueSerde: Serde[V],
                                            otherValueSerde: Serde[VO]): Joined[K, V, VO] =
    Joined.`with`(keySerde, valueSerde, otherValueSerde)
}

trait DefaultSerdes {
  implicit val stringSerde: Serde[String] = Serdes.String()
  implicit val longSerde: Serde[Long] = Serdes.Long().asInstanceOf[Serde[Long]]
  implicit val byteArraySerde: Serde[Array[Byte]] = Serdes.ByteArray()
  implicit val bytesSerde: Serde[org.apache.kafka.common.utils.Bytes] = Serdes.Bytes()
  implicit val floatSerde: Serde[Float] = Serdes.Float().asInstanceOf[Serde[Float]]
  implicit val doubleSerde: Serde[Double] = Serdes.Double().asInstanceOf[Serde[Double]]
  implicit val integerSerde: Serde[Int] = Serdes.Integer().asInstanceOf[Serde[Int]]
}