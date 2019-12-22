package jdksymlink

/**
 * @author Kevin Lee
 * @since 2015-04-03
 */
object data {
  type NameAndVersion = (String, VerStr)

  val javaBaseDirPath = "/Library/Java/JavaVirtualMachines"
  lazy val javaBaseDir = new java.io.File(s"$javaBaseDirPath")

  lazy val Bold = "\u001b[1m"
  lazy val normal = "\u001b[0m"

  final case class VerStr(major: String, minor: Option[String], patch: Option[String])

  implicit object VerStrOrdering extends Ordering[VerStr] {
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
              if (major != 0)
                major
              else
                minorInt1.compare(minorInt2)
          }

        }
    }
  }
}

