package com.lightbend.kafka.client

import com.lightbend.configuration.ApplicationParameters._
import com.lightbend.kafka.MessageListener

object DataReader {

  def main(args: Array[String]) {

    println(s"Using kafka brokers at ${KAFKA_BROKER}")

    val listener = MessageListener(KAFKA_BROKER, DATA_TOPIC, DATA_GROUP, new RecordProcessor())
    listener.start()
  }
}
