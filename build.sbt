val ProjectNamePrefix = "jdk-sym-link"
val ProjectVersion = "0.1.0"
val ProjectScalaVersion = "2.13.1"

ThisBuild / organization := "io.kevinlee"
ThisBuild / version := ProjectVersion
ThisBuild / scalaVersion := ProjectScalaVersion
ThisBuild / developers   := List(
  Developer("Kevin-Lee", "Kevin Lee", "kevin.code@kevinlee.io", url("https://github.com/Kevin-Lee"))
)
ThisBuild / scmInfo :=
  Some(ScmInfo(
    url("https://github.com/Kevin-Lee/jdk-symbolic-link")
    , "https://github.com/Kevin-Lee/jdk-symbolic-link.git"
  ))

lazy val  hedgehogVersion: String = "0.6.1"

lazy val  hedgehogRepo: Resolver =
  "bintray-scala-hedgehog" at "https://dl.bintray.com/hedgehogqa/scala-hedgehog"

lazy val  hedgehogLibs: Seq[ModuleID] = Seq(
    "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion % Test
)

lazy val justSysProcess = "io.kevinlee" %% "just-sysprocess" % "0.3.0"

val EffectieVersion = "1.8.1"
lazy val effectieCatsEffect: ModuleID = "io.kevinlee" %% "effectie-cats-effect" % EffectieVersion
lazy val effectieScalazEffect: ModuleID = "io.kevinlee" %% "effectie-scalaz-effect" % EffectieVersion

val cats: ModuleID = "org.typelevel" %% "cats-core" % "2.3.1"
val catsEffect: ModuleID = "org.typelevel" %% "cats-effect" % "2.3.1"

lazy val pirateVersion = "b3a0a3eff3a527dff542133aaf0fd935aa2940fc"
lazy val pirateUri = uri(s"https://github.com/Kevin-Lee/pirate.git#$pirateVersion")

def subProject(projectName: String, path: File): Project =
  Project(projectName, path)
    .settings(
        name := s"$ProjectNamePrefix-$projectName"
      , addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
      , resolvers += hedgehogRepo
      , testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework"))
      , libraryDependencies ++= hedgehogLibs
    )

lazy val core = subProject("core", file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
      libraryDependencies ++= Seq(cats, catsEffect, effectieCatsEffect, effectieScalazEffect, justSysProcess)
    /* Build Info { */
    , buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
    , buildInfoObject := "JdkSymLinkBuildInfo"
    , buildInfoPackage := "jdksymlink.info"
    , buildInfoOptions += BuildInfoOption.ToJson
    /* } Build Info */
    /* publish { */
    , licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
    /* } publish */

  )

lazy val cli = subProject("cli", file("cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(
      maintainer := "Kevin Lee <kevin.code@kevinlee.io>"
    , packageSummary := "JdkSymLink"
    , packageDescription := "A tool to create JDK symbolic links"
    , executableScriptName := ProjectNamePrefix
  )
  .dependsOn(core, ProjectRef(pirateUri, "pirate"))

lazy val jdkSymLink = (project in file("."))
  .enablePlugins(DevOopsGitReleasePlugin)
  .settings(
      name := ProjectNamePrefix
    , addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
    , addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
    /* GitHub Release { */
    , devOopsPackagedArtifacts := List(
        s"core/target/scala-*/${name.value}*.jar"
      , s"cli/target/universal/${name.value}*.zip"
      , s"cli/target/universal/${name.value}*.tgz"
      , s"cli/target/${name.value}*.deb"
    )
    /* } GitHub Release */
  )
  .aggregate(core, cli)

