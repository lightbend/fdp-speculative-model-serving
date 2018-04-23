package com.lightbend.akka.speculative.distributed.queryablestate

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.lightbend.akka.speculative.distributed.actors._
import com.lightbend.modelServer.model.ModelToServeStats
import com.lightbend.modelServer.model.speculative.SpeculativeExecutionStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport

import scala.concurrent.duration._

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
