ThisBuild / organization := props.Org
ThisBuild / scalaVersion := props.ProjectScalaVersion
ThisBuild / developers := List(
  Developer(
    "kevin-lee",
    "Kevin Lee",
    "kevin.code@kevinlee.io",
    url(props.GitHubUrl)
  )
)
ThisBuild / scmInfo :=
  Some(
    ScmInfo(
      url(s"${props.GitHubUrl}/jdk-symbolic-link"),
      s"${props.GitHubUrl}/jdk-symbolic-link.git",
    )
  )

lazy val core = projectCommonSettings("core")
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++=
      List(libs.justSysProcess) ++
        libs.catsAndCatsEffect ++
        libs.effectie ++
        List(libs.kittens, libs.extrasCats, libs.extrasScalaIo, libs.extrasRender, libs.justSemVer)
    /* Build Info { */,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoObject := "JdkSymLinkBuildInfo",
    buildInfoPackage := "jdksymlink.info",
    buildInfoOptions += BuildInfoOption.ToJson
    /* } Build Info */
    /* publish { */,
    licenses := List(License.MIT),
    /* } publish */

  )

lazy val pirate = ProjectRef(props.pirateUri, "pirate-scalaz")

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
      s"modules/${props.RepoName}-cli/target/native-image/${name.value}-cli-*",
      s"modules/${props.RepoName}-cli/target/universal/${name.value}*.zip",
    ),
    /* } GitHub Release */
  )
  .settings(noPublish)
  .aggregate(core, cli)

lazy val props =
  new {
    final val Org            = "io.kevinlee"
    final val GitHubUsername = "kevin-lee"
    final val RepoName       = "jdk-sym-link"

    val GitHubUrl = "https://github.com/kevin-lee"

    final val ProjectNamePrefix   = RepoName
    final val ProjectScalaVersion = "3.3.6"

    final val effectieVersion  = "2.0.0"
    final val refined4sVersion = "1.1.0"

    final val catsVersion       = "2.13.0"
    final val catsEffectVersion = "3.5.4"

    val KittensVersion = "3.5.0"

    final val ExtrasVersion = "0.44.0"

    final val JustSemVerVersion = "1.1.1"

    final val hedgehogVersion = "0.12.0"

    final val justSysprocessVersion = "1.0.0"

    final val pirateVersion = "574d85a36919280dc802009b581df9019811c27e"
    final val pirateUri     = uri(
      s"https://github.com/$GitHubUsername/pirate.git#$pirateVersion"
    )

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

    lazy val refined4s = List(
      "io.kevinlee" %% "refined4s-core"          % props.refined4sVersion,
      "io.kevinlee" %% "refined4s-cats"          % props.refined4sVersion,
      "io.kevinlee" %% "refined4s-extras-render" % props.refined4sVersion,
    )

    lazy val catsAndCatsEffect = List(
      "org.typelevel" %% "cats-core" % props.catsVersion,
      "org.typelevel" %% "cats-core" % props.catsVersion,
    )

    lazy val kittens = "org.typelevel" %% "kittens" % props.KittensVersion

    lazy val effectie = List(
      "io.kevinlee" %% "effectie-cats-effect3" % props.effectieVersion,
    )

    lazy val extrasCats    = "io.kevinlee" %% "extras-cats"     % props.ExtrasVersion
    lazy val extrasRender  = "io.kevinlee" %% "extras-render"   % props.ExtrasVersion
    lazy val extrasScalaIo = "io.kevinlee" %% "extras-scala-io" % props.ExtrasVersion

    lazy val justSemVer = "io.kevinlee" %% "just-semver" % props.JustSemVerVersion

  }

def prefixedProjectName(name: String) =
  s"${props.RepoName}${if (name.isEmpty) "" else s"-$name"}"

def projectCommonSettings(projectName: String): Project = {
  val prefixedName = prefixedProjectName(projectName)
  Project(prefixedName, file(s"modules/$prefixedName"))
    .settings(
      name := prefixedName,
      scalacOptions += "strictEquality",
      libraryDependencies ++= libs.hedgehogLibs ++ libs.refined4s,
      testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework")),
    )
}
