package com.example.calcbattle.user.actors

import akka.actor._
import akka.contrib.pattern.DistributedPubSubMediator.{Publish, Subscribe}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator, ClusterSharding, ShardRegion}
import akka.persistence.PersistentActor
import akka.routing.FromConfig
import com.example.calcbattle.user
import com.example.calcbattle.examiner

import scala.concurrent.duration._

object UserActor {

  def props(examiner: ActorRef) = Props(new UserActor(examiner))

  val userUpdatedTopic = "userUpdated"

  val nrOfShards = 60

  val idExtractor: ShardRegion.IdExtractor = {
    case msg @ user.api.Answer(uid, _, _, _) ⇒ (uid.underlying, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case msg @ user.api.Answer(uid, _, _, _) ⇒ (uid.hashCode % nrOfShards).toString
  }

  def startupClusterShardingOn(system: ActorSystem) = {

    val examiner =
      system.actorOf(FromConfig.props(), "examinerRouter")

    ClusterSharding(system).start(
      typeName      = "User",
      entryProps    = Some(props(examiner)),
      idExtractor   = idExtractor,
      shardResolver = shardResolver
    )
  }
}

class UserActor(examinerRouter: ActorRef) extends PersistentActor with ActorLogging {
  import UserActor._

  override def persistenceId: String = s"${self.path.parent.name}-${self.path.name}"

  val mediator = DistributedPubSubExtension(context.system).mediator

  var continuationCorrect = 0

  override def preStart() = {
    log.info("starting.")
    mediator ! Subscribe(userUpdatedTopic, self)
  }

  override def postStop() = {
    log.info("stopped.")
  }

  def updateState(event: user.api.Result): Unit = {
    if (event.answerIsCorrect) {
      continuationCorrect += 1
      mediator ! Publish(userUpdatedTopic, user.api.UserUpdated(event.uid, continuationCorrect))
    }
  }

  override def receiveRecover = {

    case event: user.api.Result => updateState(event)
  }

  override def receiveCommand = {

    case answer: user.api.Answer =>
      val result = user.api.Result(answer.uid, answer.isCorrect)
      persist(result)(updateState)
      sender() ! result

    case user.api.UserUpdated(uid, continuationCorrect) =>


  }

}
