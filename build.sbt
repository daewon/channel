/**
  * test
  */
lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion = "2.5.19"
lazy val slf4jVersion = "1.8.0-beta2"

name := "ioChat"

version := "0.1"

description := "s2graph http server"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
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
  "-Ywarn-numeric-widen",
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,

  "io.spray" %% "spray-json" % "1.3.5",

  "io.getquill" %% "quill" % "2.6.0",

  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

Revolver.settings
