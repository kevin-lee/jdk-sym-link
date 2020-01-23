package jdksymlink

import cats._
import cats.implicits._

import jdksymlink.core.data.YesOrNo

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  def readLnF[F[_] : Effect]: F[String] = Effect[F].effect(scala.io.StdIn.readLine)
  def putStrLnF[F[_] : Effect](value: String): F[Unit] = Effect[F].effect(println(value))

  def readYesOrNoF[F[_] : Effect : Monad](prompt: String): F[YesOrNo] = for {
    _ <- putStrLnF[F](prompt)
    answer <- readLnF[F]
    yesOrN <-  answer match {
      case "y" | "Y" =>
        Effect[F].effect(YesOrNo.yes)
      case "n" | "N" =>
        Effect[F].effect(YesOrNo.no)
      case _ =>
        readYesOrNoF[F](prompt)
    }
  } yield yesOrN

}
