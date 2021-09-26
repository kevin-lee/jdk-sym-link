package jdksymlink.core

import Utils._
import cats.*
import cats.data.EitherT
import cats.syntax.all.*
import effectie.YesNo
import effectie.cats.ConsoleEffectful.*
import effectie.cats.Effectful.*
import effectie.cats.EitherTSupport.*
import effectie.cats.{ConsoleEffect, EffectConstructor}
import effectie.instances.yesNoCanEqual
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
        _ <- eitherTRightF(
               putStrLn(
                 s"""
                    |$$ ls -l $javaBaseDirPath
                    |""".stripMargin
               )
             )

        sysProcess <- eitherTRightPure(SysProcess.singleSysProcess(Option(javaBaseDir), "ls", "-l"))
        result     <- eitherTRight(SysProcess.run(sysProcess))
        list       <- eitherTOfPure(ProcessResult.toEither(result) {
                        case ProcessResult.Success(result) =>
                          result.asRight[JdkSymLinkError]

                        case ProcessResult.Failure(code, error) =>
                          JdkSymLinkError.LsFailure(code, error.mkString("\n"), List("ls", "-l")).asLeft[List[String]]

                        case ProcessResult.FailureWithNonFatal(nonFatalThrowable) =>
                          JdkSymLinkError.CommandFailure(nonFatalThrowable, List("ls", "-l")).asLeft[List[String]]
                      })
        _          <- eitherTRightF[JdkSymLinkError](putStrLn(s"${list.mkString("\n")}\n"))
      } yield ()).value

    def slink(javaMajorVersion: JavaMajorVersion): F[Either[JdkSymLinkError, Unit]] =
      (for {
        jdkNameVersionPairs <- eitherTRight(names(javaMajorVersion))
        maybeNameVersion    <- eitherTRightF(askUserToSelectJdk(jdkNameVersionPairs))
        result              <-
          maybeNameVersion match {
            case Some((name, ver)) =>
              for {
                _      <- eitherTRight(
                            putStrLn(
                              s"""
                                 |You chose '$name'.
                                 |It will create a symbolic link to '$name' (i.e. jdk${ver.major} -> $name)
                                 |and may ask you to enter your password.
                                 |""".stripMargin
                            )
                          )
                answer <- eitherTRightF(readYesNo("Would you like to proceed? (y / n) "))
                s      <- EitherT(answer match {
                            case YesNo.Yes =>
                              lnSJdk(name, javaMajorVersion)
                            case YesNo.No  =>
                              pureOf("\nCancelled.\n".asRight[JdkSymLinkError])
                          })
              } yield s

            case None =>
              eitherTRightPure("\nCancelled.\n")
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
      javaBaseDir          <- eitherTRightPure(Option(javaBaseDirFile))
      before               <- eitherTRight(s"""${Process(s"ls -l", javaBaseDir) !!}""".stripMargin)
      lsResultLogger       <- eitherTRightPure(
                                ProcessLogger(
                                  line => println(s"\n$line: It is found so will be removed and recreated."),
                                  line => println(s"\n$line: It is not found so it will be created."),
                                )
                              )
      jdkLinkAlreadyExists <-
        eitherTRight(
          (s"ls -d $JavaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}" ! (lsResultLogger)) === 0
        )
      result               <-
        if (jdkLinkAlreadyExists) {
          for {
            isNonSymLink <-
              eitherTRight(
                (s"find $JavaBaseDirPath -type l -iname jdk${JavaMajorVersion.render(javaMajorVersion)}" !!).isEmpty
              )
            r            <-
              if (isNonSymLink) {
                val path = s"$JavaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}"
                eitherTLeftPure[List[String]](
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
                )
              } else {

                (for {
                  _                         <- eitherTRightF(
                                                 putStrLn(
                                                   s"""
                                                      |$javaBaseDirFile $$ sudo rm jdk${JavaMajorVersion.render(
                                                     javaMajorVersion
                                                   )}
                                                      |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion
                                                     .render(
                                                       javaMajorVersion
                                                     )} """.stripMargin
                                                 )
                                               )
                  rmCommandList             <- eitherTRightPure(
                                                 List("sudo", "rm", s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
                                               )
                  rmCommand :: rmCommandRest = rmCommandList
                  rmCommandProcess          <- eitherTRightPure(
                                                 SysProcess.singleSysProcess(javaBaseDir, rmCommand, rmCommandRest: _*)
                                               )
                  rmResult                  <- eitherTRight(SysProcess.run(rmCommandProcess))
                  _                         <- eitherTOfPure(ProcessResult.toEither(rmResult) {
                                                 case ProcessResult.Success(result) =>
                                                   result.asRight[JdkSymLinkError]

                                                 case ProcessResult.Failure(code, error) =>
                                                   JdkSymLinkError.LsFailure(code, error.mkString("\n"), rmCommandList).asLeft[List[String]]

                                                 case ProcessResult.FailureWithNonFatal(nonFatalThrowable) =>
                                                   JdkSymLinkError.CommandFailure(nonFatalThrowable, rmCommandList).asLeft[List[String]]
                                               })

                  lnCommandList             <- eitherTRightPure(
                                                 List("sudo", "ln", "-s", name, s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
                                               )
                  lnCommand :: lnCommandRest = lnCommandList
                  lnCommandProcess          <- eitherTRightPure(
                                                 SysProcess.singleSysProcess(javaBaseDir, lnCommand, lnCommandRest: _*)
                                               )
                  lnResult                  <- eitherTRight(SysProcess.run(lnCommandProcess))
                  result                    <- eitherTOfPure(ProcessResult.toEither(lnResult) {
                                                 case ProcessResult.Success(result) =>
                                                   result.asRight[JdkSymLinkError]

                                                 case ProcessResult.Failure(code, error) =>
                                                   JdkSymLinkError
                                                     .LsFailure(
                                                       code,
                                                       error.mkString("\n"),
                                                       lnCommandList,
                                                     )
                                                     .asLeft[List[String]]

                                                 case ProcessResult.FailureWithNonFatal(nonFatalThrowable) =>
                                                   JdkSymLinkError
                                                     .CommandFailure(
                                                       nonFatalThrowable,
                                                       lnCommandList,
                                                     )
                                                     .asLeft[List[String]]
                                               })
                } yield result)

              }
          } yield r
        } else {
          (for {
            _ <- eitherTRightF(
                   putStrLn(
                     s"""
                        |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(
                       javaMajorVersion
                     )} """.stripMargin
                   )
                 )

            lnCommandList              = List("sudo", "ln", "-s", name, s"jdk${JavaMajorVersion.render(javaMajorVersion)}")
            lnCommand :: lnCommandRest = lnCommandList
            lnCommandProcess          <- eitherTRightPure(
                                           SysProcess.singleSysProcess(
                                             javaBaseDir,
                                             lnCommand,
                                             lnCommandRest: _*
                                           )
                                         )
            lnResult                  <- eitherTRight(SysProcess.run(lnCommandProcess))
            result                    <- eitherTOfPure(ProcessResult.toEither(lnResult) {
                                           case ProcessResult.Success(result) =>
                                             result.asRight[JdkSymLinkError]

                                           case ProcessResult.Failure(code, error) =>
                                             JdkSymLinkError
                                               .LsFailure(
                                                 code,
                                                 error.mkString("\n"),
                                                 lnCommandList,
                                               )
                                               .asLeft[List[String]]

                                           case ProcessResult.FailureWithNonFatal(nonFatalThrowable) =>
                                             JdkSymLinkError
                                               .CommandFailure(
                                                 nonFatalThrowable,
                                                 lnCommandList,
                                               )
                                               .asLeft[List[String]]
                                         })
          } yield result)
        }

      r <- eitherTRightF[JdkSymLinkError](
             effectOf(Process(s"ls -l", javaBaseDir) !!)
               .flatMap(after => toResultString(before, after))
           )
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
