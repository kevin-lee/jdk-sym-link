package jdksymlink.cs

import cats.Monad
import cats.data.EitherT
import cats.syntax.all.*
import just.sysprocess.SysProcess

import java.io.File
import effectie.core.Fx
import effectie.syntax.all.*
import extras.cats.syntax.all.*
import extras.scala.io.syntax.color.*
import just.sysprocess.ProcessError
import just.semver.{ParseError, SemVer}
import just.decver.DecVer

import scala.util.matching.Regex

import jdksymlink.core.data.DotSeparatedVersion

/** @author Kevin Lee
  * @since 2022-06-06
  */
object CoursierCmd {

  val CsJavaInstalledCmd: String = "cs java --installed"

  def javaInstalled[F[*]: Fx: Monad]: F[Either[CoursierError, List[JdkByCs]]] =
    (for {
      sysProcess <- pureOf(SysProcess.singleSysProcess(none, "cs", "java", "--installed")).rightT[CoursierError]
      result     <- effectOf(sysProcess.run())
                      .t
                      .leftMap(CoursierError.JavaInstalledCmd(_))
      lines = result.outputs.map(_.split("[\\s]+installed at[\\s]+").map(_.trim).toList)
      r <- extractJdkByCs(lines)
    } yield r).value

  private def extractJdkByCs[F[*]: Fx: Monad](lines: List[List[String]]): EitherT[F, CoursierError, List[JdkByCs]] =
    lines.flatTraverse {
      case nameVersion :: path :: Nil =>
        val id      = JdkByCs.Id(nameVersion)
        val jdkPath = JdkByCs.Path(new File(path))
        nameVersion match {
          case JdkByCs.Name.NamePattern(name, version) =>
            for {
              ver <- effectOf(SemVer.parse(version))
                       .t
                       .map(JdkByCs.Version(_))
                       .leftFlatMap { err1 =>
                         effectOf(DecVer.parse(version))
                           .t
                           .map(JdkByCs.Version(_))
                           .leftFlatMap { err2 =>
                             effectOf(DotSeparatedVersion.parse(version))
                               .t
                               .map(JdkByCs.Version(_))
                               .leftMap { err3 =>
                                 CoursierError.VersionParse(version, (err1, err2, err3), nameVersion, path)
                               }
                           }
                       }
              pathString = jdkPath.value.toString.stripSuffix("/")
              theJdkPath = if pathString.endsWith("/Contents/Home")
                           then JdkByCs.Path(new File(pathString.stripSuffix("/Contents/Home")))
                           else jdkPath
            } yield List(JdkByCs(id, JdkByCs.Name(name), ver.major, ver, theJdkPath))

        }

      case somethingElse => pureOf(CoursierError.InvalidJdkInfo(somethingElse).asLeft).t
    }

  final case class JdkByCs(
    id: JdkByCs.Id,
    name: JdkByCs.Name,
    majorVersion: JdkByCs.MajorVersion,
    version: JdkByCs.Version,
    path: JdkByCs.Path
  )
  object JdkByCs {

    extension (jdkByCs: JdkByCs) {
      def render: String =
        s"""${jdkByCs.name.value.blue}:${jdkByCs.majorVersion.value.toString.green} (${jdkByCs.version.render})
           |  at ${jdkByCs.path.render}
           |---""".stripMargin
    }

    type Id = Id.Id
    object Id {
      opaque type Id = String
      def apply(id: String): Id = id

      given idCanEqual: CanEqual[Id, Id] = CanEqual.derived

      extension (id: Id) {
        def value: String = id
      }
    }

    type Name = Name.Name
    object Name {
      opaque type Name = String
      def apply(name: String): Name = name

      given nameCanEqual: CanEqual[Name, Name] = CanEqual.derived

      extension (name: Name) {
        def value: String = name
      }

      val NamePattern: Regex = "^([^:]+):(.+)$".r
    }

    type MajorVersion = MajorVersion.MajorVersion
    object MajorVersion {
      opaque type MajorVersion = Int
      def apply(majorVersion: Int): MajorVersion = majorVersion

      given majorVersionCanEqual: CanEqual[MajorVersion, MajorVersion] = CanEqual.derived

      extension (majorVersion: MajorVersion) {
        def value: Int = majorVersion
      }
    }

    type Version = Version.Version
    object Version {
      opaque type Version = SemVer | DecVer | DotSeparatedVersion
      def apply(version: SemVer | DecVer | DotSeparatedVersion): Version = version

      given versionCanEqual: CanEqual[Version, Version] = CanEqual.derived

      extension (version: Version) {
        def value: SemVer | DecVer | DotSeparatedVersion = version

        def major: MajorVersion = value match {
          case SemVer(m, n, _, _, _) =>
            MajorVersion(if (m.value == 1) n.value else m.value)
          case DecVer(m, n) =>
            MajorVersion(if (m.value == 1) n.value else m.value)
          case DotSeparatedVersion(v, vs) =>
            val vNum = v.toInt
            MajorVersion(
              if vNum == 1 then
                vs.take(1)
                  .headOption
                  .filter(_.forall(_.isDigit))
                  .fold(vNum)(_.toInt)
              else vNum
            )
        }

        def render: String = value match {
          case v: SemVer => SemVer.render(v)
          case v: DecVer => DecVer.render(v)
          case DotSeparatedVersion(v, vs) => s"$v.${vs.mkString(".")}"
        }
      }
    }

    type Path = Path.Path
    object Path {
      opaque type Path = File
      def apply(path: File): Path = path

      given pathCanEqual: CanEqual[Path, Path] = CanEqual.derived

      extension (path: Path) {
        def value: File = path

        def render: String = {
          val pathValue = value.toString
          sys
            .env
            .get("HOME")
            .filter(home => pathValue.startsWith(home))
            .fold(pathValue) { home =>
              s"$$HOME${pathValue.drop(home.length)}"
            }
        }
      }
    }

  }

  enum CoursierError derives CanEqual {
    case JavaInstalledCmd(error: ProcessError)
    case InvalidJdkInfo(jdkInfo: List[String])
    case VersionParse(
      version: String,
      error: (ParseError, DecVer.ParseError, DotSeparatedVersion.ParseError),
      nameVersion: String,
      path: String
    )
  }

}
