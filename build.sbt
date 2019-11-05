organization := "com.typesafe.akka.samples"
name := "dgraphtest"

lazy val root = (
  Project("graphino-scala", file("."))
    enablePlugins(JavaAppPackaging, GitVersioning))


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.26",
  "com.typesafe.akka" %% "akka-http" % "10.1.10",
  "com.typesafe.akka" %% "akka-stream" % "2.5.26",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10",
  "io.dgraph" % "dgraph4j" % "2.0.1",
  "io.netty" % "netty-tcnative" % "2.0.26.Final",
  "io.netty" % "netty-tcnative-boringssl-static" % "2.0.26.Final"
)

enablePlugins(JavaAppPackaging, GitVersioning)

maintainer := "igor.miletic@tamedia.ch"

releaseUseGlobalVersion := false


publishMavenStyle := true

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
