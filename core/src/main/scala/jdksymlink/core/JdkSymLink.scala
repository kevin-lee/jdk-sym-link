package jdksymlink.core

import java.io.File

import cats._
import cats.implicits._

import jdksymlink.core.data._
import jdksymlink.effect._

import scala.language.postfixOps
import sys.process._

import Utils._

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

  implicit def jdkSymLinkF[F[_] : EffectConstructor : ConsoleEffect : Monad]: JdkSymLink[F] =
    new JdkSymLinkF[F]

  final class JdkSymLinkF[F[_] : Monad](
    override implicit protected val EF: EffectConstructor[F]
  , override implicit protected val CF: ConsoleEffect[F]
  ) extends JdkSymLink[F]
    with Effectful[F]
    with ConsoleEffectful[F] {

    def listAll(javaBaseDirPath: String, javaBaseDir: File): F[Unit] =
      for {
        _ <- putStrLn(
            s"""
               |$$ ls -l $javaBaseDirPath
               |""".stripMargin
          )
        list <- effect(Process(s"ls -l", Option(javaBaseDir)) !!)
        _ <- putStrLn(s"$list\n")
      } yield ()

    def slink(javaMajorVersion: JavaMajorVersion): F[Unit] =
      for {
        jdkNameVersionPairs <- effect(names(javaMajorVersion))
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
                    effect("\nCancelled.\n")
                }
            } yield s

          case None =>
            effect("\nCancelled.\n")
        }
        _ <- putStrLn(result)
      } yield ()

    def askUserToSelectJdk(names: Vector[NameAndVersion]): F[Option[NameAndVersion]] = {
      def getAnswer(length: Int): F[Option[Int]] = for {
        choice <- readLn
        answer <- choice match {
            case "c" | "C" =>
              effect(none[Int])
            case _  =>
              if (isNonNegativeNumber(choice) && choice.toInt < length)
                effect(choice.toInt.some)
              else
                putStrLn(
                  """Please enter a number on the list:
                    |(or [c] for cancellation)""".stripMargin) *> getAnswer(length)
          }
      } yield answer

      for {
        listOfJdk <- effect(names.zipWithIndex.map { case ((name, split), index) => s"[$index] $name" })
        length <- effect(listOfJdk.length)
        _ <- putStrLn(
            s"""
               |Version(s) found:
               |${listOfJdk.mkString("\n")}
               |[c] Cancel
               |""".stripMargin
          )
        answer <- getAnswer(length)
        nameAndVersion <- effect(answer.map(names(_)))
      } yield nameAndVersion
    }

    def lnSJdk(name: String, javaMajorVersion: JavaMajorVersion): F[String] = for {
      javaBaseDir <- pureEffect(javaBaseDirFile.some)
      before <- effect(s"""${Process(s"ls -l", javaBaseDir) !!}""".stripMargin)
      lsResultLogger <- effect(ProcessLogger(
          line => println(s"\n$line: It is found so will be removed and recreated.")
        , line => println(s"\n$line: So it is not found so it will be created.")
        ))
      jdkLinkAlreadyExists <- effect((s"ls -d $javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}" !(lsResultLogger)) === 0)
      result <- if (jdkLinkAlreadyExists) {
          for {
            isNonSymLink <- effect((s"find $javaBaseDirPath -type l -iname jdk${JavaMajorVersion.render(javaMajorVersion)}" !!).isEmpty)
            r <- if (isNonSymLink) {
                putStrLn(
                  s"\n'$javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}' already exists and it's not a symbolic link so nothing will be done."
                ) *> pureEffect(1)
              } else {
                putStrLn(
                  s"""
                     |$javaBaseDirFile $$ sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}
                     |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin
                ) *> effect(
                  Process(s"sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) #&& Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) !
                )
              }
          } yield r
        } else {
          putStrLn(
            s"""
               |$javaBaseDirFile $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin) *>
            effect(Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", javaBaseDir) !)
        }

      r <- result match {
          case 0 =>
            effect(Process(s"ls -l", javaBaseDir) !!)
              .flatMap(after => toResultString(before, after))

          case _ =>
            pureEffect("\nFailed: Creating a symbolic link to JDK has failed.\n")
        }
    } yield r

    def toResultString(before: String, after: String): F[String] =
      effect(
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