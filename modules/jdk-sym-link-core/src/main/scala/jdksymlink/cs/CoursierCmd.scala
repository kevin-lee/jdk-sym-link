package jdksymlink.cs

import cats.*
import cats.derived.*
import cats.data.EitherT
import cats.syntax.all.*
import just.sysprocess.SysProcess

import java.io.File
import effectie.core.Fx
import effectie.syntax.all.*
import extras.cats.syntax.all.*
import extras.scala.io.syntax.color.*
import extras.render.Render
import extras.render.syntax.*
import just.sysprocess.ProcessError
import just.semver.{ParseError, SemVer}
import just.decver.DecVer
import refined4s.Newtype

import scala.util.matching.Regex
import jdksymlink.core.data.DotSeparatedVersion
import refined4s.modules.cats.derivation.CatsEqShow

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
  ) derives CanEqual,
        Eq,
        Show
  object JdkByCs {

    extension (jdkByCs: JdkByCs) {
      def render: String =
        s"""${jdkByCs.name.value.blue}:${jdkByCs.majorVersion.value.toString.green} (${jdkByCs.version.render})
           |  at ${jdkByCs.path.render}
           |---""".stripMargin
    }

    type Id = Id.Type
    object Id extends Newtype[String], CatsEqShow[String]

    type Name = Name.Type
    object Name extends Newtype[String], CatsEqShow[String] {
      val NamePattern: Regex = "^([^:]+):(.+)$".r
    }

    type MajorVersion = MajorVersion.Type
    object MajorVersion extends Newtype[Int], CatsEqShow[Int]

    type Version = Version.Type
    object Version extends Newtype[SemVer | DecVer | DotSeparatedVersion] {

      extension (version: Version) {
        def major: MajorVersion = version.value match {
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

      }

      given eqVersion: Eq[Version] = Eq.fromUniversalEquals

      given versionShow: Show[Version] = Show.fromToString

      given renderVersion: Render[Version] with {

        def render(a: Version): String = a.value match {
          case v: SemVer => SemVer.render(v)
          case v: DecVer => DecVer.render(v)
          case DotSeparatedVersion(v, vs) => s"$v.${vs.mkString(".")}"
        }
      }
    }

    type Path = Path.Type
    object Path extends Newtype[File] {

      given pathEq: Eq[Path] = Eq.fromUniversalEquals

      given pathShow: Show[Path] = Show.fromToString

      given renderPath: Render[Path] = { path =>
        val pathValue = path.value.toString
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
