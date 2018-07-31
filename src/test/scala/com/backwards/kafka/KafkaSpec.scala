package com.backwards.kafka

import org.scalatest.{MustMatchers, WordSpec}
import com.lightbend.kafka.scala.streams.StreamsBuilderS
import com.backwards.kafka.avro.Implicits._

/**
  * Topic: key -> value or more specifically Array[Byte] -> Array[Byte]
  * A topic in Kafka is an unbounded sequence of key-value pairs. Keys and values are raw byte array
  * e.g.
  * 00100 -> 111010,      11000 -> 000011,      00100 -> 1010101
  *
  * + schema gives:
  *
  * Stream:
  * A stream is a topic with a schema. Keys and values are no longer byte arrays but have specific types
  * e.g.
  * alice -> Paris,       bob -> Sydney,        alice -> Rome
  *
  * + aggregation gives:
  *
  * Table:
  * A table is an aggregated stream
  * e.g.
  *                                             alice : 2
  *                                             bob   : 1
  */
class KafkaSpec extends WordSpec with MustMatchers {
  def toBytes(xs: (Array[Int], Array[Int])): (Array[Byte], Array[Byte]) = xs._1.map(_.toByte) -> xs._2.map(_.toByte)

  case class MyMessage()

  "" should {
    "" in {
      // Scala analogy
      val topic: Seq[(Array[Byte], Array[Byte])] = Seq(Array(97, 108, 105, 99, 101) -> Array(80, 97, 114, 105, 115),
                                                       Array(98, 111, 98) -> Array(83, 121, 100, 110, 101, 121),
                                                       Array(97, 108, 105, 99, 101) -> Array(82, 111, 109, 101),
                                                       Array(98, 111, 98) -> Array(76, 105, 109, 97),
                                                       Array(97, 108, 105, 99, 101) -> Array(66, 101, 114, 108, 105, 110)).map(toBytes)

      // We now read the topic into a stream by adding schema information (schema-on-read). In other words, we are turning the raw, untyped topic into a “typed topic” aka stream.

      // In Scala this is achieved by the map() operation below. In this example, we end up with a stream of [String, String] pairs. Notice how we can now see what’s in the data.

      // Scala analogy
      val stream: Seq[(String, String)] = topic.map { case (k: Array[Byte], v: Array[Byte]) =>
        new String(k) -> new String(v)
      }
      // stream: Seq[(String, String)] = List(alice -> Paris, bob -> Sydney, alice -> Rome, bob -> Lima, alice -> Berlin)
      println(stream)

      // In Kafka Streams you read a topic into a KStream via StreamsBuilder#stream().
      // Here, you must define the desired schema via the Consumed.with() parameter for reading the topic’s data:
      // StreamsBuilder builder = new StreamsBuilder();
      // KStream<String, String> stream = builder.stream("input-topic", Consumed.with(Serdes.String(), Serdes.String()));
      val builder = new StreamsBuilderS

      val myMessageStream = builder
        .stream[String, MyMessage]("my-topic")

      // Now read the same topic into a table.
      // First, we need to add schema information (schema-on-read).
      // Second, we must convert the stream into a table.
      // The table semantics in Kafka say that the resulting table must map every message key in the topic to the latest message value for that key.

      // Scala analogy
      val table = topic
        .map { case (k: Array[Byte], v: Array[Byte]) => new String(k) -> new String(v) }
        .groupBy(_._1)
        .map { case (k, v) => (k, v.reduceLeft((aggV, newV) => newV)._2) }
      // table: scala.collection.immutable.Map[String,String] =
      //        Map(alice -> Berlin, bob -> Lima)
      println(table)

      // We can build the table directly from the stream, which allows us to skip the schema/type definition because the stream is already typed.
      // We can see now that a table is a derivation, an aggregation of a stream.

      // Scala analogy simplified
      val tableFromStream = stream
        .groupBy(_._1)
        .map { case (k, v) => (k, v.reduceLeft((aggV, newV) => newV)._2) }

      // In Kafka Streams you’d normally use StreamsBuilder#table() to read a Kafka topic into a KTable with a simple 1-liner:
      // KTable<String, String> table = builder.table("input-topic", Consumed.with(Serdes.String(), Serdes.String()));

      val myMessageTable = builder.table[String, MyMessage]("my-other-topic")
      // If we had used "my-topic" again, we would get the error:
      // org.apache.kafka.streams.errors.TopologyException: Invalid topology: Topic my-topic has already been registered by another source.

      // But, for the sake of illustration, you can also read the topic into a KStream first, and then perform the same aggregation step as shown above explicitly to turn the KStream into a KTable.
      // KStream<String, String> stream = ...;
      // KTable<String, String> table = stream
      //                                .groupByKey()
      //                                .reduce((aggV, newV) -> newV);

      // In summary - a table is actually an aggregated stream.
    }
  }
}