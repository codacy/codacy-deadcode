package com.codacy.tools.deadcode

sealed abstract class DeadcodeExitStatus

object DeadcodeExitStatus extends Enumeration {
  final case object SuccessNoIssues extends DeadcodeExitStatus
  final case object SuccessWithIssues extends DeadcodeExitStatus
  final case object Error extends DeadcodeExitStatus

  def fromExitCode(code: Int): DeadcodeExitStatus = code match {
    case 0 => SuccessNoIssues
    case 2 => SuccessWithIssues // deadcode returns exit code 2 when there are results
    case _ => Error
  }
}
