package com.rewardsnetwork.combos

/** A simple way to define checks that all share a fixed error type.
  * To use, either create a new `Checker[E]` and import its contents, or extend `Checker[E]` in your classes/objects.
  *
  * Example usage (extends Checker):
  * ```
  * object MyValidators extends Checker[String] {
  *   //The error type is pre-fixed to String
  *   val checkAge = check[Int] { case 20 => "Age can't be 20 for some reason" }
  *   val checkName = check[String] { case "Hitler" => "Hitler is not a good name" }
  *   val checkUser = parCheckAll(List[Validator[String, Int]](
  *     checkAge.local(_.age),
  *     checkName.local(_.name)
  *   ))
  * }
  * ```
  *
  * Alternate usage (import DSL object):
  * ```
  * val checkWithString = Checker[String]
  * import checkWithString._
  *
  * val checkAge = check[Int] { case 20 => "Age can't be 20 for some reason" }
  * ```
  */
trait Checker[E] {

  /** Same as the default `ask[E, A]`, but with a fixed error type. */
  def ask[A] = syntax.ask[E, A]

  /** Same as the default `check[E, A]`, but with a fixed error type. */
  def check[A](pf: PartialFunction[A, E]) = syntax.check(pf)

  /** Same as the default `checkReturn[E, A, B]`, but with a fixed error type. */
  def checkReturn[A, B](f: A => Either[E, B]) = syntax.checkReturn(f)

  /** Same as the default `option[E, A]`, but with a fixed error type. */
  def option[A](ifNone: => E) = syntax.option[E, A](ifNone)

  /** Same as the default `either[E, A]`, but with a fixed error type. */
  def either[A] = syntax.either[E, A]
}

object Checker {
  def apply[E] = new Checker[E] {}
}
