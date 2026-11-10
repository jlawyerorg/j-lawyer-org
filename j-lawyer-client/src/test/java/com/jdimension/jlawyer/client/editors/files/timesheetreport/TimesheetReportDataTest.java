package com.jdimension.jlawyer.client.editors.files.timesheetreport;

import com.jdimension.jlawyer.persistence.Invoice;
import com.jdimension.jlawyer.persistence.TimesheetPosition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests filtering, rounding and aggregation of the time sheet report.
 */
public class TimesheetReportDataTest {

    private final TimesheetActivityClassifier classifier = new TimesheetActivityClassifier(Arrays.asList("Telefonat", "Schriftsatz"));

    private static Date at(int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(2026, 10, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static TimesheetPosition pos(String principal, Date started, Date stopped, String description, double rate) {
        TimesheetPosition p = new TimesheetPosition();
        p.setPrincipal(principal);
        p.setStarted(started);
        p.setStopped(stopped);
        p.setName("Position");
        p.setDescription(description);
        p.setUnitPrice(BigDecimal.valueOf(rate));
        return p;
    }

    private List<TimesheetPosition> sample() {
        List<TimesheetPosition> l = new ArrayList<>();
        // 7 minutes -> 15 with interval 15
        l.add(pos("anna", at(19, 9, 0), at(19, 9, 7), "Telefonat mit Mandant", 200));
        // 60 minutes, rate 0 -> not billable
        l.add(pos("anna", at(20, 10, 0), at(20, 11, 0), "Schriftsatz Klage", 0));
        // 30 minutes, invoiced -> not billable
        TimesheetPosition invoiced = pos("bernd", at(20, 14, 0), at(20, 14, 30), "Aktenstudium", 150);
        Invoice inv = new Invoice();
        inv.setInvoiceNumber("RE-1");
        invoiced.setInvoice(inv);
        l.add(invoiced);
        // still running -> not evaluated
        l.add(pos("bernd", at(21, 8, 0), null, "Telefonat", 150));
        return l;
    }

    @Test
    public void roundsUpToInterval() {
        assertEquals(15, TimesheetReportData.roundedMinutes(at(19, 9, 0), at(19, 9, 7), 15));
        assertEquals(15, TimesheetReportData.roundedMinutes(at(19, 9, 0), at(19, 9, 15), 15));
        assertEquals(7, TimesheetReportData.roundedMinutes(at(19, 9, 0), at(19, 9, 7), 1));
        assertEquals(0, TimesheetReportData.roundedMinutes(at(19, 9, 7), at(19, 9, 0), 15));
    }

    @Test
    public void aggregatesAllPersons() {
        TimesheetReportData d = new TimesheetReportData(sample(), null, null, null, 15, classifier);
        assertEquals(3, d.getEntries().size());
        assertEquals(1, d.getRunningCount());
        assertEquals(15 + 60 + 30, d.getTotalMinutes());
        assertEquals(15, d.getBillableMinutes());
        assertEquals(Long.valueOf(75), d.getMinutesPerPrincipal().get("anna"));
        assertEquals(Long.valueOf(30), d.getMinutesPerPrincipal().get("bernd"));

        Map<String, Long> perActivity = d.getMinutesPerActivity();
        assertEquals(Long.valueOf(15), perActivity.get("Telefonat"));
        assertEquals(Long.valueOf(60), perActivity.get("Schriftsatz"));
        assertEquals(Long.valueOf(30), perActivity.get(TimesheetActivityClassifier.OTHER));
        // "sonstige" is always last
        assertEquals(TimesheetActivityClassifier.OTHER, new ArrayList<>(perActivity.keySet()).get(perActivity.size() - 1));

        assertEquals(2, d.getEntriesPerDay().size());
        assertEquals(90, d.getMinutesOfDay(LocalDate.of(2026, 10, 20)));
        assertEquals(at(19, 9, 0), d.getFirstStarted());
        assertEquals(at(20, 14, 30), d.getLastStopped());
    }

    @Test
    public void filtersByPrincipal() {
        TimesheetReportData d = new TimesheetReportData(sample(), null, null, "anna", 15, classifier);
        assertEquals(2, d.getEntries().size());
        assertEquals(0, d.getRunningCount());
        assertEquals(75, d.getTotalMinutes());
    }

    @Test
    public void filtersByPeriod() {
        TimesheetReportData d = new TimesheetReportData(sample(), at(20, 0, 0), at(20, 23, 59), null, 15, classifier);
        assertEquals(2, d.getEntries().size());
        assertEquals(90, d.getTotalMinutes());
        assertEquals(0, d.getBillableMinutes());
        assertTrue(d.getEntries().get(1).isInvoiced());
    }

    @Test
    public void emptyResult() {
        TimesheetReportData d = new TimesheetReportData(sample(), at(25, 0, 0), null, null, 15, classifier);
        assertTrue(d.getEntries().isEmpty());
        assertEquals(0, d.getTotalMinutes());
        assertNull(d.getFirstStarted());
    }
}
