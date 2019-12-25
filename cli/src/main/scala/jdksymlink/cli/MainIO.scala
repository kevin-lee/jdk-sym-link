package jdksymlink.cli

import pirate._

import scalaz._
import Scalaz._
import scalaz.effect._

import jdksymlink.core.JdkSymLinkError


/**
 * @author Kevin Lee
 * @since 2019-12-09
 */
trait MainIO[A]  {

  def command: Command[A]
 
  def run(a: A): IO[JdkSymLinkError \/ Unit]
 
  def prefs: Prefs = DefaultPrefs()
 
  def main(args: Array[String]): Unit = (for {
    exitCodeOrA <- Runners.runWithExit(args.toList, command, prefs)
    errorOrResult <- exitCodeOrA.fold[IO[JdkSymLinkError \/ Unit]](ExitCode.exitWith, run)
    _ <- errorOrResult.fold(
        err => IO.putStrLn(JdkSymLinkError.render(err)) *>
            ExitCode.exitWith(ExitCode.failure(1))
      , IO(_)
      )
  } yield ())
    .unsafePerformIO()

}