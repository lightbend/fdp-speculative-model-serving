package com.lightbend.scala.speculative.actor.queryablestate

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration._
import com.lightbend.scala.modelServer.model.ModelToServeStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport
import akka.pattern.ask
import com.lightbend.scala.modelServer.model.speculative.SpeculativeExecutionStats
import com.lightbend.scala.speculative.actor.actors._

object QueriesAkkaHttpResource extends JacksonSupport {

  implicit val askTimeout = Timeout(30.seconds)

  def storeRoutes(modelserver: ActorRef): Route =
    get {
      path("model"/Segment) { modelID =>
        onSuccess(modelserver ? GetModelServerState(modelID)) {
          case info: ModelToServeStats =>
            complete(info)
        }
      } ~
        path("models") {
          onSuccess(modelserver ? GetModels()) {
            case models: GetModelsResult =>
              complete(models)
          }
        } ~
        path("processor"/Segment) { dataType =>
          onSuccess(modelserver ? GetSpeculativeServerState(dataType)) {
            case info: SpeculativeExecutionStats =>
              complete(info)
          }
        }~
        path("processors") {
          onSuccess(modelserver ? GetDataProcessors()) {
            case processors: GetDataProcessorsResult =>
              complete(processors)
          }
        }
    }
}
