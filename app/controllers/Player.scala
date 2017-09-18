package controllers

import akka.actor.{Actor, ActorRef, Props}
import controllers.Game.{Connect, Leftgame, Ping, StateUpdate}
import controllers.WebsocketIn.NegotiationMessage
import play.api.libs.json.Json

object Player {
  def props(out: ActorRef) = Props(new Player(out))
}

class Player(out: ActorRef) extends Actor {
  override def receive = {
    case Ping() => out ! Json.obj("event" -> "wevr.ping")
    case Connect(recipient) => out ! Json.obj("event" -> "wevr.connect", "data" -> recipient)
    case NegotiationMessage(from, to, payload, messageType) => out ! Json.obj("event" -> s"wevr.$messageType", "data" -> Json.obj("from" -> from, "payload" -> payload))
    case Leftgame(player) => out ! Json.obj("event" -> "wevr.leftgame", "data" -> player.path.name)
    case StateUpdate(key, data) => out ! Json.obj("event" -> "wevr.state", "data" -> Json.obj("key" -> key, "data" -> data))
  }
}
