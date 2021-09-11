package com.rewardsnetwork.combos.refined

import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import org.scalacheck.Gen
import syntax._
import munit.ScalaCheckSuite
import org.scalacheck.Prop

class RefinedSyntaxSpec extends ScalaCheckSuite {
  property("refined should refine a value") {
    Prop.forAll(Gen.posNum[Int]) { (i: Int) =>
      val refineInt = refine[Int, Positive]
      val result = refineInt.run(i)
      assert(result == Refined.unsafeApply[Int, Positive](i).asRight)
    }
  }
}
