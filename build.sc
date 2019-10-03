import $ivy.`com.typesafe::mima-reporter:0.3.0`
import mill._
import mill.modules._
import mill.scalajslib._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import com.typesafe.tools.mima.lib.MiMaLib
import com.typesafe.tools.mima.core._

val scalaVersions = Seq("2.11.12", "2.12.8", "2.13.0")

trait CommonModule extends ScalaModule {
  def scalacOptions = T{ if (scalaVersion() startsWith "2.12") Seq("-opt:l:method") else Nil }
  def platformSegment: String

  def isScalaOld = T{ scalaVersion() startsWith "2.11" }

  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platformSegment"
  )
}
trait CommonPublishModule extends CommonModule with PublishModule with CrossScalaModule{
  def publishVersion = "1.0.0"

  protected def shade(name: String) = name + "-v1"
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.rallyhealth",
    url = "https://github.com/rallyhealth/weePickle",
    licenses = Seq(License.MIT),
    scm = SCM(
      "git://github.com/rallyhealth/weePickle.git",
      "scm:git://github.com/rallyhealth/weePickle.git"
    ),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )
}

trait CommonTestModule extends CommonModule with TestModule{
  def ivyDeps = T{
    if (isScalaOld())
      Agg(ivy"com.lihaoyi::utest::0.6.8", ivy"com.lihaoyi::acyclic:0.1.9")
    else
      Agg(ivy"com.lihaoyi::utest::0.7.1", ivy"com.lihaoyi::acyclic:0.2.0")
  }
  def testFrameworks = Seq("utest.runner.Framework")

  override def test(args: String*) = T.command{
    if (isScalaOld()) ("", Nil)
    else super.test(args: _*)()
  }

  override def sources = T.sources(
    if (isScalaOld()) Nil
    else super.sources()
  )
}

trait CommonJvmModule extends CommonPublishModule with MiMa {
  def platformSegment = "jvm"
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with CommonTestModule{
    def platformSegment = "jvm"

    override def test(args: String*) = T.command{
//      reportBinaryIssues() // TODO enable once a previous version is published.
      super.test(args: _*)()
    }
  }
}

trait CommonJsModule extends CommonPublishModule with ScalaJSModule{
  def platformSegment = "js"
  def scalaJSVersion = T{
    if (isScalaOld())
      "0.6.25"
    else
      "0.6.28"
  }
  def millSourcePath = super.millSourcePath / os.up
  trait Tests extends super.Tests with CommonTestModule{
    def platformSegment = "js"
    def scalaJSVersion = CommonJsModule.this.scalaJSVersion
  }
}

object core extends Module {

  object js extends Cross[CoreJsModule](scalaVersions: _*)
  class CoreJsModule(val crossScalaVersion: String) extends CommonJsModule {
    def artifactName = shade("weepickle-core")
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat::2.1.2"
    )

    object test extends Tests
  }

  object jvm extends Cross[CoreJvmModule](scalaVersions: _*)
  class CoreJvmModule(val crossScalaVersion: String) extends CommonJvmModule {
    def artifactName = shade("weepickle-core")
    def ivyDeps = Agg(
      ivy"org.scala-lang.modules::scala-collection-compat:2.1.2"
    )

    object test extends Tests
  }
}


object implicits extends Module {

  trait ImplicitsModule extends CommonPublishModule{
    def compileIvyDeps = T{
      Agg(
        ivy"com.lihaoyi::acyclic:${if (isScalaOld()) "0.1.8" else "0.2.0"}",
        ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
      )
    }
    def generatedSources = T{
      val dir = T.ctx().dest
      val file = dir / "weepickle" / "Generated.scala"
      ammonite.ops.mkdir(dir / "weepickle")
      val tuples = (1 to 22).map{ i =>
        def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")
        val writerTypes = commaSeparated(j => s"T$j: Writer")
        val readerTypes = commaSeparated(j => s"T$j: Reader")
        val typeTuple = commaSeparated(j => s"T$j")
        val implicitWriterTuple = commaSeparated(j => s"implicitly[Writer[T$j]]")
        val implicitReaderTuple = commaSeparated(j => s"implicitly[Reader[T$j]]")
        val lookupTuple = commaSeparated(j => s"x(${j-1})")
        val fieldTuple = commaSeparated(j => s"x._$j")
        s"""
        implicit def Tuple${i}Writer[$writerTypes]: TupleNWriter[Tuple$i[$typeTuple]] =
          new TupleNWriter[Tuple$i[$typeTuple]](Array($implicitWriterTuple), x => if (x == null) null else Array($fieldTuple))
        implicit def Tuple${i}Reader[$readerTypes]: TupleNReader[Tuple$i[$typeTuple]] =
          new TupleNReader(Array($implicitReaderTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
        """
      }

      ammonite.ops.write(file, s"""
      package com.rallyhealth.weepickle.v1.implicits
      import acyclic.file
      import language.experimental.macros
      /**
       * Auto-generated picklers and unpicklers, used for creating the 22
       * versions of tuple-picklers and case-class picklers
       */
      trait Generated extends com.rallyhealth.weepickle.v1.core.Types{
        ${tuples.mkString("\n")}
      }
    """)
      Seq(PathRef(dir))
    }

  }
  object js extends Cross[JsModule](scalaVersions: _*)
  class JsModule(val crossScalaVersion: String) extends ImplicitsModule with CommonJsModule{
    def moduleDeps = Seq(core.js())
    def artifactName = shade("weepickle-implicits")

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.js().test, core.js().test)
    }
  }

  object jvm extends Cross[JvmModule](scalaVersions: _*)
  class JvmModule(val crossScalaVersion: String) extends ImplicitsModule with CommonJvmModule{
    def moduleDeps = Seq(core.jvm())
    def artifactName = shade("weepickle-implicits")
    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm().test, core.jvm().test)
    }
  }
}

object upack extends Module {

  object js extends Cross[JsModule](scalaVersions: _*)
  class JsModule(val crossScalaVersion: String) extends CommonJsModule {
    def moduleDeps = Seq(core.js())
    def artifactName = shade("upack")

    object test extends Tests {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.js().test, core.js().test)
    }
  }

  object jvm extends Cross[JvmModule](scalaVersions: _*)
  class JvmModule(val crossScalaVersion: String) extends CommonJvmModule {
    def moduleDeps = Seq(core.jvm())
    def artifactName = shade("upack")
    object test extends Tests with CommonModule  {
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm().test, core.jvm().test)
    }
  }
}

object ujson extends Module{
  trait JsonModule extends CommonPublishModule{
    def artifactName = shade("ujson")
    trait JawnTestModule extends CommonTestModule{
      def ivyDeps = T{
        Agg(
          ivy"org.scalatest::scalatest::3.0.8",
          ivy"org.scalacheck::scalacheck::1.14.1"
        )
      }
      def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
  }

  object js extends Cross[JsModule](scalaVersions: _*)
  class JsModule(val crossScalaVersion: String) extends JsonModule with CommonJsModule{
    def moduleDeps = Seq(core.js())

    object test extends Tests with JawnTestModule
  }

  object jvm extends Cross[JvmModule](scalaVersions: _*)
  class JvmModule(val crossScalaVersion: String) extends JsonModule with CommonJvmModule{
    def moduleDeps = Seq(core.jvm())
    object test extends Tests with JawnTestModule
  }

  object argonaut extends Cross[ArgonautModule](scalaVersions: _*)
  class ArgonautModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = shade("ujson-argonaut")
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(ivy"io.argonaut::argonaut:6.2.3")
  }
  object json4s extends Cross[Json4sModule](scalaVersions: _*)
  class Json4sModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = shade("ujson-json4s")
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = Agg(
      ivy"org.json4s::json4s-ast:3.6.7",
      ivy"org.json4s::json4s-native:3.6.7"
    )
  }

  object circe extends Cross[CirceModule](scalaVersions: _*)
  class CirceModule(val crossScalaVersion: String) extends CommonPublishModule{
    def artifactName = shade("ujson-circe")
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = T{
      Agg(ivy"io.circe::circe-parser:${if (isScalaOld()) "0.11.1" else "0.12.1"}")
    }
  }

  object play extends Cross[PlayModule](scalaVersions:_*)
  class PlayModule(val crossScalaVersion: String) extends CommonPublishModule {

    def playVersion = T {
      if (isScalaOld()) "2.5.19" else "2.7.4"
    }
    def artifactName = T{
      val name = "ujson-play" + playVersion().split('.').take(2).mkString // e.g. "25", "27"
      shade(name)
    }
    def platformSegment = "jvm"
    def moduleDeps = Seq(ujson.jvm())
    def ivyDeps = T{
      Agg(
        ivy"com.typesafe.play::play-json:${playVersion()}"
      )
    }
  }
}

trait weepickleModule extends CommonPublishModule{
  def artifactName = shade("weepickle")
  def compileIvyDeps = Agg(
    ivy"com.lihaoyi::acyclic:${if (isScalaOld()) "0.1.8" else "0.2.0"}",
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}",
    ivy"org.scala-lang:scala-compiler:${scalaVersion()}"
  )
  def scalacOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8",
    "-feature",
  )
}


object weepickle extends Module{
  object jvm extends Cross[JvmModule](scalaVersions: _*)
  class JvmModule(val crossScalaVersion: String) extends weepickleModule with CommonJvmModule{
    def moduleDeps = Seq(ujson.jvm(), upack.jvm(), implicits.jvm())

    object test extends Tests with CommonModule{
      def moduleDeps = {
        super.moduleDeps ++ Seq(
          ujson.argonaut(),
          ujson.circe(),
          ujson.json4s(),
          ujson.play(),
          core.jvm().test
        )
      }
    }
  }

  object js extends Cross[JsModule](scalaVersions: _*)
  class JsModule(val crossScalaVersion: String) extends weepickleModule with CommonJsModule {
    def moduleDeps = Seq(ujson.js(), upack.js(), implicits.js())

    object test extends Tests with CommonModule{
      def moduleDeps = super.moduleDeps ++ Seq(core.js().test)
    }
  }
}

trait BenchModule extends CommonModule {
  def scalaVersion = "2.13.0"
  def millSourcePath = build.millSourcePath / "bench"
  def ivyDeps = Agg(
    ivy"io.circe::circe-core::0.12.1",
    ivy"io.circe::circe-generic::0.12.1",
    ivy"io.circe::circe-parser::0.12.1",
    ivy"com.typesafe.play::play-json::2.7.4",
    ivy"io.argonaut::argonaut:6.2.3",
    ivy"org.json4s::json4s-ast:3.6.7",
    ivy"com.lihaoyi::sourcecode::0.1.7",
  )
}

object bench extends Module {
  object js extends BenchModule with ScalaJSModule {
    def scalaJSVersion = "0.6.28"
    def platformSegment = "js"
    def moduleDeps = Seq(weepickle.js("2.13.0").test)
    def run(args: String*) = T.command {
      finalMainClassOpt() match{
        case Left(err) => mill.eval.Result.Failure(err)
        case Right(_) =>
          ScalaJSWorkerApi.scalaJSWorker().run(
            toolsClasspath().map(_.path),
            jsEnvConfig(),
            fullOpt().path.toIO
          )
          mill.eval.Result.Success(())
      }
    }
  }

  object jvm extends BenchModule with Jmh{
    def platformSegment = "jvm"
    def moduleDeps = Seq(weepickle.jvm("2.13.0").test)
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"com.fasterxml.jackson.module::jackson-module-scala:2.9.10",
      ivy"com.fasterxml.jackson.core:jackson-databind:2.9.4",
    )
  }
}

trait MiMa extends ScalaModule with PublishModule {
  def previousVersions = T {
    Seq("1.0.0")
  }

  def reportBinaryIssues = T {
    val msgs: Seq[String] = for {
      (prevArtifact, problems) <- mimaReportBinaryIssues()
        if problems.nonEmpty
    } yield {
      s"""Compared to artifact: ${prevArtifact}
         |found ${problems.size} binary incompatibilities:
         |${problems.mkString("\n")}""".stripMargin
    }

    if (msgs.nonEmpty) {
      sys.error(msgs.mkString("\n"))
    }
  }

  def mimaBinaryIssueFilters: Seq[ProblemFilter] = Seq.empty

  def previousDeps = T {
    Agg.from(previousVersions().map { version =>
      ivy"${pomSettings().organization}:${artifactId()}:${version}"
    })
  }

  def previousArtifacts = T {
    resolveDeps(previousDeps)().filter(_.path.segments.contains(artifactId()))
  }

  def mimaReportBinaryIssues: T[List[(String, List[String])]] = T {
    val currentClassfiles = compile().classes.path
    val classpath = runClasspath()

    val lib = {
      com.typesafe.tools.mima.core.Config.setup("sbt-mima-plugin", Array.empty)
      val cpstring = classpath
        .map(_.path)
        .filter(os.exists)
        .mkString(System.getProperty("path.separator"))
      new MiMaLib(
        com.typesafe.tools.mima.core.reporterClassPath(cpstring)
      )
    }

    previousArtifacts().toList.map { path =>
      val problems =
        lib.collectProblems(path.path.toString, currentClassfiles.toString)
      path.path.toString -> problems.filter { problem =>
        mimaBinaryIssueFilters.forall(_.apply(problem))
      }.map(_.description("current"))
    }
  }

}

trait Jmh extends ScalaModule {

  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.openjdk.jmh:jmh-core:1.21",
  )

  def runJmh(args: String*) = T.command {
    val (_, resources) = generateBenchmarkSources()
    Jvm.runSubprocess(
      "org.openjdk.jmh.Main",
      classPath = (runClasspath() ++ generatorDeps()).map(_.path) ++
        Seq(compileGeneratedSources().path, resources),
      mainArgs = args,
      workingDir = T.ctx.dest
    )
  }

  def compileGeneratedSources = T {
    val dest = T.ctx.dest
    val (sourcesDir, _) = generateBenchmarkSources()
    val sources = os.walk(sourcesDir).filter(os.isFile)
    os.proc("javac",
       sources.map(_.toString),
       "-cp",
       (runClasspath() ++ generatorDeps()).map(_.path.toString).mkString(":"),
       "-d",
       dest).call(dest)
    PathRef(dest)
  }

  // returns sources and resources directories
  def generateBenchmarkSources = T {
    val dest = T.ctx().dest

    val sourcesDir = dest / 'jmh_sources
    val resourcesDir = dest / 'jmh_resources

    os.remove.all(sourcesDir)
    os.makeDir.all(sourcesDir)
    os.remove.all(resourcesDir)
    os.makeDir.all(resourcesDir)

    Jvm.runSubprocess(
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      (runClasspath() ++ generatorDeps()).map(_.path),
      mainArgs = Array(
        compile().classes.path,
        sourcesDir,
        resourcesDir,
        "default"
      ).map(_.toString)
    )

    (sourcesDir, resourcesDir)
  }

  def generatorDeps = resolveDeps(
    T { Agg(ivy"org.openjdk.jmh:jmh-generator-bytecode:1.21") }
  )
}
