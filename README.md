[![License](https://img.shields.io/github/license/thinkingrock-gtd/tr-pc)](https://github.com/thinkingrock-gtd/tr-pc/blob/master/LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/thinkingrock-gtd/tr-pc/build.yml?branch=main)](https://github.com/thinkingrock-gtd/tr-pc/actions?query=workflow%3AJavaCI+branch%3Amain)

# ThinkingRock for Personal Computer

ThinkingRock is a desktop application for managing tasks and projects using the
Getting Things Done methodology.

## Build requirements

- JDK 21 or newer with `JAVA_HOME` and `PATH` configured (JDK 27 is tested)
- Apache Ant 1.10.14 or newer
- Optional: [just](https://github.com/casey/just)

The build downloads and verifies the pinned NetBeans Platform automatically.
Gradle is provided by the included wrapper.

## Build distributions

### Standard package

```shell
ant build-zip
```

Output: `dist/trgtd.zip`

This package requires Java on the target computer. After extracting it, start:

- Windows x64: `trgtd\bin\trgtd.cmd`
- Linux/macOS: `trgtd/bin/trgtd`

On Windows, `trgtd.cmd` locates Java from `JAVA_HOME` or `PATH` and passes it to
the native launcher.

### Self-contained Windows package

```shell
ant build-windows-self-contained
```

Output: `dist/trgtd-windows-x64.zip`

This is the recommended Windows distribution. It includes a stripped Java 27
runtime, so users do **not** need to install Java. Distribute the complete ZIP,
extract it, and start:

```text
trgtd\bin\trgtd.cmd
```

The build downloads a pinned Eclipse Temurin Windows JDK, verifies its SHA-256
checksum, and uses its native `jlink.exe` through PowerShell to include only
the required Java modules. Run this target from Windows or WSL with PowerShell
available.

Equivalent `just` commands are `just build-zip` and
`just build-windows-self-contained`.

## Tests

Run headless tests:

```shell
./gradlew -p gradle check
```

On Windows:

```powershell
gradlew.bat -p gradle check
```

Run all tests, including UI tests that require a graphical display:

```shell
./gradlew -p gradle check -Dtest.profile=allTests
```

Optional shortcuts are `just verify-headless`, `just verify-ci`, and
`just verify`.

## Reproducibility

Downloaded NetBeans and Windows JDK archives are checksum-verified and cached in
`netbeans-plat/downloads`. Versions and checksums are pinned in
`nbproject/platform.properties`. Gradle dependency locks and verification
metadata are also checked into the repository.

## License

ThinkingRock is licensed under the GNU General Public License v3.0 or later.
See [LICENSE](LICENSE).
