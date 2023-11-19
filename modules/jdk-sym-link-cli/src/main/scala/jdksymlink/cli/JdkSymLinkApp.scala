package jdksymlink.cli

import pirate.*
import cats.effect.*
import cats.syntax.all.*
import effectie.instances.ce3.fx.ioFx
import piratex.*

/** @author Kevin Lee
  * @since 2019-12-24
  */
object JdkSymLinkApp extends MainIo[JdkSymLinkArgs] {

  val cmd: Command[JdkSymLinkArgs] =
    Metavar.rewriteCommand(
      Help.rewriteCommand(JdkSymLinkArgs.rawCmd),
    )

  override def command: Command[JdkSymLinkArgs] = cmd

  override def prefs: Prefs = DefaultPrefs().copy(width = 100)

  override def runApp(args: JdkSymLinkArgs): IO[Either[JdkSymLinkAppError, Option[String]]] =
    JdkSymLinkRun[IO](args)
      .map(_.map(_ => none[String]))

}
