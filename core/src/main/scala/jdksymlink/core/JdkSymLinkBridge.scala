package jdksymlink.core

import java.io.File

import jdksymlink.core.data.JavaMajorVersion

/**
 * @author Kevin Lee
 * @since 2019-12-29
 */
object JdkSymLinkBridge {

  def listAll[F[_]: JdkSymLink](javaBaseDirPath: String, javaBaseDir: File): F[Unit] =
    JdkSymLink[F].listAll(javaBaseDirPath, javaBaseDir)

  def slink[F[_]: JdkSymLink](javaMajorVersion: JavaMajorVersion): F[Unit] =
    JdkSymLink[F].slink(javaMajorVersion)

}
