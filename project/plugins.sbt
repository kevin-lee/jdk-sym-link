addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.4")
addSbtPlugin("org.scalameta"  % "sbt-native-image"    % "0.3.4")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"       % "0.12.0")

val sbtDevoopsVersion     = "3.1.0"
addSbtPlugin("io.kevinlee" % "sbt-devoops-scala"     % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-sbt-extra" % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-github"    % sbtDevoopsVersion)

addSbtPlugin("io.kevinlee" % "sbt-devoops-starter" % sbtDevoopsVersion)
