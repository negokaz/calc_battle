package actors

import actors.ExaminerClient.Status
import akka.actor._
import akka.cluster.ClusterEvent.{UnreachableMember, MemberUp}
import akka.cluster.{Cluster, Member}
import com.example.calcbattle.examiner.api._

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

object ExaminerClient {

  def props() = Props(new ExaminerClient())

  sealed trait Status
  case object Open extends Status
  case object Close extends Status

}

import ExaminerClient._

class ExaminerClient extends LoggingFSM[Status, Set[Member]] {

  val cluster = Cluster(context.system)

  val examinerRole = "examiner"

  startWith(Close, Set())

  override def preStart() = {
    cluster.subscribe(self, classOf[MemberUp], classOf[UnreachableMember])
  }

  override def postStop() = {
    cluster.unsubscribe(self)
  }

  when(Close) {

    case Event(MemberUp(member), members: Set[Member]) if member hasRole examinerRole =>
      goto(Open) using members + member

    case Event(UnreachableMember(member), _) =>
      stay()
  }

  when(Open) {

    case Event(MemberUp(member), members: Set[Member]) if member hasRole examinerRole =>
      stay() using members + member

    case Event(UnreachableMember(member), members: Set[Member]) if member hasRole examinerRole =>
      val newMembers = members - member
      if (newMembers.isEmpty) goto(Close) using newMembers else stay() using newMembers

    case Event(msg: Create.type, members: Set[Member]) =>
      val address = members.toIndexedSeq(ThreadLocalRandom.current().nextInt(members.size)).address
      val service = context.actorSelection(RootActorPath(address) / "user" / "examinerService")
      val aggregator =
        context.actorOf(ExaminerAggregator.props(sender()))

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


  object ExaminerAggregator {

    def props(replyTo: ActorRef) = Props(new ExaminerAggregator(replyTo))
  }

  class ExaminerAggregator(replyTo: ActorRef) extends Actor with ActorLogging {

    context.setReceiveTimeout(5 seconds)

    def receive = {

      case question: Question =>
        replyTo ! question
        context.stop(self)

      case ReceiveTimeout =>
        log.error("5秒間応答がありませんでした")
        context.stop(self)
    }

  }
}
