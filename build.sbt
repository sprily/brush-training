import sbt.Project.projectToRef

lazy val commonSettings = Seq(
  organization := "uk.co.sprily",
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-Xlint",
    "-Ywarn-unused-import",
    "-unchecked"),
  libraryDependencies ++= commonDependencies,
  resolvers ++= commonResolvers
)

lazy val commonResolvers = Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
)

lazy val commonDependencies = Seq(

  // logging
  "com.typesafe.scala-logging"  %% "scala-logging"        % "3.1.0",
  "ch.qos.logback"               % "logback-core"         % "1.1.2",
  "ch.qos.logback"               % "logback-classic"      % "1.1.2",

  // testing
  "org.specs2"                  %% "specs2-core"          % "3.6"         % "test"
)

lazy val web = (project in file("web")).
  settings(commonSettings: _*).
  settings(
    scalaJSProjects := Seq(webJS),
    pipelineStages := Seq(scalaJSProd)
  ).
  enablePlugins(PlayScala).
  aggregate(Seq(webJS).map(projectToRef):_*)

lazy val webJS = (project in file("web-js")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "test" % "0.8.4" % "test",
      "com.github.japgolly.scalajs-react" %%% "core" % "0.8.4",
      "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % "0.8.4",
      "com.github.japgolly.scalajs-react" %%% "ext-monocle" % "0.8.4",
      "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.4"),
    jsDependencies += "org.webjars" % "react" % "0.12.1" / "react-with-addons.js" commonJSName "React"
  ).
  enablePlugins(ScalaJSPlugin, ScalaJSPlay)

// The service that runs internally, on-site.
lazy val onSite = (project in file("on-site")).
  settings(commonSettings: _*).
  settings(
    name := "brush-training-facility"
  ).
  dependsOn(web)
