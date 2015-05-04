import com.typesafe.sbt.packager.archetypes.ServerLoader.SystemV

// Packaging configuration for the on-site service
//enablePlugins(JDebPackaging)
enablePlugins(DebianPlugin)
enablePlugins(JavaServerAppPackaging)

maintainer := "Ian Murray <ian@sprily.co.uk>"
packageSummary := "Brush Training Facility Meter Display"

val Snapshot       = """^(\d+\.\d+\.\d+)-SNAPSHOT$""".r
val WithQuantifier = """^(\d+\.\d+\.\d+)-(.*)$""".r
val NoQuantifier   = """^(\d+\.\d+\.\d+)$""".r

// TODO: the following 2 settings could be shared by defining them in an autoplugin.
//       see http://www.scala-sbt.org/0.13/tutorial/Organizing-Build.html

val buildCount = SettingKey[Option[String]]("buildCount", "Used as the least significant build version number, eg the debian_revision")

val commitId = SettingKey[Option[String]]("commitId", "Used to identify the commit that this package was built from.")

buildCount := Option(System.getenv().get("GO_PIPELINE_COUNTER"))
commitId := Option(System.getenv().get("GO_REVISION"))

// Match debian versioning
version in Debian <<= (version, buildCount, commitId) { (v, build, commit) =>
  val buildStr = build.map("-" + _).getOrElse("")
  val commitStr = commit.map(_.take(8)).map("~" + _).getOrElse("")
  v match {
    case Snapshot(v)         => v + "~~" + "SNAPSHOT" + buildStr + commitStr
    case WithQuantifier(v,q) => v + "~" + q + "-" + buildStr
    case NoQuantifier(v)     => v + "-" + buildStr
  }
}

debianPackageDependencies in Debian ++= Seq("java2-runtime")
serverLoading in Debian := SystemV
bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/brush-training-facility.conf""""
