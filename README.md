# Codacy Deadcode

This is the docker engine we use at Codacy to have [Deadcode](https://github.com/tsenart/deadcode) support.

You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

## Requirements

-   Java 1.8
-   SBT
-   Docker

## Usage

### Build docker

```sh
sbt universal:stage
docker build -t codacy-deadcode .
```

### Run tool

```sh
docker run -it -v $PWD:/src codacy-deadcode:latest
```

### Run tests

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

#### Validate documentation

```sh
sbt "run json codacy-deadcode:latest"
```

#### Run integration tests

```sh
sbt "run pattern codacy-deadcode:latest"
sbt "run multiple codacy-deadcode:latest"
```

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped `deadcode` tool version/commit, but also base image, orb, or dependency bumps. Follow it top to bottom.

### 1. What this repository is

This is a **Codacy engine**: a thin Scala wrapper (`src/main/scala/com/codacy/tools/deadcode/`, built on `com.codacy:codacy-engine-scala-seed`) that packages [tsenart/deadcode](https://github.com/tsenart/deadcode) (a Go dead-code detector) as a Docker image Codacy's platform can run against a customer's source code. The wrapped tool itself is Go, but **this repository's own build system is Scala/SBT**, not Go — there is no Go source in this repo, only the Dockerfile stage that `go get`s the upstream binary.

Unlike many Codacy engines, this is **not a rule-based pattern engine with a generator**. It supports a single, fixed pattern:

- `src/main/resources/docs/patterns.json` — hand-written, one pattern (`"deadcode"`, category `UnusedCode`, no parameters). There is no `DocGenerator` and no script that produces this file — it is maintained by hand.
- `src/main/resources/docs/description/description.json` + `description/deadcode.md` — hand-written title/description for the single pattern.
- `src/main/resources/docs/tool-description.md` — short hand-maintained blurb about the tool.
- `src/main/resources/docs/multiple-tests/*` — fixtures (`patterns.xml`, `results.xml`, Go source samples) used by `codacy-plugins-test` to validate the engine's output.

Because there is no generator and only one pattern, a version bump of the underlying `deadcode` tool essentially never requires regenerating `patterns.json` — only re-checking that the tool's output format/exit-code semantics (parsed in `src/main/scala/com/codacy/tools/deadcode/{Deadcode,DeadcodeExitStatus,DeadcodeResultsParser}.scala`) haven't changed upstream.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `Dockerfile` line `RUN go get -u github.com/tsenart/deadcode` | Which version of the wrapped `deadcode` tool is bundled | This uses `go get -u`, so it floats to the latest commit on the default branch at build time — there is no pinned version/tag to bump here today. If a pin is later added (e.g. `@<tag>` or a Go module with a specific version), update it here. |
| `Dockerfile` → `FROM golang:1.17.2-alpine3.14 as builder` | Go build-time base image | Bump when the Go toolchain needs updating (e.g. for a new Go language feature or security patch). |
| `Dockerfile` → `FROM amazoncorretto:8-alpine3.14-jre` | Runtime JRE base image (runs the Scala/JVM wrapper) | Bump per the pattern of prior base-image bumps (see commit `d48509b`, "bump: Update base image CY-5141", which only touched `Dockerfile`). |
| `build.sbt` → `"com.codacy" %% "codacy-engine-scala-seed" % "5.0.2"` | Codacy's Scala engine seed/framework version | Bump to pick up seed framework fixes/features. |
| `build.sbt` → `scalaVersion := "2.12.12"` | Scala compiler version | Only bump deliberately; check `codacy-engine-scala-seed` compatibility first. |
| `.circleci/config.yml` → `codacy/base@5.1.2` orb | Shared CircleCI steps (checkout, sbt build, publish, tag) | Check the latest published version of the `codacy/base` orb. |
| `.circleci/config.yml` → `codacy/plugins-test@0.15.4` orb | Runs `codacy-plugins-test` in CI | Check the latest published version of the `codacy/plugins-test` orb (see commit `f7c336f`, "bump: Bump circleci plugins test orb", which only touched `.circleci/config.yml`). |

### 3. Step-by-step update procedure

1. **Bump the version(s)** as scoped by the task (Dockerfile base images / `go get` pin, `build.sbt` dependency version, or CircleCI orb versions).
2. **There is no docs generator to run** — `patterns.json` and `description.json` are static and only need hand-editing if the pattern itself changes (it hasn't since the tool's initial implementation).
3. **Compile, format, and test** with SBT:
   ```sh
   sbt "scalafmt::test; test:scalafmt::test; sbt:scalafmt::test; test"
   ```
4. **Build the Docker image**:
   ```sh
   sbt universal:stage
   docker build -t codacy-deadcode .
   ```
5. **Run `codacy-plugins-test` locally** before pushing:
   ```sh
   sbt "run json codacy-deadcode:latest"
   sbt "run pattern codacy-deadcode:latest"
   sbt "run multiple codacy-deadcode:latest"
   ```
   These clone/use https://github.com/codacy/codacy-plugins-test conventions; follow that repo's README if the commands need a locally-checked-out copy.
6. **Iterate on failures**, re-running only the relevant test command after each fix.
7. **Commit** the version bump(s) in one change.
8. **Push and open a PR.**
9. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

- **`go get -u` pulling a breaking upstream change**: because the Dockerfile has no pin on the `deadcode` binary, a Docker rebuild can silently pick up new upstream behavior (different exit codes, different stderr format) that breaks `DeadcodeResultsParser`/`DeadcodeExitStatus`. If `codacy-plugins-test` starts failing on parsing, check whether upstream `tsenart/deadcode` changed its output format, and consider pinning the Dockerfile to a specific commit/tag instead of `-u`.
- **CircleCI orb bump commits historically only touch `.circleci/config.yml`** (see `f7c336f`) and base-image bumps historically only touch `Dockerfile` (see `d48509b`) — if your change touches more files than that for an equivalent bump, double check you haven't mixed in unrelated edits.

### 5. Definition of done

- Version bump(s) reflected in all files that encode them (`Dockerfile`, `build.sbt`, `.circleci/config.yml`, as applicable).
- `patterns.json`/`description.json` updated only if the actual pattern/rule set changed (rare for this tool).
- `sbt` format and test commands pass locally.
- Docker image builds successfully (`sbt universal:stage && docker build`).
- `codacy-plugins-test` commands (`json`, `pattern`, `multiple`) all pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

## Specification

To read more on how to build a wrapper for a tool like this one check the specification in the
[Tool Developer Guide](https://github.com/codacy/codacy-engine-scala-seed/blob/master/README.md#how-to-integrate-an-external-analysis-tool-on-codacy)

## Limitations

This tool requires the usage of [codacy-analysis-cli](https://github.com/codacy/codacy-analysis-cli) to push the results.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt,
helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

-   Identify new Static Analysis issues
-   Commit and Pull Request Analysis with GitHub, Bitbucket and GitLab
-   Auto-comments on Commits and Pull Requests
-   Integrations with Slack, Jira, YouTrack
-   Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
