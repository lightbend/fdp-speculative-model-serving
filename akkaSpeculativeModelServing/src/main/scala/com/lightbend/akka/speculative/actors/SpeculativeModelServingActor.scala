package com.lightbend.akka.speculative.actors

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.lightbend.akka.speculative.persistence.FilePersistence
import com.lightbend.akka.speculative.processor.SimpleDesider
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelServer.model.ServingResult
import com.lightbend.modelServer.model.speculative.{ServingRequest, ServingResponse, SpeculativeExecutionStats}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

// Speculative model server manager for a given data type

class SpeculativeModelServingActor(dataType : String, tmout : Long, models : List[ActorRef]) extends Actor {

  val ACTORTIMEOUT = new FiniteDuration(100, TimeUnit.MILLISECONDS)
  val SERVERTIMEOUT = 100l

  println(s"Creating speculative model serving actor $dataType")
  private val modelProcessors = models.to[ListBuffer]
  implicit var askTimeout = Timeout(if(tmout <= 0) tmout else  SERVERTIMEOUT, TimeUnit.MILLISECONDS)
  val decider = SimpleDesider

  var state = SpeculativeExecutionStats(dataType, decider.getClass.getName, askTimeout.duration.length, getModelsNames())

  override def preStart {
    // Restore state from persistence
    val state = FilePersistence.restoreDataState(dataType)
    state._1.foreach(tmout => askTimeout = Timeout(if(tmout > 0) tmout else  SERVERTIMEOUT, TimeUnit.MILLISECONDS))
    state._2.foreach(models => {
      modelProcessors.clear()
      models.foreach(path => context.system.actorSelection(path).resolveOne(ACTORTIMEOUT).onComplete {
        case Success(ref) => modelProcessors += ref
        case _ =>
      }
    )})
  }

  override def receive = {
    // Model serving request
    case record : WineRecord =>
      val request = ServingRequest(UUID.randomUUID().toString, record)
      val zender = sender()
      val start = System.nanoTime()
      Future.sequence(
        // For every available model
         modelProcessors.toList.map(
           // Invoke model serving, map result and lift it to try
           ask(_,request)(askTimeout).mapTo[ServingResponse]).map(f => f.map(Success(_)).recover({case e => Failure(e)})))
          // collect all successful serving
         .map(_.collect{ case Success(x) => x})
          // Invoke decider
         .map(decider.decideResult(_)).mapTo[ServingResult]
          // Update stats
         .map(servingResult => {
           if(servingResult.processed)
             state = state.incrementUsage(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), servingResult.actor)
           servingResult
         })
         // respond
         .pipeTo(zender)
    // Current State request
    case request : GetSpeculativeServerState => sender() ! state
    // Configuration update
    case configuration : SetSpeculativeServer =>
      askTimeout = Timeout(if(configuration.tmout > 0) configuration.tmout else  SERVERTIMEOUT, TimeUnit.MILLISECONDS)
      modelProcessors.clear()
      modelProcessors ++= configuration.models
      state.updateConfig(askTimeout.duration.length, getModelsNames())
      FilePersistence.saveDataState(dataType, configuration.tmout, configuration.models)
      sender() ! "Done"
  }

  private def getModelsNames() : List[String] = modelProcessors.toList.map(_.path.name)
}

object SpeculativeModelServingActor{
  def props(dataType : String, tmout : Long, models : List[ActorRef]) : Props = Props(new SpeculativeModelServingActor(dataType, tmout, models))
}

case class SetSpeculativeServer(datatype : String, tmout : Long, models : List[ActorRef])

case class GetSpeculativeServerState(dataType : String)
