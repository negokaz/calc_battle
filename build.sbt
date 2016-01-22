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
      "com.typesafe.akka" %% "akka-cluster" % "2.3.13",
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator
  )
  .dependsOn(userApi)

lazy val runSeed = TaskKey[Unit]("run-seed", "run one node as seed.")

lazy val userApi = (project in file("modules/user-api"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-user-api"""
  )

lazy val user = (project in file("modules/user"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-user""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.3.13",
      "com.typesafe.akka" %% "akka-contrib" % "2.3.13", // 2.4.x では akka-cluster-tools
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.13",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.user.Main", "127.0.0.1", "0")
  )
  .dependsOn(userApi, examinerApi)

lazy val examinerApi = (project in file("modules/examiner-api"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner-api"""
  )

lazy val examiner = (project in file("modules/examiner"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.3.13",
      "com.typesafe.akka" %% "akka-contrib" % "2.3.13", // 2.4.x では akka-cluster-tools
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.13",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "2552")
  )
  .dependsOn(examinerApi)