package io.daewon

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.slf4j.LoggerFactory

trait ChannelServiceRoute extends SprayJsonSupport {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  private val logger = LoggerFactory.getLogger(this.getClass)

  def onChannelEvent(cmd: ChannelCommand): Unit = cmd match {
    case _ => logger.info(cmd.toString)
  }

  lazy val channel: Channel = new Channel(onChannelEvent)

  lazy val join = path("join" / Segment) { userId =>
    handleWebSocketMessagesForOptionalProtocol(channel.join(userId), None)
  }

  lazy val leave = path("leave" / Segment) { userId =>
    channel.leave(userId)
    complete(StatusCodes.OK)
  }

  lazy val subscribe = path("subscribe" / Segment / Segment) { (userId, topic) =>
    channel.subscribe(userId, topic)
    complete(StatusCodes.OK)
  }

  lazy val unsubscribe = path("unsubscribe" / Segment / Segment) { (userId, topic) =>
    channel.unsubscribe(userId, topic)
    complete(StatusCodes.OK)
  }

  lazy val publish = path("publish" / Segment / Segment / Segment) { (userId, topic, message) =>
    channel.publish(None, userId, topic, message)
    complete(StatusCodes.OK)
  }

  // routes
  lazy val channelRoute: Route =
    get {
      concat(join)
    } ~ put {
      concat(leave, subscribe, unsubscribe, publish)
    }

}
