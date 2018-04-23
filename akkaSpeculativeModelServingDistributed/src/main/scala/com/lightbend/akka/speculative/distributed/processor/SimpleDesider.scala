package com.lightbend.akka.speculative.distributed.processor

import com.lightbend.modelServer.model.ServingResult
import com.lightbend.modelServer.model.speculative.{DeciderTrait, ServingResponse}

object SimpleDesider extends DeciderTrait {

  // The simplest decider returning the first result
  override def decideResult(results: List[ServingResponse]): Any = {

    var result = ServingResult.noModel
    results.foreach(res => res.result.asInstanceOf[ServingResult] match {
      case r if(r.processed) => result = r
      case _ =>
    })
    result
  }
}
