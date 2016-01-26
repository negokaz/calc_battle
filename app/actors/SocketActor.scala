package actors

import akka.pattern.ask
import akka.pattern.pipe
import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.util.Timeout
import play.api.libs.json.{JsString, Writes, Json, JsValue}
import com.example.calcbattle.examiner
import com.example.calcbattle.user
import com.example.calcbattle.user.api.{UserState, UID}

import scala.concurrent.Future
import scala.concurrent.duration._

object SocketActor {
  def props(uid: UID, userRouter: ActorRef, examinerRouter: ActorRef)(out: ActorRef) =
    Props(new SocketActor(uid, userRouter, examinerRouter, out))

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

  override def preStart() = {
    userRouter ! user.api.Join(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "answer").validate[Answer].foreach { ans =>
        userRouter ! user.api.Answer(uid, ans.questionA, ans.questionB, ans.userInput)
      }
      examinerRouter ! examiner.api.Create
    }
    case q: examiner.api.Question => {
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question
    }
    case result: user.api.Result => {
      val js = Json.obj("type" -> "result", "result" -> result)
      out ! js
    }
    case user.api.UserUpdated(user) => {
      val js = Json.obj("type" -> "updateUser", "user" -> user, "finish" -> user.isCompleted)
      out ! js
    }
    case user.api.MemberUpdated(member) => {

      implicit val timeout = Timeout(5 seconds)
      import context.dispatcher

      Future.sequence {
        member.map { uid =>
          (userRouter ? user.api.GetState(uid)).map { case state: user.api.UserState => state }
        }
      } pipeTo self
    }

    case users: Set[user.api.UserState] =>
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js

  }
}
