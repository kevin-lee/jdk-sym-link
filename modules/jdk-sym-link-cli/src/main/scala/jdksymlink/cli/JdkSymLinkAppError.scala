package jdksymlink.cli

import cats.data.NonEmptyChain
import cats.syntax.all.*
import extras.render.Render
import extras.render.syntax.*
import jdksymlink.core.JdkSymLinkError

/** @author Kevin Lee
  * @since 2022-06-02
  */
enum JdkSymLinkAppError {
  case JdkSymLinkCore(jdkSymLinkError: JdkSymLinkError)
  case MultipleJdkSymLinkCore(jdkSymLinkErrors: NonEmptyChain[JdkSymLinkError])
}
object JdkSymLinkAppError {
  given renderJdkSymLinkAppError: Render[JdkSymLinkAppError] with {
    def render(jdkSymLinkAppError: JdkSymLinkAppError): String = jdkSymLinkAppError match {
      case JdkSymLinkAppError.JdkSymLinkCore(err) =>
        err.render

      case JdkSymLinkAppError.MultipleJdkSymLinkCore(jdkSymLinkErrors) =>
        jdkSymLinkErrors.map(_.render).toList.mkString("\n")
    }
  }
}
