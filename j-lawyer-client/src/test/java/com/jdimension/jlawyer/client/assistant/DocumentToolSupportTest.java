package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.services.DocumentMetadataPatch.KeywordOperation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the document output and argument handling of the assistant document tools.
 *
 * @author jens
 */
public class DocumentToolSupportTest {

    private static ArchiveFileDocumentsBean doc(String id, String name) {
        ArchiveFileDocumentsBean d = new ArchiveFileDocumentsBean();
        d.setId(id);
        d.setName(name);
        d.setSize(1024);
        return d;
    }

    private static Date date(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTime();
    }

    @Test
    public void testDocumentJsonWithoutMetadata() {
        String json = DocumentToolSupport.documentJson(doc("d1", "scan.pdf"), null, 0);
        assertTrue(json.startsWith("{\"id\": \"d1\", \"name\": \"scan.pdf\", \"size\": 1024"));
        assertFalse(json.contains("title"));
        assertFalse(json.contains("keywords"));
        assertFalse(json.contains("correspondent"));
        assertFalse(json.contains("attachmentCount"));
        assertFalse("no tags in listings", json.contains("tags"));
    }

    @Test
    public void testDocumentJsonWithMetadata() {
        ArchiveFileDocumentsBean mail = doc("m1", "mail.eml");
        mail.setTitle("Vergleichsangebot");
        ArchiveFileDocumentsBean d = doc("d1", "anlage.pdf");
        d.setTitle("Anlage \"K1\"");
        d.setKeywords("Frist, Kosten");
        d.setReceivedDate(date(2026, 9, 28, 14, 5));
        d.setCorrespondentId("c1");
        d.setCorrespondentName("RA Müller");
        d.setCorrespondentDirection(ArchiveFileDocumentsBean.CORRESPONDENT_IN);
        d.setParentId("m1");
        String json = DocumentToolSupport.documentJson(d, mail, 2);
        assertTrue(json.contains("\"title\": \"Anlage \\\"K1\\\"\""));
        assertTrue(json.contains("\"keywords\": [\"Frist\", \"Kosten\"]"));
        assertTrue(json.contains("\"receivedDate\": \"2026-09-28 14:05\""));
        assertTrue(json.contains("\"correspondent\": {\"name\": \"RA Müller\", \"contactId\": \"c1\", \"direction\": \"in\"}"));
        assertTrue(json.contains("\"parentId\": \"m1\", \"parentTitle\": \"Vergleichsangebot\""));
        assertTrue(json.contains("\"attachmentCount\": 2"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    public void testFreeTextCorrespondentHasNoContactId() {
        ArchiveFileDocumentsBean d = doc("d1", "brief.pdf");
        d.setCorrespondentName("Gericht");
        d.setCorrespondentDirection(ArchiveFileDocumentsBean.CORRESPONDENT_OUT);
        String json = DocumentToolSupport.documentJson(d, null, 0);
        assertTrue(json.contains("\"correspondent\": {\"name\": \"Gericht\", \"direction\": \"out\"}"));
    }

    @Test
    public void testCountAttachmentsIgnoresDeleted() {
        ArchiveFileDocumentsBean mail = doc("m1", "mail.eml");
        ArchiveFileDocumentsBean a1 = doc("a1", "a1.pdf");
        a1.setParentId("m1");
        ArchiveFileDocumentsBean a2 = doc("a2", "a2.pdf");
        a2.setParentId("m1");
        a2.setDeleted(true);
        ArchiveFileDocumentsBean deletedParent = doc("p2", "p2.eml");
        deletedParent.setDeleted(true);
        ArchiveFileDocumentsBean orphan = doc("o1", "o1.pdf");
        orphan.setParentId("p2");
        Map<String, Integer> counts = DocumentToolSupport.countAttachments(Arrays.asList(mail, a1, a2, deletedParent, orphan));
        assertEquals(Integer.valueOf(1), counts.get("m1"));
        assertNull("a deleted parent has no attachments", counts.get("p2"));
        assertEquals(1, counts.size());
    }

    @Test
    public void testMatches() {
        ArchiveFileDocumentsBean d = doc("d1", "scan_0815.pdf");
        d.setTitle("Klageerwiderung");
        d.setKeywords("Frist, Kosten");
        d.setCorrespondentName("Landgericht Berlin");
        assertTrue(DocumentToolSupport.matches(d, "0815"));
        assertTrue(DocumentToolSupport.matches(d, "klage"));
        assertTrue(DocumentToolSupport.matches(d, "KOSTEN"));
        assertTrue(DocumentToolSupport.matches(d, "landgericht"));
        assertFalse(DocumentToolSupport.matches(d, "Vergleich"));
        assertTrue(DocumentToolSupport.matches(d, " "));
    }

    @Test
    public void testParseDirection() {
        assertEquals(ArchiveFileDocumentsBean.CORRESPONDENT_IN, DocumentToolSupport.parseDirection(null));
        assertEquals(ArchiveFileDocumentsBean.CORRESPONDENT_IN, DocumentToolSupport.parseDirection("Von"));
        assertEquals(ArchiveFileDocumentsBean.CORRESPONDENT_OUT, DocumentToolSupport.parseDirection(" out "));
        assertEquals(ArchiveFileDocumentsBean.CORRESPONDENT_OUT, DocumentToolSupport.parseDirection("an"));
        assertEquals(ArchiveFileDocumentsBean.CORRESPONDENT_NONE, DocumentToolSupport.parseDirection("none"));
        try {
            DocumentToolSupport.parseDirection("sideways");
            fail("invalid direction accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("sideways"));
        }
    }

    @Test
    public void testParseKeywordOperation() {
        assertEquals(KeywordOperation.ADD, DocumentToolSupport.parseKeywordOperation(null));
        assertEquals(KeywordOperation.ADD, DocumentToolSupport.parseKeywordOperation("hinzufügen"));
        assertEquals(KeywordOperation.REMOVE, DocumentToolSupport.parseKeywordOperation("REMOVE"));
        assertEquals(KeywordOperation.SET, DocumentToolSupport.parseKeywordOperation("set"));
        try {
            DocumentToolSupport.parseKeywordOperation("merge");
            fail("invalid operation accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("merge"));
        }
    }

    @Test
    public void testParseDateTime() {
        assertNull(DocumentToolSupport.parseDateTime(" "));
        assertEquals(date(2026, 9, 28, 0, 0), DocumentToolSupport.parseDateTime("2026-09-28"));
        assertEquals(date(2026, 9, 28, 14, 5), DocumentToolSupport.parseDateTime("2026-09-28 14:05"));
        assertEquals(date(2026, 9, 28, 14, 5), DocumentToolSupport.parseDateTime("2026-09-28T14:05:00"));
        try {
            DocumentToolSupport.parseDateTime("28.09.2026");
            fail("invalid date accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("28.09.2026"));
        }
        try {
            DocumentToolSupport.parseDateTime("2026-02-30");
            fail("non-existing date accepted");
        } catch (IllegalArgumentException expected) {
            // lenient parsing is off
        }
    }

    @Test
    public void testSplitIds() {
        assertEquals(Arrays.asList("a", "b"), DocumentToolSupport.splitIds(" a, b ,, a"));
        assertEquals(new ArrayList<String>(), DocumentToolSupport.splitIds(null));
    }

    @Test
    public void testMetadataHeader() {
        ArchiveFileDocumentsBean d = doc("d1", "brief.pdf");
        assertEquals("", DocumentToolSupport.metadataHeader(d));
        d.setTitle("Mahnung");
        d.setCorrespondentName("Meier GmbH");
        d.setCorrespondentDirection(ArchiveFileDocumentsBean.CORRESPONDENT_OUT);
        d.setKeywords("Frist");
        String nl = System.lineSeparator();
        assertEquals("Bezeichnung: Mahnung" + nl + "An: Meier GmbH" + nl + "Schlagworte: Frist" + nl, DocumentToolSupport.metadataHeader(d));
    }

    @Test
    public void testByIdSkipsDeleted() {
        ArchiveFileDocumentsBean a = doc("a", "a.pdf");
        ArchiveFileDocumentsBean b = doc("b", "b.pdf");
        b.setDeleted(true);
        List<ArchiveFileDocumentsBean> docs = Arrays.asList(a, b);
        Map<String, ArchiveFileDocumentsBean> byId = DocumentToolSupport.byId(docs);
        assertTrue(byId.containsKey("a"));
        assertFalse(byId.containsKey("b"));
    }
}
