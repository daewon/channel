package io.daewon

import com.softwaremill.tagging._

object ChannelCommand {

  trait Topic

  trait User

}

import ChannelCommand._

sealed trait ChannelCommand extends Product

final case class Join(userId: String @@ User) extends ChannelCommand

final case class Leave(userId: String @@ User) extends ChannelCommand

final case class Subscribe(topic: String @@ Topic, userId: String @@ User) extends ChannelCommand

final case class UnSubscribe(topic: String @@ Topic, userId: String @@ User) extends ChannelCommand

final case class Publish(topic: String @@ Topic, userId: String @@ User, message: String) extends ChannelCommand

final case class Response(status: Int = 200, message: Option[String]) extends ChannelCommand

object Response {
  val OK = Response(200, None)

  val Successful = Response(200, Option("Request successful"))

  val Failed = Response(500, Option("Request failed"))

  def cond(isSuccess: Boolean): Response = if (isSuccess) Successful else Failed
}
