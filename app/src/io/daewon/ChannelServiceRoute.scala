package io.daewon

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream._
import org.slf4j.LoggerFactory
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import spray.json._

trait ChannelServiceRoute extends SprayJsonSupport { self =>
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  private val logger = LoggerFactory.getLogger(this.getClass)

  type ChannelMessage = String

  lazy val channel: Channel[ChannelMessage] = new Channel[ChannelMessage] {
    override implicit val materializer: ActorMaterializer = self.materializer

    override def onLeave(cmd: ChannelCommand): Unit = cmd match {
      case _ => logger.info(cmd.toString)
    }
  }

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
