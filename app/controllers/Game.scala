package controllers

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import controllers.Game.{Connect, Leftgame, Ping, StateUpdate}
import controllers.WebsocketIn.NegotiationMessage
import play.api.Logger
import play.api.libs.json.JsValue

import scala.collection.mutable
import scala.concurrent.duration.Duration

object Game {
  def props(roomId: String) = Props(new Game(roomId))

  case class Connect(peer: String)

  case class Ping()

  case class Leftgame(player: ActorRef)


  case class StateUpdate(key: String, value: JsValue)
}

class Game(roomId: String) extends Actor {
  val state = mutable.ListMap[String, JsValue]()

  implicit val exec = context.dispatcher
  context.system.scheduler.schedule(Duration.Zero, Duration.create(30, "second"), self, Ping())

  override def receive = {
    case StateUpdate(key, value) => state += (key -> value)
    case message@NegotiationMessage(_, to, _, _) => context.child(to).foreach {
      _ ! message
    }
    case Ping() =>
      context.children.foreach { player =>
        player ! Ping()
      }
    case leftgame@Leftgame(player) =>
      Logger.debug("a player left the game")
      context.children.foreach{
        _ ! leftgame
      }
      if (context.children.size < 2) {
        Logger.debug(s"[$roomId] taking the pill")
        self ! PoisonPill
      }
      player ! PoisonPill
    case out: ActorRef =>
      Logger.debug("creating player")
      val player = context.actorOf(Player.props(out))
      sender() ! player

      context.children.filterNot {
        _ == player
      }.foreach { ref =>
        player ! Connect(ref.path.name)
      }

      state.foreach {  entry =>
        player ! StateUpdate(entry._1, entry._2)
      }
  }
}

