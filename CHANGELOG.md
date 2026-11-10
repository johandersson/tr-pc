# Changelog

This changelog covers the changes made in this fork of
[ThinkingRock](https://github.com/thinkingrock-gtd/tr-pc). It lists only
important changes and new features. Changes from the original project are not
listed here; see the [upstream repository](https://github.com/thinkingrock-gtd/tr-pc)
for its history.

All changes listed below are Copyright (C) 2026 Johan Andersson and are
licensed under the GNU General Public License v3.0 or later, like the rest of
ThinkingRock. See [LICENSE](LICENSE).

## 2026

### New features

- **Calendar import (iCalendar / ICS).** *File > Import* converts ICS
  calendar events into scheduled actions. It supports time zones, durations,
  recurring events, cancellations, multi-day events and duplicate detection.
- **Choose whether to import historical events.** When an ICS file contains
  events before today, a dialog lets you import them, skip them or cancel.
- **Archive historical calendar events.** The Archive dialog has a new option
  that archives scheduled actions dated before the archive date, whether or
  not they are done. This includes actions in unfinished projects.
- **Self-contained Windows package** (`dist/trgtd-windows-x64.zip`). It
  bundles a stripped Java runtime, so Java does not need to be installed.
  The Windows launchers are branded and no longer depend on registry-based
  Java detection.
- **Self-contained Linux package** (`dist/trgtd-linux-x64.tar.gz`). It
  bundles a stripped Java runtime, so Java does not need to be installed.

### Improvements

- **Runs on Java 27 and NetBeans Platform 31.** Unused NetBeans Platform
  modules were removed.
- **Much faster startup**, especially on Windows. Previously the module jars
  were repeatedly closed and reopened during startup, and the runtime was
  scanned more than necessary.
- **Faster review tabs and calendar.** Tab switching avoids redundant
  refreshes. Hidden calendar views no longer refresh in the background, and
  scheduled events are looked up by day instead of rescanning every action.
- **Contextual help (F1) works again.** The JavaHelp runtime, which the
  Apache NetBeans binaries omit, is restored.
- **No startup pop-up from the original ThinkingRock web server.** The
  automatic check for upstream versions and website messages is disabled,
  because it does not apply to this fork.
- **Reproducible, verified builds.** All downloaded build inputs (NetBeans,
  JavaHelp, JDKs and Gradle dependencies) are pinned and checksum-verified.
  The Ant build works without the NetBeans IDE.

### Bug fixes

- Review Actions tabs (Do ASAP, Done and others) showed actions that should
  have been filtered out, and their counts changed after loading. They now
  show the correct actions from the start.
- Review Actions tabs could lose their columns, checkboxes or column widths
  after a restart.
- Importing a calendar could corrupt the data file.
- ICS files using the `ACKNOWLEDGED` alarm property or the `FILENAME`
  parameter failed to import.
