// build.sc

import mill._
import mill.scalalib._

trait Common extends ScalaModule {

  def scalaVersion = "2.12.8"

  def akkaHttpVersion = "10.1.5"

  def akkaVersion = "2.5.19"

  def slf4jVersion = "1.8.0-beta2"

  def circeVersion = "0.10.1"

  def scalacOptions = Seq(
    "-deprecation",
    "-unchecked",
    "-encoding", "utf8",
    "-explaintypes",

    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",

    "-Ybackend-worker-queue", "4",
    "-Yno-adapted-args",
    "-Ydelambdafy:inline",
    "-Ypartial-unification",

    "-Xfatal-warnings",
    "-Xfuture",
    "-Xcheckinit",

    "-Xlint:constant",
    "-Xlint:missing-interpolator",
    "-Xlint:private-shadow",
    "-Xlint:type-parameter-shadow",
    "-Xlint:poly-implicit-overload",
    "-Xlint:option-implicit",
    "-Xlint:unsound-match",
    "-Xlint:by-name-right-associative",
    "-Xlint:package-object-classes",

    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-extra-implicit",

    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-unused:implicits"
  )
}

object channel extends Common {

  override def ivyDeps = Agg(
    ivy"org.slf4j:slf4j-api:${slf4jVersion}",
    ivy"org.slf4j:slf4j-simple:${slf4jVersion}",

    ivy"io.getquill::quill:2.6.0",

    ivy"io.spray::spray-json:1.3.5",

    ivy"com.typesafe.akka::akka-http:${akkaHttpVersion}",
    ivy"com.typesafe.akka::akka-stream:${akkaVersion}",
    ivy"com.typesafe.akka::akka-http-spray-json:${akkaHttpVersion}",

    ivy"org.typelevel::cats-core:1.5.0",
    ivy"com.softwaremill.common::tagging:2.2.1"
  )

  object test extends Tests {
    def testFrameworks = Seq("org.scalatest.tools.Framework")

    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }

    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-http-testkit:${akkaHttpVersion}",
      ivy"com.typesafe.akka::akka-stream-testkit:${akkaVersion}",
      ivy"org.scalatest::scalatest:3.0.5"
    )
  }
}

object app extends Common {
  def moduleDeps = Seq(channel)

  override def ivyDeps = Agg(
    ivy"org.slf4j:slf4j-api:${slf4jVersion}",
    ivy"org.slf4j:slf4j-simple:${slf4jVersion}",

    ivy"io.getquill::quill:2.6.0",

    ivy"io.spray::spray-json:1.3.5",

    ivy"com.typesafe.akka::akka-http:${akkaHttpVersion}",
    ivy"com.typesafe.akka::akka-stream:${akkaVersion}",
    ivy"com.typesafe.akka::akka-http-spray-json:${akkaHttpVersion}",

    ivy"org.typelevel::cats-core:1.5.0",
    ivy"com.softwaremill.common::tagging:2.2.1"
  )

  object test extends Tests {
    def testFrameworks = Seq("org.scalatest.tools.Framework")

    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }

    override def ivyDeps = Agg(
      ivy"com.lihaoyi::os-lib:0.2.5",

      ivy"com.typesafe.akka::akka-http-testkit:${akkaHttpVersion}",
      ivy"com.typesafe.akka::akka-stream-testkit:${akkaVersion}",
      ivy"org.scalatest::scalatest:3.0.5"
    )
  }
}
