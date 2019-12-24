package jdksymlink

import java.io.File

import cats.implicits._

import scala.util.matching.Regex

/**
 * @author Kevin Lee
 * @since 2015-04-03
 */
object data {
  val javaBaseDirPath: String = "/Library/Java/JavaVirtualMachines"
  lazy val javaBaseDir: File = new File(s"$javaBaseDirPath")

  lazy val Bold: String = "\u001b[1m"
  lazy val Normal: String = "\u001b[0m"

  val Before9Pattern: Regex = """[^-]+1\.(\d)\.(\d)_(\d+)\.jdk$""".r
  val Before9AdoptOpenJdkPattern: Regex = """adoptopenjdk-(\d+)\.jdk$""".r
  val From9Pattern: Regex = """[^-]+-(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-[^\.]+)?\.jdk$""".r
  val From9PatternWithOnlyVersion: Regex = """^jdk-(\d+)\.jdk$""".r

  type NameAndVersion = (String, VerStr)

  final case class VerStr(major: String, minor: Option[String], patch: Option[String])

  object VerStr {

    implicit object VerStrOrdering extends Ordering[VerStr] {
      def compare(x: VerStr, y: VerStr): Int = (x, y) match {
        case (VerStr(v1, m1, mn1), VerStr(v2, m2, mn2)) =>
          val v = v1.toInt.compare(v2.toInt)
          if (v =!= 0) {
            v
          } else {

            ((m1, mn1), (m2, mn2)) match {
              case ((None, None), (None, None)) =>
                0
              case ((None, None), (Some(_), Some(_))) =>
                -1
              case ((Some(_), Some(_)), (None, None)) =>
                1
              case ((Some(major1), None), (Some(major2), None)) =>
                major1.toInt.compare(major2.toInt)
              case ((Some(major1), Some(minor1)), (Some(major2), Some(minor2))) =>
                val majorInt1 = major1.toInt
                val majorInt2 = major2.toInt
                val minorInt1 = minor1.toInt
                val minorInt2 = minor2.toInt
                val major = majorInt1.compare(majorInt2)
                if (major =!= 0)
                  major
                else
                  minorInt1.compare(minorInt2)
            }

          }
      }
    }

  }
}

