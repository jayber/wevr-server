package controllers

import akka.actor.{Actor, ActorRef, Props}
import controllers.Game._
import controllers.WebsocketIn.WebRTCNegotiationMessage
import play.api.libs.json.Json

object Player {
  def props(out: ActorRef) = Props(new Player(out))
}

class Player(out: ActorRef) extends Actor {
  override def receive = {
    case Ping => out ! Json.obj("event" -> "wevr.ping")
    case config@IceConfig => out ! Json.obj("event" -> "wevr.ice-config", "data" -> config.iceConfig)
    case Connect(recipient) => out ! Json.obj("event" -> "wevr.connect", "data" -> recipient)
    case WebRTCNegotiationMessage(from, to, payload, messageType) => out ! Json.obj("event" -> s"wevr.$messageType", "data" -> Json.obj("from" -> from, "payload" -> payload))
    case Departure(player) => out ! Json.obj("event" -> "wevr.leftgame", "data" -> player)
    case Reconnect => out ! Json.obj("event" -> "wevr.reconnect")
    case CheckConnections(peers) => out ! Json.obj("event" -> "wevr.check-connections", "data" -> peers)
    case StateUpdate(key, data) => out ! Json.obj("event" -> "wevr.state", "data" -> Json.obj("key" -> key, "data" -> data))
  }
}
