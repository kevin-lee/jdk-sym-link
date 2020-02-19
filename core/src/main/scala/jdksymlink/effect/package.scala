package jdksymlink

import cats._
import cats.implicits._

import jdksymlink.core.data.YesOrNo

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  def readLn[F[_] : EffectConstructor]: F[String] =
    EffectConstructor[F].effect(scala.io.StdIn.readLine)

  def putStrLn[F[_] : EffectConstructor](value: String): F[Unit] =
    EffectConstructor[F].effect(println(value))

  def readYesOrNo[F[_] : EffectConstructor : Monad](prompt: String): F[YesOrNo] = for {
    _ <- putStrLn[F](prompt)
    answer <- readLn[F]
    yesOrN <-  answer match {
      case "y" | "Y" =>
        EffectConstructor[F].effect(YesOrNo.yes)
      case "n" | "N" =>
        EffectConstructor[F].effect(YesOrNo.no)
      case _ =>
        readYesOrNo[F](prompt)
    }
  } yield yesOrN

}
