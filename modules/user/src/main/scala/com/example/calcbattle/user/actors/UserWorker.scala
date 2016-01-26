package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}
import akka.persistence.PersistentActor
import akka.routing.FromConfig
import com.example.calcbattle.user
import com.example.calcbattle.user.api.UID
import com.example.calcbattle.user.api.UserState

import scala.concurrent.duration._

object UserWorker {

  def props() = Props(new UserWorker())

  val userUpdatedTopic = "userUpdated"

  case class Reset()

}

class UserWorker extends PersistentActor with ActorLogging {
  import UserWorker._

  override def persistenceId: String = s"${self.path.parent.name}-${self.path.name}"

  /**
    * ユーザーの状態が更新されたのをクラスタ全体に通知する
    */
  val mediator = DistributedPubSub(context.system).mediator

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  val memberSetKey = ORSetKey[UID]("member-set")

  var continuationCorrect = 0

  var subscriber: Option[ActorRef] = None

  override def preStart() = {
    log.info("starting.")
    mediator   ! DistributedPubSubMediator.Subscribe(userUpdatedTopic, self)
    replicator ! Replicator.Subscribe(memberSetKey, self)
  }

  override def postStop() = {
    log.info("stopped.")
  }

  def updateState(event: user.api.Result): Unit = {
    if (event.answerIsCorrect) {
      continuationCorrect += 1
      val update = user.api.UserUpdated(UserState(event.uid, continuationCorrect))
      mediator ! DistributedPubSubMediator.Publish(userUpdatedTopic, update)
    }
  }

  def updateState(event: Reset.type): Unit = {
    continuationCorrect = 0
  }

  override def receiveRecover = {

    case event: user.api.Result => updateState(event)
    case event: Reset.type => updateState(event)
  }

  override def receiveCommand = {

    case user.api.Join(uid) =>
      val joinedUser = sender()
      log.info("User {} joined!", uid)
      context.watch(joinedUser)
      subscriber = Some(joinedUser)
      replicator ! Replicator.Update(memberSetKey, ORSet.empty[UID], Replicator.writeLocal, None) {
        _ + uid
      }

    case user.api.GetState(uid) =>
      log.info("getState sender: {}", sender())
      sender() ! user.api.UserState(uid, continuationCorrect)

    case c @ Replicator.Changed(`memberSetKey`) =>
      val data = c.get(memberSetKey)
      subscriber.foreach(_ ! user.api.MemberUpdated(data.elements))

    case answer: user.api.Answer =>
      val result = user.api.Result(answer.uid, answer.isCorrect)
      persist(result)(updateState)
      log.info("answer sender: {}", sender())
      sender() ! result

    case msg: user.api.UserUpdated =>
      if (msg.user.isCompleted) {
        self ! Reset
      }
      subscriber.foreach(_ ! msg)

    case reset: Reset.type =>
      persist(reset)(updateState)

    case Terminated(joinedUser) =>
      self ! PoisonPill

  }

}
