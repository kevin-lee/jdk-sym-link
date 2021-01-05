package jdksymlink.core

import java.io.File

import Utils._

import cats._
import cats.implicits._

import effectie.cats.ConsoleEffectful._
import effectie.cats.Effectful._
import effectie.YesNo
import effectie.cats.{ConsoleEffect, EffectConstructor}

import jdksymlink.core.data._

import scala.language.postfixOps
import sys.process._

/**
 * #############################################
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
  def listAll(javaBaseDirPath: String, javaBaseDir: File): F[Unit]
  def slink(javaMajorVersion: JavaMajorVersion): F[Unit]
}

object JdkSymLink {

  def apply[F[_] : JdkSymLink]: JdkSymLink[F] = implicitly[JdkSymLink[F]]

  implicit def jdkSymLinkF[F[_] : Monad : EffectConstructor : ConsoleEffect]: JdkSymLink[F] =
    new JdkSymLinkF[F]

  final class JdkSymLinkF[F[_] : Monad : EffectConstructor : ConsoleEffect]
    extends JdkSymLink[F] {

    def listAll(javaBaseDirPath: String, javaBaseDir: File): F[Unit] =
      for {
        _ <- putStrLn(
            s"""
               |$$ ls -l $javaBaseDirPath
               |""".stripMargin
          )
        list <- effectOf(Process(s"ls -l", Option(javaBaseDir)) !!)
        _ <- putStrLn(s"$list\n")
      } yield ()

    def slink(javaMajorVersion: JavaMajorVersion): F[Unit] =
      for {
        jdkNameVersionPairs <- effectOf(names(javaMajorVersion))
        maybeNameVersion <- askUserToSelectJdk(jdkNameVersionPairs)
        result <- maybeNameVersion match {
          case Some((name, ver)) =>
            for {
              _ <- putStrLn(
                  s"""
                     |You chose '$name'.
                     |It will create a symbolic link to '$name' (i.e. jdk${ver.major} -> $name)
                     |and may ask you to enter your password.
                     |""".stripMargin
                )
              answer <- readYesNo("Would you like to proceed? (y / n) ")
              s <- answer match {
                  case YesNo.Yes  =>
                    lnSJdk(name, javaMajorVersion)
                  case YesNo.No  =>
                    pureOf("\nCancelled.\n")
                }
            } yield s

          case None =>
            pureOf("\nCancelled.\n")
        }
        _ <- putStrLn(result)
      } yield ()

    def askUserToSelectJdk(names: Vector[NameAndVersion]): F[Option[NameAndVersion]] = {
      def getAnswer(length: Int): F[Option[Int]] = for {
        choice <- readLn
        answer <- choice match {
            case "c" | "C" =>
              effectOf(none[Int])
            case _  =>
              if (isNonNegativeNumber(choice) && choice.toInt < length)
                effectOf(choice.toInt.some)
              else
                putStrLn(
                  """Please enter a number on the list:
                    |(or [c] for cancellation)""".stripMargin) *> getAnswer(length)
          }
      } yield answer

      for {
        listOfJdk <- effectOf(names.zipWithIndex.map { case ((name, split), index) => s"[$index] $name" })
        length <- effectOf(listOfJdk.length)
        _ <- putStrLn(
            s"""
               |Version(s) found:
               |${listOfJdk.mkString("\n")}
               |[c] Cancel
               |""".stripMargin
          )
        answer <- getAnswer(length)
        nameAndVersion <- effectOf(answer.map(names(_)))
      } yield nameAndVersion
    }

    def lnSJdk(name: String, javaMajorVersion: JavaMajorVersion): F[String] = for {
      javaBaseDir <- pureOf(javaBaseDirFile.some)
      before <- effectOf(s"""${Process(s"ls -l", javaBaseDir) !!}""".stripMargin)
      lsResultLogger <- effectOf(ProcessLogger(
          line => println(s"\n$line: It is found so will be removed and recreated.")
        , line => println(s"\n$line: So it is not found so it will be created.")
        ))
      jdkLinkAlreadyExists <- effectOf((s"ls -d $javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}" !(lsResultLogger)) === 0)
      result <- if (jdkLinkAlreadyExists) {
          for {
            isNonSymLink <- effectOf((s"find $javaBaseDirPath -type l -iname jdk${JavaMajorVersion.render(javaMajorVersion)}" !!).isEmpty)
            r <- if (isNonSymLink) {
                putStrLn(
                  s"\n'$javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}' already exists and it's not a symbolic link so nothing will be done."
                ) *> pureOf(1)
              } else {
                putStrLn(
                  s"""
                     |$javaBaseDirFile $$ sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}
                     |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin
                ) *> effectOf(
                  Process(s"sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) #&& Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) !
                )
              }
          } yield r
        } else {
          putStrLn(
            s"""
               |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin) *>
            effectOf(Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) !)
        }

      r <- result match {
          case 0 =>
            effectOf(Process(s"ls -l", javaBaseDir) !!)
              .flatMap(after => toResultString(before, after))

          case _ =>
            pureOf("\nFailed: Creating a symbolic link to JDK has failed.\n")
        }
    } yield r

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