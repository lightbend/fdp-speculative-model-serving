package com.lightbend.modelServer.model

import java.io.{DataInputStream, DataOutputStream}

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.modelServer.model.tensorflow.TensorFlowModel

import scala.collection.Map
import com.lightbend.modelServer.model.PMML.PMMLModel

import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
case class ModelWithDescriptor(model: Model, descriptor: ModelToServe){}

object ModelWithDescriptor {

  private val factories = Map(
    ModelDescriptor.ModelType.PMML.name -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.name -> TensorFlowModel
  )

  private val factoriesInt = Map(
    ModelDescriptor.ModelType.PMML.index -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.index -> TensorFlowModel
  )

  def fromModelToServe(descriptor : ModelToServe): Try[ModelWithDescriptor] = Try{
    println(s"New model - $descriptor")
    factories.get(descriptor.modelType.name) match {
      case Some(factory) => ModelWithDescriptor(factory.create(descriptor),descriptor)
      case _ => throw new Throwable("Undefined model type")
    }
  }

  def readModel(input : DataInputStream) : Option[Model] = {
    input.readLong.toInt match{
      case length if length > 0 => {
        val `type` = input.readLong.toInt
        val bytes = new Array[Byte](length)
        input.read(bytes)
        factoriesInt.get(`type`) match{
          case Some(factory) => try {
            Some(factory.restore(bytes))
          }
          catch {
            case t: Throwable =>
              System.out.println("Error Deserializing model")
              t.printStackTrace()
              None
          }
          case _ => None
        }
      }
      case _ => None
    }
  }

  def writeModel(output : DataOutputStream, model: Model) : Unit = {
    if(model == null)
      output.writeLong(0l)
    else {
      try {
        val bytes = model.toBytes()
        output.writeLong(bytes.length)
        output.writeLong(model.getType)
        output.write(bytes)
      } catch {
        case t: Throwable =>
          System.out.println("Error Serializing model")
          t.printStackTrace()
      }
    }
  }
}
