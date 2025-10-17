package jdksymlink.core

import cats.syntax.all.*
import data.*

import java.io.File
import scala.sys.process.Process

/** @author Kevin Lee
  * @since 2020-01-01
  */
object Utils {

  def isPositiveNumber(text: String): Boolean    = text.matches("""[1-9][\d]*""")
  def isNonNegativeNumber(text: String): Boolean = text.matches("""[\d]+""")

  def extractVersion(name: String): Option[NameAndVersion] = name match {
    case DefaultJdk.Before9Pattern(major, minor, patch) =>
      (name, VerStr(major, Option(minor), Option(patch))).some
    case DefaultJdk.From9Pattern(major, minor, patch) =>
      (name, VerStr(major, Option(minor), Option(patch))).some
    case DefaultJdk.From9PatternWithOnlyVersion(major) =>
      (name, VerStr(major, None, None)).some
    case DefaultJdk.Before9AdoptOpenJdkPattern(major) =>
      (name, VerStr(major, None, None)).some
    case _ =>
      none[NameAndVersion]
  }

  def extractCoursierJavaVersion(name: String): Option[NameAndVersion] =
    name match {
      case Coursier.TemurinPattern(_, major, minor, patch) =>
        (name, VerStr(major, Option(minor), Option(patch))).some
      case Coursier.AdoptOpenJdkPattern(major, minor, patch) =>
        (name, VerStr(major, Option(minor), Option(patch))).some
      case Coursier.ZuluOpenJdkPattern(major, minor, patch) =>
        (name, VerStr(major, Option(minor), Option(patch))).some
      case Coursier.AmazonCorrettoOpenJdkPattern(major, minor, patch) =>
        (name, VerStr(major, Option(minor), none)).some
      case _ =>
        none[NameAndVersion]
    }

  def extractSdkManJavaVersion(name: String): Option[NameAndVersion] =
    name match {
      case SdkMan.SdkManJdkPattern(major, minor, patch, additional, name) =>
        (
          (
            List(major) ++
              Option(minor).toList ++
              Option(patch).toList ++
              Option(additional).toList
          ).mkString(".") + "-" + name,
          VerStr(major, Option(minor), Option(patch))
        ).some
      case _ =>
        none[NameAndVersion]
    }

  def names(
    javaMajorVersion: JavaMajorVersion,
    javaBaseDirFile: File,
    versionExtractor: String => Option[NameAndVersion]
  ): List[NameAndVersion] =
    (Process(Seq("bash", "-c", "ls -d */"), Option(javaBaseDirFile))
      .lazyLines)
      .map(line => if (line.endsWith("/")) line.dropRight(1) else line)
      .map(versionExtractor)
      .foldLeft(List.empty[NameAndVersion]) {
        case (acc, Some(x @ (_, VerStr(v, _, _)))) if v === javaMajorVersion.value.toString =>
          acc :+ x
        case (acc, _) =>
          acc
      }
      .sortBy((_, version) => version)

}
