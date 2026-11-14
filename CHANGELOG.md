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

- **Quick Add Next Action on the dashboard.** An embedded lower-right panel
  creates single Do ASAP actions with Add or Ctrl+S, not inbox thoughts.
  Put an existing context anywhere in the text (for example,
  `Buy groceries @Errands`); context names are case-insensitive and can contain
  spaces. Without a context, the action uses None. Unknown or conflicting
  contexts show an inline message and keep the draft. Dashboard refreshes
  leave unfinished drafts intact. Quick Add also requests native window focus
  when startup focus is not yet available, fixing initial capture focus on Windows.
- Project choosers now search current and future project names, including
  collapsed sub-projects, and adapt their initial size to project titles,
  the owner window and available screen space.
- Restored the View menu with application log and toolbar controls, without
  unrelated editor and split-window commands.
- **Dashboard Waiting For and Someday/Maybe sections.** Waiting For includes
  available Waiting For context actions and delegated actions, even without
  a follow-up date. Someday/Maybe reads its existing records directly and
  links to that screen without relying on contexts.
- **Starter GTD contexts.** New and existing data files receive missing Calls,
  Computer, Office, Home, Errands, Online, Anywhere, Agendas, Read/Review and
  Waiting For contexts. Existing names (including case and @ prefix variants)
  are preserved.
- **Dashboard Calls section.** Always shows all unfinished Calls actions, due dates
  first, with clickable phone numbers found in titles or notes. Numbers use
  standard `tel:` links and the operating system's configured calling app.
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

- Quick Add Next Action receives keyboard focus when the dashboard activates,
  including on startup, so capture can begin immediately.
- **Next actions on the dashboard.** Shows the first 10 available Do ASAP
  actions, sorted by context and then earliest due date, with undated actions
  last within each context. *Show all* expands the list in place, like every
  other limited dashboard list; each row still opens its action.
- **Dashboard auto-refresh.** Visible dashboards update after thought capture
  and project/action changes, with rapid updates batched together and no polling.
  Returning to the dashboard refreshes it again.
- **Ctrl+S in Add/Edit Thought.** The shortcut performs the same action as the
  Add/OK button, including validation; Add keeps the capture window open.
- **Runs on Java 27 and NetBeans Platform 31.** Unused NetBeans Platform
  modules were removed.
- **Much faster startup**, especially on Windows. Previously the module jars
  were repeatedly closed and reopened during startup, and the runtime was
  scanned more than necessary.
- **Faster review tabs and calendar.** Tab switching avoids redundant
  refreshes. Hidden calendar views no longer refresh in the background, and
  scheduled events are looked up by day instead of rescanning every action.
- **Faster Actions screens and saving.** Filters are re-applied in one pass
  after an action change instead of once per filter. Returning to an Actions
  tab applies only added or removed actions instead of rebuilding the whole
  list. Manual save (Ctrl+S) writes the data file in the background so the UI
  no longer freezes on large files.
- **Contextual help (F1) works again.** The JavaHelp runtime, which the
  Apache NetBeans binaries omit, is restored.
- **Ctrl+Shift+N captures a thought** from anywhere in the application, as
  an alternative to F6.
- **No startup pop-up from the original ThinkingRock web server.** The
  automatic check for upstream versions and website messages is disabled,
  because it does not apply to this fork.
- **Reproducible, verified builds.** All downloaded build inputs (NetBeans,
  JavaHelp, JDKs and Gradle dependencies) are pinned and checksum-verified.
  The Ant build works without the NetBeans IDE.

### Bug fixes

- Linux startup maximizes and validates the main window after the window system
  is ready, rather than requiring a manual resize to reveal its contents.
- Dashboard attention links now scroll the target section toward the top of
  the visible list so its entries, not just its heading, are shown.
- Standalone Linux packaging no longer invokes Windows icon-branding tools
  under WSL, so a Windows interoperability outage cannot block the Linux build.
- Someday/Maybe Add and Edit dialogs now support Ctrl+S, with the shortcut
  shown on their Add and OK buttons, matching the Thoughts dialog.
- Calls are listed separately from Next actions without a ten-call limit,
  including inactive and future-start calls. Numbers in notes are shown as
  separate clickable telephone links.
- Individual Next actions dashboard entries now open Actions / Do ASAP,
  matching the full-list link, instead of opening Projects.
- The dashboard's Do ASAP link now selects the Do ASAP tab instead of the
  first Review Actions tab.
- Normalized the Ctrl+Shift+N shortcut registration and added coverage for
  both thought-capture shortcuts.
- Gradle builds use the current JDK (21 or newer) instead of requiring a
  separate JDK 21 installation, while retaining Java 21 compatibility.
- Self-contained packaging recognizes cached modern JDKs without a `jmods`
  directory, avoiding unnecessary re-extraction of the build's own JDK.
- Review Actions tabs (Do ASAP, Done and others) showed actions that should
  have been filtered out, and their counts changed after loading. They now
  show the correct actions from the start.
- Review Actions tabs could lose their columns, checkboxes or column widths
  after a restart.
- Importing a calendar could corrupt the data file.
- ICS files using the `ACKNOWLEDGED` alarm property or the `FILENAME`
  parameter failed to import.
