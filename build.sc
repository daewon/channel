// build.sc
import mill._
import mill.scalalib._

object channel extends ScalaModule {
  def scalaVersion = "2.12.8"

  def akkaHttpVersion = "10.1.5"

  def akkaVersion = "2.5.19"

  def slf4jVersion = "1.8.0-beta2"

  override def scalacOptions = Seq(
    "-Ydelambdafy:inline",
    "-unchecked",
    "-encoding", "utf8", // Option and arguments on same line
    "-deprecation",
    "-explaintypes", // Explain type errors in more detail.

    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",

    "-Xfatal-warnings", // New lines for each options

    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.

    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen"
  )

  override def ivyDeps = Agg(
    ivy"org.slf4j:slf4j-api:${slf4jVersion}",
    ivy"org.slf4j:slf4j-simple:${slf4jVersion}",
    ivy"io.spray::spray-json:1.3.5",
    ivy"io.getquill::quill:2.6.0",
    ivy"com.typesafe.akka::akka-http:${akkaHttpVersion}",
    ivy"com.typesafe.akka::akka-http-spray-json:${akkaHttpVersion}",
    ivy"com.typesafe.akka::akka-stream:${akkaVersion}",
    ivy"org.typelevel::cats-core:1.5.0"
  )

  override def forkArgs = Seq("-Xmx4g")

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-http-testkit:${akkaHttpVersion}",
      ivy"com.typesafe.akka::akka-stream-testkit:${akkaVersion}",
      ivy"org.scalatest::scalatest:3.0.5"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }
  }

}

