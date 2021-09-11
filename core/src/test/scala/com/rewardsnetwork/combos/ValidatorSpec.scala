package com.rewardsnetwork.combos

import cats.data._
import cats.effect.IO
import cats.implicits._
import com.rewardsnetwork.combos.syntax._
import Checks._
import org.scalacheck.Prop
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.effect.PropF

class ValidatorSpec extends CatsEffectSuite with ScalaCheckEffectSuite {

  test("ask should return its input") {
    Prop.forAll { (i: Int) =>
      val result = ask[Unit, Int].run(i)
      assert(result == i.rightNec)
    }
  }

  test("askF should return its input wrapped in F") {
    PropF.forAllF { (i: Int) =>
      val resultIO = askF[IO, Unit, Int].run(i).value
      resultIO.assertEquals(i.rightNec)
    }
  }

  test("askEval should evaluate an input effect and return the result") {
    PropF.forAllF { (i: Int) =>
      val resultIO = askEval[IO, Unit, Int].run(i.pure[IO]).value
      resultIO.assertEquals(i.asRight)
    }
  }

  test("check should apply test to input") {
    Prop.forAll { (badInt: Int) =>
      val checkInt = intCheck(badInt)
      assert(checkInt(badInt) == false.leftNec)
      assert(checkInt(badInt - 1) == ().rightNec)
    }
  }

  test("checkEval should apply test to input") {
    PropF.forAllF { (badInt: Int) =>
      val checkInt = intCheckIO(badInt)
      checkInt.run(badInt).value.assertEquals(false.leftNec) >>
        checkInt.run(badInt - 1).value.assertEquals(().rightNec)
    }
  }

  test("checkAll should fail fast, parCheckAll should accumulate") {
    Prop.forAll { (badInt: Int) =>
      val checkInt = intCheck(badInt)
      val checks = List(checkInt, checkInt)
      val checkFast = checkAll(checks)
      val checkAccumulating = parCheckAll(checks)

      assert(checkFast.run(badInt) == false.asLeft)
      assert(checkAccumulating.run(badInt) == NonEmptyChain(false, false).asLeft)
    }
  }

  test("checkAllF should fail fast, parCheckAllF should accumulate") {
    PropF.forAllF { (badInt: Int) =>
      val checkInt = intCheckIO(badInt)
      val checks = List(checkInt, checkInt)
      val checkFast = checkAllF(checks)
      val checkAccumulating = parCheckAllF(checks)

      checkFast.run(badInt).value.assertEquals(false.asLeft) >>
        checkAccumulating.run(badInt).value.assertEquals(NonEmptyChain(false, false).asLeft)
    }
  }

  test("option should extract the optional value") {
    Prop.forAll { (i: Int) =>
      val getInt = option[Unit, Int](())
      assert(getInt.run(i.some) == i.asRight)
      assert(getInt.run(none) == ().leftNec)
    }
  }

  test("optionF should lift the result to F[A]") {
    PropF.forAllF { (i: Int) =>
      val getIntF = optionF[IO, Unit, Int](())
      getIntF.run(i.some).value.assertEquals(i.asRight) >>
        getIntF.run(none).value.assertEquals(().leftNec)
    }
  }

  test("optionEval should evaluate an input effect and return the result") {
    PropF.forAllF { (i: Int) =>
      val resultIO = optionEval[IO, Unit, Int](()).run(i.some.pure[IO]).value
      resultIO.assertEquals(i.asRight)
    }
  }

  test("either should extract the right-side value") {
    Prop.forAll { (i: Int) =>
      val getInt = either[Unit, Int]
      assert(getInt.run(i.asRight) == i.asRight)
      assert(getInt.run(().asLeft) == ().leftNec)
    }
  }

  test("eitherF should lift the right-side result to F[A]") {
    PropF.forAllF { (i: Int) =>
      val getIntF = eitherF[IO, Unit, Int]
      getIntF.run(i.asRight).value.assertEquals(i.asRight)
      getIntF.run(().asLeft).value.assertEquals(().leftNec)
    }
  }

  test("eitherEval should evaluate an input effect and return the result") {
    PropF.forAllF { (i: Int) =>
      val resultIO = eitherEval[IO, Unit, Int].run(i.asRight.pure[IO]).value
      resultIO.assertEquals(i.asRight)
    }
  }
}
