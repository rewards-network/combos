package com.rewardsnetwork.combos

import cats._
import cats.data._
import cats.implicits._

object syntax {

  implicit class ReturningShortCircuitOps[E, A, B](s: Kleisli[Either[E, *], A, B]) {

    /** Turns this `ShortCircuit` into a `Validator` that can accumulate errors.
      * The resulting `Validator` will still only return a single error, but it can more easily compose with other `Validator`s.
      */
    def accumulate = s.mapF(_.leftMap(NonEmptyChain.one))

    /** Maps the error type to some new type `E2`. */
    def mapLeft[E2](f: E => E2) = s.mapF(_.leftMap(f))

    /** Turns this `ShortCircuit` into one that returns its input once validated.
      * For more control, use `ask[E, A].flatMap` directly.
      */
    def returnInput = ask[E, A].failFast.flatMap(a => s.as(a))

    /** Run and return an `Option` of the first error that occurs. */
    def runOption(a: A): Option[E] = s.run(a).swap.toOption

    /** Lifts this pure `ShortCircuit` into an `FShortCircuit` for your supplied effect type `F[_]`. */
    def withF[F[_]: Applicative] = s.mapF(EitherT.fromEither[F].apply)
  }

  implicit class ReturningValidatorOps[E, A, B](v: Kleisli[EitherNec[E, *], A, B]) {

    /** Turns this `Validator` into a `ShortCircuit` that cannot accumulate errors. */
    def failFast: ReturningShortCircuit[E, A, B] = v.mapF(_.leftMap(_.head))

    /** Maps the error type `E` to some new type `E2`. */
    def mapLeft[E2](f: E => E2) = v.mapF(_.leftMap(_.map(f)))

    /** Turns this `Validator` into one that returns its input once validated.
      * For more control, use `ask[E, A].flatMap` directly.
      */
    def returnInput = ask[E, A].flatMap(a => v.as(a))

    /** Run and return only the first error. */
    def runFailFast(a: A): Either[E, B] = failFast.run(a)

    /** Run and return an `Option` of your errors. */
    def runOption(a: A): Option[NonEmptyChain[E]] = v.run(a).swap.toOption

    /** Run and return an `Option` of the first error that occurs. */
    def runFailFastOption(a: A): Option[E] = runFailFast(a).swap.toOption

    /** Lifts this pure `Validator` into an `FValidator` for your supplied effect type `F[_]`. */
    def withF[F[_]: Applicative] = v.mapF(EitherT.fromEither[F].apply)

  }

  implicit class FReturningShortCircuitOps[F[_], E, A, B](fs: Kleisli[EitherT[F, E, *], A, B]) {

    /** Turns this `FShortCircuit` into an `FValidator` that can accumulate errors.
      * The resulting `FValidator` will still only return a single error, but it can more easily compose with other `FValidator`s.
      */
    def accumulate(implicit F: Functor[F]): FReturningValidator[F, E, A, B] = fs.mapF(_.leftMap(NonEmptyChain.one))

    /** Maps the error type to some new type `E2`. */
    def mapLeft[E2](f: E => E2)(implicit F: Functor[F]) = fs.mapF(_.leftMap(f))

    /** Turns this `FShortCircuit` into one that returns its input once validated.
      * For more control, use `askF[F, E, A].flatMap` directly.
      */
    def returnInput(implicit M: Monad[F]) = askF[F, E, A].failFast.flatMap(a => fs.as(a))

    /** Run and return an `OptionT` of the first error that occurs.
      * Equivalent to `FValidator#runFailFastOptionT`.
      */
    def runOptionT(a: A)(implicit F: Functor[F]): OptionT[F, E] = fs.run(a).swap.toOption
  }

  implicit class FReturningValidatorOps[F[_], E, A, B](
      fv: Kleisli[EitherT[F, NonEmptyChain[E], *], A, B]
  ) {

    /** Turns this `FValidator` into an `FShortCircuit` that cannot accumulate errors. */
    def failFast(implicit F: Functor[F]): FReturningShortCircuit[F, E, A, B] = fv.mapF(_.leftMap(_.head))

    /** Maps the error type to some new type `E2`. */
    def mapLeft[E2](f: E => E2)(implicit F: Functor[F]) = fv.mapF(_.leftMap(_.map(f)))

    /** Turns this `FValidator` into one that returns its input once validated.
      * For more control, use `askF[F, E, A].flatMap` directly.
      */
    def returnInput(implicit M: Monad[F]) = askF[F, E, A].flatMap(a => fv.as(a))

    /** Run and return only the first error. */
    def runFailFast(a: A)(implicit F: Functor[F]): EitherT[F, E, B] = failFast.run(a)

    /** Run and return an `OptionT` of your errors. */
    def runOptionT(a: A)(implicit F: Functor[F]): OptionT[F, NonEmptyChain[E]] = fv.run(a).swap.toOption

    /** Run and return an `OptionT` of the first error that occurs. */
    def runFailFastOptionT(a: A)(implicit F: Functor[F]): OptionT[F, E] = runFailFast(a).swap.toOption
  }

  /** A no-op validator that returns its input value.
    * Useful when you want to "ask for" some extra context, like when transforming an existing validator.
    */
  def ask[E, A]: ReturningValidator[E, A, A] = Kleisli(_.asRight)

  /** Ask for a pure value `A` and lift it to an `F` context.
    * Useful for mixing pure values in with effectful validation.
    */
  def askF[F[_]: Applicative, E, A]: FReturningValidator[F, E, A, A] =
    Kleisli(a => EitherT.pure[F, NonEmptyChain[E]](a))

  /** Ask for some value effectful value `F[A]` and evaluate it, performing no validation. */
  def askEval[F[_]: Functor, E, A]: FReturningValidator[F, E, F[A], A] =
    Kleisli(fa => EitherT.liftF[F, NonEmptyChain[E], A](fa))

  /** Constructs a `ReturningValidator` that returns an output value `B`. */
  def checkReturn[E, A, B](f: A => Either[E, B]): ReturningValidator[E, A, B] =
    Kleisli(a => f(a).leftMap(NonEmptyChain.one))

  /** Constructs a `Validator` for some property.
    *
    * Example usage:
    * ```
    * case class MyCaseClass(int: Int)
    * val checkInt: Validator[Boolean, Int] = check { case 5 => false }
    * val badCaseClass = MyCaseClass(5)
    *
    * checkInt
    *   .local[MyCaseClass](_.int)
    *   .runFailFast(badCaseClass)
    * // Left(false)
    * ```
    */
  def check[E, A](pf: PartialFunction[A, E]): Validator[E, A] =
    checkReturn(a => pf.lift(a).map(_.asLeft).getOrElse(().asRight))

  /** Turns the supplied list of validators into a `ShortCircuit[E, A]`.
    * Fails fast and returns only the first error.
    * To compose with other validators after calling this, use `ShortCircuit#accumulate` to lift to a `Validator`.
    *
    * Example usage:
    * ```
    * case class MyCaseClass(int: Int, string: String)
    *
    * val checkInt: Validator[Boolean, Int] = check { case 5 => false }
    * val checkString: Validator[Boolean, String] = check { case "thing" => false}
    *
    * val badCaseClass = MyCaseClass(5, "thing")
    *
    * val checkCaseClass = checkAll(List[Validator[Boolean, MyCaseClass](
    *   checkInt.local(_.int),
    *   checkString.local(_.string)
    * ))
    *
    * checkCaseClass.run(badCaseClass)
    * // Left(false) - first error
    * ```
    */
  def checkAll[E, A](l: List[Validator[E, A]]): ShortCircuit[E, A] = {
    l.map(_.failFast).widen[Kleisli[Either[E, *], A, Unit]].sequence_
  }

  /** Like the `checkAll` that takes a list, except this takes varargs. */
  def checkAll[E, A](vs: Validator[E, A]*): ShortCircuit[E, A] = checkAll(vs.toList)

  /** Turns the supplied list of validators into a parallel set of checks that accumulates results.
    * See `checkAll` for usage.
    * This will produce a new `Validator` which will return the set of all errors accumulated when ran.
    */
  def parCheckAll[E, A](l: List[Validator[E, A]]): Validator[E, A] = {
    l.widen[Kleisli[EitherNec[E, *], A, Unit]].parSequence_
  }

  /** Turns the supplied list of validators into a parallel set of checks that accumulates results.
    * See `checkAll` for usage.
    * This will produce a new `Validator` which will return the set of all errors accumulated when ran.
    */
  def parCheckAll[E, A](vs: Validator[E, A]*): Validator[E, A] = parCheckAll(vs.toList)

  /** Like `checkReturn`, but accepts values wrapped in `F[_]`. */
  def checkReturnF[F[_]: Applicative, E, A, B](f: A => F[Either[E, B]]): FReturningValidator[F, E, A, B] =
    Kleisli(a => EitherT(f(a)).leftMap(NonEmptyChain.one))

  /** Like `check`, but accepts values wrapped in `F[_]` */
  def checkEval[F[_]: Applicative, E, A](pf: PartialFunction[A, F[E]]): FValidator[F, E, A] =
    checkReturnF[F, E, A, Unit] { a =>
      val optF = pf.lift(a).sequence
      optF.map(o => Either.fromOption(o, ()).swap)
    }

  /** Like `checkAll`, but accepts a list of `FValidator` values instead. */
  def checkAllF[F[_]: Monad, E, A](l: List[FValidator[F, E, A]]): FShortCircuit[F, E, A] =
    l.map(_.failFast)
      .widen[Kleisli[EitherT[F, E, *], A, Unit]]
      .sequence_

  /** Like `checkAllF`, but accepts varargs of `FValidator` values. */
  def checkAllF[F[_]: Monad, E, A](fvs: FValidator[F, E, A]*): FShortCircuit[F, E, A] =
    checkAllF(fvs.toList)

  /** Like `parCheckAll`, but accepts varargs of */
  def parCheckAllF[F[_]: Monad, E, A](l: List[FValidator[F, E, A]]): FValidator[F, E, A] =
    l.widen[Kleisli[EitherT[F, NonEmptyChain[E], *], A, Unit]].parSequence_

  /** Like `parCheckAllF`, but accepts varargs of `FValidator` values. */
  def parCheckAllF[F[_]: Monad, E, A](fvs: FValidator[F, E, A]*): FValidator[F, E, A] =
    parCheckAllF(fvs.toList)

  /** Extract the value supplied from an `Option[A]` */
  def option[E, A](ifNone: => E): ReturningValidator[E, Option[A], A] =
    checkReturn(Either.fromOption(_, ifNone))

  /** Same as `option[E, A]`, but lifts the value into the `F[_]` context.
    * Similar to `askF`, but extracts optional values.
    */
  def optionF[F[_]: Applicative, E, A](ifNone: => E): FReturningValidator[F, E, Option[A], A] =
    Kleisli(oa => EitherT.fromOption[F](oa, NonEmptyChain.one(ifNone)))

  /** Evaluates an effect `F[Option[A]]` and tries to extract the final `A` value if it exists.
    * See `option` for pure values, `optionF` for pure values to lift into `F`.
    */
  def optionEval[F[_]: Applicative, E, A](ifNone: => E): FReturningValidator[F, E, F[Option[A]], A] =
    Kleisli(foa => EitherT.fromOptionF[F, NonEmptyChain[E], A](foa, NonEmptyChain.one(ifNone)))

  /** Extracts the `Right` side from a supplied `Either` and returns the `Left` as an error. */
  def either[E, A]: ReturningValidator[E, Either[E, A], A] =
    checkReturn(identity)

  /** Same as `either`, but lifts the result type into the `F[_]` context.
    * Similar to `askF`, but extracts the right side value.
    */
  def eitherF[F[_]: Applicative, E, A]: FReturningValidator[F, E, Either[E, A], A] =
    Kleisli(ea => EitherT.fromEither[F](ea.leftMap(NonEmptyChain.one)))

  /** Evaluates an effect `F[Either[E, A]]` and tries to extract the final `A` value if it exists.
    * See `either` for pure values, `eitherF` for pure values to lift into `F`.
    */
  def eitherEval[F[_]: Applicative, E, A]: FReturningValidator[F, E, F[Either[E, A]], A] =
    Kleisli(fea => EitherT(fea).leftMap(NonEmptyChain.one))
}
