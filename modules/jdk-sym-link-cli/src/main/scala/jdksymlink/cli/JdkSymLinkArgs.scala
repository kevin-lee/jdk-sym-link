package jdksymlink.cli

import com.monovore.decline.*
import jdksymlink.core.data.JavaMajorVersion

/** @author Kevin Lee
  * @since 2019-12-24
  */
enum JdkSymLinkArgs derives CanEqual {

  case JdkListArgs
  case SymLinkArgs(javaMajorVersion: JavaMajorVersion)

}

object JdkSymLinkArgs {

  private val listOpts: Opts[JdkSymLinkArgs] =
    Opts.subcommand("list", "List all JDKs") {
      Opts(JdkSymLinkArgs.JdkListArgs)
    }

  private val symLinkOpts: Opts[JdkSymLinkArgs] =
    Opts.subcommand(
      "slink",
      """Create JDK symbolic link.
        |<java-version>: Major version like 11, 17, 21, etc.
        |  e.g)
        |  jdk-sym-link slink 21""".stripMargin
    ) {
      Opts
        .argument[Int](metavar = "java-version")
        .map(JavaMajorVersion(_))
        .map(JdkSymLinkArgs.SymLinkArgs(_))
    }

  val opts: Opts[JdkSymLinkArgs] = listOpts orElse symLinkOpts

}
