package com.lightbend.scala.speculative.actor.distributed.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.ModelWithDescriptor
import akka.pattern.ask
import akka.util.Timeout
import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global


// Router actor, routing both model and data to an appropriate actors
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class ModelServingManager extends Actor {

  println(s"Creating Model Serving manager")

  implicit val askTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  // Create support actors
  val modelManager = context.actorOf(ModelManager.props, "modelManager")
  val dataManager = context.actorOf(DataManager.props, "dataManager")

  override def receive = {
    // Model methods
    // Update model
    case model: ModelWithDescriptor => modelManager forward model
    // Get list of model servers
    case getModels : GetModels => modelManager forward getModels
    // Get state of the model
    case getState: GetModelServerState => modelManager forward getState
    // Convert model list into list of model servers
    case getModelServersList : GetModelActors => modelManager forward getModelServersList

    // Data methods
    // Configure Data actor
    case configuration : SetSpeculativeServerCollector =>
       ask(self, GetModelActors(configuration.models)).mapTo[GetModelActorsResult]
        .map(actors => SetSpeculativeServer(configuration.datatype, configuration.tmout, configuration.models, actors.models.toList))
        .pipeTo(dataManager)

    // process data
    case record: WineRecord => dataManager forward record
    // Get state of speculative executor
    case getState: GetSpeculativeServerState => dataManager forward getState
    // Get List of data processors
    case getProcessors : GetDataProcessors => dataManager forward getProcessors
  }
}

object ModelServingManager{
  def props : Props = Props(new ModelServingManager())
}
