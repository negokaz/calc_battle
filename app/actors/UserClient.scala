package actors

import actors.UserClient.Status
import akka.actor._
import akka.cluster.ClusterEvent.{UnreachableMember, MemberUp}
import akka.cluster.{Cluster, Member}
import com.example.calcbattle.user.api._

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

object UserClient {

  def props() = Props(new UserClient())

  sealed trait Status
  case object Open extends Status
  case object Close extends Status

}

import UserClient._

class UserClient extends LoggingFSM[Status, Set[Member]] {

  val cluster = Cluster(context.system)

  val userRole = "user"

  startWith(Close, Set())

  override def preStart() = {
    cluster.subscribe(self, classOf[MemberUp], classOf[UnreachableMember])
  }

  override def postStop() = {
    cluster.unsubscribe(self)
  }

  when(Close) {

    case Event(MemberUp(member), members: Set[Member]) if member hasRole userRole =>
      goto(Open) using members + member

    case Event(UnreachableMember(member), _) =>
      stay()
  }

  when(Open) {

    case Event(MemberUp(member), members: Set[Member]) if member hasRole userRole =>
      stay() using members + member

    case Event(UnreachableMember(member), members: Set[Member]) if member hasRole userRole =>
      val newMembers = members - member
      if (newMembers.isEmpty) goto(Close) using newMembers else stay() using newMembers

    case Event(msg: Create.type, members: Set[Member]) =>
      val address = members.toIndexedSeq(ThreadLocalRandom.current().nextInt(members.size)).address
      val service = context.actorSelection(RootActorPath(address) / "user" / "examinerService")

      service.tell(msg, aggregator)
      stay()

    case Event(question: Question, member: Set[Member]) =>

      stay()
  }

  onTransition {

    case Close -> Open =>
      log.info("Attached ExaminerService.")

  }

  initialize()
}
