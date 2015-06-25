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
  "javax.inject" % "javax.inject" % "1",
  "org.webjars" % "angularjs" % "1.3.15",
  "org.webjars.bower" % "angular-route" % "1.3.15",
  "org.webjars.bower" % "angular-resource" % "1.3.15",
  "org.webjars" % "requirejs" % "2.1.17",
  "org.webjars" % "bootstrap" % "3.3.4",
  "org.webjars" % "visjs" % "4.2.0",
  "joda-time" % "joda-time" % "2.8",
  "commons-io" % "commons-io" % "2.4"
)

pipelineStages := Seq(rjs, digest, gzip)