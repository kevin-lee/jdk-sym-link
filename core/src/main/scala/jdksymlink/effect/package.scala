package jdksymlink

import cats._
import cats.implicits._

import jdksymlink.core.data.YesOrNo

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  def readLnF[F[_]: Monad]: F[String] = Monad[F].pure(scala.io.StdIn.readLine)
  def putStrLnF[F[_]: Monad](value: String): F[Unit] = Monad[F].pure(println(value))

  def readYesOrNoF[F[_]: Monad](prompt: String): F[YesOrNo] = for {
    _ <- putStrLnF[F](prompt)
    answer <- readLnF[F]
    yesOrN <-  answer match {
      case "y" | "Y" =>
        Monad[F].pure(YesOrNo.yes)
      case "n" | "N" =>
        Monad[F].pure(YesOrNo.no)
      case _ =>
        readYesOrNoF[F](prompt)
    }
  } yield yesOrN

}
