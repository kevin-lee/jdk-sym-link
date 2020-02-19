package jdksymlink.effect

import cats.Id
import cats.effect.IO

/**
 * @author Kevin Lee
 * @since 2020-01-22
 */
trait EffectConstructor[F[_]] {
  def effect[A](a: => A): F[A]
  def pureEffect[A](a: A): F[A]
  def unit: F[Unit]
}

object EffectConstructor {

  def apply[F[_] : EffectConstructor]: EffectConstructor[F] = implicitly[EffectConstructor[F]]

  implicit val ioEffectConstructor: EffectConstructor[IO] = new EffectConstructor[IO] {

    override def effect[A](a: => A): IO[A] = IO(a)

    override def pureEffect[A](a: A): IO[A] = IO.pure(a)

    override def unit: IO[Unit] = IO.unit
  }

  implicit val idSideEffectConstructor: EffectConstructor[Id] = new EffectConstructor[Id] {

    override def effect[A](a: => A): Id[A] = a

    override def pureEffect[A](a: A): Id[A] = a

    override def unit: Id[Unit] = ()
  }

}

trait Effectful[F[_]] {
  protected def EF: EffectConstructor[F]

  def effect[A](a: => A): F[A] = EF.effect(a)

  def pureEffect[A](a: A): F[A] = EF.pureEffect(a)

  def effectUnit: F[Unit] = EF.unit
}