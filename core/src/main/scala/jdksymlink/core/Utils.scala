package jdksymlink.core

import cats.syntax.all._

import data._

import scala.sys.process.Process

/**
 * @author Kevin Lee
 * @since 2020-01-01
 */
object Utils {

  def isPositiveNumber(text: String): Boolean = text.matches("""[1-9][\d]*""")
  def isNonNegativeNumber(text: String): Boolean = text.matches("""[\d]+""")

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

  def names(javaMajorVersion: JavaMajorVersion): Vector[NameAndVersion] =
    (Process(Seq("bash", "-c", "ls -d */"), Option(javaBaseDirFile)).lazyLines)
      .map(line => if (line.endsWith("/")) line.dropRight(1) else line)
      .map(extractVersion)
      .foldLeft(Vector.empty[NameAndVersion]) {
        case (acc, Some(x@(_, VerStr(v, _, _)))) if v === javaMajorVersion.value.toString =>
          acc :+ x
        case (acc, _) =>
          acc
      }
      .sortBy((name, version) => version)

}
