package com.lightbend.akka.speculative.persistence

import java.io._

import akka.actor.ActorRef
import com.lightbend.modelServer.model.{Model, ModelToServeStats, ModelWithDescriptor}

import scala.collection.mutable.ListBuffer

object FilePersistence {

  private final val basDir = "persistence"

  def restoreModelState(model : String) : (Option[Model], Option[ModelToServeStats]) = {
    getDataInputStream(s"model_$model") match {
      case Some(input) => (ModelWithDescriptor.readModel(input), ModelToServeStats.readServingInfo(input))
      case _ => (None, None)
    }
   }

  def restoreDataState(dataType : String) : (Option[Long], Option[List[String]]) = {
    getDataInputStream(s"data_$dataType") match {
      case Some(input) => {
        val tmout = input.readLong
        val nmodels = input.readLong.toInt
        val models = new ListBuffer[String]
        1 to nmodels foreach(_ => models += input.readUTF())
        (Some(tmout), Some(models.toList))
      }
      case _ => (None, None)
    }
  }

  private def getDataInputStream(fileName: String) : Option[DataInputStream] = {
    val file = new File(basDir + "/" + fileName)
    file.exists() match {
      case true => Some(new DataInputStream(new FileInputStream(file)))
      case _ => None
    }
  }

  def saveModelState(modelID : String, model: Model, servingInfo: ModelToServeStats) : Unit = {
    val output = getDataOutputStream(s"model_$modelID")
    ModelWithDescriptor.writeModel(output, model)
    ModelToServeStats.writeServingInfo(output, servingInfo)
    output.flush()
    output.close()
  }

  def saveDataState(dataType : String, timeout: Long, models: List[ActorRef]) : Unit = {
    val output = getDataOutputStream(s"data_$dataType")
    output.writeLong(timeout)
    output.writeLong(models.size)
    models.foreach(a => output.writeUTF(a.toString()))
    output.flush()
    output.close()
  }


  private def getDataOutputStream(fileName: String) : DataOutputStream = {

    val dir = new File(basDir)
    if(!dir.exists()) {
      dir.mkdir()
    }
    val file = new File(dir, fileName)
    if(!file.exists())
      file.createNewFile()
    new DataOutputStream(new FileOutputStream(file))
  }
}
