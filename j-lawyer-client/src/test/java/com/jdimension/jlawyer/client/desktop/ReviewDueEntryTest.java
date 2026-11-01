package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 * The display signature decides whether a reloaded due list has to be rendered again.
 */
public class ReviewDueEntryTest {

    private static ReviewDueEntry entry(String summary, String description) {
        ArchiveFileReviewsBean review = new ArchiveFileReviewsBean();
        review.setSummary(summary);
        review.setDescription(description);

        ReviewDueEntry e = new ReviewDueEntry();
        e.setId("r1");
        e.setArchiveFileId("c1");
        e.setReviewReason(summary);
        e.setDue(new Date(1793520000000l));
        e.setResponsible("admin");
        e.setReview(review);
        return e;
    }

    @Test
    public void sameDataSameSignature() {
        assertEquals(entry("Frist", "x").getDisplaySignature(), entry("Frist", "x").getDisplaySignature());
    }

    @Test
    public void changedShownFieldChangesSignature() {
        assertNotEquals(entry("Frist", "x").getDisplaySignature(), entry("Termin", "x").getDisplaySignature());
    }

    @Test
    public void changedReviewChangesSignature() {
        // the entry's review is used when acting on it, e.g. postponing - it must not stay outdated
        assertNotEquals(entry("Frist", "x").getDisplaySignature(), entry("Frist", "y").getDisplaySignature());

        ReviewDueEntry withReminder = entry("Frist", "x");
        withReminder.getReview().setReminderMinutes(30);
        assertNotEquals(entry("Frist", "x").getDisplaySignature(), withReminder.getDisplaySignature());
    }

    @Test
    public void separatorInValuesDoesNotMakeEntriesEqual() {
        // joined into one string, ("a|b", "c") and ("a", "b|c") would look the same
        ReviewDueEntry first = entry("Frist", "a|b");
        first.getReview().setLocation("c");
        ReviewDueEntry second = entry("Frist", "a");
        second.getReview().setLocation("b|c");
        assertNotEquals(first.getDisplaySignature(), second.getDisplaySignature());
    }
}
