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
package au.com.trgtd.tr.view.overview.dashboard;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import tr.model.Data;
import tr.model.action.Action;
import tr.model.action.ActionStateDelegated;
import tr.model.action.ActionStateInactive;
import tr.model.action.ActionStateScheduled;
import tr.model.project.Project;
import tr.model.context.Context;
import tr.model.future.Future;
import tr.model.thought.Thought;

public class DashboardModelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Test
    public void nullDataGivesEmptyModel() {
        DashboardModel model = DashboardModel.build(null, date(NOW), 2);
        assertTrue(model.today.isEmpty());
        assertTrue(model.activeProjects.isEmpty());
        assertTrue(model.waitingFor.isEmpty());
        assertTrue(model.somedayMaybe.isEmpty());
        assertEquals(0, model.inboxCount);
    }

    @Test
    public void scheduledActionsAreSplitIntoTodayAndUpcomingSortedByTime() {
        Data data = new Data();
        Action late = scheduled(data, NOW.withHour(15));
        Action early = scheduled(data, NOW.withHour(9));
        Action tomorrow = scheduled(data, NOW.plusDays(1));
        Action inTwoDays = scheduled(data, NOW.plusDays(2).withHour(23));
        Action inThreeDays = scheduled(data, NOW.plusDays(3));
        Action yesterday = scheduled(data, NOW.minusDays(1));
        Action doneToday = scheduled(data, NOW.withHour(10));
        doneToday.setDone(true);
        for (Action a : List.of(late, early, tomorrow, inTwoDays, inThreeDays, yesterday, doneToday)) {
            data.getRootActions().add(a);
        }

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);

        assertEquals(List.of(early, late), model.today);
        assertEquals(List.of(tomorrow, inTwoDays), model.upcoming);
    }

    @Test
    public void projectsAreClassifiedAsActiveOrStalledInTreeOrder() {
        Data data = new Data();
        Project active = project(data, "Active");
        active.add(new Action(data));
        active.add(new Action(data));
        Project stalled = project(data, "Stalled");
        Action inactive = new Action(data);
        inactive.setState(new ActionStateInactive());
        stalled.add(inactive);
        Project parent = project(data, "Parent");
        Project child = project(data, "Child");
        child.add(new Action(data));
        parent.add(child);
        Project done = project(data, "Done");
        done.setDone(true);
        Project future = project(data, "Not started");
        future.setStartDate(date(NOW.plusDays(5)));
        for (Project p : List.of(active, stalled, parent, done, future)) {
            data.getRootProjects().add(p);
        }

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);

        assertEquals(List.of(active, parent, child),
                model.activeProjects.stream().map(DashboardModel.ProjectInfo::project).toList());
        assertEquals(2, model.activeProjects.get(0).activeActions());
        assertEquals(1, model.activeProjects.get(1).activeActions());
        assertEquals(0, model.activeProjects.get(1).depth());
        assertEquals(1, model.activeProjects.get(2).depth());
        assertEquals(List.of(stalled),
                model.stalledProjects.stream().map(DashboardModel.ProjectInfo::project).toList());
        assertSame(active, DashboardModel.getProject(active.getChildren(Action.class).get(0)));
    }

    @Test
    public void countsInboxDoAsapDueAndFollowUps() {
        Data data = new Data();
        Thought unprocessed = new Thought(data.getNextID());
        Thought processed = new Thought(data.getNextID());
        processed.setProcessed(true);
        data.getThoughtManager().add(unprocessed);
        data.getThoughtManager().add(processed);

        Action asap = new Action(data);
        asap.setDueDate(date(NOW.minusDays(1)));
        Action asapLater = new Action(data);
        asapLater.setStartDate(date(NOW.plusDays(3)));
        Action delegated = new Action(data);
        ActionStateDelegated state = new ActionStateDelegated();
        state.setDate(date(NOW));
        delegated.setState(state);
        for (Action a : List.of(asap, asapLater, delegated)) {
            data.getRootActions().add(a);
        }

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);

        assertEquals(1, model.inboxCount);
        assertEquals(1, model.doAsapCount);
        assertEquals(List.of(asap), model.due);
        assertEquals(List.of(delegated), model.followUps);
        assertNull(DashboardModel.getProject(asap));
    }

    @Test
    public void nextActionsSortByContextThenDueDateWithUndatedLast() {
        Data data = new Data();
        Context home = new Context(data.getNextID());
        home.setName("Home");
        Context work = new Context(data.getNextID());
        work.setName("Work");
        Action homeUndated = nextAction(data, home, "Undated", null);
        Action homeLater = nextAction(data, home, "Later", NOW.plusDays(2));
        Action workOverdue = nextAction(data, work, "Overdue", NOW.minusDays(2));
        Action homeEarlier = nextAction(data, home, "Earlier", NOW.minusDays(1));
        Action homeSameDate = nextAction(data, home, "Alphabetically first", NOW.plusDays(2));

        assertEquals(List.of(homeEarlier, homeSameDate, homeLater, homeUndated, workOverdue),
                DashboardModel.build(data, date(NOW), 2).nextActions);
    }

    @Test
    public void nextActionsIncludeProjectAndSingleActionsButExcludeUnavailableStates() {
        Data data = new Data();
        Action single = new Action(data);
        data.getRootActions().add(single);
        Project project = project(data, "Project");
        Action projectAction = new Action(data);
        project.add(projectAction);
        data.getRootProjects().add(project);
        Action done = new Action(data);
        done.setDone(true);
        Action future = new Action(data);
        future.setStartDate(date(NOW.plusDays(1)));
        Action inactive = new Action(data);
        inactive.setState(new ActionStateInactive());
        Action delegated = new Action(data);
        delegated.setState(new ActionStateDelegated());
        for (Action action : List.of(done, future, inactive, delegated, scheduled(data, NOW))) {
            data.getRootActions().add(action);
        }

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);
        assertEquals(2, model.doAsapCount);
        assertEquals(2, model.nextActions.size());
        assertTrue(model.nextActions.containsAll(List.of(single, projectAction)));
        assertTrue(DashboardModel.build(null, date(NOW), 2).nextActions.isEmpty());
    }

    @Test
    public void thousandsOfNextActionsRetainOnlyTheFirstTenInSortedOrder() {
        Data data = new Data();
        Context context = new Context(data.getNextID());
        context.setName("Work");
        for (int i = 999; i >= 0; i--) {
            nextAction(data, context, "Action " + i, NOW.plusDays(i));
        }
        DashboardModel model = DashboardModel.build(data, date(NOW), 2);

        assertEquals(1000, model.doAsapCount);
        assertEquals(10, model.nextActions.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("Action " + i, model.nextActions.get(i).getDescription());
        }
    }

    private static Action nextAction(Data data, Context context, String description, LocalDateTime due) {
        Action action = new Action(data);
        action.setContext(context);
        action.setDescription(description);
        action.setDueDate(due == null ? null : date(due));
        data.getRootActions().add(action);
        return action;
    }

    @Test
    public void callsIncludesActiveCallsAcrossStatesAndIsCappedAndSortedByDueDate() {
        Data data = new Data();
        Context calls = data.getContextManager().list().stream()
                .filter(context -> context.hasName("Calls")).findFirst().orElseThrow();
        Action undated = nextAction(data, calls, "Undated", null);
        Action done = nextAction(data, calls, "Done", NOW.minusDays(3));
        done.setDone(true);
        Action inactive = nextAction(data, calls, "Inactive", NOW.minusDays(3));
        inactive.setState(new ActionStateInactive());
        Action future = nextAction(data, calls, "Future", NOW.minusDays(3));
        future.setStartDate(date(NOW.plusDays(2)));
        Action scheduled = scheduled(data, NOW);
        scheduled.setContext(calls);
        data.getRootActions().add(scheduled);
        for (int i = 999; i >= 0; i--) {
            nextAction(data, calls, "Call " + i, NOW.plusDays(i));
        }

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);
        assertEquals(1002, model.callsCount);
        assertEquals(10, model.calls.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("Call " + i, model.calls.get(i).getDescription());
        }
        assertFalse(model.calls.contains(undated));
    }

    @Test
    public void callsRecognizesExistingContextNamesWithAtPrefixAndDifferentCase() {
        Data data = new Data();
        Context context = new Context(data.getNextID());
        context.setName(" @cALLs ");
        Action action = nextAction(data, context, "Phone supplier", null);

        assertEquals(List.of(action), DashboardModel.build(data, date(NOW), 2).calls);
    }

    @Test
    public void waitingIncludesContextAndDelegatedActionsWithoutRequiringFollowUpDates() {
        Data data = new Data();
        Context waiting = new Context(data.getNextID());
        waiting.setName(" @waiting FOR ");
        Action contextual = nextAction(data, waiting, "Context waiting", NOW.plusDays(2));
        Action delegated = new Action(data);
        delegated.setDescription("Delegated without follow-up date");
        delegated.setState(new ActionStateDelegated());
        Project project = project(data, "Waiting project");
        project.add(delegated);
        data.getRootProjects().add(project);
        Action both = nextAction(data, waiting, "Both", NOW.minusDays(1));
        ActionStateDelegated state = new ActionStateDelegated();
        state.setDate(date(NOW.plusDays(5)));
        both.setState(state);
        Action done = nextAction(data, waiting, "Done", null);
        done.setDone(true);
        Action inactive = nextAction(data, waiting, "Inactive", null);
        inactive.setState(new ActionStateInactive());
        Action future = nextAction(data, waiting, "Future", null);
        future.setStartDate(date(NOW.plusDays(1)));
        data.getRootActions().add(new Action(data));

        DashboardModel model = DashboardModel.build(data, date(NOW), 2);

        assertEquals(List.of(both, contextual, delegated), model.waitingFor);
        assertTrue("Waiting is not limited to follow-ups due today", model.followUps.isEmpty());
    }

    @Test
    public void somedayMaybeUsesDedicatedRecordsNotActionContextsOrTickleDates() {
        Data data = new Data();
        Future zebra = new Future(data.getNextID());
        zebra.setDescription("Zebra");
        Future alpha = new Future(data.getNextID());
        alpha.setDescription("Alpha");
        alpha.setTickle(date(NOW.plusDays(100)));
        data.getFutureManager().add(zebra);
        data.getFutureManager().add(alpha);
        Context someday = new Context(data.getNextID());
        someday.setName("Someday/Maybe");
        nextAction(data, someday, "Not a Someday/Maybe record", null);

        assertEquals(List.of(alpha, zebra), DashboardModel.build(data, date(NOW), 2).somedayMaybe);
    }

    @Test
    public void escapesHtml() {
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", DashboardPanel.escape("a <b> & \"c\""));
    }

    private static Project project(Data data, String name) {
        Project project = new Project(data);
        project.setDescription(name);
        return project;
    }

    private static Action scheduled(Data data, LocalDateTime when) {
        Action action = new Action(data);
        ActionStateScheduled state = new ActionStateScheduled();
        state.setDate(date(when));
        action.setState(state);
        return action;
    }

    private static Date date(LocalDateTime when) {
        return Date.from(when.atZone(ZoneId.systemDefault()).toInstant());
    }
}
