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
