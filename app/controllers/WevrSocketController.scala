package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{InjectedController, RequestHeader, WebSocket}

import scala.concurrent.ExecutionContext

@Singleton
class WevrSocketController @Inject()(implicit actorSystem: ActorSystem, exec: ExecutionContext, materializer: Materializer) extends InjectedController {

  private val originGex = """http[s]?:\/\/([a-z0-9.]+)(:[0-9]+)?(\/([a-z0-9\/\.]*))?(\?.*)?""".r("domain", "port", "path")
  private val localDomainGex =
    """localhost|127.0.0.1|192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.\d{1,3}\.\d{1,3}""".r

  def ws(roomId: String): WebSocket = WebSocket.accept[JsValue, JsValue] { header =>
    val room = synthesizeRoomId(header) + (if (roomId != "") "_" + roomId else "")
    Logger.debug(s"creating websocket, room: $room")
    ActorFlow.actorRef(out => Props(new WebsocketIn(out, room)))
  }

  private def synthesizeRoomId(header: RequestHeader): String = {
    val origin = header.headers.get("Origin").get.toLowerCase()
    Logger.debug(s"Origin: $origin")
    val result = originGex.findFirstMatchIn(origin).get
    val domain = uniqueifyLocalDomain(result.group("domain").stripSuffix("/"), header)
    Logger.debug(s"domain: $domain")
    domain
  }

  private def uniqueifyLocalDomain(domain: String, header: RequestHeader): String = {
    localDomainGex.findFirstMatchIn(domain) match {
      case Some(_) =>
        val forwarded = header.headers.get("Forwarded") orElse header.headers.get("X-Forwarded-For") orElse Some(header.connection.remoteAddress.getHostAddress)
        forwarded.get.replace(" ", "").replaceAll("\\W", "_")
      case _ => domain
    }
  }
}


