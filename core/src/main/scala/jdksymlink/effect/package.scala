package jdksymlink

import cats._
import cats.implicits._

import jdksymlink.core.data.YesOrNo

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  def readLnF[F[_] : EffectConstructor]: F[String] =
    EffectConstructor[F].effect(scala.io.StdIn.readLine)

  def putStrLnF[F[_] : EffectConstructor](value: String): F[Unit] =
    EffectConstructor[F].effect(println(value))

  def readYesOrNoF[F[_] : EffectConstructor : Monad](prompt: String): F[YesOrNo] = for {
    _ <- putStrLnF[F](prompt)
    answer <- readLnF[F]
    yesOrN <-  answer match {
      case "y" | "Y" =>
        EffectConstructor[F].effect(YesOrNo.yes)
      case "n" | "N" =>
        EffectConstructor[F].effect(YesOrNo.no)
      case _ =>
        readYesOrNoF[F](prompt)
    }
  } yield yesOrN

}
