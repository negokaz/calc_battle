package actors

import akka.actor._
import play.api.libs.json.{JsString, Writes, Json, JsValue}
import com.example.calcbattle.examiner
import com.example.calcbattle.user
import com.example.calcbattle.user.api.{UserState, UID}

import scala.concurrent.duration._

object SocketActor {
  def props(uid: UID, userRouter: ActorRef, examinerRouter: ActorRef)(out: ActorRef) =
    Props(new SocketActor(uid, userRouter, examinerRouter, out))

  case object RefreshQuestion
  case object RefreshMembers

  case class Answer(questionA: Int, questionB: Int, userInput: Int)

  implicit val uidWrites = new Writes[UID] {
    def writes(uid: UID): JsValue = JsString(uid.underlying)
  }
  implicit val userWrites = Json.writes[UserState]
  implicit val questionWrites = Json.writes[examiner.api.Question]
  implicit val resultWrites = Json.writes[user.api.Result]
  implicit val answerReads = Json.reads[Answer]

}

import SocketActor._
class SocketActor(uid: UID, userRouter: ActorRef, examinerRouter: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  var members: Set[UID] = Set()

  override def preStart() = {
    userRouter ! user.api.Join(uid)
  }

  def receive = {

    case js: JsValue =>
      (js \ "answer").validate[Answer].foreach { ans =>
        userRouter ! user.api.Answer(uid, ans.questionA, ans.questionB, ans.userInput)
        self ! RefreshQuestion
      }

      (js \ "start").validate[Boolean].foreach { start =>
        if (start) {
          self ! RefreshQuestion
        }
      }

    case RefreshQuestion =>
      log.info("refreshing question...")
      val handler = context.actorOf(QuestionHandler.props(replyTo = self))
      examinerRouter.tell(examiner.api.Create, handler)

    case q: examiner.api.Question =>
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question

    case QuestionHandler.QuestionReceiveTimeout =>
      log.warning("問題が取得できませんでした。再試行します。")
      import context.dispatcher
      context.system.scheduler.scheduleOnce(1 second, self, RefreshQuestion)

    case result: user.api.Result =>
      val js = Json.obj("type" -> "result", "result" -> result)
      out ! js

    case user.api.UserUpdated(user) =>
      val js = Json.obj("type" -> "updateUser", "user" -> user, "finish" -> user.isCompleted)
      out ! js

    case user.api.MemberUpdated(_members) =>
      members = _members
      self ! RefreshMembers

    case RefreshMembers =>
      val handler = context.actorOf(MemberStateHandler.props(members.size, replyTo = self))
      members.foreach { uid =>
        userRouter.tell(user.api.GetState(uid), handler)
      }

    case MemberStateHandler.MemberStates(users) =>
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js

    case MemberStateHandler.MemberStateGetTimeout =>
      log.warning("参加者の情報が全員分取得できませんでした。再試行します。")
      import context.dispatcher
      context.system.scheduler.scheduleOnce(1 second, self, RefreshMembers)
  }

}

object MemberStateHandler {
  def props(memberSize: Int, replyTo: ActorRef) = Props(new MemberStateHandler(memberSize, replyTo))

  case class MemberStates(states: Set[user.api.UserState])
  case object MemberStateGetTimeout
}

class MemberStateHandler(memberSize: Int, replyTo: ActorRef) extends Actor {
  import MemberStateHandler._

  context.setReceiveTimeout(5 seconds)

  var userStates: Set[user.api.UserState] = Set()

  def receive = {

    case state: user.api.UserState =>
      context.setReceiveTimeout(1 second)
      userStates += state
      if (userStates.size == memberSize) {
        replyTo ! MemberStates(userStates)
        context.stop(self)
      }

    case e: ReceiveTimeout =>
      replyTo ! MemberStateGetTimeout
      context.stop(self)

  }
}

object QuestionHandler {
  def props(replyTo: ActorRef) = Props(new QuestionHandler(replyTo))

  case object QuestionReceiveTimeout
}

class QuestionHandler(replyTo: ActorRef) extends Actor {
  import QuestionHandler._

  context.setReceiveTimeout(5 seconds)

  def receive = {

    case q: examiner.api.Question =>
      replyTo ! q
      context.stop(self)

    case e: ReceiveTimeout =>
      replyTo ! QuestionReceiveTimeout
      context.stop(self)

  }
}