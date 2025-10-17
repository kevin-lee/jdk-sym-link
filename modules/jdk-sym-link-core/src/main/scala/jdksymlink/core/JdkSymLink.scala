package jdksymlink.core

import Utils.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import effectie.core.{Fx, YesNo}
import effectie.syntax.all.*
import extras.cats.syntax.all.*
import extras.render.*
import extras.render.syntax.*
import jdksymlink.core.data.*
import jdksymlink.cs.CoursierCmd
import jdksymlink.cs.CoursierCmd.JdkByCs
import just.sysprocess.*

import java.io.File
import scala.language.postfixOps
import sys.process.*

import extras.scala.io.syntax.color.*

/** #############################################
  * ## Simple Scala script to create           ##
  * ## a symbolic link to JDK for Mac OS X     ##
  * ##                                         ##
  * ## @author Lee, SeongHyun (Kevin)          ##
  * ## @version 0.0.1 (2015-04-03)             ##
  * ## @version 0.1.0 (2019-03-20)             ##
  * ## @version 0.2.0 (2019-04-20)             ##
  * ##                                         ##
  * ## https://kevinlee.io                     ##
  * #############################################
  * @author Kevin Lee
  * @since 2019-12-22
  */
trait JdkSymLink[F[*]] {
  def listAll(javaBaseDirPath: JvmBaseDirPath, javaBaseDir: File): F[Either[JdkSymLinkError, Unit]]
  def slink(
    javaMajorVersion: JavaMajorVersion,
    jdkSourcePaths: NonEmptyList[(JvmBaseDirPath, String => Option[NameAndVersion])],
    targetPath: File
  ): F[Either[JdkSymLinkError, Unit]]
}

object JdkSymLink {

  def apply[F[*]: Monad: Fx]: JdkSymLink[F] = new JdkSymLinkF[F]

  private final class JdkSymLinkF[F[*]: Monad: Fx] extends JdkSymLink[F] {

    def listAll(javaBaseDirPath: JvmBaseDirPath, javaBaseDir: File): F[Either[JdkSymLinkError, Unit]] =
      (for {
        _ <- effectOf(!javaBaseDirPath.toPath.dirExist)
               .rightT
               .ifM(
                 putStrLn(s"${javaBaseDirPath.value} does not exist.").rightT,
                 for {
                   _ <- putStrLn(
                          s"""
                 |$$ ls -l ${javaBaseDirPath.value}
                 |""".stripMargin
                        ).rightT

                   sysProcess <- pureOrError(SysProcess.singleSysProcess(Option(javaBaseDir), "ls", "-l")).rightT
                   result     <- effectOf(sysProcess.run())
                                   .eitherT
                                   .transform {
                                     case Right(ProcessResult(result)) =>
                                       result.asRight[JdkSymLinkError]

                                     case Left(ProcessError.Failure(code, error)) =>
                                       JdkSymLinkError
                                         .LsFailure(code, error.mkString("\n"), List("ls", "-l"))
                                         .asLeft[List[String]]

                                     case Left(ProcessError.FailureWithNonFatal(nonFatalThrowable)) =>
                                       JdkSymLinkError
                                         .CommandFailure(nonFatalThrowable, List("ls", "-l"))
                                         .asLeft[List[String]]
                                   }
                   _          <- putStrLn(s"${result.mkString("\n")}\n").rightT[JdkSymLinkError]
                 } yield ()
               )
      } yield ()).value

    def slink(
      javaMajorVersion: JavaMajorVersion,
      jdkSourcePaths: NonEmptyList[(JvmBaseDirPath, String => Option[NameAndVersion])],
      targetPath: File
    ): F[Either[JdkSymLinkError, Unit]] =
      (for {
        jdkNameVersions  <- jdkSourcePaths
                              .toList
                              .filter { (path, _) =>
                                path.toPath.dirExist && path.toPath.nonEmptyInside
                              }
                              .flatMap { (path, extractVersion) =>
                                val jdkPathFile     = File(path.value)
                                val nameAndVersions = names(javaMajorVersion, jdkPathFile, extractVersion)
                                nameAndVersions.map {
                                  case a @ (name, _) =>
                                    (JvmBaseDirPath(s"${path.value}/$name"), a)
                                }
                              }
                              .rightTF
        maybeNameVersion <- askUserToSelectJdk(jdkNameVersions).rightT
        nameVersionPath = maybeNameVersion.map {
                            case JdkByCs(_, name, major, version, path) =>
                              (
                                s"${name.value}:${major.value.toString} (${version.render})",
                                major.value.toString,
                                JvmBaseDirPath(path.value.toString)
                              )
                            case (jdkPath, (name, ver)) =>
                              (name, ver.major, jdkPath)
                          }
        result <- nameVersionPath match {
                    case Some(name, majorVersion, jdkPath) =>
                      for {
                        _      <- putStrLn(
                                    s"""
                                       |You chose '$name'.
                                       |It will create a symbolic link to '$name' (i.e. jdk$majorVersion -> $name)
                                       |and may ask you to enter your password.
                                       |""".stripMargin
                                  ).rightT
                        answer <- readYesNo("Would you like to proceed? (y / n) ").rightT
                        s      <- (answer match {
                                    case YesNo.Yes =>
                                      lnSJdk(name, javaMajorVersion, jdkPath, targetPath)
                                    case YesNo.No =>
                                      pureOf("\nCancelled.\n".asRight[JdkSymLinkError])
                                  }).t
                      } yield s

                    case None =>
                      pureOf("\nCancelled.\n").rightT
                  }
        _      <- EitherT.right[JdkSymLinkError](putStrLn(result))
      } yield ()).value

    def askUserToSelectJdk(
      names: List[JdkByCs | (JvmBaseDirPath, NameAndVersion)]
    ): F[Option[JdkByCs | (JvmBaseDirPath, NameAndVersion)]] = {
      def getAnswer(length: Int): F[Option[Int]] = for {
        choice <- readLn
        answer <- choice match {
                    case "c" | "C" =>
                      effectOf(none[Int])
                    case _ =>
                      if (isNonNegativeNumber(choice) && choice.toInt < length)
                        effectOf(choice.toInt.some)
                      else
                        putStrLn(
                          """Please enter a number on the list:
                            |(or [c] for cancellation)""".stripMargin
                        ) *> getAnswer(length)
                  }
      } yield answer

      for {
        listOfJdk      <- effectOf(
                            names.zipWithIndex.map {
                              case (JdkByCs(_, name, major, version, jdkPath), index) =>
                                val nameString =
                                  s"[$index] ${name.value.blue}:${major.value.toString.green} (${version.render}) "
                                val pathString = s"at ${jdkPath.value}"
                                raw"""$nameString
                                     |  $pathString""".stripMargin
                              case ((jdkPath, (name, ver)), index) =>
                                val nameString = s"[$index] ${name.blue} (${ver.render}) "
                                val pathString = s"at ${jdkPath.value}"
                                raw"""$nameString
                                     |  $pathString""".stripMargin
                            }
                          )
        length         <- effectOf(listOfJdk.length)
        _              <- putStrLn(
                            s"""
                               |Version(s) found:
                               |${listOfJdk.mkString("\n")}
                               |[c] Cancel
                               |""".stripMargin
                          )
        answer         <- getAnswer(length)
        nameAndVersion <- effectOf(answer.map(names(_)))
      } yield nameAndVersion
    }

    def lnSJdk(
      name: String,
      javaMajorVersion: JavaMajorVersion,
      javaBaseDirPath: JvmBaseDirPath,
      javaBaseDirFile: File
    ): F[Either[JdkSymLinkError, String]] = (for {
      _                    <- putStrLn(
                                s"""===================${"=" * name.length}
                                   |Create symlink for $name
                                   |===================${"=" * name.length}
                                   |""".stripMargin
                              ).rightT
      javaBaseDir          <- pureOf(Option(javaBaseDirFile)).rightT
      before               <- pureOf(s"""${Process(s"ls -l", javaBaseDir) !!}""".stripMargin).rightT
      lsResultLogger       <- pureOf(
                                ProcessLogger(
                                  line => println(s"\n$line: It is found so will be removed and recreated."),
                                  line => println(s"\n$line: It is not found so it will be created."),
                                )
                              ).rightT
      jdkLinkAlreadyExists <-
        pureOf(
          (s"ls -d ${javaBaseDirFile.getCanonicalPath}/jdk${javaMajorVersion.render}" ! (lsResultLogger)) === 0
        ).rightT
      result               <-
        if (jdkLinkAlreadyExists) {
          for {
            isNonSymLink <-
              pureOf(
                s"find ${javaBaseDirFile.getCanonicalPath} -type l -iname jdk${javaMajorVersion.render}"
              )
                .flatTap(putStrLn(_))
                .flatMap { cmd =>
                  effectOf(
                    (cmd.!!).isEmpty
                  )
                }
                .rightT
            r            <-
              if (isNonSymLink) {
                val path = s"${javaBaseDirFile.getCanonicalPath}/jdk${javaMajorVersion.render}"
                pureOf(
                  JdkSymLinkError.PathExistsAndNoSymLink(
                    path,
                    s"\n'$path' already exists and it's not a symbolic link so nothing will be done.",
                    List(
                      "find",
                      javaBaseDirFile.getCanonicalPath,
                      "-type",
                      "l",
                      "-iname",
                      s"jdk${javaMajorVersion.render}",
                    ),
                  )
                ).leftT[List[String]]
              } else {

                (for {
                  _ <- putStrLn(
                         s"""
                            |$javaBaseDirFile $$ sudo rm jdk${javaMajorVersion.render}
                            |""".stripMargin
                       ).rightT

                  (rmCommand, rmCommandRest) = ("sudo", List("rm", s"jdk${javaMajorVersion.render}"))
                  rmCommandList              = rmCommand :: rmCommandRest
                  rmCommandProcess <- pureOf(
                                        SysProcess.singleSysProcess(javaBaseDir, rmCommand, rmCommandRest*)
                                      ).rightT
                  rmResult         <- effectOf(rmCommandProcess.run())
                                        .eitherT
                                        .transform {
                                          case Right(ProcessResult(result)) =>
                                            result.asRight[JdkSymLinkError]

                                          case Left(ProcessError.Failure(code, error)) =>
                                            JdkSymLinkError
                                              .LsFailure(code, error.mkString("\n"), rmCommandList)
                                              .asLeft[List[String]]

                                          case Left(ProcessError.FailureWithNonFatal(nonFatalThrowable)) =>
                                            JdkSymLinkError
                                              .CommandFailure(nonFatalThrowable, rmCommandList)
                                              .asLeft[List[String]]
                                        }

                  _ <- putStrLn(
                         s"""
                            |$javaBaseDirFile $$ sudo ln -s ${javaBaseDirPath.value} jdk${javaMajorVersion.render}
                            |""".stripMargin
                       ).rightT

                  (lnCommand, lnCommandRest) = (
                                                 "sudo",
                                                 List(
                                                   "ln",
                                                   "-s",
                                                   javaBaseDirPath.value,
                                                   s"jdk${javaMajorVersion.render}"
                                                 )
                                               )
                  lnCommandList              = lnCommand :: lnCommandRest
                  lnCommandProcess <- pureOf(
                                        SysProcess.singleSysProcess(javaBaseDir, lnCommand, lnCommandRest*)
                                      ).rightT
                  lnResult         <- effectOf(lnCommandProcess.run())
                                        .eitherT
                                        .transform {
                                          case Right(ProcessResult(result)) =>
                                            result.asRight[JdkSymLinkError]

                                          case Left(ProcessError.Failure(code, error)) =>
                                            JdkSymLinkError
                                              .LsFailure(
                                                code,
                                                error.mkString("\n"),
                                                lnCommandList,
                                              )
                                              .asLeft[List[String]]

                                          case Left(ProcessError.FailureWithNonFatal(nonFatalThrowable)) =>
                                            JdkSymLinkError
                                              .CommandFailure(
                                                nonFatalThrowable,
                                                lnCommandList,
                                              )
                                              .asLeft[List[String]]
                                        }
                } yield lnResult)

              }
          } yield r
        } else {
          (for {
            _ <- putStrLn(
                   s"""
                      |$javaBaseDirFile $$ sudo ln -s ${javaBaseDirPath.value} jdk${javaMajorVersion.render} """.stripMargin
                 ).rightT
            (lnCommand, lnCommandRest) = (
                                           "sudo",
                                           List(
                                             "ln",
                                             "-s",
                                             javaBaseDirPath.value,
                                             s"jdk${javaMajorVersion.render}"
                                           )
                                         )
            lnCommandList              = lnCommand :: lnCommandRest
            lnCommandProcess <- pureOf(
                                  SysProcess.singleSysProcess(
                                    javaBaseDir,
                                    lnCommand,
                                    lnCommandRest*
                                  )
                                ).rightT
            lnResult         <- effectOf(lnCommandProcess.run())
                                  .eitherT
                                  .transform {
                                    case Right(ProcessResult(result)) =>
                                      result.asRight[JdkSymLinkError]

                                    case Left(ProcessError.Failure(code, error)) =>
                                      JdkSymLinkError
                                        .LsFailure(
                                          code,
                                          error.mkString("\n"),
                                          lnCommandList,
                                        )
                                        .asLeft[List[String]]

                                    case Left(ProcessError.FailureWithNonFatal(nonFatalThrowable)) =>
                                      JdkSymLinkError
                                        .CommandFailure(
                                          nonFatalThrowable,
                                          lnCommandList,
                                        )
                                        .asLeft[List[String]]
                                  }
          } yield lnResult)
        }

      r <- effectOf(Process(s"ls -l", javaBaseDir) !!)
             .flatMap(after => toResultString(before, after))
             .rightT[JdkSymLinkError]
    } yield r).value

    def toResultString(before: String, after: String): F[String] =
      effectOf(
        s"""
           |Done!
           |
           |# Before
           |--------------------------------------
           |$before
           |======================================
           |
           |# After
           |--------------------------------------
           |$after
           |======================================
           |""".stripMargin
      )
  }

}
