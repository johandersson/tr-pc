/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.overview.dashboard;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.openide.util.NbBundle;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.action.ActionStateASAP;
import tr.model.action.ActionStateScheduled;
import tr.model.context.Context;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class QuickAddNextActionPanelTest {

    @Test
    public void focusingCaptureMovesKeyboardFocusToEditorAndPreservesDraft() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame[] frame = new JFrame[1];
        QuickAddNextActionPanel[] capture = new QuickAddNextActionPanel[1];
        CountDownLatch buttonFocused = new CountDownLatch(1);
        CountDownLatch editorFocused = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                capture[0] = new QuickAddNextActionPanel(() -> {});
                editor(capture[0]).setText("Existing draft");
                editor(capture[0]).addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        editorFocused.countDown();
                    }
                });
                JButton button = saveButton(capture[0]);
                button.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        buttonFocused.countDown();
                    }
                });
                frame[0] = new JFrame();
                frame[0].add(capture[0]);
                frame[0].setSize(400, 280);
                frame[0].setVisible(true);
                button.requestFocusInWindow();
            });
            assertTrue("Window must receive focus", buttonFocused.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> capture[0].focusEditor());
            assertTrue("Capture editor must receive focus", editorFocused.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(editor(capture[0]).isFocusOwner());
                assertEquals("Existing draft", editor(capture[0]).getText());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
        }
    }

    @Test
    public void shortcutAndButtonCreateDoAsapActionsNotThoughts() throws Exception {
        withData(data -> {
            int[] refreshes = {0};
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> refreshes[0]++);
            JTextArea editor = editor(panel);
            for (String text : List.of("Buy groceries @Errands", "@errands Buy groceries",
                    "Buy @ERRANDS groceries", "Buy\n@Errands\ngroceries")) {
                editor.setText(text);
                shortcut(panel);
                Action action = lastAction(data);
                assertEquals("Buy groceries", action.getDescription());
                assertTrue(action.getContext().hasName("Errands"));
                assertTrue(action.getState() instanceof ActionStateASAP);
                assertSame(data.getRootActions(), action.getParent());
                assertFalse(action.isDone());
                assertNull(action.getThought());
                assertEquals("", editor.getText());
            }
            editor.setText("Write to person@example.com");
            saveButton(panel).doClick();
            assertEquals("Write to person@example.com", lastAction(data).getDescription());
            assertEquals(Context.getDefault(), lastAction(data).getContext());
            assertEquals(5, data.getRootActions().size());
            assertEquals(0, data.getThoughtManager().size());
            assertEquals(5, refreshes[0]);
        });
    }

    @Test
    public void dateMarkersCreateScheduledActionsAndPreserveContexts() throws Exception {
        withData(data -> {
            int[] refreshes = {0};
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> refreshes[0]++);
            LocalDate today = LocalDate.now();
            String[] texts = {"Foo @Home -today", "-TOMORROW Foo @Home",
                "Foo -2028-02-29 @Home", "-today Foo\n@Home -today"};
            LocalDate[] dates = {today, today.plusDays(1), LocalDate.of(2028, 2, 29), today};
            ActionStateScheduled defaults = new ActionStateScheduled();
            for (int i = 0; i < texts.length; i++) {
                editor(panel).setText(texts[i]);
                if (i == 1) {
                    saveButton(panel).doClick();
                } else {
                    shortcut(panel);
                }
                Action action = lastAction(data);
                assertEquals("Foo", action.getDescription());
                assertTrue(action.getContext().hasName("Home"));
                assertTrue(action.getState() instanceof ActionStateScheduled);
                ActionStateScheduled scheduled = (ActionStateScheduled) action.getState();
                assertEquals(dates[i], scheduled.getDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate());
                assertEquals(defaults.getSchdHour(), scheduled.getSchdHour());
                assertEquals(defaults.getSchdMinute(), scheduled.getSchdMinute());
                assertEquals("", editor(panel).getText());
                assertNull(action.getThought());
            }
            editor(panel).setText("Foo -2030-12-01");
            shortcut(panel);
            assertEquals(Context.getDefault(), lastAction(data).getContext());
            assertEquals(5, refreshes[0]);
            assertEquals(0, data.getThoughtManager().size());
        });
    }

    @Test
    public void ordinaryHyphensAndContextNamesAreNotScheduleMarkers() throws Exception {
        withData(data -> {
            Context context = new Context(data.getNextID());
            context.setName("Home -today");
            data.getContextManager().add(context);
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> {});
            editor(panel).setText("Follow-up - check foo-today -todayish @Home -today");
            shortcut(panel);
            Action action = lastAction(data);
            assertEquals("Follow-up - check foo-today -todayish", action.getDescription());
            assertSame(context, action.getContext());
            assertTrue(action.getState() instanceof ActionStateASAP);
        });
    }

    @Test
    public void invalidSchedulesKeepDraftAndShowInlineFeedback() throws Exception {
        withData(data -> {
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> fail("Must not save"));
            for (String[] input : List.of(
                    new String[]{"Foo @Home -2027-02-29", "quickadd.invalid.date", "-2027-02-29"},
                    new String[]{"Foo -2028-13-01", "quickadd.invalid.date", "-2028-13-01"},
                    new String[]{"Foo -2028-1-2", "quickadd.invalid.date", "-2028-1-2"},
                    new String[]{"Foo -today -tomorrow", "quickadd.multiple.dates"},
                    new String[]{"@Home -today", "quickadd.empty"})) {
                editor(panel).setText(input[0]);
                shortcut(panel);
                assertEquals(input[0], editor(panel).getText());
                Object[] args = input.length == 3 ? new Object[]{input[2]} : new Object[0];
                assertEquals(NbBundle.getMessage(QuickAddNextActionPanel.class, input[1], args),
                        status(panel).getText());
                assertEquals(0, data.getRootActions().size());
                assertEquals(0, data.getThoughtManager().size());
            }
        });
    }

    @Test
    public void contextsWithAtPrefixesSpacesAndOverlappingNamesAreRecognized() throws Exception {
        withData(data -> {
            Context errands = new Context(data.getNextID());
            errands.setName("@Errand");
            data.getContextManager().add(errands);
            Context office = new Context(data.getNextID());
            office.setName("@Office desk");
            data.getContextManager().add(office);
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> {});
            editor(panel).setText("Buy groceries @errand");
            shortcut(panel);
            assertSame(errands, lastAction(data).getContext());
            editor(panel).setText("Write @office desk report");
            shortcut(panel);
            assertSame(office, lastAction(data).getContext());
            assertEquals("Write report", lastAction(data).getDescription());
            editor(panel).setText("@Office desk Write report @Office desk");
            shortcut(panel);
            assertEquals(3, data.getRootActions().size());
            assertSame(office, lastAction(data).getContext());
            assertEquals("Write report", lastAction(data).getDescription());
        });
    }

    @Test
    public void invalidInputKeepsDraftAndDisplaysInlineFeedbackWithoutCreatingRecords() throws Exception {
        withData(data -> {
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> fail("Must not save"));
            for (String[] input : List.of(
                    new String[]{"", "quickadd.empty"},
                    new String[]{" \n ", "quickadd.empty"},
                    new String[]{"@Errands", "quickadd.empty"},
                    new String[]{"Buy groceries @Errand", "quickadd.unknown.context", "@Errand"},
                    new String[]{"Buy groceries @ErrandsExtra", "quickadd.unknown.context", "@ErrandsExtra"},
                    new String[]{"Buy groceries @", "quickadd.unknown.context", "@"},
                    new String[]{"Buy @Errands groceries @Home", "quickadd.multiple.contexts"})) {
                editor(panel).setText(input[0]);
                shortcut(panel);
                assertEquals(input[0], editor(panel).getText());
                Object[] args = input.length == 3 ? new Object[]{input[2]} : new Object[0];
                assertEquals(NbBundle.getMessage(QuickAddNextActionPanel.class, input[1], args),
                        status(panel).getText());
                assertEquals(0, data.getRootActions().size());
                assertEquals(0, data.getThoughtManager().size());
            }
        });
    }

    @Test
    public void savingUsesCurrentDataAndReportsMissingData() throws Exception {
        withData(data -> {
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(() -> {});
            editor(panel).setText("New action");
            DataLookup.instance().setData(null);
            shortcut(panel);
            assertEquals("New action", editor(panel).getText());
            assertEquals(NbBundle.getMessage(QuickAddNextActionPanel.class, "quickadd.no.data"),
                    status(panel).getText());
            Data replacement = new Data();
            DataLookup.instance().setData(replacement);
            shortcut(panel);
            assertEquals(0, data.getRootActions().size());
            assertEquals(1, replacement.getRootActions().size());
        });
    }

    @Test
    public void draftSurvivesRefreshAndPanelFitsNarrowAndShortDashboardColumns() throws Exception {
        withData(data -> {
            DashboardPanel dashboard = new DashboardPanel();
            dashboard.refresh();
            QuickAddNextActionPanel panel = new QuickAddNextActionPanel(dashboard::refresh);
            editor(panel).setText("An unfinished draft that wraps onto several lines in a narrow column");
            String draft = editor(panel).getText();
            dashboard.refresh();
            assertEquals(draft, editor(panel).getText());
            assertTrue(editor(panel).getLineWrap());
            JScrollPane scroll = new JScrollPane(dashboard);
            scroll.setMinimumSize(new java.awt.Dimension(0, 0));
            JSplitPane column = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, panel);
            column.setResizeWeight(1);
            for (int[] size : List.of(new int[]{480, 700}, new int[]{280, 320}, new int[]{240, 220})) {
                column.setSize(size[0], size[1]);
                layout(column);
                assertTrue("Capture must remain visible", panel.getHeight() >= 180);
                assertTrue(panel.getWidth() <= column.getWidth());
                assertTrue(panel.getY() + panel.getHeight() <= column.getHeight());
                JTextArea editor = editor(panel);
                assertTrue(editor.getVisibleRect().width > 0);
                assertTrue(editor.getVisibleRect().height > 0);
                JButton save = saveButton(panel);
                assertTrue(save.getWidth() >= save.getPreferredSize().width);
                assertTrue(status(panel).getWidth() >= panel.getWidth() - 30);
            }
        });
    }

    private static void withData(Consumer<Data> test) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                DataLookup.instance().setData(data);
                test.accept(data);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    private static Action lastAction(Data data) {
        return (Action) data.getRootActions().getChildren().get(data.getRootActions().size() - 1);
    }

    private static void shortcut(QuickAddNextActionPanel panel) {
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        Object binding = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(key);
        assertNotNull(binding);
        assertNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key));
        panel.getActionMap().get(binding).actionPerformed(new ActionEvent(panel, 0, "save"));
    }

    private static JTextArea editor(QuickAddNextActionPanel panel) {
        return find(panel, JTextArea.class).stream().filter(JTextArea::isEditable).findFirst().orElseThrow();
    }

    private static JTextArea status(QuickAddNextActionPanel panel) {
        return find(panel, JTextArea.class).stream().filter(area -> !area.isEditable()).findFirst().orElseThrow();
    }

    private static JButton saveButton(QuickAddNextActionPanel panel) {
        String label = NbBundle.getMessage(QuickAddNextActionPanel.class, "quickadd.save");
        return find(panel, JButton.class).stream().filter(button -> label.equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> find(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container container) {
                result.addAll(find(container, type));
            }
        }
        return result;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) {
                layout(container);
            }
        }
    }
}
