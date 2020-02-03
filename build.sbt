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

lazy val  hedgehogVersion: String = "64eccc9ca7dbe7a369208a14a97a25d7ccbbda67"

lazy val  hedgehogRepo: Resolver =
  "bintray-scala-hedgehog" at "https://dl.bintray.com/hedgehogqa/scala-hedgehog"

lazy val  hedgehogLibs: Seq[ModuleID] = Seq(
    "qa.hedgehog" %% "hedgehog-core" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-runner" % hedgehogVersion % Test
  , "qa.hedgehog" %% "hedgehog-sbt" % hedgehogVersion % Test
)

val cats: ModuleID = "org.typelevel" %% "cats-core" % "2.1.0"
val catsEffect: ModuleID = "org.typelevel" %% "cats-effect" % "2.0.0"

lazy val core = (project in file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
      name := s"$ProjectNamePrefix-core"
    , resolvers += hedgehogRepo
    , libraryDependencies ++= hedgehogLibs ++ Seq(cats, catsEffect)
    , testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework"))
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

lazy val pirateVersion = "44486bc961b52ba889f0b8f2b23f719d0ed8ba99"
lazy val pirateUri = uri(s"https://github.com/Kevin-Lee/pirate.git#$pirateVersion")

lazy val cli = (project in file("cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(
      name := s"$ProjectNamePrefix-cli"
    , resolvers += hedgehogRepo
    , libraryDependencies ++= hedgehogLibs
    , testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework"))
    , maintainer := "Kevin Lee <kevin.code@kevinlee.io>"
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

