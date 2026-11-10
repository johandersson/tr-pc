# Contributing

Thank you for helping improve ThinkingRock. Please follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Where to contribute

This repository is the [johandersson/tr-pc fork](https://github.com/johandersson/tr-pc),
not the [upstream project](https://github.com/thinkingrock-gtd/tr-pc). Open issues
and pull requests against the repository you intend to change. When referring
to an upstream issue here, use its full URL or `thinkingrock-gtd/tr-pc#number`.

Before starting, check existing issues and pull requests. Discuss large changes
first. Bug reports should include the application version, operating system,
Java version, reproduction steps, expected behavior, and actual behavior.
Remove private task information and credentials from logs and examples.

## Development setup

1. Fork and clone the repository, then create a branch from `main`.
2. Install JDK 21 or newer and Apache Ant 1.10.14 or newer. Set `JAVA_HOME`
   to that JDK. See [build requirements](README.md#build-requirements).
3. Build with `ant build-zip`. The build prepares the pinned NetBeans
   Platform automatically; installing the NetBeans IDE is optional.
4. Run a development instance with `ant run-dev` (or `just run`).
   Use a copy of your data for manual testing, not your working task file.

Source is under `modules/<module>/src`; tests are under
`modules/<module>/test/unit/src`. Ant/NetBeans metadata lives in each module's
`nbproject` directory. Gradle uses `tests.gradle.kts` for module test builds.
Keep both build paths working. Avoid running Ant and Gradle simultaneously:
they share module build directories.

## Making a change

- Keep changes focused and follow existing Java, Swing, and localization patterns.
- Add regression tests for fixes and preserve unrelated behavior.
- Keep NetBeans `.form` metadata in sync when changing generated Swing layouts.
- Update relevant help and [CHANGELOG.md](CHANGELOG.md) for user-facing changes.
- Preserve copyright notices and the [GPL-3.0-or-later license](LICENSE).
- Do not commit generated packages, personal data files, IDE caches, or secrets.

Dependency changes must keep Ant dependencies, Gradle declarations, dependency
locks, and checksum verification metadata consistent. Do not disable dependency
verification to make a build pass.

## Validation

Use the checked-in Gradle wrapper; a separate Gradle installation is unnecessary.
For a focused change, run the affected module's tests, for example:

```shell
./gradlew -p gradle :au.com.trgtd.tr.view:test
```

Before submitting, run the headless checks and the application build sequentially:

```shell
git diff --check
./gradlew -p gradle check
ant build-zip
```

On Windows, replace `./gradlew` with `gradlew.bat`. GUI tests need a full desktop
JDK and a graphical display; run `./gradlew -p gradle check -Dtest.profile=allTests`
and check that the relevant GUI tests were not skipped. Manually exercise the
changed workflow as well. See [tests](README.md#tests) and
[distribution builds](README.md#build-distributions) for other commands.

## Pull requests

Open a pull request against `main` in the intended repository. Explain the
problem, the solution, linked issues, and the validation you performed (including
anything skipped or unavailable). Include screenshots for visual changes when
useful, without exposing personal task data. Keep unrelated cleanup separate
and respond to review feedback with focused changes.
