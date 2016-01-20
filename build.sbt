import sbt.Keys._

val namePrefix = "calc_battle"

lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-frontend""",
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator
  )
  .dependsOn(examinerApi)

lazy val examiner = (project in file("modules/examiner"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    )
  )
  .dependsOn(examinerApi)

lazy val examinerApi = (project in file("modules/examiner-api"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner-api"""
  )