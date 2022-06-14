package jdksymlink.core

import cats.data.NonEmptyList
import jdksymlink.cs.CoursierCmd.CoursierError

import java.io.{IOException, PrintWriter, StringWriter}

/** @author Kevin Lee
  * @since 2019-12-25
  */
enum JdkSymLinkError derives CanEqual {

  case LsFailure(errorCode: Int, message: String, commands: List[String])
  case PathExistsAndNoSymLink(path: String, message: String, commands: List[String])
  case CommandFailure(throwable: Throwable, commands: List[String])
  case Coursier(coursierError: CoursierError)
//  case ArgParse(argError: ArgParseError)
}

object JdkSymLinkError {

  import Shell.ColoredString.*

  extension (jdkSymLinkError: JdkSymLinkError) {
    def render: String = jdkSymLinkError match {

      case LsFailure(errorCode, message, commands) =>
        s"""${"ErrorCode".red}: ${errorCode.toString}
           |${"Error".red}: $message
           |  when running ${commands.mkString("[", " ", "]")}
           |""".stripMargin

      case PathExistsAndNoSymLink(path, message, commands) =>
        s"""${s"""Failed to run ${commands.mkString(" ")}
           |Error""".stripMargin.red}: $message
           |  when running ${commands.mkString("[", " ", "]")}
           |""".stripMargin

      case CommandFailure(throwable, commands) =>
        val out         = new StringWriter()
        val printWriter = new PrintWriter(out)
        throwable.printStackTrace(printWriter)
        val stackTrace  = out.toString
        throwable match {
          case ex: IOException =>
            s"""${"Error".red} when running ${commands.mkString("[", " ", "]")}:
               |  - ${ex.getMessage}
               |""".stripMargin

          case _ =>
            s"""${"Error".red} when running ${commands.mkString("[", " ", "]")}:
               |$stackTrace
               |""".stripMargin

        }

      case Coursier(coursierError) =>
        coursierError match {
          case CoursierError.JavaInstalledCmd(error) =>
            s"""${"Error".red} when running 'cs java --installed'
               |Error: ${error.toString}
               |""".stripMargin

          case CoursierError.InvalidJdkInfo(jdkInfo) =>
            s"""${"Error".red} when parsing JDK info from the result of 'cs java --installed'
               |JDK info:
               |${jdkInfo.mkString(" - ", "\n - ", "")}
               |""".stripMargin

          case CoursierError.VersionParse(version, errors, nameVersion, path) =>
            val (error1, error2, error3) = errors
            s"""${"Error".red} when parsing JDK version from the result of 'cs java --installed'
               |version: $version
               |  from
               |    $nameVersion at $path
               |  - SemVer ParseError: ${error1.render}
               |  - DecVer ParseError: ${error2.render}
               |  - SingleNumVersion ParseError: ${error3.render}
               |""".stripMargin
        }

    }

  }
}
