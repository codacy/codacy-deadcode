package com.codacy.tools.deadcode

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.results.{Pattern, Result}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeadcodeResultsParserSpec extends AnyWordSpec with Matchers {
  "DeadcodeResultsParser" should {
    "return empty list of results" in {
      val listOfResultsAsString = List()
      val results = DeadcodeResultsParser.parse(listOfResultsAsString)
      results shouldBe List()
    }

    "return list of issues" in {
      val listOfResultsAsString =
        List("deadcode: main.go:11:1: example is unused", "deadcode: dir/main.go:12:1: example2 is unused")
      val resultsList = List(
        Result
          .Issue(Source.File("main.go"), Result.Message("example is unused"), Pattern.Id("deadcode"), Source.Line(11)),
        Result.Issue(
          Source.File("dir/main.go"),
          Result.Message("example2 is unused"),
          Pattern.Id("deadcode"),
          Source.Line(12)
        ),
      )

      val results = DeadcodeResultsParser.parse(listOfResultsAsString)

      results shouldBe resultsList
    }

    "return list of issues and ignore invalid" in {
      val listOfResultsAsString = List(
        "deadcode: main.go:11:1: example is unused",
        "deadcode: dir/main.go:12:1: example2 is unused",
        "this is invalid"
      )
      val resultsList = List(
        Result
          .Issue(Source.File("main.go"), Result.Message("example is unused"), Pattern.Id("deadcode"), Source.Line(11)),
        Result.Issue(
          Source.File("dir/main.go"),
          Result.Message("example2 is unused"),
          Pattern.Id("deadcode"),
          Source.Line(12)
        ),
      )

      val results = DeadcodeResultsParser.parse(listOfResultsAsString)

      results shouldBe resultsList
    }
  }

}
