package com.example.calcbattle.user

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import com.example.calcbattle.user.actors.{ExaminerClient, UserActor}
import com.typesafe.config.ConfigFactory

class Main extends App {

  args match {

    case Array(hostname, port) =>
      val config = ConfigFactory.parseString(
        s"""
           |akka.remote.netty.tcp.hostname = ${args(0)}
           |akka.remote.netty.tcp.port     = ${args(1)}
           |""".stripMargin
      ).withFallback(ConfigFactory.load())

      val system = ActorSystem("application", config)

      UserActor.startupClusterShardingOn(system)

      system.awaitTermination()

    case _ =>
      throw new IllegalArgumentException("引数には <ホスト名> <ポート番号> を指定してください。")
  }
}
