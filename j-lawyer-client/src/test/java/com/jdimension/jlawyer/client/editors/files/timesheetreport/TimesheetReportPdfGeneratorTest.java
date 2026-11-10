package com.jdimension.jlawyer.client.editors.files.timesheetreport;

import com.jdimension.jlawyer.persistence.TimesheetPosition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests that the time sheet report renders as a readable PDF.
 */
public class TimesheetReportPdfGeneratorTest {

    private static Date at(int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(2026, 10, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static List<TimesheetPosition> positions(int count) {
        List<TimesheetPosition> l = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TimesheetPosition p = new TimesheetPosition();
            p.setPrincipal(i % 2 == 0 ? "anna" : "bernd");
            p.setStarted(at(1 + (i % 28), 9, 0));
            p.setStopped(at(1 + (i % 28), 9, 40));
            p.setName("Sachbearbeitung");
            p.setDescription(i % 3 == 0 ? "Telefonat mit Gericht" : "Schriftsatz");
            p.setUnitPrice(BigDecimal.valueOf(i % 4 == 0 ? 0 : 200));
            l.add(p);
        }
        return l;
    }

    @Test
    public void formatsMinutes() {
        assertEquals("0:00", TimesheetReportPdfGenerator.formatMinutes(0));
        assertEquals("9:15", TimesheetReportPdfGenerator.formatMinutes(555));
        assertEquals("12:05", TimesheetReportPdfGenerator.formatMinutes(725));
    }

    @Test
    public void generatesReadablePdf() throws Exception {
        TimesheetReportData data = new TimesheetReportData(positions(60), null, null, null, 15,
                new TimesheetActivityClassifier(Arrays.asList("Telefonat")));
        byte[] pdf = new TimesheetReportPdfGenerator().generate(data, new TimesheetReportPdfGenerator.Options()
                .title("Stundenzettel - alle Personen")
                .subtitle("123/26 Muster ./. Beispiel / Beratung")
                .headerText("Kanzlei Muster")
                .personHeading("alle Personen")
                .allPersons(true)
                .principalNames(p -> "anna".equals(p) ? "Anna Ärger" : "Bernd Groß"));

        try (PDDocument doc = PDDocument.load(pdf)) {
            assertTrue(doc.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Übersicht"));
            assertTrue(text.contains("Arbeitszeit pro Person"));
            assertTrue(text.contains("Arbeitszeit pro Tätigkeit"));
            assertTrue(text.contains("Anna Ärger"));
            assertTrue(text.contains(TimesheetActivityClassifier.OTHER));
            assertTrue(text.contains("Gesamtarbeitszeit: " + TimesheetReportPdfGenerator.formatMinutes(data.getTotalMinutes())));
            assertTrue(text.contains("Seite 1 von " + doc.getNumberOfPages()));
        }
    }

    @Test
    public void generatesPdfWithoutEntries() throws Exception {
        TimesheetReportData data = new TimesheetReportData(new ArrayList<>(), null, null, "anna", 15,
                new TimesheetActivityClassifier(null));
        byte[] pdf = new TimesheetReportPdfGenerator().generate(data, new TimesheetReportPdfGenerator.Options()
                .personHeading("Anna").allPersons(false));
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(doc).contains("keine Zeiteinträge"));
        }
    }
}
