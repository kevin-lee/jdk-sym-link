addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
addSbtPlugin("org.scalameta"    % "sbt-native-image"    % "0.3.0")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo"       % "0.10.0")

val sbtDevoopsVersion = "2.6.0"
addSbtPlugin("io.kevinlee" % "sbt-devoops-scala"     % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-sbt-extra" % sbtDevoopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-github"    % sbtDevoopsVersion)
