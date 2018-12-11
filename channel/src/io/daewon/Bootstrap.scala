package io.daewon

import scala.language.postfixOps
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util._
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Bootstrap extends App with ChannelServiceRoute {

  implicit val system: ActorSystem = ActorSystem("HttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = LoggerFactory.getLogger("Main")
  val config = ConfigFactory.load()

  val port = sys.props.get("http.port").fold(8000)(_.toInt)
  val serverStatus = s""" { "port": ${port}, "started_at": ${System.currentTimeMillis()} }"""

  val health = HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, serverStatus))

  // Allows you to determine routes to expose according to external settings.
  lazy val routes: Route = concat(
    pathEndOrSingleSlash(getFromResource("assets/index.html")),
    pathPrefix("static")(getFromResourceDirectory("assets")),
    pathPrefix("ws")(channelRoute)
  )

  val binding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", port)
  binding.onComplete {
    case Success(bound) => logger.info(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) => logger.error(s"Server could not start!", e)
  }

  scala.sys.addShutdownHook { () =>
    system.terminate()
    logger.info("System terminated")
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
