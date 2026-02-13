package jdksymlink.cli

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import effectie.instances.ce3.fx.ioFx
import effectie.syntax.all.*
import extras.render.syntax.*

/** @author Kevin Lee
  * @since 2019-12-24
  */
object JdkSymLinkApp
    extends MainIo(
      name = "jdk-sym-link",
      header = "JDK symbolic link creator",
      version = jdksymlink.info.JdkSymLinkBuildInfo.version,
    ) {

  override def main: Opts[IO[ExitCode]] =
    JdkSymLinkArgs.opts.map { args =>
      JdkSymLinkRun[IO](args)
        .map(_.map(_ => none[String]))
        .flatMap {
          case Right(Some(msg)) =>
            putStrLn[IO](msg) *> IO.pure(ExitCode.Success)

          case Right(None) =>
            IO.pure(ExitCode.Success)

          case Left(err) =>
            putErrStrLn[IO](err.render) *> IO.pure(ExitCode.Error)
        }
    }

}
