/*
 * ThinkingRock, a project management tool for Personal Computers.
 * Copyright (C) 2006 Avente Pty Ltd
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
package au.com.trgtd.tr.calendar.ical4j.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterFactoryRegistry;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyFactoryRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.XProperty;
import tr.model.Data;
import tr.model.action.Action;
import tr.model.action.ActionStateScheduled;

final class ICalendarImporter {

    private static final String UID_MARKER_PREFIX = "[ThinkingRock iCalendar UID: ";
    private static final int DEFAULT_TIMED_DURATION_MINUTES = 15;
    private static final int RECURRENCE_PAST_YEARS = 1;
    private static final int RECURRENCE_FUTURE_YEARS = 5;
    private static final int MAX_OCCURRENCES_PER_EVENT = 5000;
    private static final int MAX_ACTION_DURATION_MINUTES = 127 * 60 + 59;

    private ICalendarImporter() {
    }

    static ImportResult importFile(Data data, File file, boolean importHistoricalEvents)
            throws IOException, ParserException {
        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IOException("The selected file cannot be read.");
        }
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return importCalendar(data, input, new Date(), importHistoricalEvents);
        }
    }

    static ImportResult importCalendar(Data data, InputStream input, Date now)
            throws IOException, ParserException {
        return importCalendar(data, input, now, true);
    }

    static ImportResult importCalendar(Data data, InputStream input, Date now,
            boolean importHistoricalEvents) throws IOException, ParserException {
        PropertyFactoryRegistry properties = new PropertyFactoryRegistry();
        // RFC 9074 alarm metadata is not supported by the bundled iCal4j.
        properties.register("ACKNOWLEDGED", new PropertyFactory() {
            @Override
            public Property createProperty(String name) {
                return new XProperty(name);
            }

            @Override
            public Property createProperty(String name, ParameterList parameters, String value) {
                return new XProperty(name, parameters, value);
            }
        });
        ParameterFactoryRegistry parameters = new ParameterFactoryRegistry();
        parameters.register("ACKNOWLEDGED", XParameter::new);
        parameters.register("FILENAME", XParameter::new);
        net.fortuna.ical4j.model.Calendar calendar = new CalendarBuilder(
                CalendarParserFactory.getInstance().createParser(),
                properties, parameters,
                TimeZoneRegistryFactory.getInstance().createRegistry()).build(input);
        List<EventAction> actions = parse(calendar, now);
        Set<String> importedKeys = existingImportKeys(data);
        int imported = 0;
        int duplicates = 0;
        int skipped = 0;
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        clearTime(today);

        for (EventAction eventAction : actions) {
            if (eventAction.skipped()
                    || (!importHistoricalEvents && eventAction.start().before(today.getTime()))) {
                skipped++;
            } else if (!importedKeys.add(eventAction.key())) {
                duplicates++;
            } else {
                Action action = createAction(data, eventAction);
                if (data.getRootActions().add(action)) {
                    imported++;
                } else {
                    skipped++;
                }
            }
        }
        return new ImportResult(imported, duplicates, skipped);
    }

    private static List<EventAction> parse(
            net.fortuna.ical4j.model.Calendar calendar, Date now) {
        List<EventAction> actions = new ArrayList<>();
        ComponentList components = calendar.getComponents(Component.VEVENT);
        Set<String> overriddenOccurrences = overriddenOccurrences(components);
        for (Object component : components) {
            VEvent event = (VEvent) component;
            if (isCancelled(event) || event.getStartDate() == null) {
                actions.add(EventAction.skip());
                continue;
            }
            addEventActions(actions, event, now, overriddenOccurrences);
        }
        return actions;
    }

    private static void addEventActions(
            List<EventAction> actions,
            VEvent event,
            Date now,
            Set<String> overriddenOccurrences) {
        boolean allDay = !(event.getStartDate().getDate() instanceof DateTime);
        List<Period> periods = getPeriods(event, now);
        int count = 0;
        for (Period period : periods) {
            if (event.getRecurrenceId() == null
                    && overriddenOccurrences.contains(occurrenceKey(event, period.getStart()))) {
                continue;
            }
            if (++count > MAX_OCCURRENCES_PER_EVENT) {
                actions.add(EventAction.skip());
                break;
            }
            if (allDay) {
                addAllDayActions(actions, event, period);
            } else {
                addTimedAction(actions, event, period);
            }
        }
    }

    private static List<Period> getPeriods(VEvent event, Date now) {
        List<Period> periods = new ArrayList<>();
        if (isRecurring(event)) {
            Calendar start = Calendar.getInstance();
            start.setTime(now);
            start.add(Calendar.YEAR, -RECURRENCE_PAST_YEARS);
            Calendar end = Calendar.getInstance();
            end.setTime(now);
            end.add(Calendar.YEAR, RECURRENCE_FUTURE_YEARS);
            PeriodList consumed = event.getConsumedTime(
                    new DateTime(start.getTime()), new DateTime(end.getTime()), true);
            for (Object item : consumed) {
                periods.add((Period) item);
            }
        } else {
            Date start = event.getStartDate().getDate();
            Date end = getEnd(event, start);
            periods.add(new Period(new DateTime(start), new DateTime(end)));
        }
        return periods;
    }

    private static Date getEnd(VEvent event, Date start) {
        DateProperty end = event.getEndDate(true);
        if (end != null && end.getDate() != null) {
            return end.getDate();
        }
        return new Date(start.getTime());
    }

    private static void addAllDayActions(
            List<EventAction> actions, VEvent event, Period period) {
        Calendar day = Calendar.getInstance();
        day.setTime(period.getStart());
        clearTime(day);
        Calendar end = Calendar.getInstance();
        end.setTime(period.getEnd());
        clearTime(end);
        if (!end.after(day)) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        while (day.before(end)) {
            Date date = day.getTime();
            actions.add(createEventAction(event, date, 0));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static void addTimedAction(
            List<EventAction> actions, VEvent event, Period period) {
        long durationMillis = period.getEnd().getTime() - period.getStart().getTime();
        long durationMinutes = Math.max(
                DEFAULT_TIMED_DURATION_MINUTES, durationMillis / 60000L);
        Date start = period.getStart();
        while (durationMinutes > 0) {
            int actionDuration = (int) Math.min(
                    durationMinutes, MAX_ACTION_DURATION_MINUTES);
            actions.add(createEventAction(event, start, actionDuration));
            durationMinutes -= actionDuration;
            start = new Date(start.getTime() + actionDuration * 60000L);
        }
    }

    private static EventAction createEventAction(
            VEvent event, Date start, int durationMinutes) {
        String uid = event.getUid() == null
                ? fallbackUid(event, start)
                : sanitize(event.getUid().getValue());
        Date identityDate = event.getRecurrenceId() == null
                ? start
                : event.getRecurrenceId().getDate();
        String key = uid + "|" + identityDate.getTime();
        String description = event.getSummary() == null
                ? "Imported calendar event"
                : event.getSummary().getValue().trim();
        if (description.isEmpty()) {
            description = "Imported calendar event";
        }

        StringBuilder notes = new StringBuilder();
        if (event.getDescription() != null) {
            appendNote(notes, event.getDescription().getValue());
        }
        if (event.getLocation() != null && !event.getLocation().getValue().trim().isEmpty()) {
            appendNote(notes, "Location: " + event.getLocation().getValue().trim());
        }
        appendNote(notes, UID_MARKER_PREFIX + key + "]");
        return new EventAction(
                key,
                description,
                notes.toString(),
                new Date(start.getTime()),
                durationMinutes,
                false);
    }

    private static String fallbackUid(VEvent event, Date start) {
        String summary = event.getSummary() == null ? "" : event.getSummary().getValue();
        String seed = summary + "|" + start.getTime() + "|" + event.toString();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            return UUID.nameUUIDFromBytes(digest).toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private static void appendNote(StringBuilder notes, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (notes.length() > 0) {
            notes.append("\n\n");
        }
        notes.append(value.trim());
    }

    private static boolean isRecurring(VEvent event) {
        return event.getProperty(Property.RRULE) != null
                || event.getProperty(Property.RDATE) != null;
    }

    private static boolean isCancelled(VEvent event) {
        return event.getStatus() != null
                && Status.VEVENT_CANCELLED.getValue().equalsIgnoreCase(event.getStatus().getValue());
    }

    private static Set<String> overriddenOccurrences(ComponentList components) {
        Set<String> occurrences = new HashSet<>();
        for (Object component : components) {
            VEvent event = (VEvent) component;
            if (event.getRecurrenceId() != null) {
                occurrences.add(occurrenceKey(event, event.getRecurrenceId().getDate()));
            }
        }
        return occurrences;
    }

    private static String occurrenceKey(VEvent event, Date date) {
        String uid = event.getUid() == null
                ? fallbackUid(event, date)
                : sanitize(event.getUid().getValue());
        return uid + "|" + date.getTime();
    }

    private static Set<String> existingImportKeys(Data data) {
        Set<String> keys = new HashSet<>();
        for (Action action : data.getRootActions().getChildren(Action.class)) {
            String notes = action.getNotes();
            int offset = 0;
            while (notes != null) {
                int start = notes.indexOf(UID_MARKER_PREFIX, offset);
                if (start < 0) {
                    break;
                }
                int valueStart = start + UID_MARKER_PREFIX.length();
                int end = notes.indexOf(']', valueStart);
                if (end < 0) {
                    break;
                }
                keys.add(notes.substring(valueStart, end));
                offset = end + 1;
            }
        }
        return keys;
    }

    private static Action createAction(Data data, EventAction eventAction) {
        Action action = new Action(data);
        action.setDescription(eventAction.description());
        action.setNotes(eventAction.notes());
        ActionStateScheduled state = new ActionStateScheduled();
        state.setDate(eventAction.start());
        state.setDurationHours(eventAction.durationMinutes() / 60);
        state.setDurationMins(eventAction.durationMinutes() % 60);
        action.setState(state);
        return action;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    record ImportResult(int imported, int duplicates, int skipped) {
    }

    private record EventAction(
            String key,
            String description,
            String notes,
            Date start,
            int durationMinutes,
            boolean skipped) {

        static EventAction skip() {
            return new EventAction("", "", "", new Date(0), 0, true);
        }
    }
}
