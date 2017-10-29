package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{InjectedController, WebSocket}

import scala.concurrent.ExecutionContext

@Singleton
class WevrSocketController @Inject()(implicit actorSystem: ActorSystem, exec: ExecutionContext, materializer: Materializer) extends InjectedController {

  def ws(roomId: String): WebSocket = WebSocket.accept[JsValue, JsValue] { _ =>
    Logger.debug(s"creating websocket, room: $roomId")
    ActorFlow.actorRef(out => Props(new WebsocketIn(out, roomId)))
  }
}


