package io.daewon

import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.testkit._
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}

// ScalatestRouteTest should first extends class for implicit values
class ChannelServiceRouteTest extends WordSpec with Matchers with ScalatestRouteTest with ChannelServiceRoute {

  import com.softwaremill.tagging._
  import io.daewon.util.Util._
  import scala.concurrent.duration._

  import ChannelProtocol._
  import ChannelCommand._

  // WS creates a WebSocket request for testing
  val dun = WSProbe()
  val yanggury = WSProbe()

  val userDun: String @@ User = "dun".tag[User]
  val userYang: String @@ User = "yanggury".tag[User]

  val topicHttp: String @@ Topic = "http".tag[Topic]
  val topicCSS: String @@ Topic = "css".tag[Topic]

  // check response f
  WS(s"/join/${userDun}", dun.flow) ~> channelRoute ~> check {
    isWebSocketUpgrade shouldEqual true

    dun.sendMessage(encode(Subscribe(topicHttp, userDun)))
    dun.expectMessage(encode(Response.cond(true)))

    dun.sendMessage(encode(Subscribe(topicCSS, userDun)))
    dun.expectMessage(encode(Response.cond(true)))

    // ignore binary message
    dun.sendMessage(BinaryMessage(ByteString("dummy")))
    dun.expectNoMessage(10.millis)


    WS(s"/join/${userYang}", yanggury.flow) ~> channelRoute ~> check {
      isWebSocketUpgrade shouldEqual true

      dun.sendMessage(encode(Publish(topicCSS, userDun, "css")))
      dun.expectMessage(encode(Response.OK))

      yanggury.sendMessage(encode(Subscribe(topicHttp, userYang)))
      yanggury.expectMessage(encode(Response.cond(true)))

      // sender don't receive self message
      yanggury.sendMessage(encode(Publish(topicHttp, userDun, "websocket")))
      yanggury.expectMessage(encode(Response.OK))
    }

    // yang receive message from dun cause subscribe 'http' topic
    dun.expectMessage(encode(Publish(topicHttp, userDun, "websocket")))

    dun.sendMessage(encode(Publish(topicCSS, userDun, "css")))
    dun.expectMessage(encode(Response.OK))

    yanggury.expectNoMessage() // yang don't subscribe 'css' topic

    dun.sendCompletion()
    dun.expectCompletion()

    yanggury.sendCompletion()
    yanggury.expectCompletion()
  }
}
