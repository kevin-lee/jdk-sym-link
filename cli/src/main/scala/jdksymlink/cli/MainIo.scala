package jdksymlink.cli

import cats.data.NonEmptyList
import cats.effect.{ExitCode, *}
import cats.syntax.all.*
import effectie.cats.console.given
import effectie.cats.fx.ioFx
import effectie.core.ConsoleEffect
import effectie.syntax.all.*
import jdksymlink.cli.JdkSymLinkArgs.{ArgParseError, ArgParseFailureResult, JustMessageOrHelp}
import jdksymlink.core.JdkSymLinkError
import pirate.{ExitCode as PirateExitCode, *}

import scala.io.AnsiColor

/** @author Kevin Lee
  * @since 2019-12-09
  */
trait MainIo[A] extends IOApp {

  protected def command: Command[A]

  protected def runApp(a: A): IO[Either[JdkSymLinkAppError, Option[String]]]

  protected def prefs: Prefs

  private def exitCodeToEither(
    argParseFailureResult: ArgParseFailureResult
  ): IO[Either[JdkSymLinkAppError, Option[String]]] =
    argParseFailureResult match {
      case err @ JustMessageOrHelp(_) =>
        IO.pure(err.show.some.asRight[JdkSymLinkAppError])
      case err @ ArgParseError(_)     =>
        IO(JdkSymLinkAppError.ArgParse(err).asLeft[Option[String]])
    }

  override def run(args: List[String]): IO[ExitCode] = {
    def getArgs(
      args: List[String],
      command: Command[A],
      prefs: Prefs,
    ): IO[Either[ArgParseFailureResult, A]] = {
      import pirate.{Interpreter, Usage}
      import scalaz.{-\/, \/-}
      Interpreter.run(command.parse, args, prefs) match {
        case (ctx, -\/(e)) =>
          IO(
            Usage
              .printError(command, ctx, e, prefs)
              .fold[ArgParseFailureResult](
                ArgParseError(_),
                JustMessageOrHelp(_),
              )
              .asLeft[A],
          )
        case (_, \/-(v))   =>
          IO(v.asRight[ArgParseFailureResult])
      }
    }
    for {
      codeOrA       <- getArgs(args, command, prefs)
      errorOrResult <- codeOrA.fold(exitCodeToEither, runApp)
      code          <- errorOrResult.fold(
                         err =>
                           putErrStrLn[IO](err.render) >>
                             IO(ExitCode.Error),
                         _.fold(
                           IO(ExitCode.Success),
                         )(msg => putStrLn[IO](msg) >> IO(ExitCode.Success)),
                       )
    } yield code
  }

}
