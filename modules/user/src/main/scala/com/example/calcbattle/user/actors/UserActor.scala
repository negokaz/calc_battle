package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Props}
import akka.contrib.pattern.{ClusterSharding, ShardRegion}
import akka.persistence.PersistentActor
import com.example.calcbattle.user
import com.example.calcbattle

object UserActor {

  def props(examiner: ActorRef) = Props(new UserActor(examiner))

  val nrOfShards = 60

  val idExtractor: ShardRegion.IdExtractor = {
    case msg @ user.api.Answer(uid, _, _, _) ⇒ (uid.underlying, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case msg @ user.api.Answer(uid, _, _, _) ⇒ (uid.hashCode % nrOfShards).toString
  }

  def startupClusterShardingOn(system: ActorSystem) = {

    val examiner =
      system.actorOf(ExaminerClient.props(), ExaminerClient.name)

    ClusterSharding(system).start(
      typeName      = "User",
      entryProps    = Some(props(examiner)),
      idExtractor   = idExtractor,
      shardResolver = shardResolver
    )
  }
}

class UserActor(examiner: ActorRef) extends PersistentActor with ActorLogging {

  override def persistenceId: String = s"${self.path.parent.name}-${self.path.name}"

  var continuationCorrect = 0

  def updateState(event: user.api.Result): Unit = {
    if (event.answerIsCorrect)
      continuationCorrect += 1
  }

  override def receiveRecover = {

    case event: user.api.Result => updateState(event)
  }

  override def receiveCommand = {

    case answer: user.api.Answer =>
      val result = user.api.Result(answer.isCorrect)

      persist(result)(updateState)
      sender() ! result
      examiner ! examiner.api.

  }

}
