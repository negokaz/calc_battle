package com.example.calcbattle.user.actors

import akka.actor.{ActorSystem, Props, ActorLogging, Actor}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.sharding.{ClusterShardingSettings, ShardRegion, ClusterSharding}
import com.example.calcbattle.user

object UserActor {

  def props() = Props(new UserActor)

  val shardRegionName = "User"

  val nrOfShards = 50

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ user.api.Join(uid)     => (uid.underlying, msg)
    case msg @ user.api.GetState(uid) => (uid.underlying, msg)
    case msg @ user.api.Answer(uid, _, _, _) => (uid.underlying, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg @ user.api.Join(uid)     => (uid.hashCode % nrOfShards).toString
    case msg @ user.api.GetState(uid) => (uid.hashCode % nrOfShards).toString
    case msg @ user.api.Answer(uid, _, _, _) => (uid.hashCode % nrOfShards).toString
  }

  def startupSharding(system: ActorSystem) = {

    ClusterSharding(system).start(
      typeName    = shardRegionName,
      entityProps = UserWorker.props(),
      settings    = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId  = extractShardId
    )
  }

}

class UserActor extends Actor with ActorLogging {
  import UserActor._

  val shardRegion = ClusterSharding(context.system).shardRegion(shardRegionName)

  def receive = {
    case msg =>
      shardRegion forward msg
  }

}
