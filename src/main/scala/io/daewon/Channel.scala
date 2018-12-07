package io.daewon

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.io.Udp.SO.Broadcast
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source, SourceQueueWithComplete}
import io.daewon.Channel.{ChannelCommand, Leave}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import spray.json._

object ChannelProtocol extends DefaultJsonProtocol {

  import Channel._

  implicit val channelCommandFormat = jsonFormat0[ChannelCommand](_)
  implicit val publishFormat = jsonFormat3(Publish)

}

object Channel {

  sealed trait ChannelCommand

  final case class Join(userId: String) extends ChannelCommand

  final case class Leave(userId: String) extends ChannelCommand

  final case class Subscribe(userId: String, topic: String) extends ChannelCommand

  final case class UnSubscribe(userId: String, topic: String) extends ChannelCommand

  final case class Publish(senderId: String, topic: String, message: String) extends ChannelCommand

}

/**
  * Abstract user connections.
  */
class Channel(channelEventCallback: ChannelCommand => Unit)(implicit materializer: ActorMaterializer) {

  import Channel._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val connections =
    collection.concurrent.TrieMap.empty[String, (SourceQueueWithComplete[ChannelCommand], Source[ChannelCommand, NotUsed])]

  private val topics =
    collection.concurrent.TrieMap.empty[String, List[String]]

  def getOrCreateConnection(userId: String): (SourceQueueWithComplete[ChannelCommand], Source[ChannelCommand, NotUsed]) =
    connections.getOrElseUpdate(userId,
      Source
        .queue[ChannelCommand](100, OverflowStrategy.fail)
        .toMat(BroadcastHub.sink)(Keep.both).run
    )

  def send(cmd: ChannelCommand) = cmd match {
    case Join(userId) => join(userId)
    case Leave(userId) => leave(userId)
    case Subscribe(userId, topic) => subscribe(userId, topic)
    case UnSubscribe(userId, topic) => unsubscribe(userId, topic)
    case Publish(userId, topic, message) => publish(userId, topic, message)
  }

  def publish(senderId: String, topic: String, message: String) = {
    val subscribedUsers = topics.getOrElse(topic, Nil)
    val msg = Publish(senderId, topic, message)

    subscribedUsers.foreach { subscribeUserId =>
      connections.get(subscribeUserId).foreach { case (q, _) =>
        q.offer(msg)
      }
    }
  }

  def subscribe(userId: String, topic: String) = {
    val old = topics.getOrElseUpdate(topic, Nil)
    val isSuccess = topics.replace(topic, (userId :: old).distinct)

    isSuccess
  }

  def unsubscribe(userId: String, topic: String) = {
    val old = topics.getOrElseUpdate(userId, Nil)
    val newValue = old.filterNot(_ == topic)
    val isSuccess = topics.replace(userId, old, newValue)

    if (newValue.isEmpty) topics -= userId

    isSuccess
  }

  def leave(userId: String): Unit = {
    val (queue, _) = getOrCreateConnection(userId)
    queue.complete()
    connections -= userId

    channelEventCallback(Leave(userId))
  }

  def join(userId: String): Flow[Message, TextMessage, Unit] = {
    import ChannelProtocol._
    val (_, source) = getOrCreateConnection(userId)

    Flow[Message]
      .watchTermination() { (_, f) =>
        implicit val ec = ExecutionContext.global
        f.onComplete { _ => leave(userId) }
      }
      .mapConcat {
        case TextMessage.Strict(msg) =>
          logger.info(s"from client ${msg}")
          // parse client command
          Nil
        case tm: TextMessage =>
          tm.textStream.runWith(Sink.ignore)
          Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
      .merge(source, true)
      .map { cmd =>
        // dispatch command
        TextMessage(cmd.toString)
      }
  }
}
