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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package au.com.trgtd.tr.cal.ctlr;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class DateCtlrTest {

    @Test
    public void viewListenersDetachWhileHiddenAndRefreshOnReopening() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateCtlr controller = new DateCtlr(new Date(0));
            JPanel owner = new JPanel();
            AtomicInteger calls = new AtomicInteger();
            controller.addPropertyChangeListener(owner, event -> {
                assertEquals(controller.getDate(), event.getNewValue());
                calls.incrementAndGet();
            });

            controller.fireChange();
            assertEquals(0, calls.get());
            owner.addNotify();
            try {
                assertTrue(owner.isShowing());
                assertEquals(1, calls.get());
                controller.fireChange();
                assertEquals(2, calls.get());

                owner.setVisible(false);
                controller.setDate(new Date(7 * 86400000L));
                assertEquals(2, calls.get());

                owner.setVisible(true);
                assertEquals(3, calls.get());
                controller.fireChange();
                assertEquals(4, calls.get());

                owner.removeNotify();
                controller.fireChange();
                assertEquals(4, calls.get());

                owner.addNotify();
                assertEquals(5, calls.get());
                controller.fireChange();
                assertEquals(6, calls.get());
            } finally {
                owner.removeNotify();
            }
            controller.fireChange();
            assertEquals(6, calls.get());
        });
    }

    @Test
    public void existingNonViewListenersStillReceiveChanges() {
        DateCtlr controller = new DateCtlr();
        AtomicInteger calls = new AtomicInteger();
        controller.addPropertyChangeListener(event -> calls.incrementAndGet());

        controller.fireChange();

        assertEquals(1, calls.get());
    }
}
