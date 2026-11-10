package au.com.trgtd.tr.view.cal;

import au.com.trgtd.tr.cal.model.CalEvent;
import au.com.trgtd.tr.cal.model.Day;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.action.ActionStateASAP;
import tr.model.action.ActionStateScheduled;
import tr.model.project.Project;

public class CalModelImpTest {

    private Data original;
    private Data data;
    private CalModelImp model;
    private final Day day = new Day(date(5, 0));

    @Before
    public void setUp() {
        original = DataLookup.instance().lookup(Data.class);
        data = new Data();
        DataLookup.instance().setData(data);
        model = new CalModelImp();
    }

    @After
    public void tearDown() {
        DataLookup.instance().setData(original);
    }

    @Test
    public void fourteenWeekQueriesReuseTheIndexUntilDataChanges() {
        Action action = scheduled(date(5, 10), 120);
        data.getRootActions().add(action);
        CalEvent indexedEvent = model.getEventsScheduledTime(day).get(0);

        for (int i = 0; i < 7; i++) {
            Day current = day.addDays(i);
            List<CalEvent> timed = model.getEventsScheduledTime(current);
            assertEquals(i <= 5 ? 1 : 0, timed.size());
            if (!timed.isEmpty()) {
                assertSame(indexedEvent, timed.get(0));
            }
            assertTrue(model.getEventsScheduledAllDay(current).isEmpty());
        }

        action.setDescription("Updated");
        assertEquals("Updated", model.getEventsScheduledTime(day).get(0).getAction().getDescription());
        assertNotSame(indexedEvent, model.getEventsScheduledTime(day).get(0));
    }

    @Test
    public void dateDurationStateAndMembershipChangesInvalidateTheIndex() {
        Action action = scheduled(date(5, 10), 1);
        data.getRootActions().add(action);
        assertEquals(1, model.getEventsScheduledTime(day).size());

        ActionStateScheduled state = (ActionStateScheduled) action.getState();
        state.setDate(date(6, 10));
        assertTrue(model.getEventsScheduledTime(day).isEmpty());
        assertEquals(1, model.getEventsScheduledTime(day.next()).size());

        state.setDurationHours(0);
        assertTrue(model.getEventsScheduledTime(day.next()).isEmpty());
        assertEquals(1, model.getEventsScheduledAllDay(day.next()).size());

        action.setState(new ActionStateASAP());
        assertTrue(model.getEventsScheduledAllDay(day.next()).isEmpty());

        Action added = scheduled(date(5, 0), 0);
        data.getRootActions().add(added);
        assertEquals(1, model.getEventsScheduledAllDay(day).size());
        added.removeFromParent();
        assertTrue(model.getEventsScheduledAllDay(day).isEmpty());
    }

    @Test
    public void showDoneChangesAndDataReplacementInvalidateTheIndex() {
        Action action = scheduled(date(5, 10), 1);
        data.getRootActions().add(action);
        assertEquals(1, model.getEventsScheduledTime(day).size());
        action.setDone(true);
        assertTrue(model.getEventsScheduledTime(day).isEmpty());

        model.setShowDone(true);
        assertEquals(1, model.getEventsScheduledTime(day).size());
        model.setShowDone(false);
        assertTrue(model.getEventsScheduledTime(day).isEmpty());
        action.setDone(false);
        assertEquals(1, model.getEventsScheduledTime(day).size());

        DataLookup.instance().setData(new Data());
        assertTrue(model.getEventsScheduledTime(day).isEmpty());
        DataLookup.instance().setData(null);
        assertTrue(model.getEventsScheduledTime(day).isEmpty());
    }

    @Test
    public void preservesMidnightOverlapProjectEventsOrderingAndIndependentResults() {
        Project project = new Project(data);
        data.getRootProjects().add(project);
        Action overnight = scheduled(date(5, 23), 1);
        project.add(overnight);
        Action zebra = scheduled(date(5, 0), 0);
        zebra.setDescription("Zebra");
        Action alpha = scheduled(date(5, 0), 0);
        alpha.setDescription("Alpha");
        project.add(zebra);
        project.add(alpha);
        Action undated = scheduled(null, 1);
        project.add(undated);

        assertEquals(overnight, model.getEventsScheduledTime(day).get(0).getAction());
        assertEquals(overnight, model.getEventsScheduledTime(day.next()).get(0).getAction());
        List<CalEvent> allDay = model.getEventsScheduledAllDay(day);
        assertEquals(List.of(alpha, zebra), allDay.stream().map(CalEvent::getAction).toList());
        allDay.clear();
        assertEquals(2, model.getEventsScheduledAllDay(day).size());

        ((ActionStateScheduled) overnight.getState()).setDate(null);
        assertTrue(model.getEventsScheduledTime(day).isEmpty());
    }

    private Action scheduled(Date date, int hours) {
        Action action = new Action(data);
        action.setState(state(date, hours));
        return action;
    }

    private static ActionStateScheduled state(Date date, int hours) {
        ActionStateScheduled state = new ActionStateScheduled();
        state.setDate(date);
        state.setDurationHours(hours);
        state.setDurationMins(0);
        return state;
    }

    private static Date date(int day, int hour) {
        return Date.from(LocalDate.of(2026, 11, day).atTime(hour, 0)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

}
