package com.example.calcbattle.examiner.actors

import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.FromConfig

import com.example.calcbattle.examiner.api._

object ExaminerService {

  def props() = Props(new ExaminerService())

  val name = "examinerService"
}

class ExaminerService extends Actor with ActorLogging {

  val workerRouter =
    context.actorOf(FromConfig.props(ExaminerWorker.props()), name = "workerRouter")

  def receive = {

    case msg: Create.type =>
      workerRouter forward msg

  }

}
