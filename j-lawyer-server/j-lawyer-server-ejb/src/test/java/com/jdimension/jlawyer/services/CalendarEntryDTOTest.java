package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.persistence.EventTypes;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

/**
 * Guards the two ways a projected calendar entry can diverge from the entity without anything
 * looking broken at first glance.
 *
 * The chronological list sorts its first column as a string, so a caption that differs from the
 * entity's would break sorting silently. And the calendar panel keeps its rendered entries in a
 * collection and tests membership by identity, so an equals/hashCode that is not id-based would
 * duplicate every entry on every render.
 *
 * @author jens
 */
public class CalendarEntryDTOTest {

    private static Date date(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static ArchiveFileReviewsBean entry(int eventType, Date begin, Date end) {
        ArchiveFileReviewsBean rev = new ArchiveFileReviewsBean();
        rev.setId("entry-1");
        rev.setEventType(eventType);
        rev.setBeginDate(begin);
        rev.setEndDate(end);
        return rev;
    }

    private static void assertCaptionMatches(String what, ArchiveFileReviewsBean rev) {
        CalendarEntryDTO dto = CalendarEntryDTO.fromEntity(rev);
        Assert.assertEquals("caption drifted for " + what, rev.toString(), dto.getCaption());
    }

    @Test
    public void captionMatchesEntityForFollowUp() {
        assertCaptionMatches("follow-up",
                entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 3, 17, 9, 30), null));
    }

    @Test
    public void captionMatchesEntityForRespite() {
        assertCaptionMatches("respite",
                entry(EventTypes.EVENTTYPE_RESPITE, date(2026, 3, 17, 9, 30), null));
    }

    @Test
    public void captionMatchesEntityForAppointmentWithinOneDay() {
        assertCaptionMatches("same-day appointment",
                entry(EventTypes.EVENTTYPE_EVENT, date(2026, 3, 17, 9, 30), date(2026, 3, 17, 11, 0)));
    }

    @Test
    public void captionMatchesEntityForAppointmentSpanningDays() {
        assertCaptionMatches("multi-day appointment",
                entry(EventTypes.EVENTTYPE_EVENT, date(2026, 3, 28, 9, 30), date(2026, 4, 3, 11, 0)));
    }

    @Test
    public void captionMatchesEntityForAppointmentWithoutEnd() {
        assertCaptionMatches("appointment without end date",
                entry(EventTypes.EVENTTYPE_EVENT, date(2026, 3, 17, 9, 30), null));
    }

    @Test
    public void captionMatchesEntityWithoutDates() {
        assertCaptionMatches("entry without dates",
                entry(EventTypes.EVENTTYPE_FOLLOWUP, null, null));
    }

    @Test
    public void eventTypeNameMatchesEntity() {
        for (int type : new int[]{EventTypes.EVENTTYPE_FOLLOWUP, EventTypes.EVENTTYPE_RESPITE, EventTypes.EVENTTYPE_EVENT, 99}) {
            ArchiveFileReviewsBean rev = entry(type, date(2026, 3, 17, 9, 30), null);
            Assert.assertEquals("type name drifted for " + type,
                    rev.getEventTypeName(), CalendarEntryDTO.fromEntity(rev).getEventTypeName());
        }
    }

    @Test
    public void projectionToleratesEntryWithoutCaseAndCalendar() {
        CalendarEntryDTO dto = CalendarEntryDTO.fromEntity(entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 3, 17, 9, 30), null));
        Assert.assertNull(dto.getCaseId());
        Assert.assertNull(dto.getCalendarId());
        Assert.assertEquals(0, dto.getCalendarColor());
    }

    @Test
    public void fromEntityTakesNull() {
        Assert.assertNull(CalendarEntryDTO.fromEntity(null));
    }

    @Test
    public void equalityIsByIdSoRenderingDoesNotDuplicate() {
        CalendarEntryDTO first = CalendarEntryDTO.fromEntity(entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 3, 17, 9, 30), null));

        // same entry, reloaded from the server: a different instance, different summary even
        ArchiveFileReviewsBean reloaded = entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 4, 1, 8, 0), null);
        reloaded.setSummary("changed in the meantime");
        CalendarEntryDTO second = CalendarEntryDTO.fromEntity(reloaded);

        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());

        Collection<CalendarEntryDTO> cache = new ArrayList<>();
        cache.add(first);
        Assert.assertTrue("membership must match by id, or every render duplicates the entry",
                cache.contains(second));
    }

    @Test
    public void entriesWithDifferentIdsAreNotEqual() {
        CalendarEntryDTO first = CalendarEntryDTO.fromEntity(entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 3, 17, 9, 30), null));
        ArchiveFileReviewsBean other = entry(EventTypes.EVENTTYPE_FOLLOWUP, date(2026, 3, 17, 9, 30), null);
        other.setId("entry-2");
        Assert.assertNotEquals(first, CalendarEntryDTO.fromEntity(other));
    }

    @Test
    public void fileNumberIsComposedFromItsTwoColumns() {
        CalendarEntryDTO dto = new CalendarEntryDTO("id", EventTypes.EVENTTYPE_FOLLOWUP, "s", "d", "l",
                date(2026, 3, 17, 9, 30), null, false, "assignee",
                "case-1", "0001/26", "-B", "Kurzrubrum", "wegen", "lawyer",
                "cal-1", "Kalender", 255);
        Assert.assertEquals("0001/26-B", dto.getCaseFileNumber());
    }

    @Test
    public void projectionCtorToleratesMissingCalendarColour() {
        // the calendar is an outer join: for an entry without one the colour column comes back null
        CalendarEntryDTO dto = new CalendarEntryDTO("id", EventTypes.EVENTTYPE_FOLLOWUP, "s", "d", "l",
                date(2026, 3, 17, 9, 30), null, false, null,
                "case-1", "0001/26", null, "Kurzrubrum", "wegen", null,
                null, null, null);
        Assert.assertEquals(0, dto.getCalendarColor());
        Assert.assertEquals("0001/26", dto.getCaseFileNumber());
    }

}
