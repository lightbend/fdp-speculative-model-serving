package com.lightbend.modelServer.model.speculative

trait Decider {

  def decideResult(results: List[ServingResponse]): Any
}
