package com.jdimension.jlawyer.client.editors.files.timesheetreport;

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Tests the assignment of time entries to configured activities.
 */
public class TimesheetActivityClassifierTest {

    private final TimesheetActivityClassifier classifier = new TimesheetActivityClassifier(
            Arrays.asList("Schreiben", "Schreiben an Gericht", "Telefonat", " ", null, "Telefonat"));

    @Test
    public void matchesCaseInsensitive() {
        assertEquals("Telefonat", classifier.classify("telefonat mit Mandant"));
    }

    @Test
    public void longestMatchWins() {
        assertEquals("Schreiben an Gericht", classifier.classify("Schreiben an Gericht wegen Fristverlängerung"));
        assertEquals("Schreiben", classifier.classify("Schreiben an Gegner"));
    }

    @Test
    public void noMatchIsOther() {
        assertEquals(TimesheetActivityClassifier.OTHER, classifier.classify("Aktenstudium"));
        assertEquals(TimesheetActivityClassifier.OTHER, classifier.classify(""));
        assertEquals(TimesheetActivityClassifier.OTHER, classifier.classify(null));
    }

    @Test
    public void blankAndDuplicateActivitiesAreIgnored() {
        assertEquals(3, classifier.getActivities().size());
    }

    @Test
    public void noActivitiesConfigured() {
        assertEquals(TimesheetActivityClassifier.OTHER, new TimesheetActivityClassifier(null).classify("Telefonat"));
    }
}
