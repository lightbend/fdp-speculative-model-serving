/**
  * Created by boris on 7/14/17.
  */
import sbt._
import Versions._

object Dependencies {
  val reactiveKafka  = "com.typesafe.akka"              %% "akka-stream-kafka"        % reactiveKafkaVersion

  val akkaStream          = "com.typesafe.akka"         %% "akka-stream"              % akkaVersion
  val akkaHttp            = "com.typesafe.akka"         %% "akka-http"                % akkaHttpVersion
  val akkaHttpJsonJackson = "de.heikoseeberger"         %% "akka-http-jackson"        % akkaHttpJsonVersion

  val kafkastreams  = "org.apache.kafka"                %  "kafka-streams"            % kafkaVersion
  val kafkastreamsScala  = "com.lightbend"              %% "kafka-streams-scala"      % KafkaScalaVersion

  val kafka         = "org.apache.kafka"                %% "kafka"                    % kafkaVersion
  val kafkaclients  = "org.apache.kafka"                %  "kafka-clients"            % kafkaVersion

  val curator       = "org.apache.curator"              % "curator-test"              % Curator                 // ApacheV2


  val tensorflow    = "org.tensorflow"                  % "tensorflow"                % tensorflowVersion

  val jpmml         = "org.jpmml"                       % "pmml-evaluator"            % PMMLVersion
  val jpmmlextras   = "org.jpmml"                       % "pmml-evaluator-extension"  % PMMLVersion


  val modelsDependencies    = Seq(jpmml, jpmmlextras, tensorflow)
  val kafkabaseDependencies = Seq(reactiveKafka) ++ Seq(kafkaclients)
  val akkHTTPPSupport       = Seq(akkaHttp, akkaHttpJsonJackson)
  val akkaServerDependencies = Seq(akkaStream, akkaHttp, akkaHttpJsonJackson, reactiveKafka)
  val kafkaServerDependencies = Seq(kafkastreams, kafkastreamsScala, akkaHttp, akkaHttpJsonJackson)

}
