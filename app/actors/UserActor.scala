package actors

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import play.api.libs.json.{Writes, Json, JsValue}
import com.example.calcbattle.examiner.api._

object UserActor {
  def props(uid: UID, examiner: ActorRef)(out: ActorRef) = Props(new UserActor(uid, examiner, FieldActor.field, out))
  
  case class UpdateUsers(users: Set[User])
  case class UpdateUser(user: User, finish: Boolean)
  class UID(val id: String) extends AnyVal
  case class User(uid: UID, continuationCorrect: Int)
  
  implicit val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(user.uid.id -> user.continuationCorrect)
    }
  }
  implicit val usersWrites = new Writes[Set[User]] {
    def writes(users: Set[User]): JsValue = {
      Json.toJson(users.map { user: User =>
        user.uid.id -> user.continuationCorrect
      }.toMap)
    }
  }
  implicit val questionWrites = Json.writes[Question]
}

import UserActor._
class UserActor(uid: UID, examiner: ActorRef, field: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  override def preStart() = {
    FieldActor.field ! FieldActor.Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "result").validate[Boolean] foreach {
        field ! FieldActor.Result(_)
      }
      examiner ! Create
    }
    case q: Question => {
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question
    }
    case UpdateUser(user, finish) if sender == field => {
      val js = Json.obj("type" -> "updateUser", "user" -> user, "finish" -> finish)
      out ! js
    }
    case UpdateUsers(users) if sender == field => {
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js
    }
  }
}
