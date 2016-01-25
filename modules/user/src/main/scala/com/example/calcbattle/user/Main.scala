package com.example.calcbattle.user

import akka.actor.ActorSystem
import com.example.calcbattle.user.actors.{UserActor, UserWorker}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  args match {

    case Array(hostname, port) =>
      val config = ConfigFactory.parseString(
        s"""
           |akka.remote.netty.tcp.hostname = ${args(0)}
           |akka.remote.netty.tcp.port     = ${args(1)}
           |""".stripMargin
      ).withFallback(ConfigFactory.load())

      val system = ActorSystem("application", config)

      UserActor.startupSharding(system)

      system.actorOf(UserActor.props(), "user")

      Await.result(system.whenTerminated, Duration.Inf)

    case _ =>
      throw new IllegalArgumentException("引数には <ホスト名> <ポート番号> を指定してください。")
  }
}
