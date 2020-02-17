package jdksymlink.effect

import cats.Id
import cats.effect.IO

/**
 * @author Kevin Lee
 * @since 2020-01-22
 */
trait EffectConstructor[F[_]] {
  def effect[A](a: => A): F[A]
}

object EffectConstructor {

  def apply[F[_] : EffectConstructor]: EffectConstructor[F] = implicitly[EffectConstructor[F]]

  implicit val ioEffectConstructor: EffectConstructor[IO] = new EffectConstructor[IO] {
    override def effect[A](a: => A): IO[A] = IO(a)
  }

  implicit val idSideEffectConstructor: EffectConstructor[Id] = new EffectConstructor[Id] {
    override def effect[A](a: => A): Id[A] = a
  }

}

trait Effectful[F[_]] {
  protected def EF: EffectConstructor[F]

  def effect[A](a: => A): F[A] = EF.effect(a)
}