package jdksymlink

import cats.effect.IO

/**
 * @author Kevin Lee
 * @since 2019-12-26
 */
package object effect {

  val readLn: IO[String] = IO(scala.io.StdIn.readLine)
  def putStrLn(value: String): IO[Unit] = IO(println(value))

}
