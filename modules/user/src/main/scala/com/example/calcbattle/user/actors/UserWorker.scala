package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.WriteMajority
import akka.cluster.ddata._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.sharding.ShardRegion.Passivate
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}
import akka.persistence.{SnapshotOffer, PersistentActor}
import akka.routing.FromConfig
import com.example.calcbattle.user
import com.example.calcbattle.user.api.UID
import com.example.calcbattle.user.api.UserState

import scala.concurrent.duration._

object UserWorker {

  def props() = Props(new UserWorker())

  val userUpdatedTopic = "userUpdated"

  case class Reset()

  case class Terminate()

  case class Joined(uid: UID, subscriber: ActorRef)

}

class UserWorker extends PersistentActor with ActorLogging {
  import UserWorker._

  override def persistenceId: String = s"${self.path.parent.name}-${self.path.name}"

  /**
    * ユーザーの状態が更新されたのイベントをuserクラスタ全体に通知するのに使う
    */
  val mediator = DistributedPubSub(context.system).mediator

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  /**
    * ゲームに参加している全ユーザーのUIDをuserクラスタで共有するためのキー
    */
  val memberSetKey = ORSetKey[UID]("member-set")

  /**
    * 正解数
    */
  var continuationCorrect = 0

  /**
    * 同じブラウザで複数のタブを開いたとき(複数の Actor が Join してきたとき)に、
    * タブが残っているのにこの Actor を Stop してしまわないように、
    * 全タブの状態を死活監視できるように Set にしている。
    */
  var subscribers: Set[ActorRef] = Set()

  var uid: Option[UID] = None

  override def preStart() = {
    log.info("starting. {}", persistenceId)
    mediator   ! DistributedPubSubMediator.Subscribe(userUpdatedTopic, self)
    replicator ! Replicator.Subscribe(memberSetKey, self)
  }

  override def postStop() = {
    log.info("stopped.")
  }

  override def receiveRecover = {
    case event: Joined          => updateState(event)
    case event: user.api.Result => updateState(event)
    case Reset                  => updateState(Reset)
    case event: Terminated      => updateState(event)
  }

  def updateState(event: Joined): Unit = {
    log.info("User {} joined.", event.uid)
    context.watch(event.subscriber)
    subscribers += event.subscriber
    uid = Some(event.uid)
    replicator ! Replicator.Update(memberSetKey, ORSet.empty[UID], Replicator.writeLocal, None) {
      _ + event.uid
    }
  }

  def updateState(event: user.api.Result): Unit = {
    val previousContinuationCorrect = continuationCorrect
    continuationCorrect = if (event.answerIsCorrect) continuationCorrect + 1 else 0
    uid.foreach { uid =>
      log.info("User {} answered: {} -> {}", uid, previousContinuationCorrect, continuationCorrect)
    }
    val update = user.api.UserUpdated(UserState(event.uid, continuationCorrect))
    mediator ! DistributedPubSubMediator.Publish(userUpdatedTopic, update)
  }

  def updateState(event: Reset.type): Unit = {
    continuationCorrect = 0
  }

  def updateState(event: Terminated): Unit = {
    log.info("terminated: {}", event.actor)
    subscribers -= event.actor
    if (subscribers.isEmpty) {
      context.parent ! Passivate(stopMessage = Terminate)
    }
  }

  override def receiveCommand = {

    case msg: user.api.Join =>
      val subscriber = sender()
      persist(Joined(msg.uid, subscriber))(updateState)

    case user.api.GetState(uid) =>
      sender() ! user.api.UserState(uid, continuationCorrect)

    case c@Replicator.Changed(`memberSetKey`) =>
      val data = c.get(memberSetKey)
      subscribers.foreach(_ ! user.api.MemberUpdated(data.elements))

    case answer: user.api.Answer =>
      val result = user.api.Result(answer.uid, answer.isCorrect)
      persist(result)(updateState)
      sender() ! result

    case msg: user.api.UserUpdated =>
      if (msg.user.isCompleted) {
        self ! Reset
      }
      subscribers.foreach(_ ! msg)

    case Reset =>
      persist(Reset)(updateState)

    case terminated: Terminated =>
      persist(terminated)(updateState)

    case Terminate =>
      uid.foreach { uid=>
        replicator ! Replicator.Update(memberSetKey, ORSet.empty[UID], Replicator.writeLocal, None) {
          _ - uid
        }
      }
      context.stop(self)
  }

}
