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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import tr.model.Data;
import tr.model.Item.Item;
import tr.model.Item.ItemList;
import tr.model.action.Action;
import tr.model.action.ActionState;
import tr.model.action.ActionStateDelegated;
import tr.model.action.ActionStateScheduled;
import tr.model.future.Future;
import tr.model.project.Project;
import tr.model.thought.Thought;

/**
 * Snapshot of the data shown on the overview dashboard. Built with a single
 * pass over the projects tree and the single actions.
 */
public final class DashboardModel {

    /** Number of next actions shown before the list is expanded. */
    static final int NEXT_ACTIONS_LIMIT = 10;
    private static final Comparator<Action> NEXT_ACTION_ORDER = Comparator
            .comparing((Action action) -> action.getContext().getName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Action::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Action::getDescription, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<Action> DUE_ORDER = Comparator
            .comparing(Action::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Action::getDescription, String.CASE_INSENSITIVE_ORDER);

    /** A project, its depth in the projects tree and its active action count. */
    public record ProjectInfo(Project project, int depth, int activeActions) {}

    /** Scheduled actions for today, sorted by time. */
    public final List<Action> today = new ArrayList<>();
    /** Scheduled actions for the upcoming days after today, sorted by time. */
    public final List<Action> upcoming = new ArrayList<>();
    /** Unfinished scheduled actions whose scheduled time has passed. */
    public final List<Action> missedScheduled = new ArrayList<>();
    /** Not done, non-scheduled actions due today or earlier. */
    public final List<Action> due = new ArrayList<>();
    /** Not done, non-scheduled actions due before today. */
    public final List<Action> overdue = new ArrayList<>();
    /** Not done, non-scheduled actions due today. */
    public final List<Action> dueToday = new ArrayList<>();
    /** Delegated actions with a follow-up date today or earlier. */
    public final List<Action> followUps = new ArrayList<>();
    /** Available non-Calls Do ASAP actions, sorted by context then due date. */
    public final List<Action> nextActions = new ArrayList<>();
    /** All unfinished Calls actions, earliest due date first. */
    public final List<Action> calls = new ArrayList<>();
    /** Available delegated actions or actions in the Waiting For context. */
    public final List<Action> waitingFor = new ArrayList<>();
    /** Someday/Maybe records, independent of action contexts. */
    public final List<Future> somedayMaybe = new ArrayList<>();
    /** Not done projects with at least one active action, in tree order. */
    public final List<ProjectInfo> activeProjects = new ArrayList<>();
    /** Started, not done projects without any active action, in tree order. */
    public final List<ProjectInfo> stalledProjects = new ArrayList<>();
    public int inboxCount;
    public int doAsapCount;
    public int nextActionsCount;
    public int callsCount;

    private final long startToday;
    private final long startTomorrow;
    private final long endUpcoming;
    private final long now;

    private DashboardModel(Date now, int upcomingDays) {
        this.now = now.getTime();
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        startToday = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 1);
        startTomorrow = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, upcomingDays);
        endUpcoming = c.getTimeInMillis();
    }

    /**
     * Builds the dashboard model.
     * @param data The data, may be null.
     * @param now The current time.
     * @param upcomingDays The number of days after today to list as upcoming.
     * @return The model.
     */
    public static DashboardModel build(Data data, Date now, int upcomingDays) {
        DashboardModel model = new DashboardModel(now, upcomingDays);
        if (data == null) {
            return model;
        }
        for (Thought thought : data.getThoughtManager().list()) {
            if (!thought.isProcessed()) {
                model.inboxCount++;
            }
        }
        model.visit(data.getRootProjects(), -1);
        model.visit(data.getRootActions(), -1);
        model.somedayMaybe.addAll(data.getFutureManager().list());
        model.somedayMaybe.sort(Comparator.comparing(Future::getDescription, String.CASE_INSENSITIVE_ORDER));
        model.nextActions.sort(NEXT_ACTION_ORDER);
        model.waitingFor.sort(DUE_ORDER);
        model.calls.sort(DUE_ORDER);

        Comparator<Action> byScheduled = Comparator.comparing(DashboardModel::scheduledDate);
        model.today.sort(byScheduled);
        model.upcoming.sort(byScheduled);
        model.missedScheduled.sort(byScheduled);
        model.due.sort(Comparator.comparing(Action::getDueDate));
        model.overdue.sort(Comparator.comparing(Action::getDueDate));
        model.dueToday.sort(Comparator.comparing(Action::getDueDate));
        model.followUps.sort(Comparator.comparing(a -> ((ActionStateDelegated) a.getState()).getDate()));
        return model;
    }

    /* Returns the number of active actions in the subtree of the project. */
    private int visit(Project project, int depth) {
        boolean listed = depth >= 0 && !project.isDone();
        int activeIndex = activeProjects.size();
        int stalledIndex = stalledProjects.size();
        int active = 0;
        for (Item child : project.getChildren()) {
            if (child instanceof Action action) {
                if (classify(action)) {
                    active++;
                }
            } else if (child instanceof Project sub) {
                active += visit(sub, depth + 1);
            }
        }
        if (listed) {
            if (active > 0) {
                activeProjects.add(activeIndex, new ProjectInfo(project, depth, active));
            } else if (!isNotStarted(project)) {
                stalledProjects.add(stalledIndex, new ProjectInfo(project, depth, 0));
            }
        }
        return active;
    }

    /* Adds the action to the matching dashboard lists and returns whether it is active. */
    private boolean classify(Action action) {
        if (action.isDone()) {
            return false;
        }
        ActionState state = action.getState();
        Date start = action.getStartDate();
        if (action.isActive() && (state instanceof ActionStateDelegated
                || action.getContext().hasName("Waiting For"))
                && (start == null || start.getTime() < startTomorrow)) {
            waitingFor.add(action);
        }
        boolean call = action.getContext().hasName("Calls");
        if (call) {
            callsCount++;
            calls.add(action);
        }
        if (state instanceof ActionStateScheduled scheduled) {
            Date date = scheduled.getDate();
            if (date != null) {
                long t = date.getTime();
                if (t < now) {
                    missedScheduled.add(action);
                }
                if (t >= startToday && t < startTomorrow) {
                    today.add(action);
                } else if (t >= startTomorrow && t < endUpcoming) {
                    upcoming.add(action);
                }
            }
        } else {
            if (state instanceof ActionStateDelegated delegated) {
                Date followUp = delegated.getDate();
                if (followUp != null && followUp.getTime() < startTomorrow) {
                    followUps.add(action);
                }
            } else if (action.isStateASAP()) {
                if (start == null || start.getTime() < startTomorrow) {
                    doAsapCount++;
                    if (!call) {
                        nextActionsCount++;
                        nextActions.add(action);
                    }
                }
            }
            Date dueDate = action.getDueDate();
            if (dueDate != null && dueDate.getTime() < startTomorrow) {
                due.add(action);
                if (dueDate.getTime() < startToday) {
                    overdue.add(action);
                } else {
                    dueToday.add(action);
                }
            }
        }
        return action.isActive();
    }

    private boolean isNotStarted(Project project) {
        Date start = project.getStartDate();
        return start != null && start.getTime() >= startTomorrow;
    }

    private static Date scheduledDate(Action action) {
        Date date = ((ActionStateScheduled) action.getState()).getDate();
        return date == null ? new Date(0) : date;
    }

    /**
     * Gets the project that directly contains the action.
     * @param action The action.
     * @return The project or null for single actions.
     */
    public static Project getProject(Action action) {
        ItemList parent = action.getParent();
        if (parent instanceof Project project && !project.isRoot()) {
            return project;
        }
        return null;
    }
}
