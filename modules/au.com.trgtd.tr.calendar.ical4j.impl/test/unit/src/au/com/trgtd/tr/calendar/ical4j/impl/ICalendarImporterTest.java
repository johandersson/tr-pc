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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import tr.model.Data;
import tr.model.action.Action;
import tr.model.action.ActionStateScheduled;

public class ICalendarImporterTest {

    @Test
    public void importsTimedEventAsScheduledAction() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:timed@example.test
                DTSTAMP:20261101T120000Z
                DTSTART:20261105T100000Z
                DTEND:20261105T113000Z
                SUMMARY:Planning session
                DESCRIPTION:Review the plan
                LOCATION:Office
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(1, result.imported());
        assertTrue(data.hasChanged());
        Action action = actions(data).get(0);
        assertEquals("Planning session", action.getDescription());
        assertTrue(action.getNotes().contains("Review the plan"));
        assertTrue(action.getNotes().contains("Location: Office"));
        ActionStateScheduled state = (ActionStateScheduled) action.getState();
        assertSame(Date.class, state.getDate().getClass());
        assertEquals(Date.from(Instant.parse("2026-11-05T10:00:00Z")), state.getDate());
        assertEquals(1, state.getDurationHours());
        assertEquals(30, state.getDurationMinutes());
    }

    @Test
    public void importsEveryDayOfMultiDayAllDayEvent() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:all-day@example.test
                DTSTAMP:20261101T120000Z
                DTSTART;VALUE=DATE:20261110
                DTEND;VALUE=DATE:20261112
                SUMMARY:Conference
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(2, result.imported());
        assertEquals(LocalDate.of(2026, 11, 10), localDate(actions(data).get(0)));
        assertEquals(LocalDate.of(2026, 11, 11), localDate(actions(data).get(1)));
        for (Action action : actions(data)) {
            ActionStateScheduled state = (ActionStateScheduled) action.getState();
            assertEquals(0, state.getDurationHours());
            assertEquals(0, state.getDurationMinutes());
        }
    }

    @Test
    public void expandsRecurringEventsWithinImportWindow() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:recurring@example.test
                DTSTAMP:20261101T120000Z
                DTSTART:20261106T090000Z
                DTEND:20261106T093000Z
                RRULE:FREQ=DAILY;COUNT=3
                SUMMARY:Daily review
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(3, result.imported());
        assertEquals(3, actions(data).size());
    }

    @Test
    public void convertsTimeZoneAndDurationProperties() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:timezone@example.test
                DTSTAMP:20261101T120000Z
                DTSTART;TZID=Europe/Stockholm:20261105T100000
                DURATION:PT45M
                SUMMARY:Local appointment
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(1, result.imported());
        ActionStateScheduled state = (ActionStateScheduled) actions(data).get(0).getState();
        assertEquals(Date.from(Instant.parse("2026-11-05T09:00:00Z")), state.getDate());
        assertEquals(0, state.getDurationHours());
        assertEquals(45, state.getDurationMinutes());
    }

    @Test
    public void skipsDuplicatesOnRepeatedImport() throws Exception {
        Data data = new Data();
        String ics = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:duplicate@example.test
                DTSTAMP:20261101T120000Z
                DTSTART:20261107T090000Z
                SUMMARY:One event
                END:VEVENT
                END:VCALENDAR
                """;

        assertEquals(1, importCalendar(data, ics).imported());
        ICalendarImporter.ImportResult second = importCalendar(data, ics);

        assertEquals(0, second.imported());
        assertEquals(1, second.duplicates());
        assertEquals(1, actions(data).size());
    }

    @Test
    public void skipsCancelledAndInvalidEvents() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:cancelled@example.test
                DTSTAMP:20261101T120000Z
                DTSTART:20261108T090000Z
                STATUS:CANCELLED
                SUMMARY:Cancelled
                END:VEVENT
                BEGIN:VEVENT
                UID:missing-start@example.test
                DTSTAMP:20261101T120000Z
                SUMMARY:Missing start
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(0, result.imported());
        assertEquals(2, result.skipped());
        assertTrue(actions(data).isEmpty());
    }

    @Test
    public void cancelledRecurrenceOverrideSuppressesOriginalOccurrence() throws Exception {
        Data data = new Data();

        ICalendarImporter.ImportResult result = importCalendar(data, """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//ThinkingRock Test//EN
                BEGIN:VEVENT
                UID:exceptions@example.test
                DTSTAMP:20261101T120000Z
                DTSTART:20261106T090000Z
                DTEND:20261106T093000Z
                RRULE:FREQ=DAILY;COUNT=3
                SUMMARY:Daily review
                END:VEVENT
                BEGIN:VEVENT
                UID:exceptions@example.test
                DTSTAMP:20261101T120000Z
                RECURRENCE-ID:20261107T090000Z
                DTSTART:20261107T090000Z
                DTEND:20261107T093000Z
                STATUS:CANCELLED
                SUMMARY:Daily review
                END:VEVENT
                END:VCALENDAR
                """);

        assertEquals(2, result.imported());
        assertEquals(1, result.skipped());
        assertEquals(2, actions(data).size());
    }

    private static ICalendarImporter.ImportResult importCalendar(Data data, String ics)
            throws Exception {
        return ICalendarImporter.importCalendar(
                data,
                new ByteArrayInputStream(ics.getBytes(StandardCharsets.UTF_8)),
                Date.from(Instant.parse("2026-11-05T12:00:00Z")));
    }

    private static List<Action> actions(Data data) {
        return data.getRootActions().getChildren(Action.class);
    }

    private static LocalDate localDate(Action action) {
        ActionStateScheduled state = (ActionStateScheduled) action.getState();
        return state.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
