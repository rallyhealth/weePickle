import com.typesafe.tools.mima.core.{DirectMissingMethodProblem, ProblemFilters}
import sbt._

// root
name := "weePickle-root"
noPublish
crossScalaVersions := Nil // crossScalaVersions must be set to Nil on the aggregating project

lazy val bench = project
  .dependsOn(
    `weepickle-tests` % "compile;test",
  )
  .enablePlugins(JmhPlugin)
  .settings(
    noPublish,
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "3.1.3", // need the latest Scala 3 support
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.2",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % "2.15.2",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.msgpack" % "jackson-dataformat-msgpack" % "0.9.6",
      "com.lihaoyi" %% "sourcecode" % "0.3.1",
    )
  )

lazy val `weepickle-core` = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0"
    )
  )

lazy val `weepickle-implicits` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "3") Seq.empty else Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    },
    Compile / sourceGenerators += Def.task[Seq[File]] {
      val pkg = s"com.rallyhealth.weepickle.${shadedVersion.value}.implicits"
      val contents =
        s"""
      package $pkg
      import acyclic.file
      import language.experimental.macros
      /**
       * Auto-generated picklers and unpicklers, used for creating the 22
       * versions of tuple-picklers and case-class picklers
       */
      trait Generated extends com.rallyhealth.weepickle.v1.core.Types{
        ${
          (1 to 22).map { i =>
            def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")

            val writerTypes = commaSeparated(j => s"T$j: From")
            val readerTypes = commaSeparated(j => s"T$j: To")
            val typeTuple = commaSeparated(j => s"T$j")
            val implicitFromTuple = commaSeparated(j => s"implicitly[From[T$j]]")
            val implicitToTuple = commaSeparated(j => s"implicitly[To[T$j]]")
            val lookupTuple = commaSeparated(j => s"x(${j - 1})")
            val fieldTuple = commaSeparated(j => s"x._$j")
            s"""
        implicit def Tuple${i}From[$writerTypes]: TupleNFrom[Tuple$i[$typeTuple]] =
          new TupleNFrom[Tuple$i[$typeTuple]](Array($implicitFromTuple), x => if (x == null) null else Array($fieldTuple))
        implicit def Tuple${i}To[$readerTypes]: TupleNTo[Tuple$i[$typeTuple]] =
          new TupleNTo(Array($implicitToTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
        """
          }.mkString("\n")
        }
      }
      """

      val file = pkg.split('.').foldLeft((Compile / sourceManaged).value)(_ / _) / "Generated.scala"
      IO.write(file, contents)
      Seq(file)
    }
  )

lazy val weepickle = project
  .dependsOn(
    `weepickle-implicits`,
    weejson,
  )

/**
  * Place for tests that would otherwise cause sbt to have circular project dependencies.
  */
lazy val `weepickle-tests` = project
  .dependsOn(
    `weepickle-core` % "compile;test->test",
    `weejson-argonaut`,
    `weejson-circe`,
    `weejson-json4s`,
    `weejson-play-base`,
    `weejson` % "compile;test->test",
    `weepack` % "compile;test->test",
    `weepickle`,
    `weexml`,
    `weeyaml`,
  )
  .settings(
    noPublish,
  )

/**
  * ADTs:
  *
  *  - Value
  *  - BufferedValue
  */
lazy val weejson = project
  .dependsOn(`weejson-jackson`)

lazy val weepack = project
  .dependsOn(
    `weepickle-core` % "compile;test->test",
    weepickle,
    weejson % "test->test",
  )

/**
  * Json string parsing and generation.
  *
  * @see https://github.com/FasterXML/jackson-core
  */
lazy val `weejson-jackson` = project
  .dependsOn(`weepickle-core`)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.15.2"
    ),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.rallyhealth.weejson.v1.jackson.TextBufferCharSequence.isEmpty")
    )
  )

lazy val `weejson-circe` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % (if (scalaBinaryVersion.value == "2.11") "0.11.2" else "0.14.6")
    )
  )

lazy val `weejson-json4s` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= Seq(
      // 4.x.x breaks binary compatibility, consider for weePickle v2
      "org.json4s" %% "json4s-ast" % (if (scalaBinaryVersion.value == "3") "4.0.6" else "3.6.12"),
    )
  )
lazy val `weejson-argonaut` = project
  .dependsOn(weejson)
  .settings(
    Compile / unmanagedSourceDirectories ++= (
      if (scalaBinaryVersion.value == "2.11") Nil else Seq((Compile / sourceDirectory).value / "scala-2.12+")
    ),
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % (if (scalaBinaryVersion.value == "2.11") "6.2.6" else "6.3.9"),
    )
  )

// We need one project for 'weepickle-testing' to dependOn that is available on all Scala versions.
// This "base" project uses play-json 2.7 when on Scala 2.11 and play-json 2.10 when on later Scala versions.
lazy val `weejson-play-base` = (project in file("weejson-play"))
  .dependsOn(weepickle)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % (if (scalaBinaryVersion.value == "2.11") "2.7.4" else "2.10.1"),
    ),
    noPublish
  )

def playProject(playVersion: String, scalaVersions: Seq[String]) = {
  val playString = playVersion.split('.').take(2).mkString("play", "", "")
  val playOrg =
    if (playVersion.startsWith("3")) "org.playframework"
    else "com.typesafe.play"
  Project(s"weejson-$playString", file(s"weejson-$playString"))
    .dependsOn(weepickle)
    .settings(
      libraryDependencies += playOrg %% "play-json" % playVersion,
      Compile / unmanagedSourceDirectories ++= (`weejson-play-base` / Compile / unmanagedSourceDirectories).value,
      Test / unmanagedSourceDirectories ++= (`weejson-play-base` / Test / unmanagedSourceDirectories).value,
      crossScalaVersions := scalaVersions,
      scalaVersion := scalaVersions.last,
      ideSkipProject := true
    )
}

lazy val `weejson-play25` = playProject("2.5.19", Seq(scala211))

lazy val `weejson-play27` = playProject("2.7.4", supportedScala2Versions)

lazy val `weejson-play28` = playProject("2.8.2", Seq(scala213))

// Using play-json version numbering, which is slightly confusing. Play 2.9 uses play-json 2.10 for some reason,
// and no version of Play actually uses play-json 2.9
lazy val `weejson-play29` = playProject("2.9.4", Seq(scala213))

lazy val `weejson-play210` = playProject("2.10.2", Seq(scala213, scala3))

lazy val `weejson-play30` = playProject("3.0.0", Seq(scala213, scala3))
  .settings(mimaPreviousArtifacts := Set.empty) // TODO: remove once there's a previous artifact

lazy val `weejson-upickle` = project
  .dependsOn(weepickle)
  .settings(
    libraryDependencies ++= Seq(
      // 2.x.x/3.x.x break binary compatibility, consider for weePickle v2
      "com.lihaoyi" %% "upickle" % "1.6.0",
    ),
    mimaPreviousArtifacts := {
      if (VersionNumber(version.value).matchesSemVer(SemanticSelector("<1.6.0")))
        Set.empty
      else
        mimaPreviousArtifacts.value
    }
  )

lazy val weeyaml = project
  .dependsOn(`weejson-jackson`)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.15.2",
    )
  )

lazy val weexml = project
  .dependsOn(
    `weejson-jackson`,
    weejson % Test,
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.15.2",
    )
  )

lazy val `weepickle-macro-lint-tests` = project
  .dependsOn(weepickle)
  .settings(noPublish)
  .settings(scalacOptions := (scalacOptions.value ++ Seq("-Xlint", "-Xfatal-warnings")).distinct)
  .settings(crossScalaVersions := supportedScala2Versions) // TODO: Scala 3?
//  .settings(scalacOptions += "-Ymacro-debug-lite")
