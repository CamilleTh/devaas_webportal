import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "webportal"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
      "joda-time" % "joda-time" % "2.1",
      "commons-codec" % "commons-codec" % "1.7",
      "com.jcraft" % "jsch" % "0.1.48"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/releases/"
    )

}
