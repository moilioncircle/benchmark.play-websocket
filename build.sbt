name := """play-websocket-sample"""
version := "1.0.0"
scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
