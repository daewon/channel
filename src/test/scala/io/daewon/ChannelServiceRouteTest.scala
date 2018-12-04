package io.daewon

import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.testkit._
import org.scalatest.{Matchers, WordSpec}
import akka.util.ByteString


class ChannelServiceRouteTest extends WordSpec with Matchers with ChannelServiceRoute with ScalatestRouteTest {

  val wsClient = WSProbe()

  // WS creates a WebSocket request for testing
  val topic = "akka-ws"

  WS(s"/subscribe/${topic}", wsClient.flow) ~> channelRoute ~>
    check {
      // check response f
      isWebSocketUpgrade shouldEqual true
      import scala.concurrent.duration._

      // manually run a WS conversation
      wsClient.sendMessage("Peter")
      wsClient.expectMessage("Hello Peter!")

      wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
      wsClient.expectNoMessage(100.millis)

      wsClient.sendMessage("John")
      wsClient.expectMessage("Hello John!")

      wsClient.sendCompletion()
      wsClient.expectCompletion()

      println("done")
    }

}
