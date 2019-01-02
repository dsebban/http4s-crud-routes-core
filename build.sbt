import Dependencies._

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-Ypartial-unification",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xlog-reflective-calls",
  "-Ywarn-inaccessible",
  "-Ypatmat-exhaust-depth",
  "20",
  "-Ydelambdafy:method",
  "-Xmax-classfile-name",
  "100"
)

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.github.dsebban",
      scalaVersion := "2.12.7",
      version := "0.1.0-SNAPSHOT"
    )
  ),
  name := "http4s-crud-routes-core",
  scalafmtOnCompile := true,
  triggeredMessage := Watched.clearWhenTriggered,
  scalacOptions ++= commonScalacOptions,
  libraryDependencies ++= Seq(
    compilerPlugin(Libraries.kindProjector),
    compilerPlugin(Libraries.betterMonadicFor),
    compilerPlugin(Libraries.macroParadise)
  ),
  libraryDependencies ++= Seq(
    Libraries.cats,
    Libraries.catsMeowMtl,
    Libraries.catsPar,
    Libraries.catsEffect,
    Libraries.fs2,
    Libraries.http4sDsl,
    Libraries.http4sServer,
    Libraries.http4sCirce,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeGenericExt,
    Libraries.circeParser,
    Libraries.log4cats,
    Libraries.logback,
    Libraries.scalaTest      % "test",
    Libraries.scalaCheck     % "test",
    Libraries.catsEffectLaws % "test"
  )
)
