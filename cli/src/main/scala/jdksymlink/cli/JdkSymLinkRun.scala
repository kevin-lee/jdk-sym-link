package jdksymlink.cli

import cats.*
import cats.syntax.all.*
import cats.data.{NonEmptyChain, NonEmptyList}
import cats.effect.IO
import effectie.core.Fx
import jdksymlink.core.data.{Coursier, DefaultJdk}
import jdksymlink.core.{JdkSymLink, JdkSymLinkError, Utils}
import extras.cats.syntax.all.*
import effectie.syntax.all.*
import jdksymlink.cs.CoursierCmd
import effectie.cats.console.given
import jdksymlink.cs.CoursierCmd.{CoursierError, JdkByCs}
import scala.util.Try

/** @author Kevin Lee
  * @since 2022-06-02
  */
object JdkSymLinkRun {
  def apply[F[_]: Fx: Monad](args: JdkSymLinkArgs): F[Either[JdkSymLinkAppError, Unit]] =
    args match {
      case JdkSymLinkArgs.JdkListArgs =>
        val jdk = JdkSymLink[F]
          .listAll(DefaultJdk.JavaBaseDirPath, DefaultJdk.javaBaseDirFile)
          .t
          .transform(_.toEitherNec)

        val coursier = JdkSymLink[F]
          .listAll(Coursier.CoursierJavaBaseDirPath, Coursier.coursierJavaBaseDirFile)
          .t
          .transform(_.toEitherNec)

        val csJdk =
          effectOf {
            import scala.sys.process._
            "type cs".! == 0
          }.handleNonFatal(_ => false)
            .rightT[NonEmptyChain[JdkSymLinkError]]
            .flatMap { csFound =>
              (
                putStrLn[F]("\n>>> Getting JDKs from 'cs java --installed'. Please wait.") >>
                  CoursierCmd.javaInstalled[F]
              )
                .t
                .leftMap(JdkSymLinkError.Coursier(_))
                .transform(_.toEitherNec)
                .flatMap { lines =>
                  putStrLn[F](lines.map(_.render).mkString("\n")).rightT
                }
                .whenA(csFound)
            }

        (
          jdk,
          coursier,
          csJdk
        )
          .mapN((_, _, _) => ())
          .leftMap(JdkSymLinkAppError.MultipleJdkSymLinkCore(_))
          .value

      case JdkSymLinkArgs.SymLinkArgs(javaVersion) =>
        JdkSymLink[F]
          .slink(
            javaVersion,
            NonEmptyList.of(
              (DefaultJdk.JavaBaseDirPath, Utils.extractVersion),
              (Coursier.CoursierJavaBaseDirPath, Utils.extractCoursierJavaVersion)
            ),
            DefaultJdk.javaBaseDirFile
          )
          .t
          .leftMap(JdkSymLinkAppError.JdkSymLinkCore(_))
          .value
    }
}
