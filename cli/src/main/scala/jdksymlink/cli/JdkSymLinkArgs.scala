package jdksymlink.cli

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
sealed trait JdkSymLinkArgs

object JdkSymLinkArgs {

  final case object JdkListArgs extends JdkSymLinkArgs
  final case class SymLinkArgs(javaVersion: Int) extends JdkSymLinkArgs

  def jdkListArgs: JdkSymLinkArgs = JdkListArgs
  def symLinkArgs(javaVersion: Int): JdkSymLinkArgs = SymLinkArgs(javaVersion)

}

