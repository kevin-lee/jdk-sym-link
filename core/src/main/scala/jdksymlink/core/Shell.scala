package jdksymlink.core

import jdksymlink.core.data.{Bold, Normal}

/**
 * @author Kevin Lee
 * @since 2020-01-01
 */
object Shell {
  def bold(text: String): String = s"$Bold$text$Normal"
}
