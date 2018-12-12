package io.daewon

import scala.util.Try

trait ChannelProtocol[E, A] {
  def encode(cmd: ChannelCommand): A

  def decode(cmd: A): Either[E, ChannelCommand]
}

object ChannelProtocol extends spray.json.DefaultJsonProtocol with ChannelProtocol[Throwable, String] {

  import spray.json._
  import com.softwaremill.tagging.AnyTypeclassTaggingCompat._

  implicit val channelCommandFormat = new RootJsonFormat[ChannelCommand] {
    implicit val publishFormat = jsonFormat3(Publish)
    implicit val subscribeFormat = jsonFormat2(Subscribe)
    implicit val unSubscribeFormat = jsonFormat2(UnSubscribe)
    implicit val responseFormat = jsonFormat2(Response.apply)
    implicit val joinFormat = jsonFormat1(Join)
    implicit val leaveFormat = jsonFormat1(Leave)

    def write(obj: ChannelCommand): JsValue =
      JsObject((obj match {
        case cmd: Subscribe => cmd.toJson
        case cmd: UnSubscribe => cmd.toJson
        case cmd: Publish => cmd.toJson
        case cmd: Response => cmd.toJson
        case cmd: Join => cmd.toJson
        case cmd: Leave => cmd.toJson
      }).asJsObject.fields + (":type" -> JsString(obj.productPrefix)))

    def read(json: JsValue): ChannelCommand =
      json.asJsObject.getFields(":type") match {
        case Seq(JsString("Publish")) => json.convertTo[Publish]
        case Seq(JsString("Subscribe")) => json.convertTo[Subscribe]
        case Seq(JsString("UnSubscribe")) => json.convertTo[UnSubscribe]
        case Seq(JsString("Response")) => json.convertTo[Response]
        case Seq(JsString("Join")) => json.convertTo[Join]
        case Seq(JsString("Leave")) => json.convertTo[Leave]
        case Nil => throw new RuntimeException(s"Failed to decode: missing ':type' field")
        case _ => throw new RuntimeException(s"Failed to decode: invalid value on ':type' field")
      }
  }

  def encode(cmd: ChannelCommand): String =
    cmd.toJson.toString

  def decode(cmd: String): Either[Throwable, ChannelCommand] =
    Try(cmd.parseJson.convertTo[ChannelCommand]).toEither
}
