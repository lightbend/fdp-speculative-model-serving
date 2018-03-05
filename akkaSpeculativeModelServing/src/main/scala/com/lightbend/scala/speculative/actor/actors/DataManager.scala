package com.lightbend.scala.speculative.actor.actors

import akka.actor.{Actor, ActorRef, Props}
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.scala.modelServer.model.speculative.SpeculativeExecutionStats
import com.lightbend.scala.modelServer.model.ServingResult


// Router actor, routing both model and data to an appropriate actor
// Based on http://michalplachta.com/2016/01/23/scalability-using-sharding-from-akka-cluster/

class DataManager extends Actor {

  println(s"Creating Data manager")

  private def getDataServer(dataType: String): Option[ActorRef] = context.child(dataType)

  private def createDataServer(dataType: String, tmout : Long, models : List[ActorRef]) : ActorRef=
    context.actorOf(SpeculativeModelServingActor.props(dataType, tmout, models), dataType)

  private def getInstances : GetDataProcessorsResult =
    GetDataProcessorsResult(context.children.map(_.path.name).toSeq)


  override def receive = {

    // Configuration update
    case configuration : SetSpeculativeServer =>
      getDataServer(configuration.datatype) match {
        case Some(actor) => actor forward configuration                                           // Update existing one
        case _ =>
          createDataServer(configuration.datatype, configuration.tmout, configuration.models)     // Create the new one
          sender() ! "Done"
      }
    // process data record
    case record: WineRecord => getDataServer(record.dataType) match {
      case Some(actor) => actor forward record
      case _ =>  sender() ! ServingResult.noModel
    }
    // Get current state
    case getState: GetSpeculativeServerState => {
      getDataServer(getState.dataType) match {
        case Some(actorRef) => actorRef forward getState
        case _ => sender() ! SpeculativeExecutionStats.empty
      }
    }
    // Get List of data processors
    case getProcessors : GetDataProcessors => sender() ! getInstances
  }
}

object DataManager{
  def props : Props = Props(new DataManager())
}

case class GetDataProcessors()

case class GetDataProcessorsResult(processors : Seq[String])
