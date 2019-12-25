package jdksymlink.cli

import jdksymlink.core.data.JavaMajorVersion

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
sealed trait JdkSymLinkArgs

object JdkSymLinkArgs {

  final case object JdkListArgs extends JdkSymLinkArgs
  final case class SymLinkArgs(javaMajorVersion: JavaMajorVersion) extends JdkSymLinkArgs

  def jdkListArgs: JdkSymLinkArgs = JdkListArgs
  def symLinkArgs(javaMajorVersion: JavaMajorVersion): JdkSymLinkArgs = SymLinkArgs(javaMajorVersion)

}

