package io.daewon

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source, SourceQueueWithComplete}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import spray.json._

sealed trait ChannelCommand extends Product

final case class Join(userId: String) extends ChannelCommand

final case class Leave(userId: String) extends ChannelCommand

final case class Subscribe(userId: String, topic: String) extends ChannelCommand

final case class UnSubscribe(userId: String, topic: String) extends ChannelCommand

final case class Publish(senderId: String, topic: String, message: String) extends ChannelCommand

final case class Response(status: Int = 200, message: Option[String] = None) extends ChannelCommand

object Response {
  def apply(bool: Boolean) = bool match {
    case false => new Response(500, Option("request failed"))
    case true => new Response(200, Option("request successful"))
  }
}

trait ChannelProtocol[A] {
  def encode(cmd: ChannelCommand): A

  def decode(cmd: A): ChannelCommand
}

object ChannelProtocol extends DefaultJsonProtocol with ChannelProtocol[String] {

  implicit val channelCommandFormat = new RootJsonFormat[ChannelCommand] {
    implicit val publishFormat = jsonFormat3(Publish)
    implicit val subscribeFormat = jsonFormat2(Subscribe)
    implicit val unSubscribeFormat = jsonFormat2(UnSubscribe)
    implicit val responseFormat = jsonFormat2(Response.apply)

    def write(obj: ChannelCommand): JsValue =
      JsObject((obj match {
        case subscribe: Subscribe => subscribe.toJson
        case unsubscribe: UnSubscribe => unsubscribe.toJson
        case publish: Publish => publish.toJson
        case response: Response => response.toJson
        case _ => throw new RuntimeException("can't be here")
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))

    def read(json: JsValue): ChannelCommand =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("Publish")) => json.convertTo[Publish]
        case Seq(JsString("Subscribe")) => json.convertTo[Subscribe]
        case Seq(JsString("UnSubscribe")) => json.convertTo[UnSubscribe]
        case Seq(JsString("Response")) => json.convertTo[Response]
      }
  }

  def encode(cmd: ChannelCommand): String =
    cmd.toJson.toString

  def decode(cmd: String) =
    cmd.parseJson.convertTo[ChannelCommand]
}

/**
  * Abstract user connections.
  */
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

  def receive(sender: SourceQueueWithComplete[ChannelCommand], cmd: ChannelCommand) = {
    cmd match {
      case Join(userId) => // pass
      case Leave(userId) => leave(userId)
      case Subscribe(userId, topic) =>
        val isSuccess = subscribe(userId, topic)
        sender.offer(Response(isSuccess))
      case UnSubscribe(userId, topic) =>
        val isSuccess = unsubscribe(userId, topic)
        sender.offer(Response(isSuccess))
      case Publish(userId, topic, message) =>
        publish(Option(sender), userId, topic, message)
        sender.offer(Response())
      case Response(status, messageOpt) => // pass
    }

    Response()
  }

  def publish(sender: Option[SourceQueueWithComplete[ChannelCommand]], senderId: String, topic: String, message: String) = {
    val subscribedUsers = topics.getOrElse(topic, Nil)
    val msg = Publish(senderId, topic, message)

    subscribedUsers.foreach { subscribeUserId =>
      connections.get(subscribeUserId).foreach { case (q, _) =>
        sender match {
          case Some(sender) => if (sender != q) q.offer(msg)
          case None => q.offer(msg)
        }
      }
    }
  }

  def subscribe(userId: String, topic: String): Boolean = {
    val old = topics.getOrElseUpdate(topic, Nil)
    topics.replace(topic, (userId :: old).distinct).nonEmpty
  }

  def unsubscribe(userId: String, topic: String): Boolean = {
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

    channelEventCallback(Leave(userId))
  }

  def join(userId: String): Flow[Message, TextMessage, Unit] = {
    import ChannelProtocol._
    //     for send message to client: Merge flow with source
    val (queue, source) = getOrCreateQueue(userId)

    Flow[Message]
      .watchTermination() { (_, f) =>
        implicit val ec = ExecutionContext.global
        f.onComplete { _ => leave(userId) }
      }
      .mapConcat {
        case TextMessage.Strict(msg) =>
          receive(queue, ChannelProtocol.decode(msg))
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
      .map { cmd =>
        TextMessage(ChannelProtocol.encode(cmd))
      }
  }
}
