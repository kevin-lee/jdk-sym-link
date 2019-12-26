package jdksymlink.core

import java.io.File

import cats.implicits._
import cats.effect._

import jdksymlink.core.data._
import jdksymlink.effect._

import scala.language.postfixOps
import scala.util.matching.Regex
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
object JdkSymLink extends App {

  def bold(text: String): String = s"$Bold$text$Normal"

  def isPositiveNumber(text: String): Boolean = text.matches("""[1-9][\d]*""")
  def isNonNegativeNumber(text: String): Boolean = text.matches("""[\d]+""")

  val argPatter: Regex = """([-]+)([\w]+)""".r

  def listAll(javaBaseDirPath: String, javaBaseDir: File): IO[Unit] =
    for {
      _ <- putStrLn(
          s"""
             |$$ ls -l $javaBaseDirPath
             |""".stripMargin
        )
      list <- IO(Process(s"ls -l", Option(javaBaseDir)) !!)
      _ <- putStrLn(s"$list\n")
    } yield ()

  def slink(javaMajorVersion: JavaMajorVersion): IO[Unit] = {

    def extractVersion(name: String): Option[NameAndVersion] = name match {
      case Before9Pattern(major, minor, patch) =>
        Some((name, VerStr(major, Option(minor), Option(patch))))
      case From9Pattern(major, minor, patch) =>
        Some((name, VerStr(major, Option(minor), Option(patch))))
      case From9PatternWithOnlyVersion(major) =>
        Some((name, VerStr(major, None, None)))
      case Before9AdoptOpenJdkPattern(major) =>
        Some((name, VerStr(major, None, None)))
      case _ =>
        None
    }

    def names(javaMajorVersion: JavaMajorVersion): Vector[(String, VerStr)] =
      (Process(Seq("bash", "-c", "ls -d */"), Option(javaBaseDir)).lazyLines)
        .map(line => if (line.endsWith("/")) line.dropRight(1) else line)
        .map(extractVersion)
        .foldLeft(Vector[NameAndVersion]()) {
          case (acc, Some(x@(_, VerStr(v, _, _)))) if v === javaMajorVersion.javaMajorVersion.toString =>
            acc :+ x
          case (acc, _) =>
            acc
        }
        .sortBy(_._2)

    def askUserToSelectJdk(names: Vector[NameAndVersion]): IO[Option[NameAndVersion]] = {
      def getAnswer(length: Int): IO[Option[Int]] = for {
        choice <- readLn
        answer <- choice match {
            case "c" | "C" =>
              IO(none)
            case _  =>
              if (isNonNegativeNumber(choice) && choice.toInt < length)
                IO(choice.toInt.some)
              else
                putStrLn(
                  """Please enter a number on the list:
                    |(or [c] for cancellation)""".stripMargin) *> getAnswer(length)
          }
      } yield answer

      for {
        listOfJdk <- IO(names.zipWithIndex.map { case ((name, split), index) => s"[$index] $name" })
        length <- IO(listOfJdk.length)
        _ <- putStrLn(
            s"""
               |Version(s) found:
               |${listOfJdk.mkString("\n")}
               |[c] Cancel
               |""".stripMargin
          )
        answer <- getAnswer(length)
        nameAndVersion <- IO(answer.map(names(_)))
      } yield nameAndVersion
    }

    for {
      maybeNameVersion <- askUserToSelectJdk(names(javaMajorVersion))
      result <- maybeNameVersion match {
          case Some((name, ver)) =>
            for {
              _ <- putStrLn(
                  s"""
                     |You chose '$name'.
                     |It will create a symbolic link to '$name' (i.e. jdk${ver.major} -> $name) and may ask you to enter your password.
                     |""".stripMargin
                )
              _ <- putStrLn("Would you like to proceed? (Yes / No) or (Y / N) ")
              answer <- readLn
              s <-   answer match {
                  case "y" | "yes" | "Y" | "Yes"  =>
                    lnSJdk(name, javaMajorVersion)
                  case _ =>
                    IO("\nCancelled.\n")
                }
            } yield s

          case None =>
            IO("\nCancelled.\n")
        }
      _ <- putStrLn(result)
    } yield ()

  }


  def lnSJdk(name: String, javaMajorVersion: JavaMajorVersion): IO[String] = for {
    before <- IO(s"""${Process(s"ls -l", Option(javaBaseDir)) !!}""".stripMargin)
    lsResultLogger <- IO(ProcessLogger(
        line => println(s"\n$line: It is found so will be removed and recreated.")
      , line => println(s"\n$line: So it is not found so it will be created.")
      ))
    jdkLinkAlreadyExists <- IO((s"ls -d $javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}" !(lsResultLogger)) === 0)
    result <- if (jdkLinkAlreadyExists) {
        for {
          isNonSymLink <- IO((s"find $javaBaseDirPath -type l -iname jdk${JavaMajorVersion.render(javaMajorVersion)}" !!).isEmpty)
          r <- if (isNonSymLink) {
            putStrLn(
              s"\n'$javaBaseDirPath/jdk${JavaMajorVersion.render(javaMajorVersion)}' already exists and it's not a symbolic link so nothing will be done."
            ) *> IO(1)
          } else {
            putStrLn(
              s"""
                 |$javaBaseDir $$ sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}
                 |$javaBaseDir $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin
            ) *> IO(
              Process(s"sudo rm jdk${JavaMajorVersion.render(javaMajorVersion)}", Option(javaBaseDir)) #&& Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", Option(javaBaseDir)) !
            )
          }
        } yield r
      } else {
        putStrLn(
          s"""
             |$javaBaseDir $$ sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)} """.stripMargin) *>
          IO(Process(s"sudo ln -s $name jdk${JavaMajorVersion.render(javaMajorVersion)}", Option(javaBaseDir)) !)
      }

    r <- IO(result match {
      case 0 =>
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
           |${Process(s"ls -l", Option(javaBaseDir)) !!}
           |======================================
           |""".stripMargin

      case _ =>
        "\nFailed: Creating a symbolic link to JDK has failed.\n"
    })
  } yield r
}
