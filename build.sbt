import com.typesafe.tools.mima.core.{DirectMissingMethodProblem, ProblemFilters}
import sbt._

// root
name := "weePickle-root"
noPublish
crossScalaVersions := Nil // crossScalaVersions must be set to Nil on the aggregating project

//lazy val bench = project
//  .dependsOn(`weepickle-tests` % "compile;test", `weejson-play27`)
//  .enablePlugins(JmhPlugin)
//  .settings(
//    noPublish,
//    crossScalaVersions := Seq(scala212),
//    libraryDependencies ++= Seq(
//      "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3",
//      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % "2.12.3",
//      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3",
//      "com.lihaoyi" %% "upickle" % "1.3.8",
//      "io.circe" %% "circe-generic" % "0.13.0",
//      "io.circe" %% "circe-parser" % "0.13.0",
//      "org.msgpack" % "jackson-dataformat-msgpack" % "0.8.22",
//      "com.lihaoyi" %% "sourcecode" % "0.1.7",
//    )
//  )

lazy val `weepickle-core` = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % (if (scalaBinaryVersion.value == "3") "2.5.0" else "2.4.3")
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
    //`weejson-play27`, // TODO: add back once 2.10.0 is available
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
      "com.fasterxml.jackson.core" % "jackson-core" % "2.12.3"
    ),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.rallyhealth.weejson.v1.jackson.TextBufferCharSequence.isEmpty")
    )
  )

lazy val `weejson-circe` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % (if (scalaBinaryVersion.value == "2.11") "0.11.2" else "0.14.1")
    )
  )

lazy val `weejson-json4s` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-ast" % (if (scalaBinaryVersion.value == "3") "4.0.1" else "3.6.10"),
    )
  )
lazy val `weejson-argonaut` = project
  .dependsOn(weejson)
  .settings(
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % (if (scalaBinaryVersion.value == "3") "6.3.6" else "6.2.5") ,
    )
  )

lazy val `weejson-play27` = (project in file("weejson-play"))
  .dependsOn(weepickle)
  .settings(
    crossScalaVersions := Seq(/*scala211, scala212,*/ scala213), // TODO: 2.10.0-RCs still on Scala 3.0.0-RCs
    libraryDependencies ++= Seq(
//      if (scalaBinaryVersion.value == "3")
//        "com.typesafe.play" % "play-json_3.0.0-RC1" % "2.10.0-RC2" else
      "com.typesafe.play" %% "play-json" % "2.7.4",
//      ("com.typesafe.play" %% "play-json" % "2.7.4") cross CrossVersion.for3Use2_13)
    )
  )

def playProject(playVersion: String, scalaVersions: Seq[String]) = {
  val playString = playVersion.split('.').take(2).mkString("play", "", "")
  Project(s"weejson-$playString", file(s"weejson-$playString"))
    .dependsOn(weepickle)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % playVersion,
      )
      ,
      Compile / unmanagedSourceDirectories ++= (`weejson-play27` / Compile / unmanagedSourceDirectories).value,
      Test / unmanagedSourceDirectories ++= (`weejson-play27` / Test / unmanagedSourceDirectories).value,
      crossScalaVersions := scalaVersions,
      scalaVersion := scalaVersions.last,
      ideSkipProject := true
    )
}
//
//lazy val `weejson-play25` = playProject("2.5.19", Seq(scala211))
//
//lazy val `weejson-play28` = playProject("2.8.1", Seq(scala213))
//
//lazy val `weejson-play29` = playProject("2.9.2", Seq(scala213))

lazy val weeyaml = project
  .dependsOn(`weejson-jackson`)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.12.3",
    )
  )

lazy val weexml = project
  .dependsOn(`weejson-jackson`)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.12.3",
    )
  )
