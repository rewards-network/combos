package com.rewardsnetwork.combos

import Checks._
import cats.effect.IO

class CheckerSpec extends TestingBase {

  test("Checker[E].ask[A] == ask[E, A]") {
    forAll { i: Int =>
      val checker = Checker[Unit]
      val normalAsk = syntax.ask[Unit, Int]
      val checkerAsk = checker.ask[Int]

      normalAsk.run(i) shouldBe checkerAsk.run(i)
    }
  }

  test("Checker[E].check[A] == check[E, A]") {
    forAll { badInt: Int =>
      val checker = Checker[Boolean]
      val normalCheck = intCheck(badInt)
      val checkerCheck = checker.check[Int] {
        case i if i == badInt => false
      }

      normalCheck.run(badInt) shouldBe checkerCheck.run(badInt)
    }
  }

  test("FChecker[F, E].ask[A] == askF[F, E, A]") {
    forAll { i: Int =>
      val checker = FChecker[IO, Unit]
      val normalAsk = syntax.askF[IO, Unit, Int]
      val checkerAsk = checker.ask[Int]

      val normalResults = normalAsk.run(i).value.unsafeRunSync()
      val checkerResults = checkerAsk.run(i).value.unsafeRunSync()

      normalResults shouldBe checkerResults
    }
  }

  test("FChecker[F, E].check[A] == checkF[F, E, A]") {
    forAll { badInt: Int =>
      val checker = FChecker[IO, Boolean]
      val normalCheck = intCheckIO(badInt)
      val checkerCheck = checker.check[Int] {
        case i if i == badInt => IO.pure(false)
      }

      val normalResult = normalCheck.run(badInt).value.unsafeRunSync()
      val checkerResult = checkerCheck.run(badInt).value.unsafeRunSync()
      normalResult shouldBe checkerResult
    }
  }
}
