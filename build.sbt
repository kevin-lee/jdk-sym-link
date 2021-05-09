import SbtProjectInfo._
import just.semver.SemVer

ThisBuild / organization := "io.kevinlee"
ThisBuild / version := props.ProjectVersion
ThisBuild / scalaVersion := props.ProjectScalaVersion
ThisBuild / developers := List(
  Developer("Kevin-Lee", "Kevin Lee", "kevin.code@kevinlee.io", url("https://github.com/Kevin-Lee"))
)
ThisBuild / scmInfo :=
  Some(
    ScmInfo(
      url("https://github.com/Kevin-Lee/jdk-symbolic-link"),
      "https://github.com/Kevin-Lee/jdk-symbolic-link.git",
    )
  )

lazy val core = projectCommonSettings("core", ProjectName("core"), file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++=
      List(libs.justSysProcess) ++ libs.catsAndCatsEffect ++ libs.effectie
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

lazy val cli = projectCommonSettings("cli", ProjectName("cli"), file("cli"))
  .enablePlugins(JavaAppPackaging, NativeImagePlugin)
  .settings(
    maintainer := "Kevin Lee <kevin.code@kevinlee.io>",
    packageSummary := "JdkSymLink",
    packageDescription := "A tool to create JDK symbolic links",
    executableScriptName := props.ProjectNamePrefix,
    nativeImageOptions ++= Seq(
      "--verbose",
      "--no-fallback",
      "-H:+ReportExceptionStackTraces",
      "--initialize-at-build-time",
//      s"-H:ReflectionConfigurationFiles=${ (sourceDirectory.value / "graal" / "reflect-config.json").getCanonicalPath }",
//      "--allow-incomplete-classpath",
//      "--report-unsupported-elements-at-runtime",
    ),
  )
  .dependsOn(core, pirate)

lazy val jdkSymLink = (project in file("."))
  .enablePlugins(DevOopsGitHubReleasePlugin)
  .settings(
    name := props.ProjectNamePrefix,
    /* GitHub Release { */
    devOopsPackagedArtifacts := List(
      "cli/target/native-image/jdk-sym-link-cli"
    ),
    /* } GitHub Release */
  )
  .settings(noPublish)
  .aggregate(core, cli)

lazy val props =
  new {
    val GitHubUsername      = "Kevin-Lee"
    val RepoName            = "jdk-sym-link"
    val ProjectNamePrefix   = RepoName
    val ProjectVersion      = SbtProjectInfo.ProjectVersion
    val ProjectScalaVersion = "3.0.0-RC3"

    val effectieVersion = "1.10.0"
    val refinedVersion  = "0.9.24"

    val hedgehogVersion = "0.6.7"

    val pirateVersion = "main"
    val pirateUri     = uri(s"https://github.com/$GitHubUsername/pirate.git#$pirateVersion")

    val IncludeTest: String = "compile->compile;test->test"

    lazy val scala3cLanguageOptions =
      "-language:" + List(
        "dynamics",
        "existentials",
        "higherKinds",
        "reflectiveCalls",
        "experimental.macros",
        "implicitConversions",
      ).mkString(",")

  }

lazy val libs =
  new {

    lazy val hedgehogLibs = List(
      "qa.hedgehog" %% "hedgehog-core"   % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-runner" % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-sbt"    % props.hedgehogVersion % Test,
    )

    lazy val justSysProcess = "io.kevinlee" %% "just-sysprocess" % "0.7.0"

    lazy val newtype = "io.estatico" %% "newtype" % "0.4.4"

    lazy val refined = List(
      "eu.timepit" %% "refined" % props.refinedVersion
    )

    lazy val catsAndCatsEffect = List(
      "org.typelevel" %% "cats-core"   % "2.6.0",
      "org.typelevel" %% "cats-effect" % "2.5.0",
    )

    lazy val effectie = List(
      "io.kevinlee" %% "effectie-cats-effect"   % props.effectieVersion,
      "io.kevinlee" %% "effectie-scalaz-effect" % props.effectieVersion,
    )

  }

def prefixedProjectName(name: String) = s"${props.RepoName}${if (name.isEmpty)
  ""
else
  s"-$name"}"

def scalacOptionsPostProcess(scalaSemVer: SemVer, options: Seq[String]): Seq[String] =
  scalaSemVer match {
    case SemVer(SemVer.Major(3), SemVer.Minor(0), SemVer.Patch(_), _, _) =>
      Seq(props.scala3cLanguageOptions)
    case _: SemVer                                                       =>
      options.distinct
  }

def projectCommonSettings(id: String, projectName: ProjectName, file: File): Project =
  Project(id, file)
    .settings(
      name := prefixedProjectName(projectName.projectName),
      libraryDependencies ++= libs.hedgehogLibs ++ libs.refined,
      testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework")),
    )
