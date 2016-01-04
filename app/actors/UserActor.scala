package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{Json, JsValue}

object UserActor {
  def props(uid: String)(out: ActorRef) = Props(new UserActor(uid, FieldActor(), out))
}

case class UpdateUsers(user: Set[User])

class UserActor(uid: String, field: ActorRef, out: ActorRef) extends Actor {
  override def preStart() = {
    println("Log: UserActor#preStart")
    FieldActor() ! Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      println("Log: UserActor#receive JsValue")
      (js \ "result").validate[Boolean] map { field ! Result(uid, _) }
    }
    case Result(uid, isCorrect) if sender == field => {
      println("Log: UserActor#receive Result")
      val js = Json.obj("type" -> "result", "uid" -> uid, "isCorrect" -> isCorrect)
      out ! js
    }
    case UpdateUsers(users: Set[User]) if sender == field => {
      println("Log: UserActor#receive UpdateUsers")
      val toge = Map[String, Int]()
      users.map(u => toge + u.uid -> u.continuationCorrect)
      users.map(println(_))
      println("toge")
      toge + "1" -> 0
      println(toge)
      val js = Json.obj("type" -> "updateUsers", "uids" -> users.map(_.uid))
      out ! js
    }
    case other => {
      println("Log: UserActor#receive other")
    }
  }
}
