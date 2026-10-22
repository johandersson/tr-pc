[![License](https://img.shields.io/github/license/thinkingrock-gtd/tr-pc)](https://github.com/thinkingrock-gtd/tr-pc/blob/master/LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/thinkingrock-gtd/tr-pc/build.yml?branch=main)](https://github.com/thinkingrock-gtd/tr-pc/actions?query=workflow%3AJavaCI+branch%3Amain)

## ThinkingRock for Personal Computer (i.e. any Desktop or Laptop computer)


## License

GNU General Public License v3.0 or later

See [LICENSE](https://github.com/thinkingrock-gtd/tr-pc/blob/master/LICENSE) to see the full text.

## How to build a project
Ant builds and packages the application; Gradle runs the unit tests.
Neither requires opening or installing the NetBeans IDE.

### Warning
Before starting ThinkingRock from sources, ensure you have a backup of your production data.
We haven't yet figured out the configuration to cleanly separate the dev environment from the production configuration or data.
This includes ReviewActions.xml as well.
This issue will be solved in #33.

### Compilation from a Terminal (without Netbeans)

#### Preconditions
- Install **JDK 17** (not just a JRE), and set `JAVA_HOME` and `PATH` to it.
- Install [Apache Ant](https://ant.apache.org/manual/install.html) **1.10.14 or newer**.
- Optional: [just](https://github.com/casey/just) is installed
- Check `java -version` and `ant -version` before building. The build reports an
  actionable error for unsupported tools.

Java, the minimum Ant version, and NetBeans Platform **25** are configured in
`nbproject/platform.properties`. Ant and Gradle both read this file; Gradle derives
the matching NetBeans API release from the platform version. The Gradle wrapper
pins Gradle itself, so a separate Gradle installation is unnecessary.

#### Run TR directly from sources
- Run `ant run-dev`, or `just run`. This builds first and then launches the
  application through the calendar module. Back up production data first.

### Build artefacts and run

1. Open a terminal in the root of the repository.
2. Run `ant build-zip`, or `ant clean build-zip` for a clean rebuild.
   With `just` installed, use `just build-zip`.
3. Find `trgtd.zip` in `dist` and extract it.
4. In the extracted application folder, run the launcher in `bin` for your OS:
   `trgtd.exe` on Windows, `trgtd` on Linux, or `trgtd` on macOS.

On Windows, the executable is `.../bin/trgtd.exe`. It is the
application launcher, not a standalone executable; keep the extracted
application folders together when running it.

#### Platform downloads and reproducibility

The first build downloads the fixed Apache NetBeans binary ZIP and its official
SHA-512 checksum over HTTPS from the Apache archive. The checksum is verified
before extracting only the harness, platform, and IDE clusters into the
repository-local `netbeans-plat/25` directory. No IDE settings or update center
are needed. Root Ant builds load repository platform settings before NetBeans
private settings, so a previously selected IDE platform cannot silently replace
them.

Downloads are cached in `netbeans-plat/downloads`; subsequent builds reuse the
prepared platform without network access. An existing platform from the old
update-center bootstrap is replaced on the first build. To re-verify and
re-extract the cached distribution, delete `netbeans-plat/25/.verified-distribution`.
For a checksum error, delete the ZIP and `.sha512` file named in the error, then
retry. For download errors, check access to `https://archive.apache.org`.
The checksum protects against corrupt downloads; it is fetched from the same
trusted HTTPS source, not an independently signed or repository-pinned digest.

#### Tests without renaming files

- Run `./gradlew -p gradle check` on Linux/macOS, or
  `gradlew.bat -p gradle check` on Windows.
- Optional shortcuts: `just verify-headless` or `just verify-ci`.
- Include UI tests with `./gradlew -p gradle check -Dtest.profile=allTests`
  (requires a graphical display), or `just verify`.

Gradle's settings live in `gradle/settings.gradle.kts` and use permanently named
`tests.gradle.kts` build files, leaving the root project recognizable as an Ant
project to NetBeans. There are no enable/disable or rename steps, including when
tests fail. The first test run needs access to the Gradle and Maven repositories.

CI uses the configured JDK, runs the headless tests, builds a clean Ant release
ZIP, and uploads it as an artifact.
Generated Kotlin state in `.kotlin/` is ignored, so command-line builds do not
leave that local build state as an untracked repository change.

### Run From NetBeans

#### Precondition

- [Netbeans](https://netbeans.apache.org/download/index.html) is installed
- You have opened the tr-pc project in Netbeans

#### Run
- Open Netbeans and open the project
- Menu File -> Open Project. Navigate to the local git clone of tr-pc.\
![Project](/docs/images/readme_project.png)
- Open the node *Module*, choose one of the TR-Modules (e.g. *TR Calendar*), right-click and select *Open Project*
- Right-click the now open module, and select Run\
![Project run](/docs/images/readme_run.png)
> Note: To our current knowledge, running Ant target run on the main project "Thinking Rock" will not launch the tool. Hence the workaround with running it from e.g. the "TR Calendar" project.

> Run `ant build` from a terminal once before running individual modules in the
> IDE, to prepare the repository-local platform. Terminal builds do not require
> opening the project in the IDE first.
