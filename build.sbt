name := """graphizer-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies ++= Seq(
  "org.neo4j" % "neo4j" % "2.2.1",
  "com.google.inject" % "guice" % "3.0",
  "javax.inject" % "javax.inject" % "1"
)