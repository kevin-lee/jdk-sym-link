import sbt.*

object ProjectInfo {

  object props {
    final val Org            = "io.kevinlee"
    final val GitHubUsername = "kevin-lee"
    final val RepoName       = "jdk-sym-link"

    val GitHubUrl = "https://github.com/kevin-lee"

    final val ProjectNamePrefix   = RepoName
    final val ProjectScalaVersion = "3.7.4"

    final val effectieVersion  = "2.5.0"
    final val refined4sVersion = "1.21.0"

    final val catsVersion       = "2.13.0"
    final val catsEffectVersion = "3.5.4"

    val KittensVersion = "3.5.0"

    final val ExtrasVersion = "0.57.0"

    final val JustSemVerVersion = "1.4.0"

    final val hedgehogVersion = "0.15.1"

    final val justSysprocessVersion = "1.0.0"

    val DeclineVersion = "2.6.2"

    final val pirateVersion = "574d85a36919280dc802009b581df9019811c27e"
    final val pirateUri     = uri(
      s"https://github.com/$GitHubUsername/pirate.git#$pirateVersion"
    )

    final val IncludeTest = "compile->compile;test->test"

  }

  object libs {

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

    lazy val justSemVer = List(
      "io.kevinlee" %% "just-semver-core"   % props.JustSemVerVersion,
      "io.kevinlee" %% "just-semver-decver" % props.JustSemVerVersion,
    )

    lazy val decline = List(
      "com.monovore" %% "decline"        % props.DeclineVersion,
      "com.monovore" %% "decline-effect" % props.DeclineVersion,
    )

  }
}
