/*
 * This file is part of j-lawyer.org, licensed under the GNU Affero General Public License v3.
 * See the LICENSE file in the project root.
 */
package org.jlawyer.cloud;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NextcloudCalendarConnectorTest {

    private static final String ICAL4J_TIMEZONE_UPDATE = "net.fortuna.ical4j.timezone.update.enabled";

    private TimeZone originalDefaultTimeZone;
    private String originalTimeZoneUpdate;

    @Before
    public void useNonBerlinServerTimeZone() {
        // the server's JVM zone must not influence what is written to the calendar
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // ical4j would otherwise download zone data from tzurl.org, which makes the test depend on the network
        originalTimeZoneUpdate = System.getProperty(ICAL4J_TIMEZONE_UPDATE);
        System.setProperty(ICAL4J_TIMEZONE_UPDATE, "false");
    }

    @After
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
        if (originalTimeZoneUpdate == null) {
            System.clearProperty(ICAL4J_TIMEZONE_UPDATE);
        } else {
            System.setProperty(ICAL4J_TIMEZONE_UPDATE, originalTimeZoneUpdate);
        }
    }

    private static Date utc(int year, int month, int day, int hour, int minute) {
        return Date.from(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void timedEventInSummerTimeIsWrittenWithBerlinTzid() throws Exception {
        // 08:00 UTC is 10:00 Berlin (CEST)
        String ics = NextcloudCalendarConnector.buildEventCalendar("uid-1", "Termin", "desc", "Hamburg",
                utc(2026, 10, 12, 8, 0), utc(2026, 10, 12, 9, 0), false, -1);

        assertTrue(ics, ics.contains("DTSTART;TZID=Europe/Berlin:20261012T100000"));
        assertTrue(ics, ics.contains("DTEND;TZID=Europe/Berlin:20261012T110000"));
    }

    @Test
    public void timedEventInWinterTimeIsWrittenWithBerlinTzid() throws Exception {
        // 09:00 UTC is 10:00 Berlin (CET)
        String ics = NextcloudCalendarConnector.buildEventCalendar("uid-2", "Termin", "desc", "Hamburg",
                utc(2026, 12, 1, 9, 0), utc(2026, 12, 1, 10, 0), false, -1);

        assertTrue(ics, ics.contains("DTSTART;TZID=Europe/Berlin:20261201T100000"));
        assertTrue(ics, ics.contains("DTEND;TZID=Europe/Berlin:20261201T110000"));
    }

    @Test
    public void timedEventDoesNotUseFloatingTime() throws Exception {
        String ics = NextcloudCalendarConnector.buildEventCalendar("uid-3", "Termin", "desc", "Hamburg",
                utc(2026, 10, 12, 8, 0), utc(2026, 10, 12, 9, 0), false, -1);

        assertFalse(ics, ics.contains("DTSTART:20261012"));
        assertFalse(ics, ics.contains("DTEND:20261012"));
    }

    @Test
    public void allDayEventStaysADateValue() throws Exception {
        String ics = NextcloudCalendarConnector.buildEventCalendar("uid-4", "Frist", "desc", "",
                utc(2026, 10, 12, 0, 0), null, true, -1);

        assertTrue(ics, ics.contains("DTSTART;VALUE=DATE:20261012"));
        assertFalse(ics, ics.contains("TZID=Europe/Berlin:20261012"));
    }
}
