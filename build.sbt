import ProjectInfo.*

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
        List(libs.kittens, libs.extrasCats, libs.extrasScalaIo, libs.extrasRender) ++ libs.justSemVer
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

//lazy val pirate = ProjectRef(props.pirateUri, "pirate-scalaz")

lazy val cli = projectCommonSettings("cli")
  .enablePlugins(JavaAppPackaging, NativeImagePlugin)
  .settings(
    libraryDependencies ++= libs.decline,
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
  .dependsOn(core)
//  .dependsOn(core, pirate)

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
