import Deps._
import Deps.TestFrameworks.scalaTest
import sbt.Keys._
import java.net.URL

import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile

name := "job-man"

version      in ThisBuild := "0.5.0-SNAPSHOT"
organization in ThisBuild := "pl.newicom.jobman"
scalaVersion in ThisBuild := "2.12.7"

scalacOptions     in ThisBuild := Seq("-encoding", "utf8", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions", "-language:higherKinds", "-unchecked")

publishMavenStyle in ThisBuild := true
homepage          in ThisBuild := Some(new URL("http://github.com/pawelkaczor/job-man"))
licenses          in ThisBuild := ("MIT", new URL("http://raw.githubusercontent.com/pawelkaczor/job-man/master/LICENSE.md")) :: Nil

sonatypeProfileName := "pl.newicom"

lazy val root = (project in file("."))
  .aggregate(`job-man-api`, `job-scheduling-policy`, `job-man-core`, `job-man-rest`, `sample-app`)
  .settings(
    commonSettings,
    publishArtifact := false
  )

lazy val `job-man-api` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(Akka.actorTyped)
  )

lazy val `job-scheduling-policy` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(scalaTest % "test")
  ).dependsOn(`job-man-api`)

lazy val `job-man-core` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(Akka.persistenceTyped, Akka.persistenceQuery, Akka.streamTyped, Akka.clusterShardingTyped, Akka.slf4j, scalaTest % "test")
  ).dependsOn(`job-scheduling-policy`)

lazy val `job-man-rest` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(AkkaHttp.httpCore, AkkaHttp.jackson)
  ).dependsOn(`job-man-api`)

lazy val `sample-app` = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(scalaTest % "test")
  ).dependsOn(`job-man-core`, `job-man-rest`)

lazy val commonSettings: Seq[Setting[_]] = Publish.settings ++ Seq(
  licenses := Seq("MIT" -> url("http://raw.github.com/pawelkaczor/job-man/master/LICENSE.md")),
  startYear := Some(2018),
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  scalafmtOnCompile := true
)