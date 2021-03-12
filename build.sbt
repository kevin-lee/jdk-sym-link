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
    libraryDependencies ++= Seq(libs.justSysProcess) ++ libs.catsAndCatsEffect ++ libs.effectie
    /* Build Info { */,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoObject := "JdkSymLinkBuildInfo",
    buildInfoPackage := "jdksymlink.info",
    buildInfoOptions += BuildInfoOption.ToJson
    /* } Build Info */
    /* publish { */,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    /* } publish */

  )

lazy val pirate = ProjectRef(props.pirateUri, "pirate")

lazy val cli = projectCommonSettings("cli", ProjectName("cli"), file("cli"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    maintainer := "Kevin Lee <kevin.code@kevinlee.io>",
    packageSummary := "JdkSymLink",
    packageDescription := "A tool to create JDK symbolic links",
    executableScriptName := props.ProjectNamePrefix,
  )
  .dependsOn(core, pirate)

lazy val jdkSymLink = (project in file("."))
  .enablePlugins(DevOopsGitHubReleasePlugin)
  .settings(
    name := props.ProjectNamePrefix,
    /* GitHub Release { */
    devOopsPackagedArtifacts := List(
      s"cli/target/universal/${name.value}*.zip",
      s"cli/target/universal/${name.value}*.tgz",
      s"cli/target/${name.value}*.deb",
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
    val ProjectScalaVersion = "2.13.4"

    val effectieVersion = "1.9.0"
    val refinedVersion  = "0.9.21"

    val hedgehogVersion        = "0.6.5"

    val pirateVersion = "b3a0a3eff3a527dff542133aaf0fd935aa2940fc"
    val pirateUri     = uri(s"https://github.com/$GitHubUsername/pirate.git#$pirateVersion")

    val IncludeTest: String = "compile->compile;test->test"
  }

lazy val libs =
  new {

    lazy val hedgehogLibs: Seq[ModuleID] = Seq(
      "qa.hedgehog" %% "hedgehog-core"   % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-runner" % props.hedgehogVersion % Test,
      "qa.hedgehog" %% "hedgehog-sbt"    % props.hedgehogVersion % Test,
    )

    lazy val justSysProcess = "io.kevinlee" %% "just-sysprocess" % "0.3.0"

    lazy val newtype = "io.estatico" %% "newtype" % "0.4.4"

    lazy val refined = Seq(
      "eu.timepit" %% "refined" % props.refinedVersion
    )

    lazy val catsAndCatsEffect = Seq(
      "org.typelevel" %% "cats-core"    % "2.4.2",
      "org.typelevel" %% "cats-effect"  % "2.3.3",
    )

    lazy val effectie = Seq(
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
    case SemVer(SemVer.Major(2), SemVer.Minor(13), SemVer.Patch(patch), _, _) =>
      ((if (patch >= 3) {
          options.distinct.filterNot(_ == "-Xlint:nullary-override")
        } else {
          options.distinct
        }) ++ Seq("-Ymacro-annotations", "-language:implicitConversions")).distinct
    case _: SemVer                                                            =>
      options.distinct
  }

def projectCommonSettings(id: String, projectName: ProjectName, file: File): Project =
  Project(id, file)
    .settings(
      name := prefixedProjectName(projectName.projectName),
      addCompilerPlugin("org.typelevel" % "kind-projector"     % "0.11.2" cross CrossVersion.full),
      addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1"),
      scalacOptions := scalacOptionsPostProcess(
        SemVer.parseUnsafe(scalaVersion.value),
        scalacOptions.value,
      ),
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
      ),
      libraryDependencies ++=
        libs.hedgehogLibs ++ Seq(libs.newtype) ++ libs.refined
      /* } WartRemover and scalacOptions */,
      testFrameworks ++= Seq(TestFramework("hedgehog.sbt.Framework"))

      /* Ammonite-REPL { */,
      libraryDependencies ++=
        (scalaBinaryVersion.value match {
          case "2.13" =>
            Seq("com.lihaoyi" % "ammonite" % "2.3.8-4-88785969" % Test cross CrossVersion.full)
          case _      =>
            Seq.empty[ModuleID]
        }),
      sourceGenerators in Test +=
        (scalaBinaryVersion.value match {
          case "2.13" =>
            task {
              val file = (sourceManaged in Test).value / "amm.scala"
              IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
              Seq(file)
            }
          case _      =>
            task(Seq.empty[File])
        }),
      /* } Ammonite-REPL */

    )

lazy val noPublish: SettingsDefinition = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in sbt.Keys.`package` := true,
  skip in packagedArtifacts := true,
  skip in publish := true,
)
