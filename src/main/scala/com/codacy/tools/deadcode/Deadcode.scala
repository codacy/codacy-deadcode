package com.codacy.tools.deadcode

import better.files._
import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.codacy.plugins.api.{Options, Source}
import com.codacy.tools.scala.seed.utils.CommandRunner

import scala.util.{Success, Try}

object Deadcode extends Tool {
  private val deadcodeCommand = "/app/deadcode"
  private val deadcodePatternId = Pattern.Id("deadcode")
  private val directoriesMaxDepth = 5

  override def apply(
      source: Source.Directory,
      configuration: Option[List[Pattern.Definition]],
      filesOpt: Option[Set[Source.File]],
      options: Map[Options.Key, Options.Value]
  )(implicit specification: Tool.Specification): Try[List[Result]] = {
    val sourcePath = File(source.path)

    val results = runTool(sourcePath)
    val filteredResults = filterResultsForFiles(results, filesOpt)

    Success(filteredResults)
  }

  private def filterResultsForFiles(results: List[Result], filesOpt: Option[Set[Source.File]]): List[Result] = {
    filesOpt.fold(results) { files =>
      results.collect {
        case res: Result.Issue if files.contains(res.file) => res
      }
    }
  }

  private def runTool(root: File): List[Result] = {
    root
      .walk(directoriesMaxDepth)
      .collect {
        case directory if directory.isDirectory =>
          runToolOnSingleDirectory(directory)
      }
      .flatten
      .toList
  }

  private def runToolOnSingleDirectory(directory: File): List[Result] = {
    val command = List(deadcodeCommand, directory.path.toString)

    CommandRunner.exec(command) match {
      case Right(resultFromTool) if resultFromTool.exitCode < 2 => List.empty
      case Right(resultFromTool) if resultFromTool.exitCode == 2 => parseResults(resultFromTool.stderr)
      case Right(resultFromTool) if resultFromTool.exitCode > 2 =>
        throw new scala.Exception(
          s"Exit code: ${resultFromTool.exitCode}\n" +
            s"stdout: ${resultFromTool.stdout}\n" +
            s"sterr: ${resultFromTool.stdout}\n"
        )
      case Left(failure) =>
        throw failure
    }
  }

  private def parseResults(results: List[String]): List[Result] = {
    results.map(parseResult)
  }

  private def parseResult(result: String): Result = {
    val Array(_, filename, line, _, message) = result.split(":")
    Result.Issue(Source.File(filename.trim), Result.Message(message.trim), deadcodePatternId, Source.Line(line.toInt))
  }
}
