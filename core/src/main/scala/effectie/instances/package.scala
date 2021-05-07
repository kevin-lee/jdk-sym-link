package effectie

/** @author Kevin Lee
  * @since 2021-05-07
  */
package object instances {
  given yesNoCanEqual: CanEqual[YesNo, YesNo] = CanEqual.derived
}
