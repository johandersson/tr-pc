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
package au.com.trgtd.tr.archive;

import au.com.trgtd.tr.util.DateUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import tr.model.Data;
import tr.model.action.Action;
import tr.model.action.ActionStateScheduled;
import tr.model.project.Project;

public class ArchiveActionTest {

    @Test
    public void historicalEventsAreOptIn() {
        Data data = new Data();
        Action scheduled = scheduled(data, LocalDate.of(2020, 1, 1));
        Action done = done(data);
        data.getRootActions().add(scheduled);
        data.getRootActions().add(done);

        new ArchiveAction().selectArchived(data, cutoff(), true, false);

        assertEquals(List.of(done), data.getRootActions().getChildren(Action.class));
    }

    @Test
    public void archivesHistoricalScheduledActionsRegardlessOfDoneDate() {
        Data data = new Data();
        Action pending = scheduled(data, LocalDate.of(2020, 1, 1));
        Action completedLater = scheduled(data, LocalDate.of(2020, 1, 2));
        completedLater.setDone(true);
        completedLater.setDoneDate(date(LocalDate.of(2021, 1, 1)));
        Action atCutoff = scheduled(data, LocalDate.of(2020, 1, 3));
        ((ActionStateScheduled) atCutoff.getState()).setDate(cutoff());
        Action afterCutoff = scheduled(data, LocalDate.of(2020, 1, 4));
        Action undated = new Action(data);
        undated.setState(new ActionStateScheduled());
        data.getRootActions().add(pending);
        data.getRootActions().add(completedLater);
        data.getRootActions().add(atCutoff);
        data.getRootActions().add(afterCutoff);
        data.getRootActions().add(undated);

        new ArchiveAction().selectArchived(data, cutoff(), true, true);

        assertEquals(List.of(pending, completedLater, atCutoff),
                data.getRootActions().getChildren(Action.class));
        assertFalse(pending.isDone());
        assertTrue(completedLater.isDone());
    }

    @Test
    public void keepsTodayEvenWhenArchiveDateIsToday() {
        Data data = new Data();
        LocalDate today = LocalDate.now();
        Action yesterday = scheduled(data, today.minusDays(1));
        Action midnight = scheduled(data, today);
        Action tomorrow = scheduled(data, today.plusDays(1));
        data.getRootActions().add(yesterday);
        data.getRootActions().add(midnight);
        data.getRootActions().add(tomorrow);

        new ArchiveAction().selectArchived(data, DateUtils.getEnd(date(today)), true, true);

        assertEquals(List.of(yesterday), data.getRootActions().getChildren(Action.class));
    }

    @Test
    public void includesNestedScheduledEventsWithoutChangingDoneProjectsOnlyRule() {
        for (boolean doneProjectsOnly : new boolean[]{true, false}) {
            Data data = new Data();
            Project project = new Project(data);
            Project nested = new Project(data);
            data.getRootProjects().add(project);
            project.add(nested);
            Action scheduled = scheduled(data, LocalDate.of(2020, 1, 1));
            Action ordinaryDone = done(data);
            Action future = scheduled(data, LocalDate.of(2020, 1, 4));
            nested.add(scheduled);
            nested.add(ordinaryDone);
            nested.add(future);
            Project unrelated = new Project(data);
            unrelated.add(new Action(data));
            data.getRootProjects().add(unrelated);

            new ArchiveAction().selectArchived(data, cutoff(), doneProjectsOnly, true);

            assertEquals(List.of(project), data.getRootProjects().getChildren(Project.class));
            assertEquals(List.of(nested), project.getChildren(Project.class));
            assertEquals(doneProjectsOnly ? List.of(scheduled) : List.of(scheduled, ordinaryDone),
                    nested.getChildren(Action.class));
        }
    }

    @Test
    public void completedProjectsStillArchiveAsAWhole() {
        for (boolean historicalEvents : new boolean[]{true, false}) {
            Data data = new Data();
            Project project = new Project(data);
            Action child = done(data);
            project.add(child);
            project.setDone(true);
            project.setDoneDate(date(LocalDate.of(2020, 1, 1)));
            data.getRootProjects().add(project);

            new ArchiveAction().selectArchived(data, cutoff(), true, historicalEvents);

            assertEquals(List.of(project), data.getRootProjects().getChildren(Project.class));
            assertEquals(List.of(child), project.getChildren(Action.class));
        }
    }

    private static Action scheduled(Data data, LocalDate day) {
        Action action = new Action(data);
        ActionStateScheduled state = new ActionStateScheduled();
        state.setDate(date(day));
        action.setState(state);
        return action;
    }

    private static Action done(Data data) {
        Action action = new Action(data);
        action.setDone(true);
        action.setDoneDate(date(LocalDate.of(2020, 1, 1)));
        return action;
    }

    private static Date cutoff() {
        return DateUtils.getEnd(date(LocalDate.of(2020, 1, 3)));
    }

    private static Date date(LocalDate day) {
        return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
