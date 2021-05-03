import com.typesafe.tools.mima.plugin.MimaKeys.{mimaFailOnNoPrevious, mimaPreviousArtifacts}
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
    val scala213 = "2.13.5"
    val supportedScalaVersions = Seq(scala211, scala212, scala213)

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
    scalaVersion := scala212, // for IDE
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
    acyclic := "com.lihaoyi" %% "acyclic" % (if (scalaBinaryVersion.value == "2.11") "0.1.8" else "0.2.0"),
    autoCompilerPlugins := true,
    crossScalaVersions := autoImport.supportedScalaVersions,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.7.9" % "test",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
      "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
      compilerPlugin(acyclic.value),
      acyclic.value % "provided"
    ),
    mimaFailOnNoPrevious := false, // bintray is gone.
    mimaPreviousArtifacts ++= {
//      previousStableVersion.value
//        .map(organization.value %% moduleName.value % _.toString)
//        .toSet
      Set.empty
    },
    moduleName := s"${moduleName.value}-v${version.value.split('.').head}",
    scalacOptions ++= {
      val builder = Seq.newBuilder[String]
      builder ++= Seq(
        "-deprecation",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xfatal-warnings",
        "-encoding",
        "utf8",
        "-feature"
      )

      if (scalaBinaryVersion.value > "2.11") {
        builder += "-opt:l:method"
      }

      if (scalaBinaryVersion.value == "2.13") {
        builder ++= Seq(
          // See: https://github.com/scala/scala/pull/8373
          """-Wconf:any:warning-verbose""",
          """-Wconf:cat=deprecation:info-summary""" // Not ready to deal with 2.13 collection deprecations.
        )
      }

      builder.result()
    },
    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
}
