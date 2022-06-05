package jdksymlink.cli

import pirate.*
import Pirate.*
import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import effectie.cats.fx.ioFx
import extras.cats.syntax.all.*
import jdksymlink.core.data.{Coursier, DefaultJdk, JavaMajorVersion}
import jdksymlink.core.{JdkSymLink, JdkSymLinkError, Utils}
import jdksymlink.info.JdkSymLinkBuildInfo
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
