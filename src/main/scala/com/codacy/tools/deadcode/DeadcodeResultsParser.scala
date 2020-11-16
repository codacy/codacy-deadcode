package com.codacy.tools.deadcode

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.results.{Pattern, Result}

import scala.util.{Success, Try}

object DeadcodeResultsParser {

  def parse(results: List[String]): List[Result] = {
    results.map(parseSingleResult).collect { case Success(issue) => issue }
  }

  def parseSingleResult(result: String): Try[Result] = {
    Try {
      val Array(deadcodePattern, filename, line, _, message) = result.split(":")
      Result.Issue(
        Source.File(filename.trim),
        Result.Message(message.trim),
        Pattern.Id(deadcodePattern),
        Source.Line(line.toInt)
      )
    }
  }
}
