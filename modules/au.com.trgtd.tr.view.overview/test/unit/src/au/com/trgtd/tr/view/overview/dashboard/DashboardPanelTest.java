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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.openide.util.NbBundle;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import static org.junit.Assert.*;

public class DashboardPanelTest {

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
