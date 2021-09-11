package com.rewardsnetwork.combos

import Checks._
import cats.effect.IO
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.Prop
import org.scalacheck.effect.PropF

class CheckerSpec extends CatsEffectSuite with ScalaCheckEffectSuite {

  test("Checker[E].ask[A] == ask[E, A]") {
    Prop.forAll { (i: Int) =>
      val checker = Checker[Unit]
      val normalAsk = syntax.ask[Unit, Int]
      val checkerAsk = checker.ask[Int]

      assert(normalAsk.run(i) == checkerAsk.run(i))
    }
  }

  test("Checker[E].check[A] == check[E, A]") {
    Prop.forAll { (badInt: Int) =>
      val checker = Checker[Boolean]
      val normalCheck = intCheck(badInt)
      val checkerCheck = checker.check[Int] {
        case i if i == badInt => false
      }

      assert(normalCheck.run(badInt) == checkerCheck.run(badInt))
    }
  }

  test("FChecker[F, E].ask[A] == askF[F, E, A]") {
    PropF.forAllF { (i: Int) =>
      val checker = FChecker[IO, Unit]
      val normalAsk = syntax.askF[IO, Unit, Int]
      val checkerAsk = checker.ask[Int]

      val normalResults = normalAsk.run(i).value
      val checkerResults = checkerAsk.run(i).value

      checkerResults.flatMap(normalResults.assertEquals(_))
    }
  }

  test("FChecker[F, E].check[A] == checkF[F, E, A]") {
    Prop.forAll { (badInt: Int) =>
      val checker = FChecker[IO, Boolean]
      val normalCheck = intCheckIO(badInt)
      val checkerCheck = checker.check[Int] {
        case i if i == badInt => IO.pure(false)
      }

      val normalResult = normalCheck.run(badInt).value.unsafeRunSync()
      val checkerResult = checkerCheck.run(badInt).value.unsafeRunSync()
      assert(normalResult == checkerResult)
    }
  }
}
