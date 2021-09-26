package jdksymlink.cli

import pirate.*
import Pirate.*
import piratex.*
import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import jdksymlink.core.{JdkSymLink, JdkSymLinkError, Utils}
import jdksymlink.core.data.{Coursier, DefaultJdk, JavaMajorVersion}
import jdksymlink.info.JdkSymLinkBuildInfo

/** @author Kevin Lee
  * @since 2019-12-24
  */
object JdkSymLinkApp extends MainIo[JdkSymLinkArgs] {

  val listParser: Parse[JdkSymLinkArgs] = ValueParse(JdkSymLinkArgs.JdkListArgs)

  val symLinkJdkParser: Parse[JdkSymLinkArgs] = {
    import scalaz.*
    import Scalaz.*
    JdkSymLinkArgs.SymLinkArgs.apply |*|
      argument[Int](
        metavar("<java-version>") |+| description("Java version e.g.) 8, 11, 16")
      ).map(JavaMajorVersion.apply)
  }

  val rawCmd: Command[JdkSymLinkArgs] = {
    import scalaz.*
    import Scalaz.*
    Command(
      "jdk-sym-link",
      "JDK symbolic link creator".some,
      (subcommand(
        Command(
          "list",
          "List all JDKs".some,
          listParser
        )
      ) ||| subcommand(
        Command(
          "slink",
          "Create JDK symbolic link".some,
          symLinkJdkParser
        )
      )) <* version(JdkSymLinkBuildInfo.version)
    )
  }

  val cmd: Command[JdkSymLinkArgs] =
    Metavar.rewriteCommand(
      Help.rewriteCommand(rawCmd)
    )

  override def command: Command[JdkSymLinkArgs] = cmd

  override def run(args: JdkSymLinkArgs): IO[Either[NonEmptyList[JdkSymLinkError], Unit]] =
    args match {
      case JdkSymLinkArgs.JdkListArgs =>
        for {
          jdk      <- JdkSymLink[IO]
                        .listAll(DefaultJdk.JavaBaseDirPath, DefaultJdk.javaBaseDirFile)
          coursier <- JdkSymLink[IO]
                        .listAll(Coursier.CoursierJavaBaseDirPath, Coursier.coursierJavaBaseDirFile)
        } yield (jdk.toValidatedNel, coursier.toValidatedNel)
          .mapN((_, _) => ())
          .toEither

      case JdkSymLinkArgs.SymLinkArgs(javaVersion) =>
        JdkSymLink[IO]
          .slink(
            javaVersion,
            NonEmptyList.of(
              (DefaultJdk.JavaBaseDirPath, Utils.extractVersion),
              (Coursier.CoursierJavaBaseDirPath, Utils.extractCoursierJavaVersion)
            ),
            DefaultJdk.javaBaseDirFile
          )
          .map(_.leftMap(NonEmptyList.one(_)))
    }

}
