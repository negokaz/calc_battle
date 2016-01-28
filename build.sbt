import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._

val namePrefix = "calc_battle"

lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.7"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-frontend""",
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      // ↓使っていないので本来は不要なはずだが、消すと Cluster に参加できなくなる
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j"   % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator,

    maintainer in Docker := "",
    dockerBaseImage := "java:8-jre",
    dockerExposedPorts ++= Seq(2550, 9000),
    dockerEntrypoint := Seq("/bin/sh", "-c",
      s"""HOST_IP=`ip addr show scope global | grep 'inet' | grep -Eo '[0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+'`
          PRIMARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          PRIMARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          SECONDARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          SECONDARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          bin/${name.value} -Dconfig.resource=docker.conf $$*
      """.linesIterator.foldLeft("")(_ + _)
    )
  )
  .dependsOn(userApi, examinerApi)

lazy val runSeed = TaskKey[Unit]("run-seed", "run one node as seed.")

lazy val userApi = (project in file("modules/user-api"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-user-api"""
  )

lazy val user = (project in file("modules/user"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-user""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.1",
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.1",
      "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.7",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "org.iq80.leveldb"          % "leveldb"        % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.user.LocalMain", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.user.LocalMain", "127.0.0.1", "2551"),

    mainClass in Compile := Some("com.example.calcbattle.user.Main"),
    maintainer in Docker := "",
    dockerBaseImage := "java:8-jre",
    dockerExposedPorts := Seq(2550),
    dockerEntrypoint := Seq("/bin/sh", "-c",
      s"""HOST_IP=`ip addr show scope global | grep 'inet' | grep -Eo '[0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+'`
          PRIMARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          PRIMARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          SECONDARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          SECONDARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          bin/${name.value} -Dconfig.resource=docker.conf $$*
      """.linesIterator.foldLeft("")(_ + _)
    )
  )
  .dependsOn(userApi)

lazy val examinerApi = (project in file("modules/examiner-api"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner-api"""
  )

lazy val examiner = (project in file("modules/examiner"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      "ch.qos.logback" % "logback-classic" % "1.1.3"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.examiner.LocalMain", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.examiner.LocalMain", "127.0.0.1", "2552"),

    mainClass in Compile := Some("com.example.calcbattle.examiner.Main"),
    maintainer in Docker := "",
    dockerBaseImage := "java:8-jre",
    dockerExposedPorts := Seq(2550),
    dockerEntrypoint := Seq("/bin/sh", "-c",
      s"""HOST_IP=`ip addr show scope global | grep 'inet' | grep -Eo '[0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+'`
          PRIMARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          PRIMARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          SECONDARY_SEED_PORT_2550_TCP_ADDR=$${PRIMARY_SEED_PORT_2550_TCP_ADDR:-$${HOST_IP}}
          SECONDARY_SEED_PORT_2550_TCP_PORT=$${PRIMARY_SEED_PORT_2550_TCP_PORT:-2550}
          bin/${name.value} -Dconfig.resource=docker.conf $$*
      """.stripMargin.linesIterator.foldLeft("")(_ + _)
    )
  )
  .dependsOn(examinerApi)