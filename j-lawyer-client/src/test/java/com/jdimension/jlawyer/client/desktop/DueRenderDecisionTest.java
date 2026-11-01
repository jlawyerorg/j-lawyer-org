package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Only the automatic reloads may skip rendering an unchanged due list. Every other load renders, as
 * the displayed entries may have been changed in place before it.
 */
public class DueRenderDecisionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private static List<ReviewDueEntry> entries(Date due) {
        ArchiveFileReviewsBean review = new ArchiveFileReviewsBean();
        review.setBeginDate(due);
        ReviewDueEntry e = new ReviewDueEntry();
        e.setId("r1");
        e.setDue(due);
        e.setReview(review);
        return Arrays.asList(e);
    }

    @Test
    public void eventTriggeredLoadAfterLocalPostponeRenders() {
        // postponing changes the displayed entry in place, so it already equals the reloaded one -
        // the event-triggered load must render anyway, the headers still show the old date
        Date postponed = new Date(1794000000000l);
        List<ReviewDueEntry> displayed = entries(postponed);
        List<ReviewDueEntry> loaded = entries(postponed);
        assertFalse(DesktopPanel.canSkipDueRender(false, TODAY, TODAY, true, Boolean.TRUE, displayed, loaded));
    }

    @Test
    public void automaticReloadSkipsUnchangedEntries() {
        Date due = new Date(1794000000000l);
        assertTrue(DesktopPanel.canSkipDueRender(true, TODAY, TODAY, true, Boolean.TRUE, entries(due), entries(due)));
    }

    @Test
    public void automaticReloadRendersChangedEntries() {
        assertFalse(DesktopPanel.canSkipDueRender(true, TODAY, TODAY, true, Boolean.TRUE, entries(new Date(1794000000000l)), entries(new Date(1794086400000l))));
    }

    @Test
    public void automaticReloadRendersOnNewDayOrOtherCompactView() {
        Date due = new Date(1794000000000l);
        assertFalse(DesktopPanel.canSkipDueRender(true, TODAY.plusDays(1), TODAY, true, Boolean.TRUE, entries(due), entries(due)));
        assertFalse(DesktopPanel.canSkipDueRender(true, TODAY, TODAY, false, Boolean.TRUE, entries(due), entries(due)));
        assertFalse(DesktopPanel.canSkipDueRender(true, TODAY, null, true, null, entries(due), entries(due)));
    }
}
