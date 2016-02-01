package controllers

import akka.routing.FromConfig
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue

import scala.concurrent.Future
import com.example.calcbattle.user

import actors.{SocketActor}

class Application extends Controller {

  val uidKey = "uid"

  def index = Action { implicit request =>
    val uid = request.session.get(uidKey).getOrElse {
      // ブラウザ上で中途半端な位置で自動改行されないように"-"を消しておく
      java.util.UUID.randomUUID().toString.replace("-", "")
    }
    Ok(views.html.index(uid)).withSession {
      request.session + (uidKey -> uid)
    }
  }

  val examinerRouter = Akka.system.actorOf(FromConfig.props(), "examinerRouter")
  val userRouter     = Akka.system.actorOf(FromConfig.props(), "userRouter")

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful {
      request.session.get(uidKey) match {
        case Some(uid) =>
          Right(SocketActor.props(user.api.UID(uid), userRouter, examinerRouter))
        case None =>
          Left(Forbidden)
      }
    }
  }
}
