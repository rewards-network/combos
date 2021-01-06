package com.rewardsnetwork.combos.refined

import cats.data._
import cats.implicits._
import com.rewardsnetwork.combos._
import eu.timepit.refined.refineV
import eu.timepit.refined.api.Validate
import eu.timepit.refined.api.Refined
import cats.Monad

object syntax {

  implicit class RefineShortCircuitOps[E, A, B](rs: Kleisli[Either[E, *], A, B]) {

    /** Refines the output of this `ShortCircuit` using the predicate `P`.
      * Assumes your error is of type `String`.
      * To massage the error into a custom error type, use `refineMapLeft` instead.
      */
    def refine[P](implicit v: Validate[B, P], ev: String =:= E) =
      rs.flatMapF(b => refineV[P](b).leftMap(ev.apply))

    /** Like `refine`, but allows mapping the error from `Refined` into a custom error `E`. */
    def refineMapLeft[P](f: String => E)(implicit v: Validate[B, P]) =
      rs.flatMapF(b => refineV[P](b).leftMap(f))

    /** Turn this short circuit into a Refined `Validate` instance.
      * You must supply a function that describes the error in case of failure, and a `P` value representing your predicate.
      * Example:
      * ```
      * //Our predicate
      * case class OverEighteen()
      *
      * val checkAge = check[Unit, Int] {
      *   case i if (i <= 18) => ()
      * }
      * implicit val ageValidate = checkAge.asValidate(i => s"\$i > 18", OverEighteen())
      *
      * refineV[OverEighteen](17) //Left("Predicate failed: (17 > 18)")
      * ```
      */
    def asValidate[P](showExpr: A => String, p: P): Validate.Plain[A, P] =
      Validate.fromPredicate(a => rs.run(a).isRight, showExpr, p)
  }

  implicit class RefineValidatorCircuitOps[E, A, B](rv: Kleisli[Either[NonEmptyChain[E], *], A, B]) {

    /** Refines the output of this `Validator` using the predicate `P`.
      * Assumes your error is of type `String`.
      * To massage the error into a custom error type, use `refineMapLeft` instead.
      */
    def refine[P](implicit v: Validate[B, P], ev: String =:= E) =
      rv.flatMapF(b => refineV[P](b).leftMap(s => NonEmptyChain.one(ev(s))))

    /** Like `refine`, but allows mapping the error from `Refined` into a custom error `E`. */
    def refineMapLeft[P](f: String => E)(implicit v: Validate[B, P]) =
      rv.flatMapF(b => refineV[P](b).leftMap(s => NonEmptyChain.one(f(s))))

    /** Turn this validator into a Refined `Validate` instance
      * You must supply a function that describes the error in case of failure, and a `P` value representing your predicate.
      * Example:
      * ```
      * //Our predicate
      * case class OverEighteen()
      *
      * val checkAge = check[Unit, Int] {
      *   case i if (i <= 18) => ()
      * }
      * implicit val ageValidate = checkAge.asValidate(i => s"\$i > 18", OverEighteen())
      *
      * refineV[OverEighteen](17) //Left("Predicate failed: (17 > 18)")
      * ```
      */
    def asValidate[P](showExpr: A => String, p: P): Validate.Plain[A, P] =
      Validate.fromPredicate(a => rv.run(a).isRight, showExpr, p)
  }

  implicit class RefineFShortCircuitCircuitOps[F[_]: Monad, E, A, B](frs: Kleisli[EitherT[F, E, *], A, B]) {

    /** Refines the output of this `FShortCircuit` using the predicate `P`.
      * Assumes your error is of type `String`.
      * To massage the error into a custom error type, use `refineMapLeft` instead.
      */
    def refine[P](implicit v: Validate[B, P], ev: String =:= E) =
      frs.flatMapF(b => EitherT.fromEither[F](refineV[P](b).leftMap(ev.apply)))

    /** Like `refine`, but allows mapping the error from `Refined` into a custom error `E`. */
    def refineMapLeft[P](f: String => E)(implicit v: Validate[B, P]) =
      frs.flatMapF(b => EitherT.fromEither[F](refineV[P](b).leftMap(f)))
  }

  implicit class RefineFValidatorCircuitOps[F[_]: Monad, E, A, B](frv: Kleisli[EitherT[F, NonEmptyChain[E], *], A, B]) {

    /** Refines the output of this `FValidator` using the predicate `P`.
      * Assumes your error is of type `String`.
      * To massage the error into a custom error type, use `refineMapLeft` instead.
      */
    def refine[P](implicit v: Validate[B, P], ev: String =:= E) =
      frv.flatMapF(b => EitherT.fromEither[F](refineV[P](b).leftMap(s => NonEmptyChain.one(ev(s)))))

    /** Like `refine`, but allows mapping the error from `Refined` into a custom error `E`. */
    def refineMapLeft[P](f: String => E)(implicit v: Validate[B, P]) =
      frv.flatMapF(b => EitherT.fromEither[F](refineV[P](b).leftMap(s => NonEmptyChain.one(f(s)))))
  }

  /** Defines a `Validator` that `ask`s for input of type `A` and validates to `P` with `Refined`.
    * To refine an existing validator, please use the `.refine` extension method.
    */
  def refine[A, P](implicit v: Validate[A, P]): ReturningValidator[String, A, Refined[A, P]] =
    Kleisli(a => refineV[P](a).leftMap(NonEmptyChain.one))
}
