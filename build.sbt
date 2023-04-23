ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "o_hell_card_game"
  )

lazy val thisBuildSettings: Seq[Setting[_]] = inThisBuild(
  Seq(
    version := "0.2",
    scalaVersion := "2.13.10",
    // From https://tpolecat.github.io/2017/04/25/scalac-flags.html
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Ymacro-annotations",
    ),
    run / fork := true,
    run / connectInput := true,
    run / outputStrategy := Some(StdoutOutput),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  )
)

libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8"


libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.7.2"
)

val http4sVersion      = "0.23.18"
val catsEffect3Version = "3.3.0"
val circeVersion       = "0.14.1"
val catsVersion        = "2.9.0"



libraryDependencies ++= Seq(
  "org.typelevel"            %% "cats-core"                     % catsVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.typelevel"%% "cats-effect"% catsEffect3Version,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-optics" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")