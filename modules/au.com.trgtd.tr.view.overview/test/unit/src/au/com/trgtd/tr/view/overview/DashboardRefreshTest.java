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
package au.com.trgtd.tr.view.overview;

import au.com.trgtd.tr.view.collect.dialog.ThoughtDialog;
import au.com.trgtd.tr.view.collect.dialog.ThoughtDialogPanel;
import au.com.trgtd.tr.view.overview.dashboard.DashboardPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import org.openide.util.NbBundle;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.future.Future;
import tr.model.thought.Thought;

public class DashboardRefreshTest {

    private Data original;
    private Data data;
    private DashboardRefresh refresh;
    private DashboardPanel dashboard;
    private final AtomicInteger renders = new AtomicInteger();
    private volatile CountDownLatch rendered;
    private boolean showing = true;

    @Before
    public void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            original = DataLookup.instance().lookup(Data.class);
            data = new Data();
            DataLookup.instance().setData(data);
            dashboard = new DashboardPanel();
            refresh = new DashboardRefresh(() -> showing, () -> {
                assertTrue(SwingUtilities.isEventDispatchThread());
                dashboard.refresh();
                renders.incrementAndGet();
                if (rendered != null) {
                    rendered.countDown();
                }
            });
            refresh.start();
        });
        assertEquals(1, renders.get());
    }

    @Test
    public void repeatedAddsInOpenThoughtDialogUpdateDashboardWithoutClosingDialog() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        ThoughtDialog[] dialog = new ThoughtDialog[1];
        CountDownLatch closed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            dialog[0] = new ThoughtDialog(data);
            SwingUtilities.invokeLater(() -> {
                try {
                    dialog[0].showCreateDialog();
                } finally {
                    closed.countDown();
                }
            });
        });
        try {
            for (int i = 1; i <= 5; i++) {
                int count = i;
                rendered = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(() -> {
                    assertTrue(dialog[0].isShowing());
                    ThoughtDialogPanel panel = findThoughtPanel(dialog[0]);
                    assertNotNull(panel);
                    panel.setDescr("Captured thought " + count);
                    dialog[0].getRootPane().getDefaultButton().doClick();
                    assertTrue("Add must keep the capture window open", dialog[0].isShowing());
                });
                awaitRender();
                SwingUtilities.invokeAndWait(() -> {
                    JPanel summary = (JPanel) dashboard.getComponent(2);
                    JLabel inbox = (JLabel) summary.getComponent(0);
                    assertEquals(NbBundle.getMessage(DashboardPanel.class, "dashboard.inbox", count), inbox.getText());
                });
            }
            waitForPendingRefresh();
            assertEquals("Exactly one refresh per capture, without polling", 6, renders.get());
        } finally {
            SwingUtilities.invokeAndWait(dialog[0]::dispose);
            assertTrue(closed.await(5, TimeUnit.SECONDS));
        }
    }

    private static ThoughtDialogPanel findThoughtPanel(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof ThoughtDialogPanel panel) {
                return panel;
            }
            if (child instanceof Container container) {
                ThoughtDialogPanel panel = findThoughtPanel(container);
                if (panel != null) {
                    return panel;
                }
            }
        }
        return null;
    }

    @After
    public void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            refresh.stop();
            DataLookup.instance().setData(original);
        });
    }

    @Test
    public void thoughtCaptureAndActionEditsRefreshOnEdtAndBatchBursts() throws Exception {
        rendered = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < 20; i++) {
                data.getThoughtManager().add(new Thought(data.getNextID()));
            }
        });
        awaitRender();
        waitForPendingRefresh();
        assertEquals("One render for a burst of captured thoughts", 2, renders.get());

        Action action = new Action(data);
        rendered = new CountDownLatch(1);
        data.getRootActions().add(action);
        awaitRender();
        rendered = new CountDownLatch(1);
        action.setDescription("Changed from outside the EDT");
        awaitRender();
    }

    @Test
    public void hiddenAndStoppedViewsDoNotRenderAndReopeningRefreshes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            showing = false;
            data.getThoughtManager().add(new Thought(data.getNextID()));
        });
        waitForPendingRefresh();
        assertEquals(1, renders.get());
        SwingUtilities.invokeAndWait(() -> {
            showing = true;
            refresh.refresh();
        });
        assertEquals(2, renders.get());
        SwingUtilities.invokeAndWait(() -> {
            data.getThoughtManager().add(new Thought(data.getNextID()));
            refresh.stop();
        });
        waitForPendingRefresh();
        assertEquals(2, renders.get());
        SwingUtilities.invokeAndWait(refresh::start);
        assertEquals(3, renders.get());
    }

    @Test
    public void switchingFilesDetachesOldDataAndObservesNewThoughts() throws Exception {
        Data replacement = new Data();
        rendered = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> DataLookup.instance().setData(replacement));
        awaitRender();
        int before = renders.get();
        data.getThoughtManager().add(new Thought(data.getNextID()));
        waitForPendingRefresh();
        assertEquals(before, renders.get());
        rendered = new CountDownLatch(1);
        replacement.getThoughtManager().add(new Thought(replacement.getNextID()));
        awaitRender();

        rendered = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> DataLookup.instance().setData(null));
        awaitRender();
    }

    @Test
    public void somedayChangesRefreshAndSwitchingFilesDetachesOldRecords() throws Exception {
        Future future = new Future(data.getNextID());
        rendered = new CountDownLatch(1);
        data.getFutureManager().add(future);
        awaitRender();
        rendered = new CountDownLatch(1);
        future.setDescription("New Someday item");
        awaitRender();
        SwingUtilities.invokeAndWait(() -> assertTrue(hasLabelContaining("New Someday item")));

        Data replacement = new Data();
        rendered = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> DataLookup.instance().setData(replacement));
        awaitRender();
        int before = renders.get();
        future.setDescription("Old file must not refresh");
        waitForPendingRefresh();
        assertEquals(before, renders.get());
        rendered = new CountDownLatch(1);
        replacement.getFutureManager().add(new Future(replacement.getNextID()));
        awaitRender();
    }

    private boolean hasLabelContaining(String text) {
        for (Component component : dashboard.getComponents()) {
            if (component instanceof JLabel label && label.getText().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private void awaitRender() throws InterruptedException {
        assertTrue("Dashboard must refresh without activation or manual refresh",
                rendered.await(5, TimeUnit.SECONDS));
    }

    private static void waitForPendingRefresh() throws Exception {
        CountDownLatch elapsed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.Timer timer = new javax.swing.Timer(350, event -> elapsed.countDown());
            timer.setRepeats(false);
            timer.start();
        });
        assertTrue(elapsed.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {});
    }
}
