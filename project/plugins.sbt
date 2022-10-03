addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
addSbtPlugin("org.scalameta"  % "sbt-native-image"    % "0.3.2")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"       % "0.11.0")

val sbtDevoopsVersion = "2.23.0"
addSbtPlugin("io.kevinlee" % "sbt-devoops-scala"     % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-sbt-extra" % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-github"    % sbtDevoopsVersion)

addSbtPlugin("io.kevinlee" % "sbt-devoops-starter" % sbtDevoopsVersion)
