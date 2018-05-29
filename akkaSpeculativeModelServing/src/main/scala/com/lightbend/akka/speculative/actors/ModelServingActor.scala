package com.lightbend.akka.speculative.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import com.lightbend.akka.speculative.persistence.FilePersistence
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.speculative.{ServingRequest, ServingResponse}
import com.lightbend.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor, ServingResult}

import scala.util.Random

// Workhorse - doing model serving for a given data type/ modeltype

class ModelServingActor(modelID : String) extends Actor {

  println(s"Creating model serving actor $modelID")
  private var currentModel: Option[Model] = None
  private var newModel: Option[Model] = None
  private var currentState: Option[ModelToServeStats] = None
  private var newState: Option[ModelToServeStats] = None
  // For testing
  private val gen = new Random()

  override def preStart : Unit = {
    val state = FilePersistence.restoreModelState(modelID)
    newState = state._2
    newModel = state._1
  }

  override def receive = {

    // Update Model. This only works for the local (in memory) invocation, because ModelWithDescriptor is not serializable
    case model : ModelWithDescriptor =>
      // Update model
      println(s"Model Server $modelID has a new model $model")
      newState = Some(ModelToServeStats(model.descriptor))
      newModel = Some(model.model)
      FilePersistence.saveModelState(modelID, newModel.get, newState.get)
      sender() ! "Done"
    // Process data
    case record : ServingRequest =>
      // Process data
      newModel.foreach { model =>
        // Update model
        // close current model first
        currentModel.foreach(_.cleanup())
        // Update model
        currentModel = newModel
        currentState = newState
        newModel = None
      }

      currentModel match {
        case Some(model) =>
          val start = System.nanoTime()
          val quality = model.score(record.data.asInstanceOf[WineRecord]).asInstanceOf[Double]
           // Just for testing
          Thread.sleep(gen.nextInt(20)*10l)
          val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
//          println(s"Calculated quality - $quality calculated in $duration ms")
          currentState = currentState.map(_.incrementUsage(duration))
          sender() ! ServingResponse(record.GUID, ServingResult(modelID, quality, duration))

        case _ =>
//          println("No model available - skipping")
          sender() ! ServingResponse(record.GUID, ServingResult.noModel)
      }
    // Get current state
    case request : GetModelServerState => {
      // State query
      sender() ! currentState.getOrElse(ModelToServeStats.empty)
    }
  }
}

object ModelServingActor{
  def props(modelID : String) : Props = Props(new ModelServingActor(modelID))
}

case class GetModelServerState(ModelID : String)
