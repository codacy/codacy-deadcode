package com.codacy.tools.deadcode

import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.codacy.plugins.api.{Options, Source}

import scala.util.Try

object Deadcode extends Tool {
  override def apply(
      source: Source.Directory,
      configuration: Option[List[Pattern.Definition]],
      filesOpt: Option[Set[Source.File]],
      options: Map[Options.Key, Options.Value]
  )(implicit specification: Tool.Specification): Try[List[Result]] = ???
}
