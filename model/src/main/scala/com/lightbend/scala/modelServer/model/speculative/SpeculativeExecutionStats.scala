package com.lightbend.scala.modelServer.model.speculative

/**
 * Created by boris on 5/8/17.
 */
final case class SpeculativeExecutionStats(
  name: String,
  decider : String,
  since: Long,
  tmout : Long,
  usage: Long,
  duration: Double,
  min: Long,
  max: Long,
  models : Map[String, Long]) {

  def incrementUsage(execution: Long, model : String): SpeculativeExecutionStats = {
    copy(
      usage = usage + 1,
      duration = duration + execution,
      min = if (execution < min) execution else min,
      max = if (execution > max) execution else max,
      models = updateModels(models, model)
    )
  }
  def updateConfig(t : Long, update :List[String]) = {
    copy(
      tmout = t,
      models = updateModels(models, update)
    )
  }

  private def updateModels(current : Map[String, Long], model : String) : Map[String, Long] =
    current.contains(model) match {
      case true => current + (model -> (current(model) + 1))
      case _ => current + (model -> 1)
    }

    private def updateModels(current : Map[String, Long], update : List[String]) : Map[String, Long] = {
    val cMap = collection.mutable.Map(current.toSeq: _*)
    current.keys.foreach(k => update.contains(k) match {
        case false => cMap -= k
        case _ =>
      }
    )
    update.foreach(k => current.contains(k) match {
        case false => cMap += (k -> 0)
        case _ =>
      }
    )
    Map(cMap.toIndexedSeq : _*)
  }
}

object SpeculativeExecutionStats{

  val empty = SpeculativeExecutionStats("", "", 0l, 0l, 0l, .0, 0l, 0l, Map[String, Long]())
  def apply(name: String, decider : String, tmout: Long, modelList: List[String], since: Long = System.currentTimeMillis(), usage: Long = 0,
            duration: Double = 0.0, min: Long = 0, max: Long = 0): SpeculativeExecutionStats = {
    val m = collection.mutable.Map[String,Long]()
    modelList.foreach(k => m += (k -> 0))
    new SpeculativeExecutionStats(name, decider, since, tmout, usage, duration, min, max,  Map(m.toIndexedSeq : _*))
  }
}