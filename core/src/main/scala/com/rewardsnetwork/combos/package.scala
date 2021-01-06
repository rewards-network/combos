package com.rewardsnetwork

import cats.data._

package object combos {

  /** A `ShortCircuit` that also returns some value `B`. See `ReturningValidator`. */
  type ReturningShortCircuit[E, A, B] = Kleisli[Either[E, *], A, B]

  /** An alternative to `Validator` that is designed to have at most one error.
    * Do not create these directly. Instead, it is preferred for composability reasons to convert with `.failFast` on a `Validator`.
    */
  type ShortCircuit[E, A] = Kleisli[Either[E, *], A, Unit]

  /** A `Validator` that also returns some value `B`.
    * Useful when chaining operations together for validation, or returning some "context value".
    */
  type ReturningValidator[E, A, B] = Kleisli[EitherNec[E, *], A, B]

  /** A type that represents the possibility of accumulating errors.
    * Instead of manual composition with `Either` or `Validated`, this focuses on individual field or property testing.
    *
    * A `Validator[E, A]` will return one or more errors of type `E`, and takes an input of type `A`.
    * A validator allows you to separate the defining of validation logic from the actual data that needs to be checked.
    * For example, to validate a user's age, simply define a validator where `A` is of type `Int`.
    * To pass in a field individually, you can use `.run(user.age)`. or you can map the expected input type using `.local[User](_.age)`.
    * Using the latter strategy, it becomes possible to compose multiple pre-existing validators together to produce a single validator.
    * For examples, see the `checkAll` and `parCheckAll` functions in `syntax`.
    *
    * To create a Validator, import `com.rewardsnetwork.combos.syntax._` and use the supplied `check` function.
    * For simpler syntax when making multiple validators, extend or import the values of a `Checker`, which fixes the error type for `check`.
    */
  type Validator[E, A] = ReturningValidator[E, A, Unit]

  type FReturningShortCircuit[F[_], E, A, B] = Kleisli[EitherT[F, E, *], A, B]

  type FShortCircuit[F[_], E, A] = FReturningShortCircuit[F, E, A, Unit]

  type FReturningValidator[F[_], E, A, B] = Kleisli[EitherT[F, NonEmptyChain[E], *], A, B]

  type FValidator[F[_], E, A] = FReturningValidator[F, E, A, Unit]
}
