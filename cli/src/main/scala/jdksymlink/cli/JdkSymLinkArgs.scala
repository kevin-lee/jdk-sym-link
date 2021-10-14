package jdksymlink.cli

import jdksymlink.core.data.JavaMajorVersion

/** @author Kevin Lee
  * @since 2019-12-24
  */
enum JdkSymLinkArgs derives CanEqual {

  case JdkListArgs
  case SymLinkArgs(javaMajorVersion: JavaMajorVersion)

}
