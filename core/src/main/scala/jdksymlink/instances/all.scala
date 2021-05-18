package jdksymlink.instances

/**
 * @author Kevin Lee
 * @since 2021-05-16
 */
object all extends OptionInstances with EitherInstances with TupleInstances
trait OptionInstances {
  given canEqualOption[T](using eq: CanEqual[T, T]): CanEqual[Option[T], Option[T]] = CanEqual.derived // for `case None` in pattern matching

  given canEqualOptions[T, U](using eq: CanEqual[T, U]): CanEqual[Option[T], Option[U]] = CanEqual.derived
}
object options extends OptionInstances
trait EitherInstances {
  given canEqualEither[L1, R1, L2, R2](
    using eqL: CanEqual[L1, L2], eqR: CanEqual[R1, R2]
  ): CanEqual[Either[L1, R1], Either[L2, R2]] = CanEqual.derived
}
object eithers extends EitherInstances
trait TupleInstances {
  given canEqualEmptyTuple: CanEqual[EmptyTuple, EmptyTuple] = CanEqual.derived
  given canEqualTuple[H1, T1 <: Tuple, H2, T2 <: Tuple](
    using eqHead: CanEqual[H1, H2], eqTail: CanEqual[T1, T2]
  ): CanEqual[H1 *: T1, H2 *: T2] = CanEqual.derived

}
object tuples extends TupleInstances
