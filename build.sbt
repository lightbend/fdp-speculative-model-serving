
scalaVersion in ThisBuild := "2.11.8"
// scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild := Seq("-Xexperimental", "-unchecked", "-deprecation", "-feature")
javaOptions in ThisBuild := Seq("Xlint:unchecked")

lazy val protobufs = (project in file("./protobufs"))
    .settings(
      PB.targets in Compile := Seq(
        scalapb.gen() -> (sourceManaged in Compile).value
      )
    )

lazy val client = (project in file("./client"))
  .settings(libraryDependencies ++=  Seq(Dependencies.kafka, Dependencies.curator))
  .dependsOn(protobufs, configuration)

lazy val model = (project in file("./model"))
  .settings(libraryDependencies ++= Dependencies.modelsDependencies)
  .dependsOn(protobufs)

lazy val akkaSpeculativeModelServing = (project in file("./akkaSpeculativeModelServing"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, configuration)

lazy val akkaSpeculativeModelServingDistributed = (project in file("./akkaSpeculativeModelServingDistributed"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.1",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  )
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, configuration)

lazy val configuration = (project in file("./configuration"))

lazy val SpeculativeModelServing = (project in file(".")).
  aggregate(protobufs, client, model, configuration, akkaSpeculativeModelServing, akkaSpeculativeModelServingDistributed)
