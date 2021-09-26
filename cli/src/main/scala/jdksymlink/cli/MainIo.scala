package jdksymlink.cli

import cats.effect.*
import cats.data.NonEmptyList
import cats.syntax.all.*
import effectie.cats.ConsoleEffect
import jdksymlink.core.JdkSymLinkError
import pirate.{ExitCode as PirateExitCode, *}
import scalaz.*

import scala.io.AnsiColor

/** @author Kevin Lee
  * @since 2019-12-09
  */
trait MainIo[A] {

  def command: Command[A]

  def run(a: A): IO[Either[NonEmptyList[JdkSymLinkError], Unit]]
  private def run0(a: A): IO[NonEmptyList[JdkSymLinkError] \/ Unit] =
    run(a).map(\/.fromEither(_))

  def prefs: Prefs = DefaultPrefs()

  def exitWith[X](exitCode: ExitCode): IO[X] =
    IO(sys.exit(exitCode.code))

  def exitWithPirate[X](exitCode: PirateExitCode): IO[X] =
    IO(exitCode.fold(sys.exit(0), sys.exit(_)))

  def getArgs(args: Array[String], command: Command[A], prefs: Prefs): IO[PirateExitCode \/ A] =
    IO(Runners.runWithExit[A](args.toList, command, prefs).unsafePerformIO())

  def main(args: Array[String]): Unit = (for {
    codeOrA       <- getArgs(args, command, prefs)
    errorOrResult <- codeOrA.fold[IO[NonEmptyList[JdkSymLinkError] \/ Unit]](exitWithPirate, run0)
    _             <- errorOrResult.fold(
                       errs =>
                         ConsoleEffect[IO].putStrLn(
                           errs
                             .map(_.render)
                             .toList
                             .mkString(s"${AnsiColor.RED}Error:${AnsiColor.RESET}\n- ", "\n- ", "\n")
                         ) >>
                           exitWith(ExitCode.Error),
                       IO(_)
                     )
  } yield ())
    .unsafeRunSync()

}
