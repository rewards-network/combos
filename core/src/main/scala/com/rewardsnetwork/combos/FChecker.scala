package com.rewardsnetwork.combos

import cats.{Applicative, Functor, Monad}

trait FChecker[F[_], E] {

  /** Alias for `askF`, which will ask for a pure value `A` and lift it to an `F` context.
    * Useful for mixing pure values in with effectful validation.
    */
  def ask[A](implicit A: Applicative[F]) = syntax.askF[F, E, A]

  /** Ask for some value effectful value `F[A]` and evaluate it, performing no validation. */
  def askEval[A](implicit F: Functor[F]) = syntax.askEval[F, E, A]

  /** Alias for `checkEval`, which is like `check` but accepts result values wrapped in `F[_]` */
  def check[A](pf: PartialFunction[A, F[E]])(implicit A: Applicative[F]) = syntax.checkEval(pf)

  /** Alias for `checkReturnF`, which is like `checkReturn` but accepts result values wrapped in `F[_]` */
  def checkReturn[A, B](f: A => F[Either[E, B]])(implicit A: Applicative[F]) = syntax.checkReturnF(f)

  /** Alias for `optionF`, which is like `option` but returns values wrapped in `F[_]` */
  def option[A](ifNone: => E)(implicit A: Applicative[F]) = syntax.optionF[F, E, A](ifNone)

  /** Alias for `optionEval[F, E, A]`, which evaluates effects and then tries to return the result if it exists. */
  def optionEval[A](ifNone: => E)(implicit A: Applicative[F]) = syntax.optionEval[F, E, A](ifNone)

  /** Alias for `eitherF`, which is like `either` but returns values wrapped in `F[_]` */
  def either[A](implicit A: Applicative[F]) = syntax.eitherF[F, E, A]

  /** Alias for `eitherEval[F, E, A]`, which evaluates effects and then tries to return the `Right` value. */
  def eitherEval[A](implicit A: Applicative[F]) = syntax.eitherEval[F, E, A]
}

object FChecker {
  def apply[F[_]: Monad, E] = new FChecker[F, E] {}
}
