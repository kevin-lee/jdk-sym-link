package jdksymlink.cli

import cats.*
import cats.syntax.all.*
import cats.data.NonEmptyList
import cats.effect.IO
import effectie.core.Fx
import jdksymlink.core.data.{Coursier, DefaultJdk}
import jdksymlink.core.{JdkSymLink, Utils}
import extras.cats.syntax.all.*

/**
 * @author Kevin Lee
 * @since 2022-06-02
 */
object JdkSymLinkRun {
  def apply[F[_]: Fx: Monad](args: JdkSymLinkArgs): F[Either[JdkSymLinkAppError, Unit]] =
    args match {
      case JdkSymLinkArgs.JdkListArgs =>
        for {
          jdk      <- JdkSymLink[F]
            .listAll(DefaultJdk.JavaBaseDirPath, DefaultJdk.javaBaseDirFile)
          coursier <- JdkSymLink[F]
            .listAll(Coursier.CoursierJavaBaseDirPath, Coursier.coursierJavaBaseDirFile)
        } yield (jdk.toValidatedNel, coursier.toValidatedNel)
          .mapN((_, _) => ())
          .toEither
          .leftMap(JdkSymLinkAppError.MultipleJdkSymLinkCore(_))

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
