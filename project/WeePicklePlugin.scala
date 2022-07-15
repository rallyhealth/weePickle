import com.typesafe.tools.mima.plugin.MimaKeys.{mimaPreviousArtifacts, mimaReportBinaryIssues}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbtdynver.DynVerPlugin.autoImport.previousStableVersion

object WeePicklePlugin extends AutoPlugin {

  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {

    val scala211 = "2.11.12"
    val scala212 = "2.12.12"
    val scala213 = "2.13.8"
    val scala3 = "3.1.0"
    val supportedScala2Versions = Seq(scala211, scala212, scala213)
    val supportedScalaVersions = supportedScala2Versions :+ scala3

    lazy val ideSkipProject = SettingKey[Boolean]("ideSkipProject")

    val shadedVersion = settingKey[String]("e.g. v1")

    def noPublish = Seq(
      mimaPreviousArtifacts := Set.empty,
      publish / skip := true,
    )
  }

  import autoImport._

  private val acyclic = settingKey[ModuleID]("")

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    excludeLintKeys += autoImport.ideSkipProject,
  )

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    crossScalaVersions := supportedScalaVersions, // because SCL-18636
    licenses := Seq(("MIT License", url("https://opensource.org/licenses/mit-license.html"))),
    organization := "com.rallyhealth",
    organizationHomepage := Some(url("https://www.rallyhealth.com")),
    organizationName := "Rally Health",
    scalaVersion := scala213, // for IDE
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/rallyhealth/weePickle"),
        "scm:git@github.com:rallyhealth/weePickle.git"
      )
    ),
    shadedVersion := "v" + version.value.split('.').head,
    startYear := Some(2019),
    versionScheme := Some("semver-spec"),
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    acyclic := ("com.lihaoyi" %% "acyclic" % (if (scalaBinaryVersion.value == "2.11") "0.1.8" else "0.2.0")) cross CrossVersion.for3Use2_13,
    autoCompilerPlugins := true,
    crossScalaVersions := autoImport.supportedScalaVersions,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.7.10" % "test",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % (if (scalaBinaryVersion.value == "2.11") "3.2.3.0" else "3.2.9.0") % "test",
      "org.scalacheck" %% "scalacheck" % (if (scalaBinaryVersion.value == "2.11") "1.15.2" else "1.15.4") % "test",
      compilerPlugin(acyclic.value),
      acyclic.value % "provided"
    ),
    mimaPreviousArtifacts := {
      if (scalaBinaryVersion.value == "3") Set.empty // TODO: remove once there's a previous Scala 3 artifact
      else
        previousStableVersion.value
          .map(organization.value %% moduleName.value % _)
          .toSet
    },
    (Test / test) := {
      mimaReportBinaryIssues.value
      (Test / test).value
    },
    moduleName := s"${moduleName.value}-v${version.value.split('.').head}",
    scalacOptions ++= {
      val builder = Seq.newBuilder[String]
      builder ++= Seq(
        "-deprecation",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xfatal-warnings",
//        "-encoding",
//        "utf8",
        "-feature"
      )

      CrossVersion.partialVersion(scalaVersion.value).foreach {
        case (3, _) =>
          builder ++= Seq(
            "-Xmax-inlines",
            "128" // MacroTests exceeds default of 32 inlines, 64 is still too small
            //TODO: what new options should we add? See: https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
          )
          ()

        case (2, 13) =>
          builder ++= Seq(
            "-opt:l:method",
            // See: https://github.com/scala/scala/pull/8373
            """-Wconf:any:warning-verbose""",
            """-Wconf:cat=deprecation:info-summary""" // Not ready to deal with 2.13 collection deprecations.
          )
          ()

        case (2, 12) =>
          builder += "-opt:l:method"
          ()

        case _ => // 2.11 - nothing special
          ()
      }

      builder.result()
    },
    Test / scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "3" => Seq(
          "-Wconf:cat=deprecation:info" // scalacheck uses Stream
        )
        case _ => Nil
      }
    },
    Test / scalacOptions := {
      // FromJsonTests.scala:34:3 "A pure expression does nothing in statement position"
      (Test / scalacOptions).value.filterNot(_ == "-Xfatal-warnings")
    },
    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
}
