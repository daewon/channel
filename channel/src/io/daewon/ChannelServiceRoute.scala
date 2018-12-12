package io.daewon

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream._
import org.slf4j.LoggerFactory
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

trait ChannelServiceRoute extends SprayJsonSupport {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  private val logger = LoggerFactory.getLogger(this.getClass)

  def onChannelEvent(cmd: ChannelCommand): Unit = cmd match {
    case _ => logger.info(cmd.toString)
  }

  lazy val channel: Channel = new Channel(onChannelEvent)

  lazy val join: Route = path("join" / Segment) { userId =>
    handleWebSocketMessagesForOptionalProtocol(channel.join(userId), None)
  }

  lazy val leave: Route = path("leave" / Segment) { userId =>
    channel.leave(userId)
    complete(StatusCodes.OK)
  }

  lazy val subscribe: Route = path("subscribe" / Segment / Segment) { (userId, topic) =>
    channel.subscribe(userId, topic)
    complete(StatusCodes.OK)
  }

  lazy val unsubscribe: Route = path("unsubscribe" / Segment / Segment) { (userId, topic) =>
    channel.unsubscribe(userId, topic)
    complete(StatusCodes.OK)
  }

  lazy val publish: Route = path("publish" / Segment / Segment / Segment) { (userId, topic, message) =>
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
