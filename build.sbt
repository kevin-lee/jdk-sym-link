val ProjectVersion = "0.1.0"
val ProjectScalaVersion = "2.13.1"

ThisBuild / organization := "io.kevinlee"
ThisBuild / version := ProjectVersion
ThisBuild / scalaVersion := ProjectScalaVersion
ThisBuild / developers   := List(
  Developer("Kevin-Lee", "Kevin Lee", "kevin.code@kevinlee.io", url("https://github.com/Kevin-Lee"))
)

val humbleDownloader = (project in file("."))
  .settings(
    name := "jdk-symbolic-link"
  , libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.1.0"
    , "org.typelevel" %% "cats-effect" % "2.0.0"
    )
    , addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
    , addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

