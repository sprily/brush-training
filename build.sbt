import sbt.Project.projectToRef

lazy val commonSettings = Seq(
  organization := "uk.co.sprily",
  version := "0.1.2-SNAPSHOT",
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
  "scalaz-bintray"  at "http://dl.bintray.com/scalaz/releases",
  "Sprily Releases" at "https://repo.sprily.co.uk/nexus/content/repositories/releases"
)

lazy val commonDependencies = Seq(

  // config
  "com.github.kxbmap"           %% "configs"              % "0.2.4",

  // logging
  "com.typesafe.scala-logging"  %% "scala-logging"        % "3.1.0",
  "ch.qos.logback"               % "logback-core"         % "1.1.2",
  "ch.qos.logback"               % "logback-classic"      % "1.1.2",

  // testing
  "org.specs2"                  %% "specs2-core"          % "3.6"         % "test",
  "org.specs2"                  %% "specs2-junit"         % "3.6"         % "test"
)

lazy val web = (project in file("web")).
  settings(commonSettings: _*).
  settings(

    // An attempt to prevent the root project's propogation of the debian:packageBin
    // Task from building the web sub-project.  The reason it does is because web
    // enables the Play plugin, which must auto-enable the native packager, but attempts
    // to disable it again don't work.  So this workaround is to hard-code the Task itself
    // to not do anything useful.  There's a SO question on a similar problem, with no
    // solution yet:
    //
    // http://stackoverflow.com/questions/28948964/dont-publish-a-docker-image-for-each-sbt-subproject
    packageBin in Debian := file(""),

    scalaJSProjects := Seq(webJS),
    pipelineStages := Seq(scalaJSProd),
    libraryDependencies ++= Seq(
      "com.vmunier"           %% "play-scalajs-scripts" % "0.2.1",
      "org.scalaz.stream"     %% "scalaz-stream"        % "0.7a",
      "uk.co.sprily"          %% "dh-modbus"            % "0.1.0-SNAPSHOT"
    )
  ).
  enablePlugins(PlayScala).
  aggregate(Seq(webJS).map(projectToRef):_*)

lazy val webJS = (project in file("web-js")).
  settings(commonSettings: _*).
  settings(
    scalaJSStage := FastOptStage,
    persistLauncher in Compile := true,
    persistLauncher in Test    := false,
    skip in packageJSDependencies := false,
    libraryDependencies ++= Seq(
      "org.monifu" %%% "monifu" % "1.0-M1",
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
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
  enablePlugins(PlayScala).
  dependsOn(web)
