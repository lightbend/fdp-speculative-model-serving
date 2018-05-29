package com.lightbend.akka.speculative.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.ModelWithDescriptor
import com.lightbend.speculative.speculativedescriptor.SpeculativeDescriptor

import scala.concurrent.ExecutionContext.Implicits.global


// Router actor, routing both model and data to an appropriate actors
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class ModelServingManager extends Actor {

  implicit val askTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  println(s"Creating Model Serving manager")

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

    // Data methods
    // Configure Data actor
    case configuration : SpeculativeDescriptor =>
      val replyTo = sender()
      ask(modelManager, GetModelActors(configuration.models)).mapTo[GetModelActorsResult]
        .map(actors => SetSpeculativeServer(configuration.datatype, configuration.tmout, actors.models.toList))
        .pipeTo(dataManager)(replyTo)

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