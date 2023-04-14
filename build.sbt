ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "o_hell_card_game"
  )

libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.7.2"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")