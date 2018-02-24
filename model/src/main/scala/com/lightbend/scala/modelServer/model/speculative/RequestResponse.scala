package com.lightbend.scala.modelServer.model.speculative

import com.lightbend.scala.modelServer.model.ServingResult

import scala.util.Random

// Because we are doing everything in memory, we implement local equivalent to protobufs

case class ServingRequest(GUID : String, data : Any)

case class ServingQualifier(key : String, value : String)

case class ServingResponse(GUID : String, result : Any, confidence : Option[Double], qualifiers : List[ServingQualifier])

object ServingResponse{

  val gen = Random
  val qualifiers = List(ServingQualifier("key", "value"))

  def apply(GUID: String,  result: ServingResult): ServingResponse = {
    new ServingResponse(GUID, result, Some(gen.nextDouble()), qualifiers)
  }
}