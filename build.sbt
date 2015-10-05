organization := "nl.targeter"

name := "akka-stream-experiment"

version := "0.1"

scalaVersion := "2.11.7"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"


libraryDependencies ++= Seq(
  "net.fehmicansaglam" %% "tepkin" % "0.6-SNAPSHOT",
  "com.lambdaworks" % "scrypt" % "1.4.0"
)
