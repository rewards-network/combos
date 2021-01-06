package com.rewardsnetwork.combos

import cats.effect.IO
import com.rewardsnetwork.combos.syntax._

object Checks {
  def intCheck(badInt: Int) = check[Boolean, Int] {
    case i if i == badInt => false
  }
  def intCheckIO(badInt: Int) = checkEval[IO, Boolean, Int] {
    case i if i == badInt => IO.pure(false)
  }
}
