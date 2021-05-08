package jdksymlink.core

import data.Eql
import java.io.{IOException, PrintWriter, StringWriter}

/** @author Kevin Lee
  * @since 2019-12-25
  */
enum JdkSymLinkError derives Eql {

  case LsFailure(errorCode: Int, message: String, commands: List[String])
  case PathExistsAndNoSymLink(path: String, message: String, commands: List[String])
  case CommandFailure(throwable: Throwable, commands: List[String])

}

object JdkSymLinkError {
  def lsFailure(errorCode: Int, message: String, commands: List[String]): JdkSymLinkError =
    LsFailure(errorCode, message, commands)

  def pathExistsAndNoSymLink(path: String, message: String, commands: List[String]): JdkSymLinkError =
    PathExistsAndNoSymLink(path, message, commands)

  def commandFailure(throwable: Throwable, commands: List[String]): JdkSymLinkError =
    CommandFailure(throwable, commands)

  def render(jdkSymLinkError: JdkSymLinkError): String = jdkSymLinkError match {
    case LsFailure(errorCode, message, commands) =>
      s"""${Shell.red("ErrorCode")}: ${errorCode.toString}
         |${Shell.red("Error")}: $message
         |  when running ${commands.mkString("[", " ", "]")}
         |""".stripMargin

    case PathExistsAndNoSymLink(path, message, commands) =>
      s"""${Shell.red(
      s"""Failed to run ${commands.mkString(" ")}
         |Error""".stripMargin)}: $message
         |  when running ${commands.mkString("[", " ", "]")}
         |""".stripMargin

    case CommandFailure(throwable, commands) =>
      val out         = new StringWriter()
      val printWriter = new PrintWriter(out)
      throwable.printStackTrace(printWriter)
      val stackTrace = out.toString
      throwable match {
        case ex: IOException =>
          s"""${Shell.red("Error")} when running ${commands.mkString("[", " ", "]")}:
             |  - ${ex.getMessage}
             |""".stripMargin
        case _ =>
          s"""${Shell.red("Error")} when running ${commands.mkString("[", " ", "]")}:
             |$stackTrace
             |""".stripMargin

      }

  }
}
