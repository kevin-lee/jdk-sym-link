package jdksymlink

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
object JdkSymbolicLink extends App {
  import sys.process._
  import scala.io.StdIn
  import scala.language.postfixOps

  import data._

  def bold(text: String): String = s"$Bold$text$normal"

  def isPositiveNumber(text: String) = text.matches("""[\d]+""")

  def help(): Unit = printHelp(Nil)

  def printHelp(whatever: List[String]): Unit =
    println(s"""
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
             """.stripMargin)

  if (args.isEmpty) {
    help()
    sys.exit(1)
  }

  val Command1s = Map("l" -> listAll _, "s" -> slink _, "h" -> printHelp _)

  val Command2s = Map("list" -> listAll _, "slink" -> slink _, "help" -> printHelp _)

  val argPatter = """([-]+)([\w]+)""".r

  (args.toList match {
    case argPatter("--", arg) :: rest => Command2s.get(arg).map(_(rest))
    case argPatter("-", arg) :: rest => Command1s.get(arg).map(_(rest))
    case _ => None
  }) match {
    case Some(_) =>
      println("Done\n")
    case None =>
      println(s"""
                 |Unknown args: ${args.mkString(" ")}
                 |
                 |  # To see available args please run
                 |  ln-s-jdk --help
                 |
                 |  #or just
                 |  ln-s-jdk
             """.stripMargin)
      sys.exit(1)
  }

  def listAll(args: List[String]): Unit = {
    println(s"""
               |$$ ls -l $javaBaseDirPath
               |
               |${Process(s"ls -l", Option(javaBaseDir)) !!}
           """.stripMargin)
  }

  def slink(args: List[String]): Unit = {

    val version = args match {
      case x :: xs => x.trim
      case _ => ""
    }

    if (version.isEmpty || !isPositiveNumber(version)) {
      println(s"The argument must be a positive integer. Entered: [${args.mkString(", ")}]")
      sys.exit(1)
    }

    val Before9Pattern = """[^-]+1\.(\d)\.(\d)_(\d+)\.jdk$""".r
    val Before9AdoptOpenJdkPattern = """adoptopenjdk-(\d+)\.jdk$""".r
    val From9Pattern = """[^-]+-(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-[^\.]+)?\.jdk$""".r
    val From9PatternWithOnlyVersion = """^jdk-(\d+)\.jdk$""".r

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

    val names = (Process(Seq("bash", "-c", "ls -d */"), Option(javaBaseDir)).lazyLines)
      .map(line => if (line.endsWith("/")) line.dropRight(1) else line)
      .map(extractVersion)
      .foldLeft(Vector[NameAndVersion]()) {
        case (acc, Some(x@(_, VerStr(v, _, _)))) if v == version =>
          acc :+ x
        case (acc, _) =>
          acc
      }
      .sortBy(_._2)

    def askUserToSelectJdk(names: Vector[NameAndVersion]): Option[NameAndVersion] = {
      val listOfJdk = names.zipWithIndex.map { case ((name, split), index) => s"[$index] $name" }

      println(s"""
                 |Version(s) found:
                 |${listOfJdk.mkString("\n")}
                 |[c] Cancel
             """.stripMargin)

      val length = listOfJdk.length

      @scala.annotation.tailrec
      def getAnswer(choice: String): Option[String] = choice match {
        case "c" | "C" => None
        case whatever if isPositiveNumber(choice) && choice.toInt < length => Option(choice)
        case _ =>
          print("Please enter a number on the list: ")
          getAnswer(StdIn.readLine())
      }
      getAnswer("x").map(_.toInt).map(names(_))
    }

    println(askUserToSelectJdk(names).flatMap {
      case (name, ver) =>

        println(s"""
                   |You chose '$name'.
                   |It will create a symbolic link to '$name' (i.e. jdk${ver.major} -> $name) and may ask you to enter your password.""".stripMargin)
        print("Would you like to proceed? (Yes / No) or (Y / N) ")

        StdIn.readLine() match {
          case "y" | "yes" | "Y" | "Yes"  =>

            val before = s"""${Process(s"ls -l", Option(javaBaseDir)) !!}""".stripMargin

            val lsResultLogger = ProcessLogger(line => println(s"\n$line: It is found so will be removed and recreated."),
              line => println(s"\n$line: So it is not found so it will be created."))
            val result = s"ls -d $javaBaseDirPath/jdk$version" !(lsResultLogger) match {
              case 0 =>
                if ((s"find $javaBaseDirPath -type l -iname jdk$version" !!).isEmpty) {
                  println(s"\n'$javaBaseDirPath/jdk$version' already exists and it's not a symbolic link so nothing will be done.")
                  1
                } else {
                  println(s"""
                             |$javaBaseDir $$ sudo rm jdk$version
                             |$javaBaseDir $$ sudo ln -s $name jdk$version """.stripMargin)
                  Process(s"sudo rm jdk$version", Option(javaBaseDir)) #&& Process(s"sudo ln -s $name jdk$version", Option(javaBaseDir)) !
                }
              case _ =>
                println(s"""
                           |$javaBaseDir $$ sudo ln -s $name jdk$version """.stripMargin)
                Process(s"sudo ln -s $name jdk$version", Option(javaBaseDir)) !
            }

            Option(result match {
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
          case _ =>
            None
        }
    }
    .getOrElse("\nCancelled.\n"))

  }
}
