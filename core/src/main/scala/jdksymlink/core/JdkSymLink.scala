package jdksymlink.core

import Utils.*
import cats.*
import cats.data.EitherT
import cats.syntax.all.*
import effectie.YesNo
import effectie.cats.ConsoleEffectful.*
import effectie.cats.Effectful.*
import extras.cats.syntax.either.*
import effectie.cats.{ConsoleEffect, EffectConstructor}
import jdksymlink.core.data.*
import just.sysprocess.*

import canequal.all.given

import java.io.File
import scala.language.postfixOps
import sys.process.*

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
trait JdkSymLink[F[_]] {
  def listAll(javaBaseDirPath: String, javaBaseDir: File): F[Either[JdkSymLinkError, Unit]]
  def slink(javaMajorVersion: JavaMajorVersion): F[Either[JdkSymLinkError, Unit]]
}

object JdkSymLink {

  def apply[F[_]: JdkSymLink]: JdkSymLink[F] = summon[JdkSymLink[F]]

  given jdkSymLinkF[F[_]: Monad: EffectConstructor: ConsoleEffect]: JdkSymLink[F] with {

    def listAll(javaBaseDirPath: String, javaBaseDir: File): F[Either[JdkSymLinkError, Unit]] =
      (for {
        _ <- putStrLn(
               s"""
                  |$$ ls -l $javaBaseDirPath
                  |""".stripMargin
             ).rightT

        sysProcess <- pureOf(SysProcess.singleSysProcess(Option(javaBaseDir), "ls", "-l")).rightT
        result     <- effectOf(sysProcess.run())
                        .eitherT
                        .transform {
                          case Right(ProcessResult(result)) =>
                            result.asRight[JdkSymLinkError]

                          case Left(ProcessError.Failure(code, error)) =>
                            JdkSymLinkError.LsFailure(code, error.mkString("\n"), List("ls", "-l")).asLeft[List[String]]

                          case Left(ProcessError.FailureWithNonFatal(nonFatalThrowable)) =>
                            JdkSymLinkError.CommandFailure(nonFatalThrowable, List("ls", "-l")).asLeft[List[String]]
                        }
        _          <- putStrLn(s"${result.mkString("\n")}\n").rightT[JdkSymLinkError]
      } yield ()).value

    def slink(javaMajorVersion: JavaMajorVersion): F[Either[JdkSymLinkError, Unit]] =
      (for {
        jdkNameVersionPairs <- pureOf(names(javaMajorVersion)).rightT
        maybeNameVersion    <- askUserToSelectJdk(jdkNameVersionPairs).rightT
        result              <- maybeNameVersion match {
                                 case Some((name, ver)) =>
                                   for {
                                     _      <- putStrLn(
                                                 s"""
                                                    |You chose '$name'.
                                                    |It will create a symbolic link to '$name' (i.e. jdk${ver.major} -> $name)
                                                    |and may ask you to enter your password.
                                                    |""".stripMargin
                                               ).rightT
                                     answer <- readYesNo("Would you like to proceed? (y / n) ").rightT
                                     s      <- EitherT(answer match {
                                                 case YesNo.Yes =>
                                                   lnSJdk(name, javaMajorVersion)
                                                 case YesNo.No  =>
                                                   pureOf("\nCancelled.\n".asRight[JdkSymLinkError])
                                               })
                                   } yield s

                                 case None =>
                                   pureOf("\nCancelled.\n").rightT
                               }
        _                   <- EitherT.right[JdkSymLinkError](putStrLn(result))
      } yield ()).value

    def askUserToSelectJdk(names: Vector[NameAndVersion]): F[Option[NameAndVersion]] = {
      def getAnswer(length: Int): F[Option[Int]] = for {
        choice <- readLn
        answer <-
          choice match {
            case "c" | "C" =>
              effectOf(none[Int])
            case _         =>
              if (isNonNegativeNumber(choice) && choice.toInt < length)
                effectOf(choice.toInt.some)
              else
                putStrLn("""Please enter a number on the list:
                           |(or [c] for cancellation)""".stripMargin) *> getAnswer(length)
          }
      } yield answer

      for {
        listOfJdk      <- effectOf(names.zipWithIndex.map {
                            case ((name, split), index) =>
                              s"[$index] $name"
                          })
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

    def lnSJdk(name: String, javaMajorVersion: JavaMajorVersion): F[Either[JdkSymLinkError, String]] = (for {
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
          (s"ls -d $JavaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}" ! (lsResultLogger)) === 0
        ).rightT
      result               <-
        if (jdkLinkAlreadyExists) {
          for {
            isNonSymLink <-
              pureOf(
                (s"find $JavaBaseDirPath -type l -iname jdk${JavaMajorVersion.render(javaMajorVersion)}" !!).isEmpty
              ).rightT
            r            <-
              if (isNonSymLink) {
                val path = s"$JavaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}"
                pureOf(
                  JdkSymLinkError.PathExistsAndNoSymLink(
                    path,
                    s"\n'$path' already exists and it's not a symbolic link so nothing will be done.",
                    List(
                      "find",
                      "javaBaseDirPath",
                      "-type",
                      "l",
                      "-iname",
                      s"jdk${JavaMajorVersion.render(javaMajorVersion)}",
                    ),
                  )
                ).leftT[List[String]]
              } else {

                (for {
                  _ <- putStrLn(
                         s"""
                            |$javaBaseDirFile $$ sudo rm jdk${JavaMajorVersion.render(
                           javaMajorVersion
                         )}
                            |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion
                           .render(
                             javaMajorVersion
                           )} """.stripMargin
                       ).rightT

                  rmCommandList             <- pureOf(
                                                 List("sudo", "rm", s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
                                               ).rightT
                  rmCommand :: rmCommandRest = rmCommandList
                  rmCommandProcess          <- pureOf(
                                                 SysProcess.singleSysProcess(javaBaseDir, rmCommand, rmCommandRest: _*)
                                               ).rightT
                  rmResult                  <- effectOf(rmCommandProcess.run())
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

                  lnCommandList             <- pureOf(
                                                 List("sudo", "ln", "-s", name, s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
                                               ).rightT
                  lnCommand :: lnCommandRest = lnCommandList
                  lnCommandProcess          <- pureOf(
                                                 SysProcess.singleSysProcess(javaBaseDir, lnCommand, lnCommandRest: _*)
                                               ).rightT
                  lnResult                  <- effectOf(lnCommandProcess.run())
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
                      |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(
                     javaMajorVersion
                   )} """.stripMargin
                 ).rightT

            lnCommandList              = List("sudo", "ln", "-s", name, s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
            lnCommand :: lnCommandRest = lnCommandList
            lnCommandProcess          <- pureOf(
                                           SysProcess.singleSysProcess(
                                             javaBaseDir,
                                             lnCommand,
                                             lnCommandRest: _*
                                           )
                                         ).rightT
            lnResult                  <- effectOf(lnCommandProcess.run())
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
