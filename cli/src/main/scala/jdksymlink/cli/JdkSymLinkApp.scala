package jdksymlink.cli

import pirate._
import Pirate._
import piratex._

import scalaz._
import Scalaz._
import scalaz.effect._

import jdksymlink.core.JdkSymLinkError
import jdksymlink.info.JdkSymLinkBuildInfo

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
object JdkSymLinkApp extends MainIO[JdkSymLinkArgs] {

  val listParser: Parse[JdkSymLinkArgs] = ValueParse(JdkSymLinkArgs.jdkListArgs)

  val symLinkJdkParser: Parse[JdkSymLinkArgs] = JdkSymLinkArgs.symLinkArgs _ |*| {
    flag[Int](
        both('v', "java-version")
      , metavar("<java-version>") |+| description("Java version e.g.) 8, 11, 13")
      )
    }

  val rawCmd: Command[JdkSymLinkArgs] = Command(
      "jdk-sym-link"
    , "JDK symbolic link creator".some
    , (subcommand(
        Command(
          "list"
        , "List all JDKs".some
        , listParser
        )
      ) ||| subcommand(
        Command(
          "slink"
        , "Create JDK symbolic link".some
        , symLinkJdkParser
        )
      )) <* version(JdkSymLinkBuildInfo.version)
    )

  val cmd: Command[JdkSymLinkArgs] =
    Metavar.rewriteCommand(
      Help.rewriteCommand(rawCmd)
    )

  override def command: Command[JdkSymLinkArgs] = cmd

  override def run(a: JdkSymLinkArgs): IO[JdkSymLinkError \/ Unit] =
    IO.putStrLn(s"args: $a") *> IO(().right)

}
