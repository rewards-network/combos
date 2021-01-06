package com.rewardsnetwork.combos

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers

abstract class TestingBase extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks {}
