# Combos
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.rewardsnetwork/combos_2.13?label=latest&server=https%3A%2F%2Foss.sonatype.org)


A validation library for Scala

## Setup
This library is published for both Scala 2.12 and 2.13.
Scala 3 support will be coming soon.
```
libraryDependencies += "com.rewardsnetwork" %% "combos" % "<latest tag>"
libraryDependencies += "com.rewardsnetwork" %% "combos-refined" % "<latest tag>" //Optional - adds Refined support
```

## API Docs
* [Core](https://javadoc.io/doc/com.rewardsnetwork/combos_2.13/latest/com/rewardsnetwork/combos/index.html)
* [Refined](https://javadoc.io/doc/com.rewardsnetwork/combos-refined_2.13/latest/com/rewardsnetwork/combos/refined/index.html)

## License
Copyright 2020 Rewards Network Establishment Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Motivation
This library was birthed from some projects that needed to share validation logic across multiple projects and sub-projects.
Normal validation often focuses on functions that take in an object with several fields, which is harder to test in isolation as you need to create a lot of mock data.
You can define functions that take in smaller values, such as a single field, but wiring those together is not directly composable.
This library uses Cats to implement certain validation patterns that are applicable for both fail-fast and error-accumulating scenarios alike, and allows you to plug different validations into each other to create more complicated validation processes.

The end result is a library that allows you to separate your validation logic, data definition, and testing logic in a much easier, more compact way than doing it strictly with functions alone can provide.

## Basic Usage
A `Validator[E, A]` is a type that validates values of type `A` and produces errors of type `E`.
As opposed to manually composing `Either` and `Validated` values together, a `Validator` allows you to focus on individual "checks" that need to be performed, and then can be composed and "ran" later into one full `Either` value containing your errors.

To get started, `import com.rewardsnetwork.combos.syntax._` and take a look at the `check` function, which allows you to create a validator.
To compose validators, look at `checkAll` for fail-fast validation, and `parCheckAll` for accumulating errors.
To pass a validator a value to be tested, use `.run`, or if you are only expecting a single error value you can use `.runFailFast`.

When using `checkAll`, it will return a `ShortCircuit[E, A]` which is equivalent to a `Validator[E, A]`, except it can only return a single error value.
`parCheckAll` returns another `Validator` that can be composed with other validators, and returns all possible error values.
You can transform between the two using `.failFast` on `Validator` and `.accumulate` on `ShortCircuit`.

When composing validators, you will want to change their input type with `.local`, which acts as a `map`-like function for the input value.
In this way, you can validate case classes and other data structures simply by defining a way to get from your more specific type to the field you are trying to validate.

Example usage:
```scala
case class MyCaseClass(int: Int, string: String)

val checkInt: Validator[Boolean, Int] = check { case 5 => false }
val checkString: Validator[Boolean, String] = check { case "thing" => false }

val checkCaseClass = parCheckAll(List[Validator[Boolean, MyCaseClass]](
  checkInt.local(_.int),
  checkString.local(_.string)
))

val badCaseClass = MyCaseClass(5, "thing")

checkCaseClass.run(badCaseClass)
// Left(NonEmptyChain(false, false)) -- both errors

checkCaseClass.failFast.runFailFast(badCaseClass)
// Left(false) -- first error only
```

## Checker
`Checker` is a mix-in trait or importable DSL that is just a shorter way to define multiple validators.
Say you are validating the numerous fields of a case class, and are providing those all in an object.
It would be very tedious to have to specify the error type for every single validation, so a `Checker` solves that for you.
You can `extend Checker[E]` and you will get a `check[A]` function that has a fixed error type in your local scope.
If you feel uneasy about extending the mix-in, simply create a checker and import its values, like so:
```scala
val checker = Checker[String]
checker.check[Boolean] { case false => "can't be false" }

import checker._
check[boolean] { case false => "can't be false" }

//The above two are equivalent
```

## Returning Values & Ask
A `Validator` and `ShortCircuit` are instances of `ReturningValidator` and `ReturningShortCircuit` respectively.
These are the same as before, except they also have a known return value.
This can be particularly useful when you are building "chained validators" where the output from one validator should be fed into a subsequent one.

To produce one of these values, use `checkReturn` instead of `check`, which returns the source input.
It can then be mapped, flatMapped, and transformed like any other monadic value.

Sometimes you will want to "ask for" a value, but not immediately validate it, possibly to use it as part of a more complex validation scenario.
Consider this example where you are validating a user's age, and want to return the validated `User` object given you know the user's name:
```scala
case class User(name: String, age: Int)

val askName = ask[String, String]

val checkAge = checkReturn[String, Int] {
  case age if (age < 18) => "User is not an adult"
}

// Checks the user's age, then adds in a name and returns the validated user.
val checkUser = askName.local[User](_.name).flatMap { name =>
  checkAge
    .local[User](_.age)
    .as(age => User(name, age))
}

checkUser.run(User("Ryan", 18)) //Right(User("Ryan", 18))
```

In addition, there are special `option` and `either` constructors that will `ask` for a value, and if it exists, try to return it.
These are especially useful when building staged validation where some input is optional and you need to extract it regardless.

## Special Syntax
Every `Validator` and derivative thereof has special syntax you can use from implicits.
Assuming you have `syntax._` imported, you will get access to these for every validator:

* `failFast` - Turns into a `ShortCircuit` that can only return at most one error.
* `mapLeft` - Map the error type `E` to a new value of type `E2`.
* `returnInput` - Returns the input to this validator after running.
* `runFailFast` - Shorthand for `.failFast.run`
* `runOption` - Discards any return value, and returns an `Option` of the errors
* `runFailFastOption` - Shorthand for `.failFast.runOption`
* `withF` - Lifts this validator to operate within the effect type `F[_]` specified. Only available on pure validators.

For every `ShortCircuit`, these are available:

* `accumulate` - Turns into a `Validator` that can now accumulate multiple values.
* `mapLeft` - Map the error type `E` to a new value of type `E2`.
* `returnInput` - Returns the input to this short circuit after running.
* `runOption` - Discards any return value, and returns an `Option` of the error.
* `withF` - Lifts this short circuit to operate within the effect type `F[_]` specified. Only available on pure (non-effectful) short circuits.

## Effects
This library also supports arbitrary effects `F[_]` such as Cats Effect `IO`.
For most functionality to work, your `F` needs at least a `Monad` instance from Cats.
For our examples, `IO` should work just fine.

You can use all of the same operators as the regular validators, except appended with an `F`.
For example, `check` becomes `checkF`, and `ask` becomes `askF`.
You can shorten the type signature burden on yourself significantly if you use an `FChecker`, which is the same as a `Checker` except it also fixes the `F[_]` type as well as the error type `E`.

Effectful validators can also evaluate effects in `F` and extract their values.
See `askEval`, `optionEval`, and `eitherEval` for ways to get a value of `F[_]` and evaluate it before proceeding with validation.

To lift a pure validator into an effectful one, use the `withF` operator.
It works similarly to `.lift` on  `Kleisli`, but it also ensures that the resulting validator can still accumulate errors via `EitherT`.

**N.B.** Prefer using `.withF` in cases where you still want to compose with other validators.

## Refined Support
This library optionally supports refining validators using the popular `refined` library.
To use, add the `combos-refined` dependency to your project, and `import com.rewardsnetwork.combos.refined.syntax._`.
It adds the following new operation:

* `refine[A, P]` - Ask for a value `A` and validate that it is refinable to `A Refined P`.

It also enables the following extension methods on existing validators:

* `refine[P]` - Refines the output of this existing validator with `P`. Assumes your error is of type `String`
* `refineMapLeft[P]` - Refines your output, and also lets you specify a function `String => E` to produce a custom error from this validation.
* `asValidate[P]` - Creates a refined `Validate[T, P]` instance where `T` is the output type of your validator. Can be used to provide integrations with refined including compile-time validation.

For example, assume we have the following refinement type defined using `refined`:
```scala
type PosInt = Int Refined Positive
```

We can ask for a positive integer using the following:
```scala
val askPosInt: ReturningValidator[String, Int, Int Refined Positive] = refine[Int, Positive]
```

The other syntax methods work similarly with regards to `P`, the predicate part of your refined type.