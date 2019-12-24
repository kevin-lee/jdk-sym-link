package jdksymlink

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
sealed trait JdkSymbolicLinkArgs

object JdkSymbolicLinkArgs {

  final case object JdkListArgs extends JdkSymbolicLinkArgs
  final case class SymLinkArgs(javaVersion: Int) extends JdkSymbolicLinkArgs

  def jdkListArgs: JdkSymbolicLinkArgs = JdkListArgs
  def symLinkArgs(javaVersion: Int): JdkSymbolicLinkArgs = SymLinkArgs(javaVersion)

}

