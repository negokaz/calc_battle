package com.example.calcbattle.examiner.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.example.calcbattle.examiner.api._

import scala.util.Random

object ExaminerWorker {

  def props() = Props(new ExaminerWorker)

  val name = "examinerWorker"

}

/**
  * 問題を作ります
  */
class ExaminerWorker extends Actor with ActorLogging {

  override def preStart() = {
    log.info("Hi, I'm a examiner.")
  }

  def receive = {

    case Create =>
      val left  = random()
      val right = random()
      sender() ! Question(left, right)

      log.info("Created a question: ({}, {})", left, right)
  }

  def random() = Random.nextInt(90) + 10

}
