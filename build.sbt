
name := """graphpizer-server"""

version := "1.0.0-dev"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

resolvers += "PlantUML repository" at "https://oss.sonatype.org/content/repositories/releases"

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
  "commons-io" % "commons-io" % "2.4",
  "net.sourceforge.plantuml" % "plantuml" % "8027"
)

pipelineStages := Seq(rjs, digest, gzip)

TwirlKeys.templateFormats += ("plantuml" -> "templates.PlantUmlFormat")

maintainer in Linux := "Martin Helmich <kontakt@martin-helmich.de>"

packageSummary in Linux := "PHP code analytics engine"

packageDescription := "GraPHPizer is a PHP source code analytics engine written in Scala"

rpmRelease := "1"

rpmVendor := "https://github.com/martin-helmich"

rpmUrl := Some("https://github.com/martin-helmich/graphpizer-server")

rpmLicense := Some("GPL-3.0")
