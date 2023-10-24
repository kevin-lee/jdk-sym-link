package jdksymlink.core

import java.io.File
import cats.syntax.all.*

import scala.util.matching.Regex
import jdksymlink.core.data.Path

import scala.util.Try

/** @author Kevin Lee
  * @since 2015-04-03
  */
object data {

  type Path = Path.Path
  object Path {
    opaque type Path = String
    def apply(path: String): Path = path

    given pathCanEqual: CanEqual[Path, Path] = CanEqual.derived

    extension (path: Path) {
      def value: String = path

      def dirExist: Boolean = {
        import sys.process.*
        List("[", "-d", path.value, "]").! == 0
      }

      def fileExist: Boolean = {
        import sys.process.*
        List("[", "-f", path.value, "]").! == 0
      }

      def nonEmptyInside: Boolean = {
        import sys.process.*
        s"ls -A ${path.value}".lazyLines.nonEmpty
      }
    }
  }

  type JvmBaseDirPath = JvmBaseDirPath.JvmBaseDirPath
  object JvmBaseDirPath {
    opaque type JvmBaseDirPath = String
    def apply(jvmBaseDirPath: String): JvmBaseDirPath = jvmBaseDirPath

    given jvmBaseDirPathCanEqual: CanEqual[JvmBaseDirPath, JvmBaseDirPath] = CanEqual.derived

    extension (jvmBaseDirPath: JvmBaseDirPath) {
      def value: String = jvmBaseDirPath
      def toPath: Path  = Path(value)
    }
  }

  object DefaultJdk {
    val JavaBaseDirPath: JvmBaseDirPath = JvmBaseDirPath("/Library/Java/JavaVirtualMachines")
    lazy val javaBaseDirFile: File      = new File(JavaBaseDirPath.value)

    val Before9Pattern: Regex              = """[^-]+1\.(\d)\.(\d)_(\d+)\.jdk$""".r
    val Before9AdoptOpenJdkPattern: Regex  = """adoptopenjdk-(\d+)\.jdk$""".r
    val From9Pattern: Regex                = """[^-]+-(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-[^\.]+)?\.jdk$""".r
    val From9PatternWithOnlyVersion: Regex = """^jdk-(\d+)\.jdk$""".r
  }

  object SdkMan {
    /* Do not change it to val. If it's val, the native build gets the HOME of the build environment. */
    def homeDir: String = sys.env("HOME")

    /* Do not change it to val. If it's val, the native build uses the HOME of the build environment. */
    lazy val JavaBaseDirPath: JvmBaseDirPath = JvmBaseDirPath(s"$homeDir/.sdkman/candidates/java")
    lazy val javaBaseDirFile: File           = new File(JavaBaseDirPath.value)

    val SdkManJdkPattern = """([\w]+)(?:\.([\w]+))?(?:\.([\w]+))?(?:\.([\w]+))?-([\w]+)""".r
  }

  object Coursier {
    def homeDir: String = sys.env("HOME")

    lazy val CoursierJavaBaseDirPath       = JvmBaseDirPath(s"$homeDir/Library/Caches/Coursier/jvm")
    lazy val coursierJavaBaseDirFile: File = new File(CoursierJavaBaseDirPath.value)
//    temurin-debugimage:1.8.0-302
    val TemurinPattern                     = """temurin(-[a-zA-Z]+)?@(?:\d+)\.(\d+)(?:\.(\d+))?(?:[-\.]([\d]+))?""".r
    val AdoptOpenJdkPattern                = """adopt@(?:\d+)\.(\d+)\.(\d+)(?:-)([\d]+)?""".r
    val ZuluOpenJdkPattern                 = """zulu@(?:\d+)\.(\d+)\.(\d+)(?:-)([\d]+)?""".r
    val AmazonCorrettoOpenJdkPattern       = """amazon-corretto@(?:\d+)\.(\d+)\.(\d+)(?:-)((?:[\d]+)(?:\.[\d]+)*)?""".r
  }

  opaque type JavaMajorVersion = Int
  object JavaMajorVersion {
    def apply(javaMajorVersion: Int): JavaMajorVersion = javaMajorVersion

    extension (javaMajorVersion: JavaMajorVersion) {
      def value: Int     = javaMajorVersion
      def render: String =
        javaMajorVersion.value.toString
    }
  }

  type NameAndVersion = (String, VerStr)

  final case class VerStr(
    major: String,
    minor: Option[String],
    patch: Option[String]
  ) derives CanEqual

  object VerStr {

    extension (verStr: VerStr) {
      def render: String = s"${verStr.major}${verStr.minor.fold("")("." + _)}${verStr.patch.fold("")("." + _)}"
    }

    given verStrOrdering: Ordering[VerStr] with {
      def compare(x: VerStr, y: VerStr): Int = (x, y) match {
        case (VerStr(v1, m1, mn1), VerStr(v2, m2, mn2)) =>
          val v = v1.toInt.compare(v2.toInt)
          if (v != 0) {
            v
          } else {

            ((m1, mn1), (m2, mn2)) match {
              case ((None, None), (None, None)) =>
                0
              case ((None, None), (Some(_), Some(_))) =>
                -1
              case ((None, None), (None, Some(_))) =>
                -1
              case ((None, None), (Some(_), None)) =>
                -1
              case ((Some(_), Some(_)), (None, None)) =>
                1
              case ((None, Some(_)), (None, None)) =>
                1
              case ((Some(_), None), (None, None)) =>
                1
              case ((Some(_), None), (None, Some(_))) =>
                1
              case ((None, Some(_)), (Some(_), None)) =>
                -1
              case ((None, Some(_)), (Some(_), Some(_))) =>
                -1

              case ((Some(major1), None), (Some(major2), Some(_))) =>
                val m = major1.toInt.compareTo(major2.toInt)
                if (m == 0)
                  -1
                else
                  m

              case ((Some(_), Some(_)), (None, Some(_))) =>
                1

              case ((Some(major1), Some(_)), (Some(major2), None)) =>
                val m = major1.toInt.compareTo(major2.toInt)
                if (m == 0)
                  1
                else
                  m

              case ((None, Some(minor1)), (None, Some(minor2))) =>
                minor1.toInt.compareTo(minor2.toInt)

              case ((Some(major1), None), (Some(major2), None)) =>
                major1.toInt.compare(major2.toInt)

              case ((Some(major1), Some(minor1)), (Some(major2), Some(minor2))) =>
                val majorInt1 = major1.toInt
                val majorInt2 = major2.toInt
                val minorInt1 = minor1.toInt
                val minorInt2 = minor2.toInt
                val major     = majorInt1.compare(majorInt2)
                if (major != 0)
                  major
                else
                  minorInt1.compare(minorInt2)
            }

          }
      }
    }

  }

  final case class DotSeparatedVersion(value: String, rest: List[String]) derives CanEqual
  object DotSeparatedVersion {

    def parse(version: String): Either[DotSeparatedVersion.ParseError, DotSeparatedVersion] =
      Try(version.split("\\.").toList)
        .toEither
        .left
        .map { err =>
          DotSeparatedVersion.ParseError(version, err.getMessage)
        }
        .flatMap {
          case v1 :: vs =>
            Right(DotSeparatedVersion(v1, vs))
          case Nil =>
            Left(DotSeparatedVersion.ParseError(version, "No version found"))
        }

    final case class ParseError(value: String, error: String)
    object ParseError {
      extension (parseError: ParseError) {
        def render: String = s"DotSeparatedVersion.ParseError(value=${parseError.value}, error=${parseError.error})"
      }
    }
  }

}
