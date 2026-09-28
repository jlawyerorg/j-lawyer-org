/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.documents.DocumentKeywords;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.services.DocumentMetadataPatch.KeywordOperation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Document related helpers of the assistant tools: the JSON representation of a document with
 * its metadata and the preparation of tool arguments. Kept free of server calls so it can be
 * unit tested.
 *
 * @author jens
 */
public class DocumentToolSupport {

    private DocumentToolSupport() {
    }

    /**
     * Counts the attachments (children) of the documents of a case. Deleted documents are ignored
     * as parents and as children, so a document whose parent is in the recycle bin counts as
     * independent.
     *
     * @param documents the documents of a case
     * @return document id to its number of attachments; documents without attachments are missing
     */
    public static Map<String, Integer> countAttachments(Collection<ArchiveFileDocumentsBean> documents) {
        Map<String, ArchiveFileDocumentsBean> byId = new HashMap<>();
        for (ArchiveFileDocumentsBean d : documents) {
            if (!d.isDeleted()) {
                byId.put(d.getId(), d);
            }
        }
        Map<String, Integer> counts = new HashMap<>();
        for (ArchiveFileDocumentsBean d : byId.values()) {
            if (d.getParentId() != null && byId.containsKey(d.getParentId())) {
                counts.merge(d.getParentId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * @param documents the documents of a case
     * @return id to document, without deleted documents
     */
    public static Map<String, ArchiveFileDocumentsBean> byId(Collection<ArchiveFileDocumentsBean> documents) {
        Map<String, ArchiveFileDocumentsBean> byId = new HashMap<>();
        for (ArchiveFileDocumentsBean d : documents) {
            if (!d.isDeleted()) {
                byId.put(d.getId(), d);
            }
        }
        return byId;
    }

    /**
     * Whether a document matches a search term in its file name, title, keywords or
     * correspondent name (case-insensitive, partial match).
     *
     * @param doc the document
     * @param query the search term
     * @return true if it matches
     */
    public static boolean matches(ArchiveFileDocumentsBean doc, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return contains(doc.getName(), q) || contains(doc.getTitle(), q) || contains(doc.getKeywords(), q) || contains(doc.getCorrespondentName(), q);
    }

    private static boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    /**
     * The JSON object of a document as returned by the listing tools: id, name, size, creation
     * date and folder, plus the metadata fields that are set. Tags and messages are not included
     * (see get_document_details).
     *
     * @param doc the document
     * @param parent the parent document, null if there is none or it is not available
     * @param attachmentCount the number of attachments of the document
     * @return the JSON object
     */
    public static String documentJson(ArchiveFileDocumentsBean doc, ArchiveFileDocumentsBean parent, int attachmentCount) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        sb.append(", \"size\": ").append(doc.getSize());
        if (doc.getCreationDate() != null) {
            sb.append(", \"creationDate\": \"").append(formatDay(doc.getCreationDate())).append("\"");
        }
        if (doc.getFolder() != null) {
            sb.append(", \"folder\": \"").append(ToolJsonUtils.escapeJson(doc.getFolder().getName())).append("\"");
        }
        appendMetadata(sb, doc, parent, attachmentCount);
        return sb.append("}").toString();
    }

    /**
     * Appends the metadata fields that are set, each preceded by a comma.
     *
     * @param sb the JSON object being built
     * @param doc the document
     * @param parent the parent document, may be null
     * @param attachmentCount the number of attachments
     */
    public static void appendMetadata(StringBuilder sb, ArchiveFileDocumentsBean doc, ArchiveFileDocumentsBean parent, int attachmentCount) {
        if (doc.getTitle() != null && !doc.getTitle().isBlank()) {
            sb.append(", \"title\": \"").append(ToolJsonUtils.escapeJson(doc.getTitle())).append("\"");
        }
        List<String> keywords = DocumentKeywords.split(doc.getKeywords());
        if (!keywords.isEmpty()) {
            sb.append(", \"keywords\": ").append(stringArray(keywords));
        }
        if (doc.getReceivedDate() != null) {
            sb.append(", \"receivedDate\": \"").append(formatDateTime(doc.getReceivedDate())).append("\"");
        }
        if (doc.hasCorrespondent()) {
            sb.append(", \"correspondent\": {\"name\": \"").append(ToolJsonUtils.escapeJson(doc.getCorrespondentName())).append("\"");
            if (doc.getCorrespondentId() != null) {
                sb.append(", \"contactId\": \"").append(ToolJsonUtils.escapeJson(doc.getCorrespondentId())).append("\"");
            }
            sb.append(", \"direction\": \"").append(directionName(doc.getCorrespondentDirection())).append("\"}");
        }
        if (doc.getParentId() != null) {
            sb.append(", \"parentId\": \"").append(ToolJsonUtils.escapeJson(doc.getParentId())).append("\"");
            if (parent != null) {
                sb.append(", \"parentTitle\": \"").append(ToolJsonUtils.escapeJson(parent.getDisplayTitle())).append("\"");
            }
        }
        if (attachmentCount > 0) {
            sb.append(", \"attachmentCount\": ").append(attachmentCount);
        }
    }

    /**
     * A few lines describing the metadata of a document, put in front of its text when it is
     * attached to an assistant chat as context. Empty if no metadata is set.
     *
     * @param doc the document
     * @return the lines, each ending with a line separator
     */
    public static String metadataHeader(ArchiveFileDocumentsBean doc) {
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();
        if (doc.getTitle() != null && !doc.getTitle().isBlank()) {
            sb.append("Bezeichnung: ").append(doc.getTitle()).append(nl);
        }
        if (doc.hasCorrespondent()) {
            String label = doc.getCorrespondentDirection() == ArchiveFileDocumentsBean.CORRESPONDENT_OUT ? "An" : "Von";
            sb.append(label).append(": ").append(doc.getCorrespondentName()).append(nl);
        }
        if (doc.getReceivedDate() != null) {
            sb.append("Eingang: ").append(formatDateTime(doc.getReceivedDate())).append(nl);
        }
        List<String> keywords = DocumentKeywords.split(doc.getKeywords());
        if (!keywords.isEmpty()) {
            sb.append("Schlagworte: ").append(String.join(", ", keywords)).append(nl);
        }
        return sb.toString();
    }

    /**
     * @param direction one of the CORRESPONDENT_* constants
     * @return "in", "out" or "none"
     */
    public static String directionName(int direction) {
        if (direction == ArchiveFileDocumentsBean.CORRESPONDENT_IN) {
            return "in";
        } else if (direction == ArchiveFileDocumentsBean.CORRESPONDENT_OUT) {
            return "out";
        }
        return "none";
    }

    /**
     * Parses the direction of a correspondent as given to a tool.
     *
     * @param value "in"/"von"/"eingehend", "out"/"an"/"ausgehend" or "none"/"keine"; null or
     * empty means incoming
     * @return one of the CORRESPONDENT_* constants
     * @throws IllegalArgumentException for other values
     */
    public static int parseDirection(String value) {
        if (value == null || value.isBlank()) {
            return ArchiveFileDocumentsBean.CORRESPONDENT_IN;
        }
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "in":
            case "von":
            case "incoming":
            case "eingehend":
                return ArchiveFileDocumentsBean.CORRESPONDENT_IN;
            case "out":
            case "an":
            case "outgoing":
            case "ausgehend":
                return ArchiveFileDocumentsBean.CORRESPONDENT_OUT;
            case "none":
            case "keine":
                return ArchiveFileDocumentsBean.CORRESPONDENT_NONE;
            default:
                throw new IllegalArgumentException("Ungültige Richtung '" + value + "' - erlaubt sind in, out oder none");
        }
    }

    /**
     * Parses the keyword operation of a tool.
     *
     * @param value "add", "remove" or "set" (also "hinzufügen", "entfernen", "ersetzen"); null or
     * empty means add
     * @return the operation
     * @throws IllegalArgumentException for other values
     */
    public static KeywordOperation parseKeywordOperation(String value) {
        if (value == null || value.isBlank()) {
            return KeywordOperation.ADD;
        }
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "add":
            case "hinzufügen":
                return KeywordOperation.ADD;
            case "remove":
            case "entfernen":
                return KeywordOperation.REMOVE;
            case "set":
            case "ersetzen":
                return KeywordOperation.SET;
            default:
                throw new IllegalArgumentException("Ungültige keywordOperation '" + value + "' - erlaubt sind add, remove oder set");
        }
    }

    /**
     * Parses a date given to a tool: "yyyy-MM-dd", "yyyy-MM-dd HH:mm" or "yyyy-MM-ddTHH:mm[:ss]".
     *
     * @param value the date
     * @return the date, null if the value is empty
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    public static Date parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().replace('T', ' ');
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String p : patterns) {
            SimpleDateFormat f = new SimpleDateFormat(p);
            f.setLenient(false);
            try {
                if (v.length() == p.length()) {
                    return f.parse(v);
                }
            } catch (ParseException ex) {
                // try the next pattern
            }
        }
        throw new IllegalArgumentException("Ungültiges Datum '" + value + "' - erwartet yyyy-MM-dd oder yyyy-MM-dd HH:mm");
    }

    /**
     * Splits a comma separated list of ids, dropping empty entries and duplicates.
     *
     * @param value the list, may be null
     * @return the ids in their order
     */
    public static List<String> splitIds(String value) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (value != null) {
            for (String id : value.split(",")) {
                if (!id.isBlank()) {
                    ids.add(id.trim());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    static String formatDay(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    static String formatDateTime(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    static String stringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(ToolJsonUtils.escapeJson(values.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }
}
