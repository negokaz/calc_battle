package controllers

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.routing.FromConfig
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue

import scala.concurrent.Future
import com.example.calcbattle.user

import actors.{SocketActor}

class Application @Inject()(system: ActorSystem) extends Controller {
  val UID = "uid"
  val counter: AtomicInteger = new AtomicInteger(0)

  def index = Action { implicit request =>
    val uid = request.session.get(UID).getOrElse {
      counter.incrementAndGet().toString
    }
    Ok(views.html.index(uid)).withSession {
      request.session + (UID -> uid)
    }
  }

  val examinerRouter = system.actorOf(FromConfig.props(), "examinerRouter")
  val userRouter     = system.actorOf(FromConfig.props(), "userRouter")

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful(request.session.get(UID) match {
      case None => Left(Forbidden)
      case Some(uid) => Right(SocketActor.props(new user.api.UID(uid), userRouter, examinerRouter))
    })
  }
}
