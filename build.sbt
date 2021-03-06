import sbt.Project.projectToRef

lazy val commonSettings = Seq(
  organization := "uk.co.sprily",
  version := "0.1.2",
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-Xlint",
    "-Ywarn-unused-import",
    "-unchecked"),
  resolvers ++= commonResolvers
)

lazy val commonSettingsWithDeps = Seq(
  libraryDependencies ++= commonDependencies
) ++ commonSettings

lazy val commonResolvers = Seq(
  "scalaz-bintray"  at "http://dl.bintray.com/scalaz/releases",
  "Sprily Releases" at "https://repo.sprily.co.uk/nexus/content/repositories/releases",
  "Sprily Third Party" at "https://repo.sprily.co.uk/nexus/content/repositories/thirdparty"
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

lazy val webJS = (project in file("web-js")).
  settings(commonSettingsWithDeps: _*).
  settings(
    requiresDOM := true,
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
      "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.4",
      "com.github.japgolly.scalacss"      %%% "core" % "0.2.0",
      "com.github.japgolly.scalacss"      %%% "ext-react" % "0.2.0",
      "com.lihaoyi"                       %%% "upickle" % "0.2.8",
      "com.lihaoyi"                       %%% "utest" % "0.3.1"
    ),
    jsDependencies ++= Seq(
      "org.webjars" % "react"          % "0.12.1" / "react-with-addons.min.js" commonJSName "React",
      "org.webjars" % "log4javascript" % "1.4.10" / "js/log4javascript.js"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  ).
  enablePlugins(ScalaJSPlugin, ScalaJSPlay, SbtWeb).
  dependsOn(webSharedJs)

lazy val webShared = (crossProject.crossType(CrossType.Pure) in file("web-shared")).
  settings(commonSettings:_*).
  settings(
    libraryDependencies ++= Seq(
      "org.webjars" % "font-awesome" % "4.3.0-1" % Provided,
      "org.webjars" % "bootstrap" % "3.3.4" % Provided,
      "com.lihaoyi"           %% "upickle"              % "0.2.8"
    ),
    LessKeys.compress in Assets := true
  ).
  jsConfigure(_ enablePlugins ScalaJSPlay).
  jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val webSharedJvm = webShared.jvm
lazy val webSharedJs  = webShared.js

// The service that runs internally, on-site.
lazy val onSite = (project in file("on-site")).
  settings(commonSettingsWithDeps: _*).
  settings(
    name := "brush-training-facility",

    // some mapping names end up with double-slashes (in particular, the config
    // files defined in on-site/conf.  The problem with this is that the .deb
    // file that's created ends up with a mis-configured conffiles; resulting
    // in those files not being correctly defined as configuration files, and
    // so they end up being overwritten on a package update.
    mappings in Universal := {
      (mappings in Universal).value.map {
        case (file, name) => (file, name.replace("//", "/"))
      }
    },
    scalaJSProjects := Seq(webJS),
    pipelineStages := Seq(scalaJSProd),
    libraryDependencies ++= Seq(
      "com.vmunier"           %% "play-scalajs-scripts" % "0.2.1",
      "org.scalaz.stream"     %% "scalaz-stream"        % "0.7a",
      "uk.co.sprily"          %% "dh-modbus"            % "0.1.3",
      "uk.co.sprily"          %% "dh-util"              % "0.1.3",
      "uk.co.sprily"          %% "scala-mqtt-core"      % "0.1.4",
      "uk.co.sprily"          %% "scala-mqtt-logback"   % "0.1.4",
      "org.scodec"            %% "scodec-core"          % "1.7.0",
      "org.scodec"            %% "scodec-bits"          % "1.0.6",
      "com.adrianhurt"        %% "play-bootstrap3"      % "0.4.2",
      "com.google.guava"       % "guava"                % "18.0"
    ),
    
    // Despite what the docs say [1], our custom postinst script is *replacing*
    // the one generated to start/stop services etc.  This prevents that behaviour,
    // and instead appends our customisation to the existing postinst script.
    //
    // [1] http://www.scala-sbt.org/sbt-native-packager/formats/debian.html#id7
    debianMakePostinstScript := {

      val result = for {
        maintPostinst  <- debianMakePostinstScript.value
        toAppend       = (debianControlScriptsDirectory / "postinst-append").value
        if toAppend.exists
        appendContents = IO.read(toAppend)
      } yield (maintPostinst, appendContents)

      result.foreach { case (f, content) =>
        IO.write(f, content, append=true)
      }

      result.map(_._1)
    }
  ).
  enablePlugins(PlayScala).
  dependsOn(webSharedJvm).
  aggregate(Seq(webJS).map(projectToRef):_*)
