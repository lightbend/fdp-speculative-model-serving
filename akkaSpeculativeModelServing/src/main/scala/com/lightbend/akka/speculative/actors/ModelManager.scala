package com.lightbend.akka.speculative.actors

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.modelServer.model.{ModelToServeStats, ModelWithDescriptor}


// Router actor, routing both model and data to an appropriate actor
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class ModelManager extends Actor {

  println(s"Creating Model manager")

  // This is just for testing
  def gen = ThreadLocalRandom.current()

  private def getModelServer(modelID: String): ActorRef =
    context.child(modelID).getOrElse(context.actorOf(ModelServingActor.props(modelID), modelID))

  private def getInstances : GetModelsResult =
    GetModelsResult(context.children.map(_.path.name).toSeq)

  override def receive = {
    // Redirect to model update. This only works for the local (in memory) invocation, because ModelWithDescriptor is not serializable
    case model: ModelWithDescriptor =>
      // This is just for testing
      val models = getInstances.models
      val modelServer = getModelServer(models(gen.nextInt(models.size)))
      modelServer forward model
    // Get State of model server
    case getState: GetModelServerState => {
      context.child(getState.ModelID) match {
        case Some(actorRef) => actorRef forward getState
        case _ => sender() ! ModelToServeStats.empty
      }
    }
    // Get current list of existing models
    case getModels : GetModels => sender() ! getInstances
    // Create actors from names. Support method for data processor configuration
    case createList : GetModelActors => sender() ! GetModelActorsResult(createList.models.map(getModelServer(_)))
  }
}

object ModelManager{
  def props : Props = Props(new ModelManager())
}

case class GetModels()

case class GetModelsResult(models : Seq[String])

case class GetModelActors(models : Seq[String])

case class GetModelActorsResult(models : Seq[ActorRef])