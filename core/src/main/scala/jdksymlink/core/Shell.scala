package jdksymlink.core

import scala.io.AnsiColor

/** @author Kevin Lee
  * @since 2020-01-01
  */
object Shell {

  enum Color derives CanEqual {
    case Black
    case Red
    case Green
    case Yellow
    case Blue
    case Magenta
    case Cyan
    case White
    case BlackBg
    case RedBg
    case GreenBg
    case YellowBg
    case BlueBg
    case MagentaBg
    case CyanBg
    case WhiteBg
    case Reset
    case Bold
    case Underlined
    case Blink
    case Reversed
    case Invisible
  }

  object Color {

    extension (color: Color) {
      def render: String = color match {
        case Black =>
          AnsiColor.BLACK

        case Red =>
          AnsiColor.RED

        case Green =>
          AnsiColor.GREEN

        case Yellow =>
          AnsiColor.YELLOW

        case Blue =>
          AnsiColor.BLUE

        case Magenta =>
          AnsiColor.MAGENTA

        case Cyan =>
          AnsiColor.CYAN

        case White =>
          AnsiColor.WHITE

        case BlackBg =>
          AnsiColor.BLACK_B

        case RedBg =>
          AnsiColor.RED_B

        case GreenBg =>
          AnsiColor.GREEN_B

        case YellowBg =>
          AnsiColor.YELLOW_B

        case BlueBg =>
          AnsiColor.BLUE_B

        case MagentaBg =>
          AnsiColor.MAGENTA_B

        case CyanBg =>
          AnsiColor.CYAN_B

        case WhiteBg =>
          AnsiColor.WHITE_B

        case Reset =>
          AnsiColor.RESET

        case Bold =>
          AnsiColor.BOLD

        case Underlined =>
          AnsiColor.UNDERLINED

        case Blink =>
          AnsiColor.BLINK

        case Reversed =>
          AnsiColor.REVERSED

        case Invisible =>
          AnsiColor.INVISIBLE
      }

      def toAnsi: String = Color.render(color)
    }

  }

  object ColoredString {

    extension (text: String) {

      def colored(color: Color): String =
        s"${color.toAnsi}$text${Color.Reset.toAnsi}"

      def bold: String = colored(Color.Bold)
      def underlined: String = colored(Color.Underlined)

      def red: String = colored(Color.Red)
      def green: String = colored(Color.Green)
      def blue: String = colored(Color.Blue)
    }
  }

}
