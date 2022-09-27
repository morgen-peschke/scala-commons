import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule
import mill._, scalalib._, scalafmt._
import mill.scalalib.publish._

val Scala12 = "2.12.16"
val Scala13 = "2.13.8"

val CatsCore = ivy"org.typelevel::cats-core:2.7.0"
val CatsParse = ivy"org.typelevel::cats-parse:0.3.7"
val CatsEffect = ivy"org.typelevel::cats-effect:3.3.14"
val SuperTagged = ivy"org.rudogma::supertagged:2.0-RC2"
val SourceCode = ivy"com.lihaoyi::sourcecode:0.3.0"

val ScalaCheck = ivy"org.scalacheck::scalacheck:1.16.0"
val ScalaTest = ivy"org.scalatest::scalatest:3.2.13"
val WordSpec = Set(ScalaTest, ivy"org.scalatest::scalatest-wordspec:3.2.13")
val PropSpec = Set(
  ScalaTest,
  ivy"org.scalatest::scalatest-propspec:3.2.13",
  ivy"org.scalatestplus::scalacheck-1-16:3.2.12.0"
)
val MUnit = ivy"org.scalameta::munit:0.7.29"

trait StyleModule extends ScalafmtModule with ScalafixModule {
  override def scalafixIvyDeps = super.scalafixIvyDeps() ++ Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0",
    ivy"org.typelevel::typelevel-scalafix:0.1.5"
  )

  def commonScalacOptions = Seq(
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-Ywarn-unused",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Xfatal-warnings",
    "-language:higherKinds"
  )

  override def forkEnv: T[Map[String, String]] = super.forkEnv().updated("SCALACTIC_FILL_FILE_PATHNAMES", "yes")

  def versionSpecificOptions(version: String) = version match {
    case Scala12 =>
      Seq(
        "-Ywarn-adapted-args",
        "-Ywarn-inaccessible",
        "-Ywarn-unused-import",
        "-Ypartial-unification"
      )
    case _ => Seq()
  }

  def crossScalaVersion: String

  override def scalacOptions =
    super.scalacOptions() ++ commonScalacOptions ++ versionSpecificOptions(crossScalaVersion)

  override def scalaDocOptions = super.scalaDocOptions() ++ Seq("-no-link-warnings")

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"com.olegpy::better-monadic-for:0.3.1",
    ivy"org.typelevel:::kind-projector:0.13.2"
  )
}

trait CommonModule
    extends CrossScalaModule
    with StyleModule
    with PublishModule {

  override def artifactName: T[String] = T { s"commons-${super.artifactName()}" }

  def publishVersion: T[String] = "0.2.0"

  override def pomSettings: T[PomSettings] = PomSettings(
    description = "Scala Commons - common utilities for Scala projects",
    organization = "com.github.morgen-peschke",
    url = "https://github.com/morgen-peschke/scala-commons",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("morgen-peschke", "scala-commons"),
    developers = Seq(
      Developer(
        "morgen-peschke",
        "Morgen Peschke",
        "https://github.com/morgen-peschke"
      )
    )
  )

  protected def outerCrossScalaVersion: String = crossScalaVersion
}

trait UsingScalaTestModule extends TestModule.ScalaTest with StyleModule
trait UsingMunitTestModule extends TestModule.Munit with StyleModule

object core extends Cross[CoreModule](Scala12, Scala13)
class CoreModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps = Agg(CatsCore, CatsParse, SuperTagged)

  object test extends Tests with UsingScalaTestModule {
    override def moduleDeps: Seq[JavaModule] =
      super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg.from(
      WordSpec ++ PropSpec + ivy"org.python:jython-slim:2.7.2"
    )

    override def crossScalaVersion: String = outerCrossScalaVersion
  }
}

object collections extends Cross[CollectionsModule](Scala12, Scala13)
class CollectionsModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(CatsCore)

  object test extends Tests with UsingScalaTestModule {
    override def moduleDeps: Seq[JavaModule] =
      super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg.from(WordSpec ++ PropSpec)
  }
}

object scalacheck extends Cross[ScalaCheckModule](Scala12, Scala13)
class ScalaCheckModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(ScalaCheck)

  override def moduleDeps: Seq[PublishModule] = super.moduleDeps ++ Seq(
    core(crossScalaVersion),
    collections(crossScalaVersion)
  )

  object test extends Tests with UsingScalaTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg.from(PropSpec)
  }
}

object decline extends Cross[DeclineModule](Scala12, Scala13)
class DeclineModule(val crossScalaVersion: String) extends CommonModule {
  override def moduleDeps: Seq[PublishModule] = super.moduleDeps ++ Seq(core(crossScalaVersion))

  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
    CatsCore,
    ivy"com.monovore::decline:2.3.0"
  )
}

object shims extends Cross[ShimsModule](Scala12, Scala13)
class ShimsModule(val crossScalaVersion: String) extends CommonModule {
  override def moduleDeps: Seq[PublishModule] = super.moduleDeps ++ Seq(core(crossScalaVersion))

  object test extends Tests with UsingScalaTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg.from(WordSpec)
  }
}

object testing extends Cross[TestingModule](Scala12, Scala13)
class TestingModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Seq(SourceCode, SuperTagged, CatsEffect, CatsCore)

  object test extends Tests with UsingMunitTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(MUnit)
  }
}

object munit extends Cross[MunitModule](Scala12, Scala13)
class MunitModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Seq(MUnit)

  override def moduleDeps: Seq[PublishModule] = super.moduleDeps :+ testing(crossScalaVersion)

  object test extends Tests with UsingMunitTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(MUnit)
  }
}

object scalatest extends Cross[ScalaTestModule](Scala12, Scala13)
class ScalaTestModule(val crossScalaVersion: String) extends CommonModule {
  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Seq(ScalaTest)

  override def moduleDeps: Seq[PublishModule] = super.moduleDeps :+ testing(crossScalaVersion)

  object test extends Tests with UsingScalaTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg.from(WordSpec)
  }
}