import Deps._
import Deps.TestFrameworks.{scalaTest, _}
import sbt.Keys._
import java.net.URL

name := "job-man"

version      in ThisBuild := "0.5.0-SNAPSHOT"
organization in ThisBuild := "pl.newicom.jobman"
scalaVersion in ThisBuild := "2.12.7"

publishMavenStyle in ThisBuild := true
homepage          in ThisBuild := Some(new URL("http://github.com/pawelkaczor/job-man"))
licenses          in ThisBuild := ("MIT", new URL("http://raw.githubusercontent.com/pawelkaczor/job-man/master/LICENSE.md")) :: Nil

sonatypeProfileName := "pl.newicom"

lazy val root = (project in file("."))
  .aggregate(`job-man-core`)
  .settings(
    commonSettings,
    publishArtifact := false
  )


lazy val `job-man-core` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(Akka.actor, scalaTest % "test")
  )


lazy val commonSettings: Seq[Setting[_]] = Publish.settings ++ Seq(
  licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/job-man/master/LICENSE.md")),
  startYear := Some(2018),
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
)