lazy val commonSettings = Seq(
  organization := "uk.co.sprily",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-Xlint",
    "-Ywarn-unused-import",
    "-unchecked"),
  libraryDependencies ++= commonDependencies
)

lazy val commonDependencies = Seq(
  "com.typesafe.scala-logging"  %% "scala-logging"        % "3.1.0",
  "ch.qos.logback"               % "logback-core"         % "1.1.2",
  "ch.qos.logback"               % "logback-classic"      % "1.1.2"
)


// The service that runs internally, on-site.
lazy val onSite = (project in file("on-site")).
  settings(commonSettings: _*).
  settings(
    name := "brush-training-facility"
)
