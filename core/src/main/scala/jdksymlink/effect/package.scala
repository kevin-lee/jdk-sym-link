package jdksymlink

import cats.effect.IO
import jdksymlink.core.data.YesOrNo

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  val readLn: IO[String] = IO(scala.io.StdIn.readLine)
  def putStrLn(value: String): IO[Unit] = IO(println(value))

  def readYesOrNo(prompt: String): IO[YesOrNo] = for {
    _ <- putStrLn(prompt)
    answer <- readLn
    yesOrN <-  answer match {
      case "y" | "Y" =>
        IO(YesOrNo.yes)
      case "n" | "N" =>
        IO(YesOrNo.no)
      case _ =>
        readYesOrNo(prompt)
    }
  } yield yesOrN

}
