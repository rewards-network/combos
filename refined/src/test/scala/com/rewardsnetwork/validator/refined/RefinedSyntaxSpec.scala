package com.rewardsnetwork.combos.refined

import cats.implicits._
import com.rewardsnetwork.combos.TestingBase
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import org.scalacheck.Gen
import syntax._

class RefinedSyntaxSpec extends TestingBase {
  test("refined should refine a value") {
    forAll(Gen.posNum[Int]) { i: Int =>
      val refineInt = refine[Int, Positive]
      val result = refineInt.run(i)
      result shouldBe Refined.unsafeApply[Int, Positive](i).asRight
    }
  }
}
