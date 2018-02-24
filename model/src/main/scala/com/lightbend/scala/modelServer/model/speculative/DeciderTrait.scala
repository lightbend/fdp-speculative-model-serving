package com.lightbend.scala.modelServer.model.speculative

trait DeciderTrait {

  def decideResult(results: List[ServingResponse]): Any
}
