package jdksymlink

import cats.effect._
import cats.implicits._

import jdksymlink.data._

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
object JdkSymbolicLinkextends extends App {

  val readLn: IO[String] = IO(scala.io.StdIn.readLine)
  def putStrLn(value: String): IO[Unit] = IO(println(value))

  def bold(text: String): String = s"$Bold$text$Normal"

  def isPositiveNumber(text: String): Boolean = text.matches("""[1-9][\d]*""")

  def help(): IO[Unit] = printHelp(Nil)

  def printHelp(whatever: List[String]): IO[Unit] =
    putStrLn(
      s"""
         |Usage:
         |${bold("ln-s-jdk")} [arguments]
         |
         |  ${bold("-l")}, ${bold("--list")}: list all JDK installed
         |    e.g.)
         |    # list JDKs
         |    ln-s-jdk --list
         |    ln-s-jdk -l
         |
         |  ${bold("-s")}, ${bold("--slink")} version_number: Create a new symbolic link to the default jdk (i.e. jdk# => actual folder).
         |
         |    # To set the default JDK for Java 9
         |    ln-s-jdk --slink 9
         |    ln-s-jdk -s 9
         |
         |    # To set the default JDK for Java 8
         |    ln-s-jdk --slink 8
         |    ln-s-jdk -s 8
         |""".stripMargin
    )

  val Command1s: Map[String, List[String] => IO[Unit]] = Map("l" -> listAll, "s" -> slink, "h" -> printHelp)

  val Command2s: Map[String, List[String] => IO[Unit]] = Map("list" -> listAll, "slink" -> slink, "help" -> printHelp)

  val argPatter: Regex = """([-]+)([\w]+)""".r

  (if (args.isEmpty) {
    (help() *> IO(sys.exit(1)))
  } else {
    ((args.toList match {
      case argPatter("--", arg) :: rest => Command2s.get(arg).map(_(rest))
      case argPatter("-", arg) :: rest => Command1s.get(arg).map(_(rest))
      case _ => None
    }) match {
      case Some(io) =>
        for {
          _ <- io
          _ <- putStrLn("Done\n")
        } yield ()
      case None =>
        putStrLn(
          s"""
             |Unknown args: ${args.mkString(" ")}
             |
             |  # To see available args please run
             |  ln-s-jdk --help
             |
             |  #or just
             |  ln-s-jdk
             |""".stripMargin
        ) *> IO(sys.exit(1))
    })
  }).unsafeRunSync()

  def listAll(args: List[String]): IO[Unit] =
    putStrLn(
      s"""
         |$$ ls -l $javaBaseDirPath
         |
         |${Process(s"ls -l", Option(javaBaseDir)) !!}
         |""".stripMargin
    )

  def slink(args: List[String]): IO[Unit] = {

    val version = args match {
      case x :: xs => x.trim
      case _ => ""
    }

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

    lazy val names: Vector[(String, VerStr)] =
      (Process(Seq("bash", "-c", "ls -d */"), Option(javaBaseDir)).lazyLines)
        .map(line => if (line.endsWith("/")) line.dropRight(1) else line)
        .map(extractVersion)
        .foldLeft(Vector[NameAndVersion]()) {
          case (acc, Some(x@(_, VerStr(v, _, _)))) if v == version =>
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
              if (isPositiveNumber(choice) && choice.toInt < length)
                IO(choice.toInt.some)
              else
                putStrLn("Please enter a number on the list: ") *> getAnswer(length)
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
        nameAndVersion = answer.
          map(names(_))
      } yield nameAndVersion
    }


    if (version.isEmpty || !isPositiveNumber(version)) {
      putStrLn(
          s"The argument must be a positive integer. Entered: [${args.mkString(", ")}]"
        ) *> IO(sys.exit(1))
    } else {
      for {
        maybeNameVersion <- askUserToSelectJdk(names)
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
                      lnSJdk(name, version)
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

  }


  def lnSJdk(name: String, version: String): IO[String] = for {

    before <- IO(s"""${Process(s"ls -l", Option(javaBaseDir)) !!}""".stripMargin)

    lsResultLogger <- IO(ProcessLogger(
        line => println(s"\n$line: It is found so will be removed and recreated."),
        line => println(s"\n$line: So it is not found so it will be created.")
      ))
    jdkLinkAlreadyExists <- IO(s"ls -d $javaBaseDirPath/jdk$version" !(lsResultLogger)).map(_ === 0)
    result <- if (jdkLinkAlreadyExists) {
        if ((s"find $javaBaseDirPath -type l -iname jdk$version" !!).isEmpty) {
          putStrLn(
            s"\n'$javaBaseDirPath/jdk$version' already exists and it's not a symbolic link so nothing will be done."
          ) *> IO(1)
        } else {
          putStrLn(
            s"""
               |$javaBaseDir $$ sudo rm jdk$version
               |$javaBaseDir $$ sudo ln -s $name jdk$version """.stripMargin
          ) *> IO(
            Process(s"sudo rm jdk$version", Option(javaBaseDir)) #&& Process(s"sudo ln -s $name jdk$version", Option(javaBaseDir)) !
          )
        }
      } else {
        putStrLn(
          s"""
             |$javaBaseDir $$ sudo ln -s $name jdk$version """.stripMargin) *>
          IO(Process(s"sudo ln -s $name jdk$version", Option(javaBaseDir)) !)
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
