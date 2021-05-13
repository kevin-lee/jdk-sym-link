package jdksymlink.core

import java.io.{IOException, PrintWriter, StringWriter}

/** @author Kevin Lee
  * @since 2019-12-25
  */
enum JdkSymLinkError derives CanEqual {

  case LsFailure(errorCode: Int, message: String, commands: List[String])
  case PathExistsAndNoSymLink(path: String, message: String, commands: List[String])
  case CommandFailure(throwable: Throwable, commands: List[String])

}

object JdkSymLinkError {

  import Shell.ColoredString._

  extension (jdkSymLinkError: JdkSymLinkError) {
    def render: String = jdkSymLinkError match {

      case LsFailure(errorCode, message, commands) =>
        s"""${"ErrorCode".red}: ${errorCode.toString}
           |${"Error".red}: $message
           |  when running ${commands.mkString("[", " ", "]")}
           |""".stripMargin

      case PathExistsAndNoSymLink(path, message, commands) =>
        s"""${
        s"""Failed to run ${commands.mkString(" ")}
           |Error""".stripMargin.red}: $message
           |  when running ${commands.mkString("[", " ", "]")}
           |""".stripMargin

      case CommandFailure(throwable, commands) =>
        val out         = new StringWriter()
        val printWriter = new PrintWriter(out)
        throwable.printStackTrace(printWriter)
        val stackTrace = out.toString
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
    }

  }
}
