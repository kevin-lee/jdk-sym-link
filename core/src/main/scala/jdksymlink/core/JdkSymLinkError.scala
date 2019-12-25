package jdksymlink.core

/**
 * @author Kevin Lee
 * @since 2019-12-25
 */
sealed trait JdkSymLinkError

object JdkSymLinkError {
  def render(jdkSymLinkError: JdkSymLinkError): String = ???
}
