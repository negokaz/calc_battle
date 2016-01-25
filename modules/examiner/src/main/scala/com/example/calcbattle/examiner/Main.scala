package com.example.calcbattle.examiner

import akka.actor.{ActorSystem}
import com.example.calcbattle.examiner.actors.{ExaminerActor}
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

      system.actorOf(ExaminerActor.props(), ExaminerActor.name)

      Await.result(system.whenTerminated, Duration.Inf)

    case _ =>
      throw new IllegalArgumentException("引数には <ホスト名> <ポート番号> を指定してください。")
  }
}
