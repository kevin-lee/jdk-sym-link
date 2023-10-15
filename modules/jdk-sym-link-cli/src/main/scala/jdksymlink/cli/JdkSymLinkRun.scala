package jdksymlink.cli

import cats.*
import cats.data.NonEmptyList
import cats.syntax.all.*
import effectie.core.Fx
import extras.cats.syntax.all.*
import jdksymlink.core.data.{DefaultJdk, SdkMan}
import jdksymlink.core.{JdkSymLink, JdkSymLinkError, Utils}
import jdksymlink.cs.CoursierCmd

/** @author Kevin Lee
  * @since 2022-06-02
  */
object JdkSymLinkRun {
  def apply[F[*]: Fx: Monad](
    args: JdkSymLinkArgs
  ): F[Either[JdkSymLinkAppError, Unit]] = {
    val jdkSlink = JdkSymLink[F]
    args match {
      case JdkSymLinkArgs.JdkListArgs =>
        val jdk = jdkSlink
          .listAll(DefaultJdk.JavaBaseDirPath, DefaultJdk.javaBaseDirFile)
          .t
          .transform(_.toEitherNec)

        val sdkMan = jdkSlink
          .listAll(SdkMan.JavaBaseDirPath, SdkMan.javaBaseDirFile)
          .t
          .transform(_.toEitherNec)

        (jdk, sdkMan)
          .parMapN((_, _) => ())
          .leftMap(JdkSymLinkAppError.MultipleJdkSymLinkCore(_))
          .value

      case JdkSymLinkArgs.SymLinkArgs(javaVersion) =>
        jdkSlink
          .slink(
            javaVersion,
            NonEmptyList.of(
              (DefaultJdk.JavaBaseDirPath, Utils.extractVersion),
              (SdkMan.JavaBaseDirPath, Utils.extractSdkManJavaVersion)
            ),
            DefaultJdk.javaBaseDirFile
          )
          .t
          .leftMap(JdkSymLinkAppError.JdkSymLinkCore(_))
          .value
    }
  }
}
