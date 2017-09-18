package controllers

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import controllers.Game.{Leftgame, StateUpdate}
import controllers.WebsocketIn.NegotiationMessage
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object WebsocketIn {

  case class NegotiationMessage(from: String, to: String, payload: JsValue, messageType: String)

}

class WebsocketIn(out: ActorRef, roomId: String)(implicit actorSystem: ActorSystem) extends Actor {

  implicit private val timeout: Timeout = Timeout(5 seconds)
  implicit private val exec: ExecutionContextExecutor = context.dispatcher

  private val game = actorSystem.actorSelection(s"user/game-$roomId").resolveOne
    .recover { case e =>
      Logger.debug(s"creating new Game: $roomId")
      actorSystem.actorOf(Game.props(roomId), s"game-$roomId")
    }
  private val player = game.flatMap(game => game ? out).mapTo[ActorRef]

  override def receive: Receive = {
    case message: JsValue =>
      Logger.debug(Json.stringify(message))
      (message \ "event").as[String] match {
        case "wevr.offer" => tellGameWithPlayer { player => NegotiationMessage(player.path.name, (message \ "data" \ "to").as[String], (message \ "data" \ "payload").as[JsValue], "offer")
        }
        case "wevr.answer" => tellGameWithPlayer { player => NegotiationMessage(player.path.name, (message \ "data" \ "to").as[String], (message \ "data" \ "payload").as[JsValue], "answer")
        }
        case "wevr.ice-candidate" => tellGameWithPlayer { player => NegotiationMessage(player.path.name, (message \ "data" \ "to").as[String], (message \ "data" \ "payload").as[JsValue], "ice-candidate")
        }
        case "wevr.state" => game.foreach { _ ! StateUpdate((message \ "data" \ "key").as[String],(message \ "data" \ "data").as[JsValue])}
        case _ =>
      }
  }

  override def postStop(): Unit = {
    Logger.debug("socket stopped")
    tellGameWithPlayer { player => Leftgame(player) }
  }

  def tellGameWithPlayer(f: (ActorRef) => Any): Unit = {
    game.zip(player).foreach { gameAndPlayer => gameAndPlayer._1 ! f(gameAndPlayer._2) }
  }
}