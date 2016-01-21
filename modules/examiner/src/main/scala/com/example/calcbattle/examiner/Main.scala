package com.example.calcbattle.examiner

import akka.actor.{ActorSystem}
import com.example.calcbattle.examiner.actors.{ExaminerService, ExaminerWorker}
import com.typesafe.config.ConfigFactory

object Main extends App {

  val config =
    ConfigFactory.parseString(
      s"""
         |akka.remote.netty.tcp.hostname = ${args(0)}
         |akka.remote.netty.tcp.port     = ${args(1)}
         |""".stripMargin
    ).withFallback(ConfigFactory.load())

  val system = ActorSystem("application", config)

  system.actorOf(ExaminerWorker.props(), ExaminerWorker.name)
  system.actorOf(ExaminerService.props(), ExaminerService.name)

  system.awaitTermination()
}
