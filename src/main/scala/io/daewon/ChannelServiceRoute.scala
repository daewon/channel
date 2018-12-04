package io.daewon

import akka.NotUsed
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl._

trait ChannelServiceRoute extends SprayJsonSupport {
  implicit val materializer: ActorMaterializer

  val clients = collection.concurrent.TrieMap.empty[String, (SourceQueueWithComplete[Message], Source[Message, NotUsed])]

  def lookupQueue(topic: String): (SourceQueueWithComplete[Message], Source[Message, NotUsed]) = clients.getOrElseUpdate(topic, {
    Source.queue[Message](100, OverflowStrategy.fail).preMaterialize
  })

  def lookupChannel(topic: String): Flow[Message, Message, NotUsed] = {
    val (queue, source) = lookupQueue(topic)

    Flow[Message]
      .merge(source, true)
      .mapConcat {
        case tm: TextMessage =>
          TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
  }

  val publish = path("publish" / Segment / Segment) { (topic, message) =>
    val (q, _) = lookupQueue(topic)
    q.offer(TextMessage(message))
    //    Source.single(TextMessage(message)).via(lookupChannel(topic)).to(Sink.ignore).run()
    complete(StatusCodes.OK)
  }

  val subscribe = path("subscribe" / Segment) { topic =>
    //      handleWebSocketMessagesForOptionalProtocol(lookupChannel(id), None)
    handleWebSocketMessagesForOptionalProtocol(lookupChannel(topic), None)
  }

  // routes
  val channelRoute: Route =
    get {
      concat(subscribe)
    } ~ put {
      concat(publish)
    }

}
