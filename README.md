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
Copyright 2021 Rewards Network Establishment Services

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
This library originates from some projects that needed to share validation logic across multiple sub-projects.
Normally, you often see validation focus on functions that take in an object with several fields, which is harder to test in isolation as you need to create a lot of test data.
You can define functions that take in smaller values, such as a single field, but wiring those together through function composition often results in a lot of boilerplate.
This library uses Cats to implement certain validation patterns that work for both failing fast with a single error, or accumulating errors, with multiple common validation scenarios supported as special syntax.

The end result is a library that allows you to separate your validation functions, data definition, and testing logic for each of your validations in an easier, more compact way than doing it strictly with functions alone can provide.

## Basic Usage
A `Validator[E, A]` is a type that validates values of type `A` and produces errors of type `E`.
As opposed to manually composing `Either` and `Validated` values together and figuring out how the types work, a `Validator` allows you to focus on individual "checks" that need to be performed, and then can be composed and "ran" later into one full `Either` value containing your errors.

To get started, `import com.rewardsnetwork.combos.syntax._` and take a look at the `check` function, which allows you to create a validator.
The way it works is simple: you provide a partial function (like `{ case x => ... }`) that returns some error value for each branch.
Anything that does not match is assumed valid, and passes through without returning an error.
To run one, just use `.run(a)` for some `A` value.

You can compose validators together just like any `Monad`, as under the hood, a `Validator` is a `cats.data.Kleisli` value.
This just means, it's a function that we've wrapped with a special data type that allows us to transform its inputs and outputs, among other things.
This means you can use checks in for-comprehensions, as they support `flatMap`, and you can also accumulate them together using `traverse` on a `List` of them.
To simplify this for yourself, look at the `checkAll` function for fail-fast validating lists of validators, and `parCheckAll` for accumulating errors.

Example usage:
```scala
case class MyCaseClass(int: Int, string: String)

//Checks that the supplied int is not equal to 5
val checkInt: Validator[Boolean, Int] = check { case 5 => false }

//Checks that the supplied string is not equal to "thing"
val checkString: Validator[Boolean, String] = check { case "thing" => false }

//Defines a validator that gets each error in  the supplied list
val checkCaseClass = parCheckAll(List[Validator[Boolean, MyCaseClass]](
  checkInt.local(_.int),
  checkString.local(_.string)
))

val badCaseClass = MyCaseClass(5, "thing")

checkCaseClass.run(badCaseClass)
// Left(NonEmptyChain(false, false)) -- both errors

checkCaseClass.failFast.run(badCaseClass)
// Left(false) -- first error only
```

As you can see in the example above, by default you will accumulate errors.
You have to explicitly opt into short-circuiting, which is the opposite of how validation usually works with libraries like Cats, using `Validated`.
Certain operations, such as `checkAll`, will return a `ShortCircuit[E, A]` which is equivalent to a `Validator[E, A]` except in that it can only return a single error value.
`parCheckAll`, on the other hand, returns another `Validator` that can be composed with other validators, and returns all possible error values.
You can transform between the two using `.failFast` on `Validator` and `.accumulate` on `ShortCircuit`.

When composing validators, you will want to change their input type with `.local`, which acts as a `map`-like function for the input value.
In this way, you can validate case classes and other data structures simply by defining a way to get from your more specific type to the field you are trying to validate.

## Checker
`Checker` is a mix-in trait that is a helpful, shorter way to define multiple validators.
Say you are validating the numerous fields of a case class, and are providing those validation functions all together in some object or package.
It would be very tedious to have to specify the error type for every single validation if they are the same, so a `Checker` solves that for you by fixing the error type.
You can `extend Checker[E]` and you will get a `check[A]` function (among others) that fixes the error type to `E`.
If you feel uneasy about extending the mix-in, you can also simply create a checker and import its values, like so:
```scala
//Without a checker:
check[String, Boolean] { case false => "can't be false" }

//You can extend Checker to simplify your syntax like so:
object BoolChecks extends Checker[String] {
  val checkFalse: Validator[String, Boolean] = check[Boolean] { case false => "can't be false" }
}

//Or create a Checker DSL object
val checker = new Checker[String]
import checker._
check[boolean] { case false => "can't be false" }
```

## Returning Values & Ask
A `Validator` and `ShortCircuit` are instances of slightly more verbose types `ReturningValidator` and `ReturningShortCircuit` respectively.
These are the similar to their non-`Returning` siblings, except they also have a known return value.
By default, all `Validator` values return `Unit`, to simplify the type signature and allow you to focus on your checks.
By using a `ReturningValidator` instead you can create chained validators that depend on the output value of a previous validator, which can sometimes be useful.

To make one of these values, use `checkReturn` instead of `check`, which returns the source input.
It can then be mapped, flatMapped, and transformed like any other monadic value.

Sometimes you will want to "ask for" a value as input to your validator, but not immediately validate it, possibly to use it as part of a more complex validation scenario.
Consider this example where you are validating a `User` which has a name and an age, and you want to validate that the user is a legal adult (over 18 years).
We can use `ask` to get the full `User`, and filter down to the specific field we want to validate before returning a validated `Adult` value:
```scala
case class User(name: String, age: Int)

case class Adult(name: String)

val askUser = ask[String, User]

val checkAge = check[String, Int] {
  case age if (age < 18) => "User is not an adult"
}

// Checks the user's age, and if it fits, return a valid Adult.
val checkUserIsAdult = askUser.flatMap { user =>
  checkAge
    .local[User](_.age)
    .as(Adult(user.name)) //Shorthand syntax for `.map(_ => Adult(user.name))` from Cats
}

checkUserIsAdult.run(User("Ryan", 18)) //Right(Adult("Ryan"))
```

In addition, there are special `option` and `either` constructors that will `ask` for a value, and if it exists, try to return it.
These are especially useful when building validations where some input is optional and you need to extract it before continuing.

```scala
import java.time.LocalDate
import cats.syntax.all._

case class UserWithDateOfBirth(name: String, age: Int, dob: LocalDate)

//Assume we may not know the user's date of birth
val maybeDob: Option[LocalDate] = LocalDate.of(2000, 4, 20).some

val exampleUser = User("Ryan", 18)

//Ask for the date of birth, and if it exists, return as UserWithDateOfBirth
val validateUserWithDob = option[String, LocalDate]("Date of birth does not exist").map { dob =>
  UserWithDateOfBirth(exampleUser.name, exampleUser.age, dob)
}

validateUserWithDob.runFailFast(maybeDob)
//Right(UserWithDateOfBirth("Ryan", 18, 2000-04-20))

validateUserWithDob.runFailFast(none)
//Left("Date of birth does not exist")
```

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
For our examples, `IO` should work just fine, but the library does not depend on Cats Effect.

You can use all of the same operators as the regular validators, except appended with an `F`.
For example, `check` becomes `checkF`, and `ask` becomes `askF`.
`askF` works similarly to how you would use `eval` operators in something like FS2, where you are just evaluating an effect and continuing with your validations.
You can shorten the type signature burden on yourself significantly if you use an `FChecker`, which is the same as a `Checker` except it also fixes the `F[_]` type as well as the error type `E`.

Effectful validators can also evaluate effects in `F` and extract their values.
See `askEval`, `optionEval`, and `eitherEval` for ways to get a value of `F[_]` and evaluate it before proceeding with validation.

To lift a pure validator into an effectful one, use the `withF` operator.
It works similarly to `.lift` on  `Kleisli`, but it also ensures that the resulting validator can still accumulate errors via `EitherT`.

**N.B.** Prefer using `.withF` in cases where you still want to compose with other validators.

Example:
```scala
import cats.effect.IO

//Lets assume this is today - we only want records from today
val today = LocalDate.of(2021, 1, 1)

//We are pulling some data from a database that looks like this
case class RawData(id: String, dateUploaded: LocalDate, bytes: Array[Byte])

//It isn't usable until it is in this shape
case class ParsedData(id: String, contents: String)

//Lets get that data, validate it, and return it using validators
val getRecentDataFromDatabase: IO[RawData] = ???
val checkDate = check[String, LocalDate] { case date if (date != today) => s"Received data is not from today ($date)" }
val parseRawData: FValidator[IO, String, RawData] = askF[IO, String, RawData].flatMap { rawData =>
  //We can use .local to map the expected input, just like on Kleisli
  checkDate
    .local[RawData](_.dateUploaded)
    .as(ParsedData(rawData.id, new String(rawData.bytes)))
}

//When ran, gives us an EitherT, which we can turn back into IO using .value
val getAndValidateData: IO[Either[String, ParsedData] = parseRawData.runFailFast(getRecentDAtaFromDatabase).value
```

## Refined Support
We fully support the [fthomas/refined](https://github.com/fthomas/refined) library for validating your data using refined predicates, as well as creating new predicates from your validations easily.
To use, add the `combos-refined` dependency to your project, and `import com.rewardsnetwork.combos.refined.syntax._`.
It adds the following new syntax for creating validators:

* `refine[A, P]` - Ask for a value `A` and validate that it is refinable to `A Refined P`. Returns the final refined value.

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
