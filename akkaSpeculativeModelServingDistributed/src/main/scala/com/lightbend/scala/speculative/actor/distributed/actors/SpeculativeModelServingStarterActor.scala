package com.lightbend.scala.speculative.actor.distributed.actors

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.speculative.ServingRequest
import com.lightbend.scala.speculative.actor.distributed.persistence.FilePersistence

import scala.collection.mutable.ListBuffer
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import akka.pattern.ask
import akka.util.Timeout
import com.lightbend.scala.speculative.actor.distributed.modelserver.AkkaModelServer

// Speculative model server manager for a given data type

class SpeculativeModelServingStarterActor(dataType : String,  models : List[ActorRef], collector : ActorRef) extends Actor {

  implicit val askTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  println(s"Creating speculative model serving starter actor $dataType with models $models and collector $collector")

  private val modelProcessors = models.to[ListBuffer]

  override def preStart {
    val state = FilePersistence.restoreDataState(dataType)
    state._2 match {
      case Some(models) =>
        modelProcessors.clear()
        ask(AkkaModelServer.modelserver, GetModelActors(models)).mapTo[GetModelActorsResult].onComplete {
          case Success(servers) => modelProcessors ++= servers.asInstanceOf[GetModelActorsResult].models
          case _ =>
        }
      case _ =>   // Do nothing
    }
  }

  override def receive = {
    // Model serving request
    case record : WineRecord =>
      val request = ServingRequest(UUID.randomUUID().toString, record)
      collector ! StartSpeculative(request.GUID, System.nanoTime(), sender(), modelProcessors.size)
      modelProcessors.foreach( _ tell(request, collector))
    // Configuration update
    case configuration : SetSpeculativeServerStarter =>
      modelProcessors.clear()
      modelProcessors ++= configuration.models
  }

  private def getModelsNames() : List[String] = modelProcessors.toList.map(_.path.name)
}

object SpeculativeModelServingStarterActor{
  def props(dataType : String, models : List[ActorRef], collector : ActorRef) : Props = Props(new SpeculativeModelServingStarterActor(dataType,  models, collector))
}

case class SetSpeculativeServerStarter(datatype : String, models : List[ActorRef])
