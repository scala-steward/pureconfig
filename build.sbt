import scala.math.Ordering.Implicits._

import Dependencies.Version._
import Utilities._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

ThisBuild / organization := "com.github.pureconfig"

// Enable the OrganizeImports Scalafix rule and semanticdb for scalafix.
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// taken from https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

lazy val core = (project in file("core"))
  .enablePlugins(BoilerplatePlugin)
  .settings(commonSettings)

lazy val testkit = (project in file("testkit"))
  .settings(commonSettings)
  .dependsOn(core)

lazy val tests = (project in file("tests"))
  .enablePlugins(BoilerplatePlugin)
  .settings(commonSettings)
  .dependsOn(core, testkit)

// aggregates pureconfig-core and pureconfig-generic with the original "pureconfig" name
lazy val bundle = (project in file("bundle"))
  .enablePlugins(ModuleMdocPlugin)
  .settings(commonSettings, mdocOut := file("."))
  .dependsOn(core, generic)

lazy val docs = (project in file("docs"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings)
  .dependsOn(bundle)

def genericModule(proj: Project) = proj
  .dependsOn(core)
  .dependsOn(testkit % "test")
  .settings(commonSettings)
  .settings(name := s"pureconfig-${baseDirectory.value.getName}")

def module(proj: Project) = genericModule(proj)
  .enablePlugins(ModuleMdocPlugin)

lazy val akka = module(project) in file("modules/akka")
lazy val `akka-http` = module(project) in file("modules/akka-http")
lazy val cats = module(project) in file("modules/cats")
lazy val `cats-effect` = module(project) in file("modules/cats-effect")
lazy val `cats-effect2` = module(project) in file("modules/cats-effect2")
lazy val circe = module(project) in file("modules/circe")
lazy val cron4s = module(project) in file("modules/cron4s")
lazy val enum = module(project) in file("modules/enum")
lazy val enumeratum = module(project) in file("modules/enumeratum")
lazy val fs2 = module(project) in file("modules/fs2")
lazy val generic = genericModule(project) in file("modules/generic") dependsOn `generic-base`
lazy val `generic-scala3` = genericModule(project) in file("modules/generic-scala3") dependsOn `generic-base`
lazy val `generic-base` = genericModule(project) in file("modules/generic-base")
lazy val hadoop = module(project) in file("modules/hadoop")
lazy val http4s = module(project) in file("modules/http4s")
lazy val http4s022 = module(project) in file("modules/http4s022")
lazy val ip4s = module(project) in file("modules/ip4s")
lazy val javax = module(project) in file("modules/javax")
lazy val joda = module(project) in file("modules/joda")
lazy val magnolia = module(project) in file("modules/magnolia") dependsOn `generic-base`
lazy val pekko = module(project) in file("modules/pekko")
lazy val `pekko-http` = module(project) in file("modules/pekko-http")
lazy val `scala-xml` = module(project) in file("modules/scala-xml")
lazy val scalaz = module(project) in file("modules/scalaz")
lazy val spark = module(project) in file("modules/spark")
lazy val squants = module(project) in file("modules/squants")
lazy val sttp = module(project) in file("modules/sttp")
lazy val yaml = module(project) in file("modules/yaml")
lazy val `zio-config` = module(project) in file("modules/zio-config")

// Workaround for https://github.com/scalacenter/scalafix/issues/1488
val scalafixCheckAll = taskKey[Unit]("No-arg alias for 'scalafixAll --check'")

lazy val commonSettings = Seq(
  // format: off
  homepage := Some(url("https://github.com/pureconfig/pureconfig")),
  licenses := Seq("Mozilla Public License, version 2.0" -> url("https://www.mozilla.org/MPL/2.0/")),

  developers := List(
    Developer("melrief", "Mario Pastorelli", "pastorelli.mario@gmail.com", url("https://github.com/melrief")),
    Developer("leifwickland", "Leif Wickland", "leifwickland@gmail.com", url("https://github.com/leifwickland")),
    Developer("jcazevedo", "Joao Azevedo", "joao.c.azevedo@gmail.com", url("https://github.com/jcazevedo")),
    Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("https://github.com/ruippeixotog")),
    Developer("derekmorr", "Derek Morr", "morr.derek@gmail.com", url("https://github.com/derekmorr"))
  ),

  scalaVersion := scala213,

  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),

  crossVersionSharedSources(Compile / unmanagedSourceDirectories),
  crossVersionSharedSources(Test / unmanagedSourceDirectories),

  scalacOptions ++= lintFlags.value,

  Test / scalacOptions ~= { _.filterNot(_.contains("-Ywarn-unused")) },

  Compile / console / scalacOptions -= "-Ywarn-unused",
  Test / console / scalacOptions := (Compile / console / scalacOptions).value,

  scalafmtOnCompile := true,
  scalafixOnCompile := true,
  scalafixCheckAll := scalafixAll.toTask(" --check").value,

  autoAPIMappings := true,

  publishMavenStyle := true,
  Test / publishArtifact := false,
  publishTo := {
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else localStaging.value
  },
  // format: on
)

// add support for Scala version ranges such as "scala-2.13+" or "scala-3-" in source folders (single version folders
// such as "scala-2.13" are natively supported by SBT).
def crossVersionSharedSources(unmanagedSrcs: SettingKey[Seq[File]]) = {
  unmanagedSrcs ++= {
    val versionNumber = CrossVersion.partialVersion(scalaVersion.value)
    val expectedVersions = Seq(scala213, scala3).flatMap(CrossVersion.partialVersion)
    expectedVersions.flatMap { case v @ (major, minor) =>
      List(
        if (versionNumber.exists(_ <= v)) unmanagedSrcs.value.map { dir => new File(dir.getPath + s"-$major.$minor-") }
        else Nil,
        if (versionNumber.exists(_ >= v)) unmanagedSrcs.value.map { dir => new File(dir.getPath + s"-$major.$minor+") }
        else Nil
      )
    }.flatten
  }
}

lazy val lintFlags = forScalaVersions {
  case (2, 13) =>
    List(
      "-encoding",
      "UTF-8", // arg for -encoding
      "-feature",
      "-unchecked",
      "-deprecation",
      // do not create copy constructor for case classes with private fields
      "-Xsource-features:case-apply-copy-access",
      "-Xsource:3",
      // Required by SemanticDB compiler plugin
      "-Yrangepos",
      "-Ywarn-unused",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    )

  case (3, _) =>
    List(
      "-encoding",
      "UTF-8", // arg for -encoding
      "-feature",
      "-unchecked",
      "-old-syntax",
      "-no-indent"
    )

  case (maj, min) => throw new Exception(s"Unknown Scala version $maj.$min")
}

// Use the same Scala 2.13 version in the root project as in subprojects
scalaVersion := scala213

// Setting no cross build for the aggregating root project so that we can have proper per project exclusions.
crossScalaVersions := Nil

// do not publish the root project
publish / skip := true

releaseCrossBuild := true
releaseTagComment := s"Release ${(ThisBuild / version).value}"
releaseCommitMessage := s"Set version to ${(ThisBuild / version).value}"
releaseNextCommitMessage := s"Set version to ${(ThisBuild / version).value}"

// redefine the release process due to https://github.com/sbt/sbt-release/issues/184
// and to append `sonatypeReleaseAll`
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
