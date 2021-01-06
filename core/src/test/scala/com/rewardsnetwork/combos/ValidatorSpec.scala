package com.rewardsnetwork.combos

import cats.data._
import cats.effect.IO
import cats.implicits._
import com.rewardsnetwork.combos.syntax._
import Checks._

class ValidatorSpec extends TestingBase {

  test("ask should return its input") {
    forAll { i: Int =>
      val result = ask[Unit, Int].run(i)
      result shouldBe i.rightNec
    }
  }

  test("askF should return its input wrapped in F") {
    forAll { i: Int =>
      val resultIO = askF[IO, Unit, Int].run(i).value
      resultIO.unsafeRunSync() shouldBe i.rightNec
    }
  }

  test("askEval should evaluate an input effect and return the result") {
    forAll { i: Int =>
      val resultIO = askEval[IO, Unit, Int].run(i.pure[IO]).value
      resultIO.unsafeRunSync() shouldBe i.asRight
    }
  }

  test("check should apply test to input") {
    forAll { badInt: Int =>
      val checkInt = intCheck(badInt)
      checkInt.run(badInt) shouldBe false.leftNec
      checkInt.run(badInt - 1) shouldBe ().rightNec
    }
  }

  test("checkEval should apply test to input") {
    forAll { badInt: Int =>
      val checkInt = intCheckIO(badInt)
      checkInt.run(badInt).value.unsafeRunSync() shouldBe false.leftNec
      checkInt.run(badInt - 1).value.unsafeRunSync() shouldBe ().rightNec
    }
  }

  test("checkAll should fail fast, parCheckAll should accumulate") {
    forAll { badInt: Int =>
      val checkInt = intCheck(badInt)
      val checks = List(checkInt, checkInt)
      val checkFast = checkAll(checks)
      val checkAccumulating = parCheckAll(checks)

      checkFast.run(badInt) shouldBe false.asLeft
      checkAccumulating.run(badInt) shouldBe NonEmptyChain(false, false).asLeft
    }
  }

  test("checkAllF should fail fast, parCheckAllF should accumulate") {
    forAll { badInt: Int =>
      val checkInt = intCheckIO(badInt)
      val checks = List(checkInt, checkInt)
      val checkFast = checkAllF(checks)
      val checkAccumulating = parCheckAllF(checks)

      checkFast.run(badInt).value.unsafeRunSync() shouldBe false.asLeft
      checkAccumulating.run(badInt).value.unsafeRunSync() shouldBe NonEmptyChain(false, false).asLeft
    }
  }

  test("option should extract the optional value") {
    forAll { i: Int =>
      val getInt = option[Unit, Int](())
      getInt.run(i.some) shouldBe i.asRight
      getInt.run(none) shouldBe ().leftNec
    }
  }

  test("optionF should lift the result to F[A]") {
    forAll { i: Int =>
      val getIntF = optionF[IO, Unit, Int](())
      getIntF.run(i.some).value.unsafeRunSync() shouldBe i.asRight
      getIntF.run(none).value.unsafeRunSync() shouldBe ().leftNec
    }
  }

  test("optionEval should evaluate an input effect and return the result") {
    forAll { i: Int =>
      val resultIO = optionEval[IO, Unit, Int](()).run(i.some.pure[IO]).value
      resultIO.unsafeRunSync() shouldBe i.asRight
    }
  }

  test("either should extract the right-side value") {
    forAll { i: Int =>
      val getInt = either[Unit, Int]
      getInt.run(i.asRight) shouldBe i.asRight
      getInt.run(().asLeft) shouldBe ().leftNec
    }
  }

  test("eitherF should lift the right-side result to F[A]") {
    forAll { i: Int =>
      val getIntF = eitherF[IO, Unit, Int]
      getIntF.run(i.asRight).value.unsafeRunSync() shouldBe i.asRight
      getIntF.run(().asLeft).value.unsafeRunSync() shouldBe ().leftNec
    }
  }

  test("eitherEval should evaluate an input effect and return the result") {
    forAll { i: Int =>
      val resultIO = eitherEval[IO, Unit, Int].run(i.asRight.pure[IO]).value
      resultIO.unsafeRunSync() shouldBe i.asRight
    }
  }
}
