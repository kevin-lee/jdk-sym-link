package jdksymlink.cli

import jdksymlink.core.data.{Eql, JavaMajorVersion}

/**
 * @author Kevin Lee
 * @since 2019-12-24
 */
enum JdkSymLinkArgs derives Eql {

  case JdkListArgs
  case SymLinkArgs(javaMajorVersion: JavaMajorVersion)

}
