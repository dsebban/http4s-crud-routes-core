import sbt._

object Dependencies {

  object Versions {
    val cats                = "1.5.0"
    val catsMeowMtl         = "0.2.0"
    val catsPar             = "0.2.0"
    val catsEffect          = "1.1.0"
    val fs2                 = "1.0.2"
    val http4s              = "0.20.0-M4"
    val http4sTracer        = "1.2.0"
    val circe               = "0.10.1"
    val shapeless           = "2.3.3"
    val log4cats            = "0.2.0-RC2"

    val betterMonadicFor    = "0.3.0-M2"
    val acyclic             = "0.1.7"
    val kindProjector       = "0.9.7"
    val macroParadise       = "2.1.0"
    val logback             = "1.2.1"
    val scalaCheck          = "1.14.0"
    val scalaTest           = "3.0.5"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = "io.circe"    %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats                = "org.typelevel"         %% "cats-core"                  % Versions.cats
    lazy val catsKernel          = "org.typelevel"         %% "cats-kernel"                % Versions.cats
    lazy val catsMeowMtl         = "com.olegpy"            %% "meow-mtl"                   % Versions.catsMeowMtl
    lazy val catsPar             = "io.chrisdavenport"     %% "cats-par"                   % Versions.catsPar
    lazy val catsEffect          = "org.typelevel"         %% "cats-effect"                % Versions.catsEffect
    lazy val fs2                 = "co.fs2"                %% "fs2-core"                   % Versions.fs2
    lazy val shapeless           = "com.chuusai"           %% "shapeless"                  % Versions.shapeless

    lazy val http4sDsl           = http4s("http4s-dsl")
    lazy val http4sBlazeServer   = http4s("http4s-blaze-server")
    lazy val http4sCirce         = http4s("http4s-circe")
    lazy val http4sCore          = http4s("http4s-core")
    lazy val http4sServer        = http4s("http4s-server")
    lazy val circeCore           = circe("circe-core")
    lazy val circeGeneric        = circe("circe-generic")
    lazy val circeGenericExt     = circe("circe-generic-extras")
    lazy val circeParser         = circe("circe-parser")
    lazy val log4cats            = "io.chrisdavenport"     %% "log4cats-slf4j"             % Versions.log4cats

    // Compiler plugins
    lazy val betterMonadicFor    = "com.olegpy"            %% "better-monadic-for"         % Versions.betterMonadicFor
    lazy val kindProjector       = "org.spire-math"        %% "kind-projector"             % Versions.kindProjector
    lazy val macroParadise       = "org.scalamacros"       %  "paradise"                   % Versions.macroParadise cross CrossVersion.full
    // Runtime
    lazy val logback             = "ch.qos.logback"        %  "logback-classic"            % Versions.logback

    lazy val acyclic             = "com.lihaoyi"           %% "acyclic"                    % Versions.acyclic 


    // Test
    lazy val scalaTest           = "org.scalatest"         %% "scalatest"                  % Versions.scalaTest
    lazy val scalaCheck          = "org.scalacheck"        %% "scalacheck"                 % Versions.scalaCheck
    lazy val catsEffectLaws      = "org.typelevel"         %% "cats-effect-laws"           % Versions.catsEffect
  }


}
