package jdksymlink.cli

import pirate._
import Pirate._
import piratex._

import scalaz._
import Scalaz._

import cats.effect._

import jdksymlink.core.{JdkSymLink, JdkSymLinkError}
import jdksymlink.core.data.{JavaMajorVersion, javaBaseDirFile, JavaBaseDirPath}
import jdksymlink.info.JdkSymLinkBuildInfo

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
object JdkSymLinkApp extends MainIo[JdkSymLinkArgs] {

  val listParser: Parse[JdkSymLinkArgs] = ValueParse(JdkSymLinkArgs.JdkListArgs)

  val symLinkJdkParser: Parse[JdkSymLinkArgs] =
    JdkSymLinkArgs.SymLinkArgs.apply |*|
      argument[Int](
        metavar("<java-version>") |+| description("Java version e.g.) 8, 11, 13")
      ).map(JavaMajorVersion.apply)

  val rawCmd: Command[JdkSymLinkArgs] = Command(
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

  val cmd: Command[JdkSymLinkArgs] =
    Metavar.rewriteCommand(
      Help.rewriteCommand(rawCmd)
    )

  override def command: Command[JdkSymLinkArgs] = cmd

  override def run(args: JdkSymLinkArgs): IO[JdkSymLinkError \/ Unit] =
    (args match {
      case JdkSymLinkArgs.JdkListArgs =>
        JdkSymLink[IO].listAll(JavaBaseDirPath, javaBaseDirFile)

      case JdkSymLinkArgs.SymLinkArgs(javaVersion) =>
        JdkSymLink[IO].slink(javaVersion)
    }).map(\/.fromEither)

}
