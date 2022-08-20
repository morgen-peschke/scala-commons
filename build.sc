import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule
import mill._, scalalib._, scalafmt._
import mill.scalalib.publish._

val Scala12 = "2.12.16"
val Scala13 = "2.13.8"

trait CommonModule extends CrossScalaModule with ScalafmtModule with ScalafixModule with PublishModule {
  def publishVersion = "0.1.0"

  def pomSettings = PomSettings(
    description = "Scala Commons - common utilities for Scala projects",
    organization = "com.github.morgen-peschke",
    url = "https://github.com/morgen-peschke/scala-commons",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("morgen-peschke", "scala-commons"),
    developers = Seq(
      Developer("morgen-peschke", "Morgen Peschke", "https://github.com/morgen-peschke")
    )
  )

  def crossScalaVersion: String

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

  def versionSpecificOptions(version: String) = version match {
    case Scala12 => Seq(
      "-Ywarn-adapted-args",
      "-Ywarn-inaccessible",
      "-Ywarn-unused-import",
      "-Ypartial-unification"
    )
    case _ => Seq()
  }

  def scalacOptions = commonScalacOptions ++ versionSpecificOptions(crossScalaVersion)

  def scalaDocOptions = Seq("-no-link-warnings")

  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")
}

object core extends Cross[CoreModule](Scala12, Scala13)
class CoreModule(val crossScalaVersion: String)
  extends CommonModule {

  override def artifactName = "commons-core"

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:2.7.0",
    ivy"org.rudogma::supertagged:2.0-RC2",
    ivy"org.typelevel::cats-parse:0.3.7"
  )

  object test extends Tests with TestModule.ScalaTest {

    override def moduleDeps = super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def ivyDeps = Agg(
      ivy"org.scalacheck::scalacheck:1.16.0",
      ivy"org.scalatest::scalatest:3.2.13",
      ivy"org.scalatest::scalatest-wordspec:3.2.13",
      ivy"org.scalatest::scalatest-propspec:3.2.13",
      ivy"org.scalatestplus::scalacheck-1-16:3.2.12.0",
      ivy"org.python:jython-slim:2.7.2"
    )
  }
}

object collections extends Cross[CollectionsModule](Scala12, Scala13)
class CollectionsModule(val crossScalaVersion: String)
  extends CommonModule {

  override def artifactName = "commons-collections"

  object test extends Tests with TestModule.ScalaTest {

    override def moduleDeps = super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def ivyDeps = Agg(
      ivy"org.scalacheck::scalacheck:1.16.0",
      ivy"org.scalatest::scalatest:3.2.13",
      ivy"org.scalatest::scalatest-wordspec:3.2.13",
      ivy"org.scalatest::scalatest-propspec:3.2.13",
      ivy"org.scalatestplus::scalacheck-1-16:3.2.12.0",
      ivy"org.python:jython-slim:2.7.2"
    )
  }
}

object scalacheck extends Cross[ScalaCheckModule](Scala12, Scala13)
class ScalaCheckModule(val crossScalaVersion: String)
  extends CommonModule {

  override def artifactName = "commons-scalacheck"

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:2.7.0",
    ivy"org.scalacheck::scalacheck:1.16.0"
  )

  override def moduleDeps = super.moduleDeps ++ Seq(core(crossScalaVersion))

  object test extends Tests with TestModule.ScalaTest {
    override def ivyDeps = Agg(
      ivy"org.scalacheck::scalacheck:1.16.0",
      ivy"org.scalatest::scalatest:3.2.13",
      ivy"org.scalatest::scalatest-propspec:3.2.13",
      ivy"org.scalatestplus::scalacheck-1-16:3.2.12.0"
    )
  }
}

object decline extends Cross[DeclineModule](Scala12, Scala13)
class DeclineModule(val crossScalaVersion: String)
  extends CommonModule {

  override def artifactName = "commons-decline"

  override def moduleDeps = super.moduleDeps ++ Seq(core(crossScalaVersion))

  override def ivyDeps = Agg(ivy"com.monovore::decline:2.3.0")
}