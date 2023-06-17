package jdksymlink.cli

import cats.Show
import jdksymlink.core.data.JavaMajorVersion
import jdksymlink.info.JdkSymLinkBuildInfo
import pirate.*
import Pirate.*

/** @author Kevin Lee
  * @since 2019-12-24
  */
enum JdkSymLinkArgs derives CanEqual {

  case JdkListArgs
  case SymLinkArgs(javaMajorVersion: JavaMajorVersion)

}

object JdkSymLinkArgs {

  val listParser: Parse[JdkSymLinkArgs] = ValueParse(JdkSymLinkArgs.JdkListArgs)

  val symLinkJdkParser: Parse[JdkSymLinkArgs] = {
    import scalaz.*
    import Scalaz.*
    JdkSymLinkArgs.SymLinkArgs.apply |*|
      argument[Int](
        metavar("<java-version>") |+| description("Java version e.g.) 8, 11, 17")
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

  final case class JustMessageOrHelp(messages: List[String])
  object JustMessageOrHelp {
    given show: Show[JustMessageOrHelp] = _.messages.mkString("\n")
  }
  final case class ArgParseError(errors: List[String])
  object ArgParseError {
    given show: Show[ArgParseError] = _.errors.mkString("\n")
  }

  type ArgParseFailureResult =
    JustMessageOrHelp | ArgParseError

}
