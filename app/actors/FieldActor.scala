package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import play.libs.Akka
import SocketActor._

object FieldActor {
  lazy val field = Akka.system().actorOf(Props[FieldActor])
  
  case class Result(isCorrect: Boolean)
  case class Subscribe(uid: UID)
}

class FieldActor extends Actor {
  import FieldActor._
  var users = Map[ActorRef, User]()

  def receive = {
    case Result(isCorrect) => {
      users.get(sender) match {
        case Some(user) => {
          val updateUser = user.copy(continuationCorrect = if (isCorrect) user.continuationCorrect + 1 else 0)
          val finish = updateUser.continuationCorrect >= 5
          users = users.updated(sender, updateUser)

          users.keys.foreach { userActor =>
            userActor ! SocketActor.UpdateUser(updateUser, finish)
          }
        }
        case None => {
          // 送信元のActorRefが存在しない場合
        }
      }
    }
    case Subscribe(uid: UID) => {
      users += (sender -> User(uid, 0))
      context watch sender
      
      val updateUsers = SocketActor.UpdateUsers(users.values.toSet)
      users.keys.foreach { userActor =>
        userActor ! updateUsers
      }
    }
    case Terminated(user) => {
      users -= user
      
      val updateUsers = SocketActor.UpdateUsers(users.values.toSet)
      users.keys.foreach { userActor =>
        userActor ! updateUsers
      }
    }
  }
}
