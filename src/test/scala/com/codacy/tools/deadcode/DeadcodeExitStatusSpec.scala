package com.codacy.tools.deadcode

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeadcodeExitStatusSpec extends AnyWordSpec with Matchers {
  "DeadcodeExitStatus" should {
    "return SuccessNoIssues on status 0" in {
      val successNoIssues = DeadcodeExitStatus.fromExitCode(0)
      successNoIssues shouldEqual DeadcodeExitStatus.SuccessNoIssues
    }

    "return SuccessWithIssues on status 2" in {
      val successWithIssues = DeadcodeExitStatus.fromExitCode(2)
      successWithIssues shouldEqual DeadcodeExitStatus.SuccessWithIssues
    }

    "return Error on status 1" in {
      val error = DeadcodeExitStatus.fromExitCode(1)
      error shouldEqual DeadcodeExitStatus.Error
    }

    "return Error on status above 2" in {
      val error = DeadcodeExitStatus.fromExitCode(10)
      error shouldEqual DeadcodeExitStatus.Error
    }

    "return Error on status bellow 0" in {
      val error = DeadcodeExitStatus.fromExitCode(-10)
      error shouldEqual DeadcodeExitStatus.Error
    }
  }

}
