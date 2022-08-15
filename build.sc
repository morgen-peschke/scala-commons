import mill._, scalalib._, scalafmt._

val Scala12 = "2.12.16"
val Scala13 = "2.13.8"

trait CommonModule extends CrossScalaModule with ScalafmtModule {
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
}

object core extends Cross[CoreModule](Scala12, Scala13)
class CoreModule(val crossScalaVersion: String)
  extends CommonModule

object collections extends Cross[CollectionsModule](Scala12, Scala13)
class CollectionsModule(val crossScalaVersion: String)
  extends CommonModule {

  def moduleDeps = Seq(core(crossScalaVersion))

  object test extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.13")
  }
}