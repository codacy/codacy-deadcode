package com.codacy.tools.deadcode

import com.codacy.tools.scala.seed.DockerEngine

object Engine extends DockerEngine(Deadcode)()
