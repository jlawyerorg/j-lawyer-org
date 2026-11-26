package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.client.assistant.MailAttachmentTextSupport.Budget;
import com.jdimension.jlawyer.client.assistant.MailAttachmentTextSupport.Kind;
import com.jdimension.jlawyer.client.assistant.MailAttachmentTextSupport.Result;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the attachment text extraction of the assistant tool get_email.
 *
 * @author jens
 */
public class MailAttachmentTextSupportTest {

    private static final MailAttachmentTextSupport.ServerExtractor NO_SERVER = (data, name, max) -> {
        throw new AssertionError("server must not be called");
    };

    @Test
    public void testClassifyByExtension() {
        assertEquals(Kind.PDF, MailAttachmentTextSupport.classify("Rechnung.PDF", "application/octet-stream").getKind());
        assertEquals(Kind.TEXT, MailAttachmentTextSupport.classify("nachricht.xml", null).getKind());
        assertEquals(Kind.HTML, MailAttachmentTextSupport.classify("seite.htm", null).getKind());
        assertEquals(Kind.CALENDAR, MailAttachmentTextSupport.classify("termin.ics", null).getKind());
        assertEquals(Kind.SERVER, MailAttachmentTextSupport.classify("Schriftsatz.docx", null).getKind());
        assertEquals(Kind.SERVER, MailAttachmentTextSupport.classify("weitergeleitet.eml", null).getKind());
        assertEquals(Kind.SERVER, MailAttachmentTextSupport.classify("tabelle.ods", null).getKind());
    }

    @Test
    public void testClassifyByContentType() {
        assertEquals(Kind.PDF, MailAttachmentTextSupport.classify("ohne-endung", "application/pdf; name=x").getKind());
        assertEquals(Kind.SERVER, MailAttachmentTextSupport.classify(null, "message/rfc822").getKind());
        assertEquals(Kind.CALENDAR, MailAttachmentTextSupport.classify("invite", "text/calendar; method=REQUEST").getKind());
        assertEquals(Kind.TEXT, MailAttachmentTextSupport.classify("", "text/plain").getKind());
        assertEquals(Kind.SERVER, MailAttachmentTextSupport.classify("x", "application/vnd.openxmlformats-officedocument.wordprocessingml.document").getKind());
    }

    @Test
    public void testClassifySkipped() {
        assertSkipped("foto.jpg", null);
        assertSkipped("scan", "image/tiff");
        assertSkipped("akten.zip", null);
        assertSkipped("smime.p7s", "application/pkcs7-signature");
        assertSkipped("winmail.dat", "application/ms-tnef");
        assertSkipped("programm.exe", "application/octet-stream");
    }

    private static void assertSkipped(String name, String contentType) {
        MailAttachmentTextSupport.Classification c = MailAttachmentTextSupport.classify(name, contentType);
        assertEquals(Kind.SKIP, c.getKind());
        assertNotNull(c.getReason());
    }

    @Test
    public void testTextDecodingWithFallback() {
        Result utf8 = MailAttachmentTextSupport.extract(Kind.TEXT, "a.txt", "Grüße".getBytes(StandardCharsets.UTF_8), 100, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_OK, utf8.getStatus());
        assertEquals("Grüße", utf8.getText());

        Result latin1 = MailAttachmentTextSupport.extract(Kind.TEXT, "a.csv", "Grüße".getBytes(StandardCharsets.ISO_8859_1), 100, NO_SERVER);
        assertEquals("Grüße", latin1.getText());
    }

    @Test
    public void testHtml() {
        Result r = MailAttachmentTextSupport.extract(Kind.HTML, "a.html", "<html><body><p>Hallo Welt</p></body></html>".getBytes(StandardCharsets.UTF_8), 100, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_OK, r.getStatus());
        assertTrue(r.getText().contains("Hallo Welt"));
        assertFalse(r.getText().contains("<p>"));
    }

    @Test
    public void testTruncation() {
        Result r = MailAttachmentTextSupport.extract(Kind.TEXT, "a.txt", "0123456789".getBytes(StandardCharsets.UTF_8), 4, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_TRUNCATED, r.getStatus());
        assertTrue(r.isTruncated());
        assertEquals("0123", r.getText());
        assertEquals(10, r.getTotalChars());
    }

    @Test
    public void testServerExtractionDetectsTruncation() {
        Result r = MailAttachmentTextSupport.extract(Kind.SERVER, "a.docx", new byte[]{1}, 5,
                (data, name, max) -> "abcdefghij".substring(0, Math.min(10, max)));
        assertEquals(MailAttachmentTextSupport.STATUS_TRUNCATED, r.getStatus());
        assertEquals("abcde", r.getText());
        assertEquals(-1, r.getTotalChars());

        Result exact = MailAttachmentTextSupport.extract(Kind.SERVER, "a.docx", new byte[]{1}, 10, (data, name, max) -> "abcdefghij");
        assertEquals(MailAttachmentTextSupport.STATUS_OK, exact.getStatus());
    }

    @Test
    public void testServerErrorDoesNotThrow() {
        Result r = MailAttachmentTextSupport.extract(Kind.SERVER, "a.docx", new byte[]{1}, 10, (data, name, max) -> {
            throw new Exception("kaputt");
        });
        assertEquals(MailAttachmentTextSupport.STATUS_ERROR, r.getStatus());
        assertTrue(r.getNote().contains("kaputt"));
        assertNull(r.getText());
    }

    @Test
    public void testEmptyContent() {
        assertEquals(MailAttachmentTextSupport.STATUS_NO_TEXT, MailAttachmentTextSupport.extract(Kind.TEXT, "a.txt", new byte[0], 10, NO_SERVER).getStatus());
        assertEquals(MailAttachmentTextSupport.STATUS_NO_TEXT, MailAttachmentTextSupport.extract(Kind.TEXT, "a.txt", "  \n ".getBytes(StandardCharsets.UTF_8), 10, NO_SERVER).getStatus());
    }

    @Test
    public void testPdfWithText() throws Exception {
        Result r = MailAttachmentTextSupport.extract(Kind.PDF, "a.pdf", pdf("Hallo Anlage"), 1000, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_OK, r.getStatus());
        assertTrue(r.getText().contains("Hallo Anlage"));
    }

    @Test
    public void testPdfWithoutText() throws Exception {
        Result r = MailAttachmentTextSupport.extract(Kind.PDF, "scan.pdf", pdf(null), 1000, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_NO_TEXT, r.getStatus());
        assertNotNull(r.getNote());
    }

    @Test
    public void testBrokenPdf() {
        Result r = MailAttachmentTextSupport.extract(Kind.PDF, "a.pdf", "kein pdf".getBytes(StandardCharsets.UTF_8), 1000, NO_SERVER);
        assertEquals(MailAttachmentTextSupport.STATUS_ERROR, r.getStatus());
    }

    @Test
    public void testBudget() {
        Budget b = new Budget(100, 3);
        assertTrue(b.hasRoom());
        assertEquals(60, b.allowance(60));
        b.consume(60);
        assertEquals(40, b.allowance(60));
        b.consume(40);
        assertFalse(b.hasRoom());

        Budget count = new Budget(1000, 2);
        count.consume(0);
        count.consume(0);
        assertFalse(count.hasRoom());
    }

    private static byte[] pdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
