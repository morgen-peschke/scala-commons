import mill._, scalalib._, scalafmt._
import mill.scalalib.publish._

val Scala12 = "2.12.16"
val Scala13 = "2.13.8"

trait CommonModule extends CrossScalaModule with ScalafmtModule with PublishModule {
  def publishVersion = "0.0.1"

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
    "-Xfatal-warnings"
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
}

object core extends Cross[CoreModule](Scala12, Scala13)
class CoreModule(val crossScalaVersion: String)
  extends CommonModule {

  def artifactName = "commons-core"
}

object collections extends Cross[CollectionsModule](Scala12, Scala13)
class CollectionsModule(val crossScalaVersion: String)
  extends CommonModule {

  def artifactName = "commons-collections"

  def moduleDeps = Seq(core(crossScalaVersion))

  object test extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.13")
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
}