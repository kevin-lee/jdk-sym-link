import SbtProjectInfo._

ThisBuild / organization := props.Org
ThisBuild / version := props.ProjectVersion
ThisBuild / scalaVersion := props.ProjectScalaVersion
ThisBuild / developers := List(
  Developer("kevin-lee", "Kevin Lee", "kevin.code@kevinlee.io", url("https://github.com/kevin-lee"))
)
ThisBuild / scmInfo :=
  Some(
    ScmInfo(
      url("https://github.com/kevin-lee/jdk-symbolic-link"),
      "https://github.com/kevin-lee/jdk-symbolic-link.git",
    )
  )

lazy val core = projectCommonSettings("core")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++=
      List(libs.justSysProcess) ++
        libs.catsAndCatsEffect ++
        libs.effectie ++
        List(libs.extrasCats, libs.extrasScalaIo, libs.justSemVer)
    /* Build Info { */,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoObject := "JdkSymLinkBuildInfo",
    buildInfoPackage := "jdksymlink.info",
    buildInfoOptions += BuildInfoOption.ToJson
    /* } Build Info */
    /* publish { */,
    licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    /* } publish */

  )

lazy val pirate = ProjectRef(props.pirateUri, "pirate")

lazy val cli = projectCommonSettings("cli")
  .enablePlugins(JavaAppPackaging, NativeImagePlugin)
  .settings(
    maintainer := "Kevin Lee <kevin.code@kevinlee.io>",
    packageSummary := "JdkSymLink",
    packageDescription := "A tool to create JDK symbolic links",
    executableScriptName := props.ProjectNamePrefix,
    nativeImageVersion := "22.2.0",
    nativeImageJvm := "graalvm-java17",
    nativeImageOptions ++= Seq(
      "--verbose",
      "--no-fallback",
      "-H:+ReportExceptionStackTraces",
      "--initialize-at-build-time",
//      s"-H:ReflectionConfigurationFiles=${ (sourceDirectory.value / "graal" / "reflect-config.json").getCanonicalPath }",
//      "--allow-incomplete-classpath",
      "--report-unsupported-elements-at-runtime",
    ),
  )
  .dependsOn(core, pirate)

lazy val jdkSymLink = (project in file("."))
  .enablePlugins(DevOopsGitHubReleasePlugin)
  .settings(
    name := props.ProjectNamePrefix,
    /* GitHub Release { */
    devOopsPackagedArtifacts := List(
      s"modules/cli/target/native-image/${name.value}-cli",
      s"modules/cli/target/universal/${name.value}*.zip",
    ),
    /* } GitHub Release */
  )
  .settings(noPublish)
  .aggregate(core, cli)

lazy val props =
  new {
    final val Org                 = "io.kevinlee"
    final val GitHubUsername      = "kevin-lee"
    final val RepoName            = "jdk-sym-link"
    final val ProjectNamePrefix   = RepoName
    final val ProjectVersion      = SbtProjectInfo.ProjectVersion
    final val ProjectScalaVersion = "3.3.0"

    final val effectieVersion = "2.0.0-beta11"
    final val refinedVersion  = "0.11.0"

    final val catsVersion       = "2.10.0"
    final val catsEffectVersion = "3.5.1"

    final val ExtrasVersion = "0.40.0"

    final val JustSemVerVersion = "0.6.0"

    final val hedgehogVersion = "0.10.1"

    final val justSysprocessVersion = "1.0.0"

    final val pirateVersion = "deec3408b08a751de9b2df2d17fc1ab7b8daeaaf"
    final val pirateUri     = uri(s"https://github.com/$GitHubUsername/pirate.git#$pirateVersion")

    final val IncludeTest = "compile->compile;test->test"

  }

lazy val libs =
  new {

    lazy val hedgehogLibs = List(
      "qa.hedgehog" %% "hedgehog-core"   % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-runner" % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-sbt"    % props.hedgehogVersion % Test,
    )

    lazy val justSysProcess = "io.kevinlee" %% "just-sysprocess" % props.justSysprocessVersion

    lazy val refined = List(
      "eu.timepit" %% "refined" % props.refinedVersion
    )

    lazy val catsAndCatsEffect = List(
      "org.typelevel" %% "cats-core"   % props.catsVersion,
      "org.typelevel" %% "cats-effect" % props.catsEffectVersion,
    )

    lazy val effectie = List(
      "io.kevinlee" %% "effectie-cats-effect3" % props.effectieVersion,
    )

    lazy val extrasCats    = "io.kevinlee" %% "extras-cats"     % props.ExtrasVersion
    lazy val extrasScalaIo = "io.kevinlee" %% "extras-scala-io" % props.ExtrasVersion

    lazy val justSemVer = "io.kevinlee" %% "just-semver" % props.JustSemVerVersion

  }

def prefixedProjectName(name: String) = s"${props.RepoName}${if (name.isEmpty) "" else s"-$name"}"

def projectCommonSettings(projectName: String): Project = {
  val prefixedName = prefixedProjectName(projectName)
  Project(prefixedName, file(s"modules/$prefixedName"))
    .settings(
      name := prefixedName,
      useAggressiveScalacOptions := true,
      libraryDependencies ++= libs.hedgehogLibs ++ libs.refined,
      testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework")),
    )
}
