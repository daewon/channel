package io.daewon

import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.testkit._
import akka.util.ByteString
import spray.json._
import org.scalatest.{Matchers, WordSpec}

// ScalatestRouteTest should first extends class for implicit values
class ChannelServiceRouteTest extends WordSpec with Matchers with ScalatestRouteTest with ChannelServiceRoute {

  import scala.concurrent.duration._
  import ChannelProtocol._

  // WS creates a WebSocket request for testing
  val dun = WSProbe()
  val yanggury = WSProbe()

  val userDun = "dun"
  val userYang = "yanggury"

  val topic = "http"

  // check response f
  WS(s"/join/${userDun}", dun.flow) ~> channelRoute ~> check {
    isWebSocketUpgrade shouldEqual true

    dun.sendMessage(encode(Subscribe(userDun, topic)))
    dun.expectMessage(encode(Response()))

    // ignore binary message
    dun.sendMessage(BinaryMessage(ByteString("abcdef")))
    dun.expectNoMessage(10.millis)

    WS(s"/join/${userYang}", yanggury.flow) ~> channelRoute ~> check {
      isWebSocketUpgrade shouldEqual true

      yanggury.sendMessage(encode(Subscribe(userYang, topic)))
      yanggury.expectMessage(encode(Response()))

      // sender don't receive self message
      yanggury.sendMessage(encode(Publish(userDun, topic, "websocket")))
      yanggury.expectMessage(encode(Response()))

      yanggury.sendCompletion()
      yanggury.expectCompletion()
    }

    // yang receivet message from dun cause subscribe 'http' topic
    dun.expectMessage(encode(Publish(userDun, topic, "websocket")))

    dun.sendCompletion()
    dun.expectCompletion()
  }
}
