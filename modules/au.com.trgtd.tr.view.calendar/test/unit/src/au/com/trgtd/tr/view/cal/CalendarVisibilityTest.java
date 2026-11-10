package au.com.trgtd.tr.view.cal;

import au.com.trgtd.tr.cal.ctlr.DateCtlr;
import au.com.trgtd.tr.cal.ctlr.WeekPanelCtlr;
import au.com.trgtd.tr.cal.model.CalEvent;
import au.com.trgtd.tr.cal.model.Day;
import au.com.trgtd.tr.cal.view.MonthPanel;
import au.com.trgtd.tr.cal.view.YearPanel;
import java.awt.Color;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalendarVisibilityTest {

    @Test
    public void hiddenMonthDoesNotRebuildAndRefreshesWhenShownAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateCtlr dates = new DateCtlr();
            CountingModel model = new CountingModel();
            verifyVisibility(new MonthPanel(dates, model, date -> {}), dates, model);
        });
    }

    @Test
    public void hiddenYearDoesNotRebuildAndRefreshesWhenShownAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateCtlr dates = new DateCtlr();
            CountingModel model = new CountingModel();
            verifyVisibility(new YearPanel(dates, model, date -> {}, date -> {}, Color.WHITE),
                    dates, model);
        });
    }

    @Test
    public void hiddenWeekDoesNotQueryEventsAndRefreshesWhenShownAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateCtlr dates = new DateCtlr();
            CountingModel model = new CountingModel();
            verifyVisibility(new WeekPanelCtlr(model, dates, 0, 23).getWeekPanel(), dates, model);
        });
    }

    private static void verifyVisibility(JPanel panel, DateCtlr dates, CountingModel model) {
        int initial = model.queries;
        panel.addNotify();
        try {
            assertTrue(panel.isShowing());
            assertTrue(model.queries > initial);
            int shown = model.queries;
            dates.fireChange();
            assertTrue(model.queries > shown);

            panel.setVisible(false);
            int hidden = model.queries;
            dates.fireChange();
            dates.setDate(dates.getDay().addDays(8).getDate());
            assertEquals("Hidden views must perform zero event queries", hidden, model.queries);

            panel.setVisible(true);
            assertTrue("Reopened views must refresh", model.queries > hidden);
        } finally {
            panel.removeNotify();
        }
        int removed = model.queries;
        dates.fireChange();
        assertEquals("Removed views must remain detached", removed, model.queries);
    }

    private static class CountingModel extends CalModelImp {
        private int queries;

        @Override
        public Map<Date, List<CalEvent>> getEventsMap() {
            queries++;
            return Map.of();
        }

        @Override
        public List<CalEvent> getEventsScheduledTime(Day day) {
            queries++;
            return List.of();
        }

        @Override
        public List<CalEvent> getEventsScheduledAllDay(Day day) {
            queries++;
            return List.of();
        }
    }
}
