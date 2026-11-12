[![License](https://img.shields.io/github/license/johandersson/tr-pc)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/johandersson/tr-pc/build.yml?branch=main)](https://github.com/johandersson/tr-pc/actions?query=branch%3Amain)

# ThinkingRock for Personal Computer (fork)

ThinkingRock is a desktop application for managing tasks and projects using the
Getting Things Done methodology.

> **This is a fork.** It is a modified version of
> [ThinkingRock](https://github.com/thinkingrock-gtd/tr-pc), originally
> developed by Avente Pty Ltd (Australia, <https://trgtd.com.au>) and
> maintained as open source by the ThinkingRock contributors. This fork is
> maintained by Johan Andersson and is not affiliated with or endorsed by
> Avente Pty Ltd or the upstream project.

## What's new in this fork

Main changes in 2026:

- Import calendar events from iCalendar (ICS) files, with the option to skip
  historical events
- Archive historical calendar events
- Self-contained Windows and Linux packages that need no installed Java
- Runs on Java 27 and NetBeans Platform 31
- Much faster startup and faster review tabs and calendar
- Fixes for Review Actions tabs that showed the wrong actions or lost columns

See [CHANGELOG.md](CHANGELOG.md) for all important changes.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, validation
commands, and issue and pull request guidance.

## Build requirements

- JDK 21 or newer with `JAVA_HOME` and `PATH` configured (JDK 27 is tested)
- Apache Ant 1.10.14 or newer
- Optional: [just](https://github.com/casey/just)

The build reuses a matching NetBeans Platform already registered with the IDE,
or downloads and verifies the pinned platform when no matching installation is
available.
It also restores the JavaHelp runtime omitted from the Apache binary distribution,
so Contextual Help and F1 work without installing additional libraries.
Gradle is provided by the included wrapper.
Ant and Gradle use the JDK running the build and target Java 21 bytecode.
JDK 25 or newer does not require a separate JDK 21 installation. For UI tests,
use a full desktop JDK and a graphical display, not a headless-only JDK.

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

### Self-contained Linux package

```shell
ant build-linux-self-contained
```

Output: `dist/trgtd-linux-x64.tar.gz`

This is the recommended Linux distribution. Like the Windows package it
includes a stripped Java 27 runtime, so users do **not** need to install Java.
Extract it with `tar xzf trgtd-linux-x64.tar.gz` and start:

```text
trgtd/bin/trgtd
```

The build downloads a pinned Eclipse Temurin Linux JDK, verifies its SHA-256
checksum, and runs its `jlink` with the same module list as the Windows
package. Run this target on Linux (including WSL) with `tar` and `unzip`
available.

Equivalent `just` commands are `just build-zip`,
`just build-windows-self-contained` and `just build-linux-self-contained`.

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

Downloaded NetBeans, JavaHelp, and Windows JDK archives are checksum-verified
and cached in `netbeans-plat/downloads`. A registered NetBeans installation is
copied into the local platform cache without modifying the installation.
Versions and checksums are pinned in
`nbproject/platform.properties`. Gradle dependency locks and verification
metadata are also checked into the repository.

## Authors and copyright

- **Original program:** Copyright (C) 2006 Avente Pty Ltd. Jeremy Moore of
  Avente Pty Ltd published the source code as open source.
- **Upstream open-source contributors:** Urs Joss, Radek Čáp, degrown and
  selfemp, through [thinkingrock-gtd/tr-pc](https://github.com/thinkingrock-gtd/tr-pc).
  See the Git history for their individual contributions.
- **Fork modifications:** Copyright (C) 2026 Johan Andersson. The changes are
  listed in [CHANGELOG.md](CHANGELOG.md). Source files changed in this fork
  have a `Modifications Copyright (C) 2026 Johan Andersson` line in their
  headers, and files created in this fork have a
  `Copyright (C) 2026 Johan Andersson` line. All original copyright notices
  are kept.

The About dialog (*Help > About*) shows the same attribution.

## License

ThinkingRock, including this fork and its modifications, is free software:
you can redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version. See [LICENSE](LICENSE).

This program is distributed in the hope that it will be useful, but **WITHOUT
ANY WARRANTY**; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

The JavaHelp runtime is distributed under GPL v2 with the Classpath Exception.
Its license is included as `META-INF/LICENSE.javahelp` in the packaged NetBeans
JavaHelp module.
