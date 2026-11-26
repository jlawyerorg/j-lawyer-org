package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.email.CommonMailUtils;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Text extraction for the attachments of an e-mail as returned by the assistant tool get_email
 * with includeAttachments=true. Decides per attachment whether and how its text is extracted and
 * keeps the amount of returned text within a per-attachment limit and an overall budget. The
 * only server call (Tika extraction for office formats) is passed in as {@link ServerExtractor},
 * so the class can be unit tested.
 *
 * @author jens
 */
public class MailAttachmentTextSupport {

    /** default number of characters returned per attachment */
    public static final int DEFAULT_CHARS_PER_ATTACHMENT = 10000;
    /** upper bound of characters per attachment, same as get_document_text */
    public static final int MAX_CHARS_PER_ATTACHMENT = 30000;
    /** characters returned for all attachments of one message together */
    public static final int TOTAL_BUDGET_CHARS = 40000;
    /** attachments larger than this are not downloaded */
    public static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    /** at most this many attachments of one message are extracted */
    public static final int MAX_EXTRACTED_ATTACHMENTS = 10;
    /** PDF extraction stops after this page */
    public static final int MAX_PDF_PAGES = 15;

    public static final String STATUS_OK = "ok";
    public static final String STATUS_TRUNCATED = "truncated";
    public static final String STATUS_NO_TEXT = "noText";
    public static final String STATUS_ENCRYPTED = "encrypted";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_BUDGET_EXCEEDED = "budgetExceeded";
    public static final String STATUS_ERROR = "error";

    /**
     * How the text of an attachment is obtained.
     */
    public enum Kind {
        PDF, TEXT, HTML, CALENDAR, SERVER, SKIP
    }

    /**
     * Server side text extraction (Apache Tika), e.g. IntegrationServiceRemote.extractText.
     */
    @FunctionalInterface
    public interface ServerExtractor {

        String extract(byte[] data, String fileName, int maxChars) throws Exception;
    }

    /**
     * Result of the classification of an attachment.
     */
    public static class Classification {

        private final Kind kind;
        private final String reason;

        Classification(Kind kind, String reason) {
            this.kind = kind;
            this.reason = reason;
        }

        public Kind getKind() {
            return kind;
        }

        /**
         * @return why the attachment is skipped, null unless the kind is SKIP
         */
        public String getReason() {
            return reason;
        }
    }

    /**
     * Result of the text extraction of one attachment.
     */
    public static class Result {

        private final String status;
        private final String text;
        private final String note;
        private final int totalChars;

        Result(String status, String text, String note, int totalChars) {
            this.status = status;
            this.text = text;
            this.note = note;
            this.totalChars = totalChars;
        }

        public String getStatus() {
            return status;
        }

        /**
         * @return the (possibly truncated) text, null if no text is returned
         */
        public String getText() {
            return text;
        }

        /**
         * @return an explanation for the model, may be null
         */
        public String getNote() {
            return note;
        }

        /**
         * @return number of characters before truncation, -1 if unknown
         */
        public int getTotalChars() {
            return totalChars;
        }

        public boolean isTruncated() {
            return STATUS_TRUNCATED.equals(status);
        }
    }

    /**
     * Character budget shared by all attachments of one message.
     */
    public static class Budget {

        private int remainingChars;
        private int remainingAttachments;

        public Budget(int totalChars, int maxAttachments) {
            this.remainingChars = totalChars;
            this.remainingAttachments = maxAttachments;
        }

        /**
         * @return true if another attachment may be extracted
         */
        public boolean hasRoom() {
            return remainingChars > 0 && remainingAttachments > 0;
        }

        /**
         * @param perAttachmentMax the per-attachment limit
         * @return the number of characters the next attachment may return
         */
        public int allowance(int perAttachmentMax) {
            return Math.max(0, Math.min(perAttachmentMax, remainingChars));
        }

        /**
         * Books an extracted attachment.
         *
         * @param chars number of characters returned for it
         */
        public void consume(int chars) {
            remainingChars -= Math.max(0, chars);
            remainingAttachments--;
        }

        public int getRemainingChars() {
            return remainingChars;
        }
    }

    private MailAttachmentTextSupport() {
    }

    /**
     * Decides how the text of an attachment is obtained, based on the file extension and, if
     * that is not conclusive, the content type. Does not look at the size.
     *
     * @param name attachment file name, may be null
     * @param contentType MIME type, may be null
     * @return the classification, never null
     */
    public static Classification classify(String name, String contentType) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        String ct = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        int semicolon = ct.indexOf(';');
        if (semicolon >= 0) {
            ct = ct.substring(0, semicolon).trim();
        }
        String ext = "";
        int dot = n.lastIndexOf('.');
        if (dot >= 0 && dot < n.length() - 1) {
            ext = n.substring(dot + 1);
        }

        if ("winmail.dat".equals(n) || "application/ms-tnef".equals(ct) || "application/vnd.ms-tnef".equals(ct)) {
            return skip("Outlook-Container (winmail.dat), Text nicht extrahierbar");
        }

        switch (ext) {
            case "pdf":
                return of(Kind.PDF);
            case "txt":
            case "csv":
            case "md":
            case "json":
            case "xml":
                return of(Kind.TEXT);
            case "html":
            case "htm":
                return of(Kind.HTML);
            case "ics":
                return of(Kind.CALENDAR);
            case "doc":
            case "docx":
            case "dot":
            case "dotx":
            case "odt":
            case "fodt":
            case "rtf":
            case "xls":
            case "xlsx":
            case "ods":
            case "ppt":
            case "pptx":
            case "odp":
            case "eml":
                return of(Kind.SERVER);
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "tif":
            case "tiff":
            case "heic":
            case "webp":
                return skip("Bild, keine Texterkennung");
            case "zip":
            case "7z":
            case "rar":
            case "gz":
            case "tar":
                return skip("Archiv, Inhalt wird nicht entpackt");
            case "p7s":
            case "p7m":
            case "p7c":
            case "sig":
                return skip("Signatur oder verschlüsselter Container");
            default:
                break;
        }

        // no or unknown extension - fall back to the content type
        if ("application/pdf".equals(ct)) {
            return of(Kind.PDF);
        }
        if ("text/html".equals(ct)) {
            return of(Kind.HTML);
        }
        if ("text/calendar".equals(ct)) {
            return of(Kind.CALENDAR);
        }
        if ("message/rfc822".equals(ct)) {
            return of(Kind.SERVER);
        }
        if (ct.startsWith("text/") || "application/json".equals(ct) || "application/xml".equals(ct)) {
            return of(Kind.TEXT);
        }
        if (ct.startsWith("image/")) {
            return skip("Bild, keine Texterkennung");
        }
        if (ct.startsWith("application/vnd.openxmlformats-officedocument") || ct.startsWith("application/vnd.oasis.opendocument")
                || "application/msword".equals(ct) || "application/vnd.ms-excel".equals(ct)
                || "application/vnd.ms-powerpoint".equals(ct) || "application/rtf".equals(ct)) {
            return of(Kind.SERVER);
        }
        return skip("Format wird nicht unterstützt");
    }

    /**
     * Extracts the text of an attachment whose content has been loaded.
     *
     * @param kind the classification kind, not SKIP
     * @param name the attachment file name
     * @param data the attachment content
     * @param maxChars characters to return at most
     * @param server server side extraction for office formats and forwarded e-mails
     * @return the result, never null; failures are reported as status error
     */
    public static Result extract(Kind kind, String name, byte[] data, int maxChars, ServerExtractor server) {
        if (data == null || data.length == 0) {
            return new Result(STATUS_NO_TEXT, null, "Anhang ist leer", -1);
        }
        String text;
        boolean pdfCut = false;
        try {
            switch (kind) {
                case PDF:
                    try (PDDocument pdf = PDDocument.load(data)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSortByPosition(true);
                        stripper.setStartPage(1);
                        int pages = pdf.getNumberOfPages();
                        stripper.setEndPage(Math.min(pages, MAX_PDF_PAGES));
                        pdfCut = pages > MAX_PDF_PAGES;
                        text = stripper.getText(pdf);
                    }
                    break;
                case TEXT:
                case CALENDAR:
                    text = decode(data);
                    break;
                case HTML:
                    text = CommonMailUtils.html2Text(decode(data));
                    break;
                case SERVER:
                    // ask for one character more to detect truncation
                    text = server.extract(data, name, maxChars + 1);
                    break;
                default:
                    return new Result(STATUS_SKIPPED, null, "Format wird nicht unterstützt", -1);
            }
        } catch (InvalidPasswordException ipe) {
            return new Result(STATUS_ENCRYPTED, null, "PDF ist kennwortgeschützt", -1);
        } catch (Throwable t) {
            return new Result(STATUS_ERROR, null, "Text konnte nicht extrahiert werden: " + t.getMessage(), -1);
        }

        if (text == null || text.isBlank()) {
            String note = kind == Kind.PDF ? "Kein Text erkennbar (vermutlich gescanntes PDF, keine Texterkennung)" : "Kein Text enthalten";
            return new Result(STATUS_NO_TEXT, null, note, 0);
        }
        text = text.strip();
        return truncate(text, maxChars, kind == Kind.SERVER, pdfCut);
    }

    /**
     * Cuts the text to the given length.
     *
     * @param text the full text
     * @param maxChars characters to keep
     * @param lengthUnknown true if the text was already capped by the extractor, so its full length
     * is unknown
     * @param pdfCut true if only the first pages of a PDF were read
     * @return the result with status ok or truncated
     */
    static Result truncate(String text, int maxChars, boolean lengthUnknown, boolean pdfCut) {
        if (text.length() > maxChars) {
            int total = lengthUnknown || pdfCut ? -1 : text.length();
            return new Result(STATUS_TRUNCATED, text.substring(0, maxChars), null, total);
        }
        if (pdfCut) {
            return new Result(STATUS_TRUNCATED, text, "Nur die ersten " + MAX_PDF_PAGES + " Seiten ausgewertet", -1);
        }
        return new Result(STATUS_OK, text, null, text.length());
    }

    /**
     * Decodes text content as UTF-8, falling back to ISO-8859-1 for legacy encodings.
     *
     * @param data the bytes
     * @return the decoded text
     */
    static String decode(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException ex) {
            return new String(data, StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Builds a result without text for an attachment that is not extracted.
     *
     * @param status skipped or budgetExceeded
     * @param note the reason
     * @return the result
     */
    public static Result notExtracted(String status, String note) {
        return new Result(status, null, note, -1);
    }

    private static Classification of(Kind kind) {
        return new Classification(kind, null);
    }

    private static Classification skip(String reason) {
        return new Classification(Kind.SKIP, reason);
    }
}
