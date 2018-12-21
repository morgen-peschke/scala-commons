
name := "Common Libraries"

lazy val root =
  (project in file("."))
    .settings(
      version := "1.0.0",
      scalaVersion := "2.12.3",
      crossScalaVersions := Seq("2.11.8"),
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-deprecation",
        "-unchecked",
        "-feature",
        "-Ywarn-adapted-args",
        "-Ywarn-inaccessible",
        "-Ywarn-unused",
        "-Ywarn-dead-code",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Xfatal-warnings"),
      scalacOptions in (Compile, doc) += s"-doc-external-doc:${scalaInstance.value.libraryJar}#http://www.scala-lang.org/api/${scalaVersion.value}/",
      scapegoatVersion := "1.3.0",
      scapegoatReports := Seq("html"),
      coverageMinimum := 80,
      coverageHighlighting := true,
      coverageOutputXML := false,
      libraryDependencies ++= Seq(
        "org.scalactic" %% "scalactic" % "3.0.1" % "test",
        "org.scalatest" %% "scalatest" % "3.0.2" % "test"))
