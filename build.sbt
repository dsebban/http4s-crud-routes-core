import Dependencies._

lazy val commonScalacOptions = Seq(
  "-P:acyclic:force",
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
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
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
  autoCompilerPlugins := true,
  triggeredMessage := Watched.clearWhenTriggered,
  scalacOptions ++= commonScalacOptions,
  libraryDependencies ++= Seq(
    compilerPlugin(Libraries.kindProjector),
    compilerPlugin(Libraries.betterMonadicFor),
    compilerPlugin(Libraries.macroParadise),
    compilerPlugin(Libraries.acyclic)
  ),
  libraryDependencies ++= Seq(
    Libraries.cats,
    Libraries.catsKernel,
    Libraries.catsMeowMtl,
    Libraries.catsPar,
    Libraries.catsEffect,
    Libraries.fs2,
    Libraries.shapeless,
    Libraries.http4sDsl,
    Libraries.http4sServer,
    Libraries.http4sCore,
    Libraries.http4sBlazeServer,
    Libraries.http4sCirce,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeGenericExt,
    Libraries.circeParser,
    Libraries.log4cats,
    Libraries.logback,
    Libraries.acyclic        % "provided",
    Libraries.scalaTest      % "test",
    Libraries.scalaCheck     % "test",
    Libraries.catsEffectLaws % "test"
  )
)
