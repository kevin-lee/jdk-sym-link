package jdksymlink.cli

import cats.*
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import com.monovore.decline.*
import effectie.core.*
import effectie.instances.ce3.fx.ioFx
import effectie.syntax.all.*

/** @author Kevin Lee
  * @since 2019-12-09
  */
abstract class MainIo[A](
  protected val name: String,
  protected val header: String,
  protected val helpFlag: Boolean = true,
//  protected val helpFormat: HelpFormat = HelpFormat.autoColors(sys.env),
  protected val helpFormat: HelpFormat =
    if (sys.env.get("NO_COLOR").fold("")(_.trim) == "")
      HelpFormat.Colors
    else
      HelpFormat.Plain,
  protected val version: String = ""
) extends IOApp {

  def main: Opts[IO[ExitCode]]

  val renderHelp: Help => String = MainIo.renderHelp(helpFormat)

  override final def run(args: List[String]): IO[ExitCode] = {
    MainIo
      .run[IO](name, header, helpFlag, renderHelp, Option(version).filter(_.nonEmpty))(main, args)
  }

}
object MainIo {

  def run[F[*]: Fx: Monad: Console](
    name: String,
    header: String,
    helpFlag: Boolean = true, // scalafix:ok DisableSyntax.defaultArgs
    helpRender: Help => String,
    version: Option[String] = None // scalafix:ok DisableSyntax.defaultArgs
  )(opts: Opts[F[ExitCode]], args: List[String]): F[ExitCode] =
    run(Command(name, header, helpFlag)(version.map(addVersionFlag(opts)).getOrElse(opts)), args)(helpRender)

  private def run[F[*]: Fx: Monad: Console](
    command: Command[F[ExitCode]],
    args: List[String]
  )(helpRender: Help => String): F[ExitCode] =
    for {
      parseResult <- effectOf(
                       command.parse(
                         PlatformApp.ambientArgs getOrElse args,
                         PlatformApp.ambientEnvs getOrElse sys.env
                       )
                     )
      exitCode    <- parseResult.fold(printHelp[F](helpRender), identity)
    } yield exitCode

  private[MainIo] def printHelp[F[*]: Console: Functor](helpRender: Help => String)(help: Help): F[ExitCode] =
    Console[F].errorln(helpRender(help)).as {
      if (help.errors.nonEmpty) ExitCode.Error
      else ExitCode.Success
    }

  private[MainIo] def renderHelp(helpFormat: HelpFormat): Help => String =
    if (helpFormat.colorsEnabled)
      help => help.render(helpFormat)
    else
      help => help.toString

  private[MainIo] def addVersionFlag[F[*]: Console: Functor](
    opts: Opts[F[ExitCode]]
  )(version: String): Opts[F[ExitCode]] = {
    val flag = Opts.flag(
      long = "version",
      short = "v",
      help = "Print the version number and exit.",
      visibility = Visibility.Partial
    )

    flag.as(Console[F].println(version).as(ExitCode.Success)) orElse opts
  }

}
