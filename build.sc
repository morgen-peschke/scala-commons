import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule
import mill._, scalalib._, scalafmt._
import mill.scalalib.publish._

val Scala12 = "2.12.16"
val Scala13 = "2.13.8"

val CatsCore = ivy"org.typelevel::cats-core:2.7.0"
val CatsParse = ivy"org.typelevel::cats-parse:0.3.7"

val SuperTagged = ivy"org.rudogma::supertagged:2.0-RC2"

val ScalaCheck = ivy"org.scalacheck::scalacheck:1.16.0"
val ScalaTest = ivy"org.scalatest::scalatest:3.2.13"
val WordSpec = Set(ScalaTest, ivy"org.scalatest::scalatest-wordspec:3.2.13")
val PropSpec = Set(
  ScalaTest,
  ivy"org.scalatest::scalatest-propspec:3.2.13",
  ivy"org.scalatestplus::scalacheck-1-16:3.2.12.0"
)

trait StyleModule extends ScalafmtModule with ScalafixModule {
  override def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")

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
    commonScalacOptions ++ versionSpecificOptions(crossScalaVersion)

  override def scalaDocOptions = Seq("-no-link-warnings")

  override def scalacPluginIvyDeps = Agg(
    ivy"com.olegpy::better-monadic-for:0.3.1",
    ivy"org.typelevel:::kind-projector:0.13.2"
  )
}

trait CommonModule
    extends CrossScalaModule
    with StyleModule
    with PublishModule {
  def publishVersion = "0.1.0"

  def pomSettings = PomSettings(
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

trait CommonTestModule extends TestModule.ScalaTest with StyleModule

object core extends Cross[CoreModule](Scala12, Scala13)
class CoreModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "commons-core"

  override def ivyDeps = Agg(CatsCore, CatsParse, SuperTagged)

  object test extends Tests with CommonTestModule {
    override def moduleDeps =
      super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def ivyDeps = Agg.from(
      WordSpec ++ PropSpec + ivy"org.python:jython-slim:2.7.2"
    )

    override def crossScalaVersion = outerCrossScalaVersion
  }
}

object collections extends Cross[CollectionsModule](Scala12, Scala13)
class CollectionsModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "commons-collections"

  object test extends Tests with CommonTestModule {
    override def moduleDeps =
      super.moduleDeps ++ Seq(scalacheck(crossScalaVersion))

    override def crossScalaVersion = outerCrossScalaVersion

    override def ivyDeps = Agg.from(WordSpec)
  }
}

object scalacheck extends Cross[ScalaCheckModule](Scala12, Scala13)
class ScalaCheckModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "commons-scalacheck"

  override def ivyDeps = Agg(ScalaCheck)

  override def moduleDeps = super.moduleDeps ++ Seq(core(crossScalaVersion))

  object test extends Tests with CommonTestModule {
    override def crossScalaVersion = outerCrossScalaVersion

    override def ivyDeps = Agg.from(PropSpec)
  }
}

object decline extends Cross[DeclineModule](Scala12, Scala13)
class DeclineModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "commons-decline"

  override def moduleDeps = super.moduleDeps ++ Seq(core(crossScalaVersion))

  override def ivyDeps = Agg(
    CatsCore,
    ivy"com.monovore::decline:2.3.0"
  )
}

object shims extends Cross[ShimsModule](Scala12, Scala13)
class ShimsModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "commons-shims"

  override def moduleDeps = super.moduleDeps ++ Seq(core(crossScalaVersion))

  object test extends Tests with CommonTestModule {
    override def crossScalaVersion: String = outerCrossScalaVersion

    override def ivyDeps = Agg.from(WordSpec)
  }
}
