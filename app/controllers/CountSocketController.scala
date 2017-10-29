package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.util.Timeout
import controllers.Game.{Count, CountReply, Ping}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{InjectedController, WebSocket}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

@Singleton
class CountSocketController @Inject()(implicit actorSystem: ActorSystem, exec: ExecutionContext, materializer: Materializer) extends InjectedController {

  def ws(): WebSocket = WebSocket.accept[JsValue, JsValue] { _ =>
    Logger.debug(s"creating websocket for count")
    ActorFlow.actorRef(out => Props(new DummyCountWebsocket(out)))
  }
}

class DummyCountWebsocket(out: ActorRef)(implicit actorSystem: ActorSystem) extends Actor {

  private val countWebsocket = actorSystem.actorOf(Props(new CountWebsocket(out)), s"count-${out.path.name}")

  override def receive = {
    case msg => countWebsocket ! msg
  }
}

class CountWebsocket(out: ActorRef) extends Actor {

  implicit private val timeout: Timeout = Timeout(5 seconds)
  implicit private val exec: ExecutionContextExecutor = context.dispatcher
  context.system.scheduler.schedule(Duration.Zero, Duration.create(30, "second"), self, Ping())

  private val games = context.system.actorSelection(s"user/game-*")
  games ! Count()

  override def receive = {
    case Ping() => out ! Json.obj("event" -> "ping")
    case CountReply(count, roomId) =>
      Logger.trace(s"count reply: $roomId = $count")
      out ! Json.obj("event" -> "count", "data" -> Json.obj("count" -> count, "roomId" -> roomId))
    case msg =>
      Logger.debug("unknown websocket event: " + msg)
  }
}


