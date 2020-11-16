import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := "codacy-deadcode"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-engine-scala-seed" % "5.0.2",
  "com.github.pathikrit" %% "better-files" % "3.9.1"
).map(_.withSources())

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % Test

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

mappings in Universal ++= {
  (resourceDirectory in Compile).map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

Universal / javaOptions ++= Seq("-XX:MinRAMPercentage=60.0", "-XX:MaxRAMPercentage=95.0")

val dockerUser = "docker"
val dockerGroup = "docker"

Docker / daemonUser := dockerUser
Docker / daemonGroup := dockerGroup

dockerBaseImage := "deadcode-base"

mainClass in Compile := Some("com.codacy.tools.deadcode.Engine")

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    List(
      Cmd("RUN", s"adduser -u 2004 -D $dockerUser"),
      cmd,
      Cmd("RUN", "mv /opt/docker/docs /docs"),
      ExecCmd("RUN", Seq("chown", "-R", s"$dockerUser:$dockerGroup", "/docs"): _*)
    )
  case other => List(other)
}
