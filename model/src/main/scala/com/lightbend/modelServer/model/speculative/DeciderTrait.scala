package com.lightbend.modelServer.model.speculative

trait DeciderTrait {

  def decideResult(results: List[ServingResponse]): Any
}
