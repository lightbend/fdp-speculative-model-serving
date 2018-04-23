package com.lightbend.akka.speculative.distributed.modelserver

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.lightbend.akka.speculative.distributed.actors.{ModelServingManager, SetSpeculativeServerCollector}
import com.lightbend.akka.speculative.distributed.queryablestate.QueriesAkkaHttpResource
import com.lightbend.configuration.ApplicationParameters._
import com.lightbend.modelServer.model.{DataRecord, ModelToServe, ModelWithDescriptor, ServingResult}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by boris on 7/21/17.
 */
object AkkaModelServer {

  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val askTimeout = Timeout(30.seconds)

  println(s"Using kafka brokers at ${KAFKA_BROKER} ")

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(DATA_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(KAFKA_BROKER)
    .withGroupId(MODELS_GROUP)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelserver = system.actorOf(ModelServingManager.props)

  def main(args: Array[String]): Unit = {

    // Create necessary actors
    val models = List("model1", "model2", "model3")
    ask(modelserver, SetSpeculativeServerCollector("wine", 100, models)).onComplete{
      case Success(r) => println("Data Server initialized")
      case _ =>
    }
    // Stabilize
    Thread.sleep(500)

    println("Starting Kafka readers")

    // Model stream processing
    Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(MODELS_TOPIC))
      .map(record => ModelToServe.fromByteArray(record.value)).collect { case Success(a) => a }
      .map(record => ModelWithDescriptor.fromModelToServe(record)).collect { case Success(a) => a }
      .mapAsync(1)(elem => modelserver ? elem)
      .runWith(Sink.ignore) // run the stream, we do not read the results directly

    // Data stream processing
    Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(DATA_TOPIC))
      .map(record => DataRecord.fromByteArray(record.value)).collect { case Success(a) => a }
      .mapAsync(1)(elem => (modelserver ? elem).mapTo[ServingResult])
      .runForeach(result => {
        result.processed match {
          case true => println(s"Calculated quality - ${result.result} by actor ${result.actor}. Calculated in ${result.duration} ms")
          case _ => println ("No model available - skipping")
        }
      })

    // Rest Server
    startRest(modelserver)
  }

  def startRest(modelserver: ActorRef): Unit = {

    implicit val timeout = Timeout(10.seconds)
    val host = "127.0.0.1"
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
        case ex =>
          println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
      }
  }
}
