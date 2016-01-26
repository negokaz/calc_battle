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
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.0",
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j"   % "2.4.1",
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator
  )
  .dependsOn(userApi, examinerApi)

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
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.0",
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.1",
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.1",
      "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.user.Main", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.user.Main", "127.0.0.1", "2551")
  )
  .dependsOn(userApi)

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
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.0",
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "0"),
      fullRunTask(runSeed, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "2552")
  )
  .dependsOn(examinerApi)