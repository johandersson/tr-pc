/*
 * ThinkingRock, a project management tool for Personal Computers.
 * Copyright (C) 2026 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package au.com.trgtd.tr.view.overview.dashboard;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.openide.util.NbBundle;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.action.ActionStateDelegated;
import tr.model.action.ActionStateInactive;
import tr.model.action.ActionStateScheduled;
import tr.model.future.Future;
import tr.model.project.Project;
import tr.model.thought.Thought;
import static org.junit.Assert.*;

public class DashboardPanelTest {

    @Test
    public void callsSectionIsVisibleWithoutCalls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                DataLookup.instance().setData(new Data());
                DashboardPanel panel = new DashboardPanel();
                panel.refresh();
                for (String key : List.of("dashboard.calls", "dashboard.calls.none")) {
                    String text = NbBundle.getMessage(DashboardPanel.class, key);
                    assertTrue(key, List.of(panel.getComponents()).stream()
                            .anyMatch(c -> c instanceof JLabel label && label.getText().contains(text)));
                }
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void allCallsAreRenderedOnlyInCallsWithNoteOnlyPhoneLinks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                var context = data.getContextManager().list().stream()
                        .filter(c -> c.hasName("Calls")).findFirst().orElseThrow();
                for (int i = 0; i < 15; i++) {
                    Action call = new Action(data);
                    call.setContext(context);
                    call.setDescription("Phone-row-" + i);
                    call.setNotes("+46 70 123 45 67");
                    data.getRootActions().add(call);
                }
                DataLookup.instance().setData(data);
                List<URI> dialed = new ArrayList<>();
                DashboardPanel panel = new DashboardPanel(() -> {}, dialed::add);
                panel.refresh();
                boolean inCalls = false;
                int rows = 0;
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label) {
                        if (label.getText().equals(NbBundle.getMessage(
                                DashboardPanel.class, "dashboard.calls") + " (15)")) {
                            inCalls = true;
                        }
                        if (label.getText().contains("Phone-row-")) {
                            assertTrue("Calls must not appear in Next actions", inCalls);
                            rows++;
                        }
                        if (label.getText().contains("&nbsp;&nbsp;+46 70 123 45 67")) {
                            assertTrue(inCalls);
                            click(label);
                        }
                    }
                }
                assertEquals(15, rows);
                assertEquals(15, dialed.size());
                assertTrue(dialed.stream().allMatch(URI.create("tel:+46701234567")::equals));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void waitingAndSomedaySectionsShowEmptyMessages() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                DataLookup.instance().setData(new Data());
                DashboardPanel panel = new DashboardPanel();
                panel.refresh();
                for (String key : List.of("dashboard.waiting", "dashboard.waiting.none",
                        "dashboard.someday", "dashboard.someday.none")) {
                    String text = NbBundle.getMessage(DashboardPanel.class, key);
                    assertTrue(key, List.of(panel.getComponents()).stream()
                            .anyMatch(c -> c instanceof JLabel label && label.getText().contains(text)));
                }
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void waitingAndSomedayRowsAreEscapedAndSomedayRowsSelectTheRecord() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Action waiting = new Action(data);
                waiting.setDescription("Waiting <&>");
                waiting.setState(new ActionStateDelegated());
                data.getRootActions().add(waiting);
                Future future = new Future(data.getNextID());
                future.setDescription("Someday <&>");
                data.getFutureManager().add(future);
                DataLookup.instance().setData(data);
                List<Future> selected = new ArrayList<>();
                DashboardPanel panel = new DashboardPanel(() -> {}, uri -> {}, selected::add);
                panel.refresh();
                int rows = 0;
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label && (label.getText().contains("Waiting &lt;&amp;&gt;")
                            || label.getText().contains("Someday &lt;&amp;&gt;"))) {
                        rows++;
                        assertTrue(label.getMouseListeners().length > 0);
                        if (label.getText().contains("Someday")) {
                            click(label);
                        }
                    }
                }
                assertEquals(2, rows);
                assertEquals(List.of(future), selected);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void somedayListCanExpandBeyondCollapsedRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                for (int i = 0; i < 12; i++) {
                    Future future = new Future(data.getNextID());
                    future.setDescription("Someday-row-" + i);
                    data.getFutureManager().add(future);
                }
                DataLookup.instance().setData(data);
                DashboardPanel panel = new DashboardPanel();
                panel.refresh();
                assertEquals(8, countSomedayRows(panel));
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label && label.getText().contains(
                            NbBundle.getMessage(DashboardPanel.class, "dashboard.show.all", 12))) {
                        click(label);
                        break;
                    }
                }
                assertEquals(12, countSomedayRows(panel));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    private static long countSomedayRows(DashboardPanel panel) {
        return List.of(panel.getComponents()).stream()
                .filter(c -> c instanceof JLabel label && label.getText().contains("Someday-row-")).count();
    }

    @Test
    public void callsSectionIncludesSeparateClickableNumbersFromTitleAndNotes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Action action = new Action(data);
                action.setContext(data.getContextManager().list().stream()
                        .filter(context -> context.hasName("Calls")).findFirst().orElseThrow());
                action.setDescription("Call <supplier> +46 70 123 45 67");
                action.setNotes("Backup: 020-7946-0958");
                data.getRootActions().add(action);
                DataLookup.instance().setData(data);
                List<URI> dialed = new ArrayList<>();
                DashboardPanel panel = new DashboardPanel(() -> {}, dialed::add);
                panel.refresh();
                boolean inCalls = false;
                int phoneLinks = 0;
                boolean actionLink = false;
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label) {
                        if (label.getText().equals(NbBundle.getMessage(DashboardPanel.class, "dashboard.calls") + " (1)")) {
                            inCalls = true;
                        } else if (inCalls && label.getText().contains("&nbsp;&nbsp;")) {
                            phoneLinks++;
                            assertTrue(label.getMouseListeners().length > 0);
                            click(label);
                        } else if (inCalls && label.getText().contains("&lt;supplier&gt;")) {
                            actionLink = true;
                        }
                    }
                }
                assertTrue(actionLink);
                assertEquals(2, phoneLinks);
                assertEquals(List.of(URI.create("tel:+46701234567"), URI.create("tel:02079460958")), dialed);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void nextActionRowsSelectTheirActionAndFooterOpensDoAsap() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Action action = new Action(data);
                action.setDescription("Navigate to Do ASAP");
                data.getRootActions().add(action);
                DataLookup.instance().setData(data);
                int[] navigations = {0};
                List<Action> selected = new ArrayList<>();
                DashboardPanel panel = new DashboardPanel(() -> navigations[0]++,
                        uri -> fail("Not a call"), selected::add, future -> fail("Not a someday item"));
                panel.refresh();
                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label
                            && (label.getText().contains("Navigate to Do ASAP")
                            || label.getText().equals(NbBundle.getMessage(
                                    DashboardPanel.class, "dashboard.next.all", 1)))) {
                        click(label);
                    }
                }
                assertEquals(List.of(action), selected);
                assertEquals("Only the footer should open Actions / Do ASAP", 1, navigations[0]);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void scheduledRowsSelectTheirExactAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Action action = new Action(data);
                action.setDescription("Scheduled dashboard action");
                ActionStateScheduled state = new ActionStateScheduled();
                state.setDate(new java.util.Date(System.currentTimeMillis() + 60_000L));
                action.setState(state);
                data.getRootActions().add(action);
                DataLookup.instance().setData(data);
                List<Action> selected = new ArrayList<>();
                DashboardPanel panel = new DashboardPanel(() -> fail("Not the footer"),
                        uri -> fail("Not a call"), selected::add, future -> fail("Not a someday item"));
                panel.refresh();

                for (Component component : panel.getComponents()) {
                    if (component instanceof JLabel label
                            && label.getText().contains("Scheduled dashboard action")) {
                        click(label);
                    }
                }

                assertEquals(List.of(action), selected);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void attentionCardShowsEveryRequiredWarningAndDashboardGroups() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                data.getThoughtManager().add(new Thought(data.getNextID()));

                Project stalled = new Project(data);
                stalled.setDescription("Stalled project");
                Action inactive = new Action(data);
                inactive.setState(new ActionStateInactive());
                stalled.add(inactive);
                data.getRootProjects().add(stalled);

                Action missed = new Action(data);
                missed.setDescription("Missed scheduled action");
                ActionStateScheduled scheduled = new ActionStateScheduled();
                scheduled.setDate(new java.util.Date(System.currentTimeMillis() - 86_400_000L));
                missed.setState(scheduled);

                Action followUp = new Action(data);
                ActionStateDelegated delegated = new ActionStateDelegated();
                delegated.setDate(new java.util.Date(System.currentTimeMillis() - 86_400_000L));
                followUp.setState(delegated);

                Action overdue = new Action(data);
                overdue.setDueDate(new java.util.Date(System.currentTimeMillis() - 86_400_000L));
                for (Action action : List.of(missed, followUp, overdue)) {
                    data.getRootActions().add(action);
                }

                DataLookup.instance().setData(data);
                DashboardPanel panel = new DashboardPanel();
                panel.refresh();
                List<JLabel> labels = labels(panel);

                for (String key : List.of("dashboard.attention.inbox", "dashboard.attention.stalled",
                        "dashboard.attention.missed", "dashboard.attention.followups",
                        "dashboard.attention.overdue")) {
                    String text = NbBundle.getMessage(DashboardPanel.class, key, 1);
                    assertTrue(key, labels.stream().anyMatch(label -> label.getText().contains(text)
                            && label.getMouseListeners().length > 0));
                }
                for (String key : List.of("dashboard.group.plan", "dashboard.group.act",
                        "dashboard.group.followup", "dashboard.group.review")) {
                    String text = NbBundle.getMessage(DashboardPanel.class, key);
                    assertTrue(key, labels.stream().anyMatch(label -> label.getText().equalsIgnoreCase(text)));
                }
                assertTrue(labels.stream().anyMatch(label -> label.getText().contains("Missed scheduled action")
                        && label.getMouseListeners().length > 0));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    private static List<JLabel> labels(Component component) {
        List<JLabel> labels = new ArrayList<>();
        if (component instanceof JLabel label) {
            labels.add(label);
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                labels.addAll(labels(child));
            }
        }
        return labels;
    }

    @Test
    public void attentionJumpShowsStalledProjectRowsNotJustTheHeading() throws Exception {
        Data[] original = new Data[1];
        DashboardPanel[] dashboard = new DashboardPanel[1];
        JScrollPane[] scroll = new JScrollPane[1];
        JLabel[] heading = new JLabel[1];
        JLabel[] firstProject = new JLabel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                original[0] = DataLookup.instance().lookup(Data.class);
                Data data = new Data();
                for (int i = 0; i < 15; i++) {
                    Project project = new Project(data);
                    project.setDescription("Stalled-row-" + i);
                    Action inactive = new Action(data);
                    inactive.setState(new ActionStateInactive());
                    project.add(inactive);
                    data.getRootProjects().add(project);
                }
                DataLookup.instance().setData(data);
                DashboardPanel panel = new DashboardPanel();
                dashboard[0] = panel;
                panel.refresh();
                JScrollPane pane = new JScrollPane(panel);
                scroll[0] = pane;
                pane.setSize(480, 260);
                pane.doLayout();
                pane.getViewport().doLayout();
                panel.doLayout();
                String stalledHeading = NbBundle.getMessage(DashboardPanel.class, "dashboard.stalled") + " (15)";
                heading[0] = labels(panel).stream()
                        .filter(label -> label.getText().equals(stalledHeading)).findFirst().orElseThrow();
                firstProject[0] = labels(panel).stream()
                        .filter(label -> label.getText().contains("Stalled-row-")).findFirst().orElseThrow();
                assertTrue(heading[0].getY() > pane.getViewport().getExtentSize().height);
                String warning = NbBundle.getMessage(DashboardPanel.class, "dashboard.attention.stalled");
                click(labels(panel).stream().filter(label -> label.getText().contains(warning))
                        .findFirst().orElseThrow());
            });
            SwingUtilities.invokeAndWait(() -> {
                var viewport = scroll[0].getViewport();
                int expectedY = Math.min(heading[0].getY(),
                        dashboard[0].getHeight() - viewport.getExtentSize().height);
                assertEquals(expectedY, viewport.getViewPosition().y);
                assertTrue("The first project must be fully visible",
                        viewport.getViewRect().contains(firstProject[0].getBounds()));
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> DataLookup.instance().setData(original[0]));
        }
    }

    private static void click(JLabel label) {
        MouseEvent click = new MouseEvent(label, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 1, 1, 1, false);
        for (var listener : label.getMouseListeners()) {
            listener.mouseClicked(click);
        }
    }

    @Test
    public void nextActionRowsStayCappedAndLinkToFullScreenAtEveryBoundary() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                for (int count : new int[]{0, 1, 10, 11, 1000}) {
                    Data data = new Data();
                    for (int i = 0; i < count; i++) {
                        Action action = new Action(data);
                        action.setDescription("Next-action-row-" + i + "<&>");
                        data.getRootActions().add(action);
                    }
                    DataLookup.instance().setData(data);
                    DashboardPanel panel = new DashboardPanel();
                    panel.refresh();
                    List<JLabel> rows = new ArrayList<>();
                    JLabel allLink = null;
                    for (Component component : panel.getComponents()) {
                        if (component instanceof JLabel label) {
                            if (label.getText().contains("Next-action-row-")) {
                                rows.add(label);
                                assertTrue(label.getText().contains("&lt;&amp;&gt;"));
                                assertTrue(label.getMouseListeners().length > 0);
                            }
                            if (label.getText().equals(NbBundle.getMessage(
                                    DashboardPanel.class, "dashboard.next.all", count))) {
                                allLink = label;
                            }
                        }
                    }
                    assertEquals(Math.min(10, count), rows.size());
                    if (count == 0) {
                        assertNull(allLink);
                    } else {
                        assertNotNull(allLink);
                        assertTrue(allLink.getMouseListeners().length > 0);
                        assertTrue(panel.getComponentZOrder(allLink)
                                > panel.getComponentZOrder(rows.getLast()));
                    }
                }
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }
}
