package com.lightbend.configuration

object ApplicationParameters {
  val KAFKA_BROKER = "localhost:9092"

  val STORE_NAME = "ModelStore"
  val STORE_ID = 42


  val DATA_TOPIC = "mdata"
  val MODELS_TOPIC = "models"
  val SPECULATIVE_TOPIC = "speculative"

  val DATA_GROUP = "wineRecordsGroup"
  val MODELS_GROUP = "modelRecordsGroup"
  val SPECULATIVE_GROUP = "speculativeRecordsGroup"
}
