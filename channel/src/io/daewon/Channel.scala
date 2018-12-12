package io.daewon

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source, SourceQueueWithComplete}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import com.softwaremill.tagging._
import ChannelCommand._

class Channel(channelEventCallback: ChannelCommand => Unit)(implicit materializer: ActorMaterializer) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val connections =
    collection.concurrent.TrieMap.empty[String, (SourceQueueWithComplete[ChannelCommand], Source[ChannelCommand, NotUsed])]

  private val topics =
    collection.concurrent.TrieMap.empty[String, List[String]]

  def getOrCreateQueue(userId: String): (SourceQueueWithComplete[ChannelCommand], Source[ChannelCommand, NotUsed]) = {
    connections.getOrElseUpdate(userId, {
      Source
        .queue[ChannelCommand](256, OverflowStrategy.fail)
        .toMat(BroadcastHub.sink)(Keep.both)
        .run
    })
  }

  def receive(sender: SourceQueueWithComplete[ChannelCommand], cmd: ChannelCommand): Response = {
    cmd match {
      case Join(_) => // pass
      case Leave(userId) => leave(userId)
      case Subscribe(topic, userId) =>
        val isSuccess = subscribe(topic, userId)
        sender.offer(Response.cond(isSuccess))
      case UnSubscribe(topic, userId) =>
        val isSuccess = unsubscribe(topic, userId)
        sender.offer(Response.cond(isSuccess))
      case Publish(topic, userId, message) =>
        publish(Option(sender), topic, userId, message)
        sender.offer(Response.OK)
      case response: Response =>
        sender.offer(response)
    }

    Response.OK
  }

  def publish(sender: Option[SourceQueueWithComplete[ChannelCommand]], topic: String, userId: String, message: String): Unit = {
    val subscribedUsers = topics.getOrElse(topic, Nil)
    val msg = Publish(topic.taggedWith[Topic], userId.taggedWith[User], message)

    subscribedUsers.foreach { subscribeUserId =>
      connections.get(subscribeUserId).foreach { case (q, _) =>
        sender match {
          case Some(sender) => if (sender != q) q.offer(msg)
          case None => q.offer(msg)
        }
      }
    }
  }

  def subscribe(topic: String, userId: String): Boolean = {
    val old = topics.getOrElseUpdate(topic, Nil)
    topics.replace(topic, (userId :: old).distinct).nonEmpty
  }

  def unsubscribe(topic: String, userId: String): Boolean = {
    val old = topics.getOrElseUpdate(userId, Nil)
    val newValue = old.filterNot(_ == topic)
    val isSuccess = topics.replace(userId, old, newValue)

    if (newValue.isEmpty) topics -= userId

    isSuccess
  }

  def leave(userId: String): Unit = {
    connections.get(userId).foreach { case (queue, _) =>
      queue.complete()
      connections -= userId
    }

    channelEventCallback(Leave(userId.taggedWith[User]))
  }

  def join(userId: String): Flow[Message, TextMessage, Unit] = {
    import ChannelProtocol._
    //     for send message to client: Merge flow with source
    val (queue, source) = getOrCreateQueue(userId)

    Flow[Message]
      .watchTermination() { (_, f) =>
        f.onComplete { _ => leave(userId) }(ExecutionContext.global)
      }
      .mapConcat { // from client
        case TextMessage.Strict(msg) =>
          logger.info(msg)
          logger.info(ChannelProtocol.decode(msg).toString)

          ChannelProtocol.decode(msg) match {
            case Left(e) => receive(queue, Response(500, Option(e.getMessage)))
            case Right(cmd) => receive(queue, cmd)
          }

          Nil
        case tm: TextMessage =>
          tm.textStream.runWith(Sink.ignore)
          Nil
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Nil
        case _ => Nil
      }
      .merge(source, true)
      .map { cmd => // to client
        TextMessage(ChannelProtocol.encode(cmd))
      }
  }
}
