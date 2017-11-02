package controllers

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import controllers.Game._
import controllers.WebsocketIn.WebRTCNegotiationMessage
import play.api.Logger
import play.api.libs.json.JsValue

import scala.collection.mutable
import scala.concurrent.duration.Duration

object Game {
  def props(roomId: String) = Props(new Game(roomId))

  case class Connect(peer: String)

  case class Ping()

  case class Leftgame(player: ActorRef)

  case class Departure(player: String)

  case class StateUpdate(key: String, value: JsValue)

  case class CheckConnections(peers: Seq[String])

  case class StartConnectionCheck()

  case class PeerPingFailures(failures: Seq[String])

  case class Reconnect()

  case class Count()

  case class CountReply(count: Int, roomId: String)

}

class Game(roomId: String) extends Actor {
  val state = mutable.ListMap[String, JsValue]()
  var lastConnectionCheck: Option[ActorRef] = None

  implicit val exec = context.dispatcher
  context.system.scheduler.schedule(Duration.Zero, Duration.create(30, "second"), self, Ping())
  context.system.scheduler.schedule(Duration.create(1, "minute"), Duration.create(1, "minute"), self, StartConnectionCheck())

  override def receive = {
    case Count() =>
      Logger.debug(s"replying to Count(): $roomId")
      sender() ! CountReply(context.children.size, roomId)
    case PeerPingFailures(failures) => failures.foreach { failure =>
      context.child(failure).foreach {
        _ ! Reconnect()
      }
      val children = context.children.map(_.path.name).to[Seq]
      val missingChildren = failures.diff(children)
      missingChildren.foreach { missing =>
        val departure = Departure(missing)
        context.children.foreach { child =>
          child ! departure
        }
      }

    }
    case StateUpdate(key, value) => state += (key -> value)
    case message@WebRTCNegotiationMessage(_, to, _, _) => context.child(to).foreach {
      _ ! message
    }
    case Ping() =>
      context.children.foreach { player =>
        player ! Ping()
      }
    case StartConnectionCheck() =>
      if (context.children.size > 1) {
        val seq = context.children.toIndexedSeq
        val finalIndex = lastConnectionCheck.map { actor =>
          var index = seq.indexOf(actor)
          if (index == seq.size - 1) 0 else index + 1
        }.getOrElse(0)
        val target = seq(finalIndex)
        Logger.trace(s"checking connections for: $finalIndex-${target.path.name}")
        target ! CheckConnections(seq.filterNot {
          _ == target
        }.map {
          _.path.name
        })
        lastConnectionCheck = Some(target)
      }
    case leftgame@Leftgame(player) =>
      Logger.debug(s"player ${player.path.name} left game $roomId")
      val departure = Departure(player.path.name)
      context.children.foreach {
        _ ! departure
      }
      if (context.children.size < 2) {
        Logger.debug(s"$roomId taking the pill")
        self ! PoisonPill
      }
      player ! PoisonPill

      broadcastCount(-1)

    case out: ActorRef =>
      Logger.debug("creating player...")
      val player = context.actorOf(Player.props(out))
      Logger.debug("name = " + player.path.name)
      sender() ! player

      context.children.filterNot {
        _ == player
      }.foreach { ref =>
        player ! Connect(ref.path.name)
      }

      state.foreach { entry =>
        player ! StateUpdate(entry._1, entry._2)
      }

      broadcastCount()
  }

  private def broadcastCount(adjust: Int = 0) = {
    val selection = context.system.actorSelection(s"user/count-*")
    selection ! CountReply(context.children.size + adjust, roomId)
  }
}

