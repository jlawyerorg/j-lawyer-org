/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.ToolCall;
import com.jdimension.jlawyer.ai.ToolDefinition;
import com.jdimension.jlawyer.ai.ToolParameter;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.documents.DocumentPreview;
import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.AppUserBean;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.persistence.ArchiveFileHistoryBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.persistence.CalendarSetup;
import com.jdimension.jlawyer.persistence.EventTypes;
import com.jdimension.jlawyer.persistence.InstantMessage;
import com.jdimension.jlawyer.persistence.Invoice;
import com.jdimension.jlawyer.persistence.InvoicePool;
import com.jdimension.jlawyer.persistence.InvoicePosition;
import com.jdimension.jlawyer.persistence.InvoiceType;
import com.jdimension.jlawyer.persistence.PartyTypeBean;
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CalendarServiceRemote;
import com.jdimension.jlawyer.services.InvoiceServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import com.jdimension.jlawyer.services.MessagingServiceRemote;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Central registry for AI tool definitions and execution logic.
 * Tools query and modify j-lawyer data via EJB services.
 * The client sends tool definitions to j-lawyer-ai, which forwards them to the LLM.
 * When the LLM requests a tool call, the client executes it here.
 *
 * @author jens
 */
public class ToolRegistry {

    private static final Logger log = Logger.getLogger(ToolRegistry.class.getName());

    private static final String SETTINGS_PREFIX = "assistant.tool.alwaysAllow.";

    private static final int WEB_TIMEOUT_MS = 10000;
    private static final int MAX_CONTENT_CHARS = 8000;
    private static final String WEB_USER_AGENT = "Mozilla/5.0 (compatible; j-lawyer.org Legal Assistant)";

    private static final List<ToolDefinition> TOOLS = new ArrayList<>();

    private final Set<String> sessionApprovedTools = new HashSet<>();

    // Cached reference data (lazy-loaded)
    private List<CalendarSetup> cachedCalendars;
    private List<PartyTypeBean> cachedPartyTypes;
    private List<InvoiceType> cachedInvoiceTypes;
    private List<InvoicePool> cachedInvoicePools;
    private List<AppUserBean> cachedUsers;

    static {
        // Existing tools
        TOOLS.add(new ToolDefinition("search_cases", "Sucht nach Akten anhand eines Suchbegriffs. Gibt eine Liste von Akten mit Aktenzeichen, Kurzrubrum und Sachgebiet zurück.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Aktensuche", true))));

        TOOLS.add(new ToolDefinition("get_case", "Ruft Details einer Akte ab, einschließlich Beteiligte und Aktennotiz.",
                Arrays.asList(new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true))));

        TOOLS.add(new ToolDefinition("search_contacts", "Sucht nach Kontakten/Adressen anhand eines Suchbegriffs.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Kontaktsuche", true))));

        TOOLS.add(new ToolDefinition("list_case_documents", "Listet alle Dokumente einer Akte auf.",
                Arrays.asList(new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true))));

        TOOLS.add(new ToolDefinition("get_document_text", "Extrahiert den Textinhalt eines Dokuments (PDF oder Textdatei).",
                Arrays.asList(new ToolParameter("documentId", "string", "ID des Dokuments. Es darf kein Dokumentname als Parameter übergeben werden.", true))));

        // New read-only tools
        TOOLS.add(new ToolDefinition("get_case_by_id", "Ruft Details einer Akte anhand der internen ID ab.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_current_date_time", "Gibt das aktuelle Datum und die Uhrzeit zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("get_history_for_case", "Gibt die Änderungshistorie einer Akte zurück.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_events_for_case", "Gibt alle Kalenderereignisse (Wiedervorlagen, Fristen, Termine) einer Akte zurück.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_parties_for_case", "Gibt alle Beteiligten einer Akte mit vollständigen Kontaktdaten zurück.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_all_open_events", "Gibt alle offenen Kalenderereignisse (Wiedervorlagen, Fristen, Termine) zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("get_all_open_events_between_dates", "Gibt alle offenen Kalenderereignisse zwischen zwei Daten zurück.",
                Arrays.asList(
                        new ToolParameter("fromDate", "string", "Startdatum im ISO-8601-Format (z.B. 2025-03-01T00:00:00)", true),
                        new ToolParameter("toDate", "string", "Enddatum im ISO-8601-Format (z.B. 2025-03-31T23:59:59)", true))));

        TOOLS.add(new ToolDefinition("get_all_open_invoices", "Gibt alle offenen Rechnungen zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("get_document_content", "Gibt den Inhalt eines Dokuments als Base64-kodierten String zurück.",
                Arrays.asList(new ToolParameter("documentId", "string", "ID des Dokuments", true))));

        // New write tools
        TOOLS.add(new ToolDefinition("create_event", "Erstellt ein Kalenderereignis (Wiedervorlage, Frist oder Termin) in einer Akte.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("summary", "string", "Zusammenfassung/Betreff des Ereignisses", true),
                        new ToolParameter("type", "string", "Typ: Wiedervorlage, Frist oder Termin", true),
                        new ToolParameter("beginDate", "string", "Startdatum im ISO-8601-Format", true),
                        new ToolParameter("endDate", "string", "Enddatum im ISO-8601-Format", true),
                        new ToolParameter("calendar", "string", "Name des Kalenders", true),
                        new ToolParameter("assignee", "string", "Benutzername des Verantwortlichen (optional)", false),
                        new ToolParameter("description", "string", "Beschreibung (optional)", false),
                        new ToolParameter("location", "string", "Ort (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_note", "Erstellt eine Aktennotiz als HTML-Dokument in einer Akte.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("content", "string", "Inhalt der Notiz (HTML erlaubt: b, i, br, ul, li)", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_or_get_contact", "Erstellt einen neuen Kontakt oder gibt einen bestehenden ähnlichen Kontakt zurück.",
                Arrays.asList(
                        new ToolParameter("name", "string", "Nachname (erforderlich wenn keine Firma angegeben)", false),
                        new ToolParameter("firstName", "string", "Vorname (optional)", false),
                        new ToolParameter("company", "string", "Firma (erforderlich wenn kein Name angegeben)", false),
                        new ToolParameter("city", "string", "Stadt", true),
                        new ToolParameter("zipCode", "string", "Postleitzahl", true),
                        new ToolParameter("street", "string", "Straße (optional)", false),
                        new ToolParameter("email", "string", "E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Telefon (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("add_party_to_case", "Fügt einen bestehenden Kontakt als Beteiligten zu einer Akte hinzu.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("contactId", "string", "ID des Kontakts", true),
                        new ToolParameter("partyType", "string", "Beteiligtentyp (z.B. Mandant, Gegner)", true),
                        new ToolParameter("reference", "string", "Aktenzeichen des Beteiligten (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_invoice", "Erstellt eine neue Rechnung in einer Akte.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("invoicePool", "string", "Name des Rechnungskreises", true),
                        new ToolParameter("invoiceType", "string", "Name des Rechnungstyps", true),
                        new ToolParameter("currency", "string", "Währung im ISO-4217-Format (z.B. EUR)", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_invoice_position", "Fügt eine Position zu einer bestehenden Rechnung hinzu.",
                Arrays.asList(
                        new ToolParameter("invoiceId", "string", "ID der Rechnung", true),
                        new ToolParameter("name", "string", "Bezeichnung der Position", true),
                        new ToolParameter("units", "string", "Menge (Dezimalzahl)", true),
                        new ToolParameter("unitPrice", "string", "Einzelpreis (Dezimalzahl)", true),
                        new ToolParameter("description", "string", "Beschreibung (optional)", false),
                        new ToolParameter("taxRate", "string", "Steuersatz in Prozent (Standard: 19.0)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_instant_message", "Erstellt eine Verfügung/Sofortnachricht in einer Akte.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("content", "string", "Inhalt der Nachricht", true),
                        new ToolParameter("recipient", "string", "Benutzername des Empfängers (optional, wird als @Erwähnung hinzugefügt)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_case", "Erstellt eine neue Akte. Das Aktenzeichen wird automatisch vom Server vergeben.",
                Arrays.asList(
                        new ToolParameter("name", "string", "Kurzrubrum / Bezeichnung der Akte", true),
                        new ToolParameter("reason", "string", "Grund/Gegenstand (optional)", false),
                        new ToolParameter("subjectField", "string", "Sachgebiet (optional)", false),
                        new ToolParameter("lawyer", "string", "Benutzername des Anwalts (optional)", false),
                        new ToolParameter("assistant", "string", "Benutzername des Sachbearbeiters (optional)", false),
                        new ToolParameter("notice", "string", "Aktennotiz (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_contact", "Erstellt einen neuen Kontakt. Führt keine Ähnlichkeitssuche durch — dafür gibt es create_or_get_contact.",
                Arrays.asList(
                        new ToolParameter("name", "string", "Nachname (erforderlich wenn keine Firma angegeben)", false),
                        new ToolParameter("firstName", "string", "Vorname (optional)", false),
                        new ToolParameter("company", "string", "Firma (erforderlich wenn kein Name angegeben)", false),
                        new ToolParameter("salutation", "string", "Anrede (optional, z.B. Herr, Frau)", false),
                        new ToolParameter("title", "string", "Titel (optional, z.B. Dr., Prof.)", false),
                        new ToolParameter("street", "string", "Straße (optional)", false),
                        new ToolParameter("zipCode", "string", "Postleitzahl (optional)", false),
                        new ToolParameter("city", "string", "Stadt (optional)", false),
                        new ToolParameter("country", "string", "Land (optional)", false),
                        new ToolParameter("email", "string", "E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Telefon (optional)", false),
                        new ToolParameter("mobile", "string", "Mobiltelefon (optional)", false),
                        new ToolParameter("fax", "string", "Fax (optional)", false),
                        new ToolParameter("website", "string", "Webseite (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("update_case", "Aktualisiert eine bestehende Akte. Nur die angegebenen Felder werden geändert.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("name", "string", "Neues Kurzrubrum / Bezeichnung (optional)", false),
                        new ToolParameter("reason", "string", "Neuer Grund/Gegenstand (optional)", false),
                        new ToolParameter("subjectField", "string", "Neues Sachgebiet (optional)", false),
                        new ToolParameter("lawyer", "string", "Neuer Anwalt - Benutzername (optional)", false),
                        new ToolParameter("assistant", "string", "Neuer Sachbearbeiter - Benutzername (optional)", false),
                        new ToolParameter("notice", "string", "Neue Aktennotiz (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("update_contact", "Aktualisiert einen bestehenden Kontakt. Nur die angegebenen Felder werden geändert.",
                Arrays.asList(
                        new ToolParameter("contactId", "string", "Interne ID des Kontakts", true),
                        new ToolParameter("name", "string", "Neuer Nachname (optional)", false),
                        new ToolParameter("firstName", "string", "Neuer Vorname (optional)", false),
                        new ToolParameter("company", "string", "Neue Firma (optional)", false),
                        new ToolParameter("salutation", "string", "Neue Anrede (optional)", false),
                        new ToolParameter("title", "string", "Neuer Titel (optional)", false),
                        new ToolParameter("street", "string", "Neue Straße (optional)", false),
                        new ToolParameter("zipCode", "string", "Neue Postleitzahl (optional)", false),
                        new ToolParameter("city", "string", "Neue Stadt (optional)", false),
                        new ToolParameter("country", "string", "Neues Land (optional)", false),
                        new ToolParameter("email", "string", "Neue E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Neues Telefon (optional)", false),
                        new ToolParameter("mobile", "string", "Neues Mobiltelefon (optional)", false),
                        new ToolParameter("fax", "string", "Neues Fax (optional)", false),
                        new ToolParameter("website", "string", "Neue Webseite (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        // Web tools (read-only, RISK_LOW)
        TOOLS.add(new ToolDefinition("web_search",
                "Führt eine Websuche durch und gibt eine Liste von Ergebnissen zurück (Titel, URL, Textausschnitt). Nützlich für aktuelle Informationen, Rechtsprechung, oder Fakten die nicht in den Akten enthalten sind.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Websuche", true))));

        TOOLS.add(new ToolDefinition("fetch_url",
                "Lädt den Textinhalt einer Webseite herunter. Gibt den extrahierten Text ohne HTML-Tags zurück. Nützlich um Details einer Webseite zu lesen, die z.B. aus einer Websuche stammt.",
                Arrays.asList(new ToolParameter("url", "string", "Die URL der Webseite", true))));
    }

    public List<ToolDefinition> getToolDefinitions() {
        return TOOLS;
    }

    public String getRiskLevel(String toolId) {
        for (ToolDefinition td : TOOLS) {
            if (td.getId().equals(toolId)) {
                return td.getRiskLevel();
            }
        }
        return ToolDefinition.RISK_HIGH;
    }

    public boolean requiresApproval(String toolId) {
        String risk = getRiskLevel(toolId);
        return !ToolDefinition.RISK_LOW.equals(risk);
    }

    public String execute(String toolId, String argumentsJson) {
        try {
            JsonObject args = (JsonObject) Jsoner.deserialize(argumentsJson);
            switch (toolId) {
                case "search_cases":
                    return executeSearchCases(args);
                case "get_case":
                    return executeGetCase(args);
                case "search_contacts":
                    return executeSearchContacts(args);
                case "list_case_documents":
                    return executeListCaseDocuments(args);
                case "get_document_text":
                    return executeGetDocumentText(args);
                case "get_case_by_id":
                    return executeGetCaseById(args);
                case "get_current_date_time":
                    return executeGetCurrentDateTime(args);
                case "get_history_for_case":
                    return executeGetHistoryForCase(args);
                case "get_events_for_case":
                    return executeGetEventsForCase(args);
                case "get_parties_for_case":
                    return executeGetPartiesForCase(args);
                case "get_all_open_events":
                    return executeGetAllOpenEvents(args);
                case "get_all_open_events_between_dates":
                    return executeGetAllOpenEventsBetweenDates(args);
                case "get_all_open_invoices":
                    return executeGetAllOpenInvoices(args);
                case "get_document_content":
                    return executeGetDocumentContent(args);
                case "create_event":
                    return executeCreateEvent(args);
                case "create_note":
                    return executeCreateNote(args);
                case "create_or_get_contact":
                    return executeCreateOrGetContact(args);
                case "add_party_to_case":
                    return executeAddPartyToCase(args);
                case "create_invoice":
                    return executeCreateInvoice(args);
                case "create_invoice_position":
                    return executeCreateInvoicePosition(args);
                case "create_instant_message":
                    return executeCreateInstantMessage(args);
                case "create_case":
                    return executeCreateCase(args);
                case "create_contact":
                    return executeCreateContact(args);
                case "update_case":
                    return executeUpdateCase(args);
                case "update_contact":
                    return executeUpdateContact(args);
                case "web_search":
                    return executeWebSearch(args);
                case "fetch_url":
                    return executeFetchUrl(args);
                default:
                    return ToolJsonUtils.error("Unbekanntes Werkzeug: " + toolId);
            }
        } catch (Exception ex) {
            log.error("Error executing tool " + toolId, ex);
            return ToolJsonUtils.error(ex.getMessage());
        }
    }

    public boolean isApproved(String toolId) {
        if (sessionApprovedTools.contains(toolId)) {
            return true;
        }
        String setting = UserSettings.getInstance().getSetting(SETTINGS_PREFIX + toolId, "false");
        return "true".equalsIgnoreCase(setting);
    }

    public void approveForSession(String toolId) {
        sessionApprovedTools.add(toolId);
    }

    public void approveAlways(String toolId) {
        UserSettings.getInstance().setSetting(SETTINGS_PREFIX + toolId, "true");
    }

    public String getToolDisplayName(String toolId) {
        for (ToolDefinition td : TOOLS) {
            if (td.getId().equals(toolId)) {
                return td.getDescription();
            }
        }
        return toolId;
    }

    public String formatToolCallSummary(ToolCall tc) {
        try {
            JsonObject args = (JsonObject) Jsoner.deserialize(tc.getArguments());
            switch (tc.getToolName()) {
                case "search_cases":
                    return "Aktensuche: '" + args.getOrDefault("query", "") + "'";
                case "get_case":
                    return "Aktendetails: " + args.getOrDefault("fileNumber", "");
                case "search_contacts":
                    return "Kontaktsuche: '" + args.getOrDefault("query", "") + "'";
                case "list_case_documents":
                    return "Dokumentenliste: " + args.getOrDefault("fileNumber", "");
                case "get_document_text":
                    return "Dokumenttext: " + args.getOrDefault("documentId", "");
                case "get_case_by_id":
                    return "Aktendetails (ID): " + args.getOrDefault("caseId", "");
                case "get_current_date_time":
                    return "Aktuelles Datum/Uhrzeit";
                case "get_history_for_case":
                    return "Aktenhistorie: " + args.getOrDefault("caseId", "");
                case "get_events_for_case":
                    return "Termine der Akte: " + args.getOrDefault("caseId", "");
                case "get_parties_for_case":
                    return "Beteiligte der Akte: " + args.getOrDefault("caseId", "");
                case "get_all_open_events":
                    return "Alle offenen Termine";
                case "get_all_open_events_between_dates":
                    return "Termine: " + args.getOrDefault("fromDate", "") + " - " + args.getOrDefault("toDate", "");
                case "get_all_open_invoices":
                    return "Alle offenen Rechnungen";
                case "get_document_content":
                    return "Dokumentinhalt (Base64): " + args.getOrDefault("documentId", "");
                case "create_event":
                    return "Termin erstellen: " + args.getOrDefault("summary", "");
                case "create_note":
                    return "Notiz erstellen in Akte: " + args.getOrDefault("caseId", "");
                case "create_or_get_contact":
                    return "Kontakt erstellen/suchen: " + args.getOrDefault("name", args.getOrDefault("company", ""));
                case "add_party_to_case":
                    return "Beteiligten hinzufügen: " + args.getOrDefault("partyType", "");
                case "create_invoice":
                    return "Rechnung erstellen: " + args.getOrDefault("invoiceType", "");
                case "create_invoice_position":
                    return "Rechnungsposition: " + args.getOrDefault("name", "");
                case "create_instant_message":
                    return "Verfügung erstellen in Akte: " + args.getOrDefault("caseId", "");
                case "create_case":
                    return "Akte erstellen: " + args.getOrDefault("name", "");
                case "create_contact":
                    return "Kontakt erstellen: " + args.getOrDefault("name", args.getOrDefault("company", ""));
                case "update_case":
                    return "Akte aktualisieren: " + args.getOrDefault("caseId", "");
                case "update_contact":
                    return "Kontakt aktualisieren: " + args.getOrDefault("contactId", "");
                case "web_search":
                    return "Websuche: '" + args.getOrDefault("query", "") + "'";
                case "fetch_url":
                    return "Webseite laden: " + args.getOrDefault("url", "");
                default:
                    return tc.getToolName() + ": " + tc.getArguments();
            }
        } catch (Exception ex) {
            return tc.getToolName() + ": " + tc.getArguments();
        }
    }

    // =========================================================================
    // Cached reference data helpers
    // =========================================================================

    private List<CalendarSetup> getCachedCalendars() throws Exception {
        if (cachedCalendars == null) {
            cachedCalendars = ToolJsonUtils.getLocator().lookupCalendarServiceRemote().getAllCalendarSetups();
        }
        return cachedCalendars;
    }

    private List<PartyTypeBean> getCachedPartyTypes() throws Exception {
        if (cachedPartyTypes == null) {
            cachedPartyTypes = ToolJsonUtils.getLocator().lookupSystemManagementRemote().getPartyTypes();
        }
        return cachedPartyTypes;
    }

    private List<InvoiceType> getCachedInvoiceTypes() throws Exception {
        if (cachedInvoiceTypes == null) {
            cachedInvoiceTypes = ToolJsonUtils.getLocator().lookupInvoiceServiceRemote().getAllInvoiceTypes();
        }
        return cachedInvoiceTypes;
    }

    private List<InvoicePool> getCachedInvoicePools() throws Exception {
        if (cachedInvoicePools == null) {
            cachedInvoicePools = ToolJsonUtils.getLocator().lookupInvoiceServiceRemote().getAllInvoicePools();
        }
        return cachedInvoicePools;
    }

    private List<AppUserBean> getCachedUsers() throws Exception {
        if (cachedUsers == null) {
            cachedUsers = ToolJsonUtils.getLocator().lookupSecurityServiceRemote().getUsersHavingRole("loginRole");
        }
        return cachedUsers;
    }

    // =========================================================================
    // Existing tool implementations
    // =========================================================================

    private String executeSearchCases(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean[] results = svc.searchEnhanced(query, false, new String[]{}, new String[]{});

        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        int limit = Math.min(results.length, 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            ArchiveFileBean c = results[i];
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(c.getId())).append("\"");
            sb.append(", \"fileNumber\": \"").append(ToolJsonUtils.escapeJson(c.getFileNumber())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(c.getName())).append("\"");
            if (c.getReason() != null) {
                sb.append(", \"reason\": \"").append(ToolJsonUtils.escapeJson(c.getReason())).append("\"");
            }
            if (c.getSubjectField() != null) {
                sb.append(", \"subjectField\": \"").append(ToolJsonUtils.escapeJson(c.getSubjectField())).append("\"");
            }
            sb.append(", \"archived\": ").append(c.isArchived());
            sb.append("}");
        }
        sb.append("], \"totalResults\": ").append(results.length);
        if (results.length > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetCase(JsonObject args) throws Exception {
        String fileNumber = (String) args.get("fileNumber");
        if (fileNumber == null || fileNumber.trim().isEmpty()) {
            return ToolJsonUtils.error("Aktenzeichen fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean[] results = svc.searchEnhanced(fileNumber, false, new String[]{}, new String[]{});
        ArchiveFileBean caseBean = null;
        for (ArchiveFileBean r : results) {
            if (fileNumber.equals(r.getFileNumber())) {
                caseBean = r;
                break;
            }
        }
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + fileNumber);
        }

        return buildCaseJson(caseBean, svc);
    }

    private String executeSearchContacts(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        AddressServiceRemote svc = locator.lookupAddressServiceRemote();
        AddressBean[] results = svc.searchSimple(query);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        int limit = Math.min(results.length, 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendContactJson(sb, results[i]);
        }
        sb.append("], \"totalResults\": ").append(results.length);
        if (results.length > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeListCaseDocuments(JsonObject args) throws Exception {
        String fileNumber = (String) args.get("fileNumber");
        if (fileNumber == null || fileNumber.trim().isEmpty()) {
            return ToolJsonUtils.error("Aktenzeichen fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean[] results = svc.searchEnhanced(fileNumber, false, new String[]{}, new String[]{});
        ArchiveFileBean caseBean = null;
        for (ArchiveFileBean r : results) {
            if (fileNumber.equals(r.getFileNumber())) {
                caseBean = r;
                break;
            }
        }
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + fileNumber);
        }

        Collection<ArchiveFileDocumentsBean> docs = svc.getDocuments(caseBean.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : docs) {
            if (doc.isDeleted()) {
                continue;
            }
            if (count > 0) {
                sb.append(",");
            }
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
            sb.append(", \"size\": ").append(doc.getSize());
            if (doc.getCreationDate() != null) {
                sb.append(", \"creationDate\": \"").append(sdf.format(doc.getCreationDate())).append("\"");
            }
            if (doc.getFolder() != null) {
                sb.append(", \"folder\": \"").append(ToolJsonUtils.escapeJson(doc.getFolder().getName())).append("\"");
            }
            sb.append("}");
            count++;
        }
        sb.append("], \"totalDocuments\": ").append(count);
        sb.append("}");
        return sb.toString();
    }

    private String executeGetDocumentText(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileDocumentsBean doc = svc.getDocument(documentId);
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        String text = svc.getDocumentPreview(documentId, DocumentPreview.TYPE_TEXT).getText();

        int maxLength = 30000;
        boolean truncated = false;
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
            truncated = true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"documentId\": \"").append(ToolJsonUtils.escapeJson(documentId)).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        sb.append(", \"text\": \"").append(ToolJsonUtils.escapeJson(text)).append("\"");
        if (truncated) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // New read-only tool implementations
    // =========================================================================

    private String executeGetCaseById(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        return buildCaseJson(caseBean, svc);
    }

    private String executeGetCurrentDateTime(JsonObject args) throws Exception {
        String now = ToolJsonUtils.formatDate(new Date());
        return "{\"currentDateTime\": \"" + now + "\"}";
    }

    private String executeGetHistoryForCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        ArchiveFileHistoryBean[] history = svc.getHistoryForArchiveFile(caseId, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\"");
        sb.append(", \"history\": [");
        int limit = Math.min(history.length, 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            ArchiveFileHistoryBean h = history[i];
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(h.getId())).append("\"");
            if (h.getChangeDate() != null) {
                sb.append(", \"changeDate\": \"").append(ToolJsonUtils.formatDate(h.getChangeDate())).append("\"");
            }
            if (h.getChangeDescription() != null) {
                sb.append(", \"changeDescription\": \"").append(ToolJsonUtils.escapeJson(h.getChangeDescription())).append("\"");
            }
            if (h.getPrincipal() != null) {
                sb.append(", \"principal\": \"").append(ToolJsonUtils.escapeJson(h.getPrincipal())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        if (history.length > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetEventsForCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();
        Collection<ArchiveFileReviewsBean> events = calSvc.getReviews(caseId);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\"");
        sb.append(", \"events\": [");
        int count = 0;
        for (ArchiveFileReviewsBean ev : events) {
            if (count > 0) {
                sb.append(",");
            }
            appendEventJson(sb, ev);
            count++;
            if (count >= 50) {
                break;
            }
        }
        sb.append("], \"totalEvents\": ").append(events.size());
        if (events.size() > 50) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetPartiesForCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        List<ArchiveFileAddressesBean> parties = svc.getInvolvementDetailsForCase(caseId);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\"");
        sb.append(", \"parties\": [");
        int count = 0;
        for (ArchiveFileAddressesBean p : parties) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append("{");
            if (p.getReferenceType() != null) {
                sb.append("\"partyType\": \"").append(ToolJsonUtils.escapeJson(p.getReferenceType().getName())).append("\"");
            }
            if (p.getReference() != null && !p.getReference().isEmpty()) {
                sb.append(", \"reference\": \"").append(ToolJsonUtils.escapeJson(p.getReference())).append("\"");
            }
            if (p.getAddressKey() != null) {
                AddressBean addr = p.getAddressKey();
                sb.append(", \"contactId\": \"").append(ToolJsonUtils.escapeJson(addr.getId())).append("\"");
                if (addr.getName() != null) {
                    sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(addr.getName())).append("\"");
                }
                if (addr.getFirstName() != null) {
                    sb.append(", \"firstName\": \"").append(ToolJsonUtils.escapeJson(addr.getFirstName())).append("\"");
                }
                if (addr.getCompany() != null && !addr.getCompany().isEmpty()) {
                    sb.append(", \"company\": \"").append(ToolJsonUtils.escapeJson(addr.getCompany())).append("\"");
                }
                if (addr.getCity() != null && !addr.getCity().isEmpty()) {
                    sb.append(", \"city\": \"").append(ToolJsonUtils.escapeJson(addr.getCity())).append("\"");
                }
                if (addr.getStreet() != null && !addr.getStreet().isEmpty()) {
                    sb.append(", \"street\": \"").append(ToolJsonUtils.escapeJson(addr.getStreet())).append("\"");
                }
                if (addr.getZipCode() != null && !addr.getZipCode().isEmpty()) {
                    sb.append(", \"zipCode\": \"").append(ToolJsonUtils.escapeJson(addr.getZipCode())).append("\"");
                }
                if (addr.getEmail() != null && !addr.getEmail().isEmpty()) {
                    sb.append(", \"email\": \"").append(ToolJsonUtils.escapeJson(addr.getEmail())).append("\"");
                }
                if (addr.getPhone() != null && !addr.getPhone().isEmpty()) {
                    sb.append(", \"phone\": \"").append(ToolJsonUtils.escapeJson(addr.getPhone())).append("\"");
                }
            }
            sb.append("}");
            count++;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeGetAllOpenEvents(JsonObject args) throws Exception {
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();
        Collection<ArchiveFileReviewsBean> events = calSvc.getAllOpenReviews();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"events\": [");
        int count = 0;
        for (ArchiveFileReviewsBean ev : events) {
            if (count > 0) {
                sb.append(",");
            }
            appendEventJson(sb, ev);
            count++;
            if (count >= 50) {
                break;
            }
        }
        sb.append("], \"totalEvents\": ").append(events.size());
        if (events.size() > 50) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetAllOpenEventsBetweenDates(JsonObject args) throws Exception {
        String fromDateStr = (String) args.get("fromDate");
        String toDateStr = (String) args.get("toDate");
        if (fromDateStr == null || fromDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Startdatum (fromDate) fehlt");
        }
        if (toDateStr == null || toDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Enddatum (toDate) fehlt");
        }

        Date fromDate = ToolJsonUtils.parseIsoDate(fromDateStr);
        Date toDate = ToolJsonUtils.parseIsoDate(toDateStr);
        if (fromDate == null) {
            return ToolJsonUtils.error("Startdatum konnte nicht geparst werden: " + fromDateStr);
        }
        if (toDate == null) {
            return ToolJsonUtils.error("Enddatum konnte nicht geparst werden: " + toDateStr);
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();
        // status 0 = open, type -1 = all types
        Collection<ArchiveFileReviewsBean> events = calSvc.searchReviews(0, -1, fromDate, toDate);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fromDate\": \"").append(ToolJsonUtils.escapeJson(fromDateStr)).append("\"");
        sb.append(", \"toDate\": \"").append(ToolJsonUtils.escapeJson(toDateStr)).append("\"");
        sb.append(", \"events\": [");
        int count = 0;
        for (ArchiveFileReviewsBean ev : events) {
            if (count > 0) {
                sb.append(",");
            }
            appendEventJson(sb, ev);
            count++;
            if (count >= 50) {
                break;
            }
        }
        sb.append("], \"totalEvents\": ").append(events.size());
        if (events.size() > 50) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetAllOpenInvoices(JsonObject args) throws Exception {
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        InvoiceServiceRemote invSvc = locator.lookupInvoiceServiceRemote();
        List<Invoice> invoices = invSvc.getInvoicesByStatus(
                Invoice.STATUS_OPEN, Invoice.STATUS_OPEN_REMINDER1, Invoice.STATUS_OPEN_REMINDER2,
                Invoice.STATUS_OPEN_REMINDER3, Invoice.STATUS_OPEN_NONENFORCEABLE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"invoices\": [");
        int limit = Math.min(invoices.size(), 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            Invoice inv = invoices.get(i);
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(inv.getId())).append("\"");
            if (inv.getName() != null) {
                sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(inv.getName())).append("\"");
            }
            if (inv.getInvoiceNumber() != null) {
                sb.append(", \"invoiceNumber\": \"").append(ToolJsonUtils.escapeJson(inv.getInvoiceNumber())).append("\"");
            }
            sb.append(", \"status\": \"").append(ToolJsonUtils.escapeJson(inv.getStatusString())).append("\"");
            if (inv.getTotal() != null) {
                sb.append(", \"total\": ").append(inv.getTotal());
            }
            if (inv.getTotalGross() != null) {
                sb.append(", \"totalGross\": ").append(inv.getTotalGross());
            }
            if (inv.getCurrency() != null) {
                sb.append(", \"currency\": \"").append(ToolJsonUtils.escapeJson(inv.getCurrency())).append("\"");
            }
            if (inv.getDueDate() != null) {
                sb.append(", \"dueDate\": \"").append(sdf.format(inv.getDueDate())).append("\"");
            }
            if (inv.getCreationDate() != null) {
                sb.append(", \"creationDate\": \"").append(sdf.format(inv.getCreationDate())).append("\"");
            }
            if (inv.getInvoiceType() != null) {
                sb.append(", \"invoiceType\": \"").append(ToolJsonUtils.escapeJson(inv.getInvoiceType().getDisplayName())).append("\"");
            }
            if (inv.getArchiveFileKey() != null) {
                sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(inv.getArchiveFileKey().getId())).append("\"");
                sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(inv.getArchiveFileKey().getFileNumber())).append("\"");
            }
            if (inv.getContact() != null) {
                sb.append(", \"contactId\": \"").append(ToolJsonUtils.escapeJson(inv.getContact().getId())).append("\"");
                sb.append(", \"contactName\": \"").append(ToolJsonUtils.escapeJson(inv.getContact().toDisplayName())).append("\"");
            }
            sb.append("}");
        }
        sb.append("], \"totalInvoices\": ").append(invoices.size());
        if (invoices.size() > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetDocumentContent(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileDocumentsBean doc = svc.getDocument(documentId);
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        byte[] content = svc.getDocumentContent(documentId);
        if (content == null) {
            return ToolJsonUtils.error("Dokumentinhalt konnte nicht geladen werden: " + documentId);
        }

        // Cap at 1MB
        if (content.length > 1024 * 1024) {
            return ToolJsonUtils.error("Dokument ist zu groß (max. 1 MB): " + content.length + " Bytes");
        }

        String base64 = java.util.Base64.getEncoder().encodeToString(content);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"documentId\": \"").append(ToolJsonUtils.escapeJson(documentId)).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        sb.append(", \"size\": ").append(content.length);
        sb.append(", \"contentBase64\": \"").append(base64).append("\"");
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // New write tool implementations
    // =========================================================================

    private String executeCreateEvent(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String summary = (String) args.get("summary");
        String typeStr = (String) args.get("type");
        String beginDateStr = (String) args.get("beginDate");
        String endDateStr = (String) args.get("endDate");
        String calendarName = (String) args.get("calendar");
        String assignee = (String) args.get("assignee");
        String description = (String) args.get("description");
        String location = (String) args.get("location");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (summary == null || summary.trim().isEmpty()) {
            return ToolJsonUtils.error("Zusammenfassung fehlt");
        }
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Ereignistyp fehlt (Wiedervorlage, Frist oder Termin)");
        }
        if (beginDateStr == null || beginDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Startdatum fehlt");
        }
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Enddatum fehlt");
        }
        if (calendarName == null || calendarName.trim().isEmpty()) {
            return ToolJsonUtils.error("Kalendername fehlt");
        }

        // Map type string to int
        int eventType;
        switch (typeStr.trim().toLowerCase()) {
            case "wiedervorlage":
                eventType = EventTypes.EVENTTYPE_FOLLOWUP;
                break;
            case "frist":
                eventType = EventTypes.EVENTTYPE_RESPITE;
                break;
            case "termin":
                eventType = EventTypes.EVENTTYPE_EVENT;
                break;
            default:
                return ToolJsonUtils.error("Unbekannter Ereignistyp: " + typeStr + ". Erlaubt: Wiedervorlage, Frist, Termin");
        }

        // Parse dates
        Date beginDate = ToolJsonUtils.parseIsoDate(beginDateStr);
        Date endDate = ToolJsonUtils.parseIsoDate(endDateStr);
        if (beginDate == null) {
            return ToolJsonUtils.error("Startdatum konnte nicht geparst werden: " + beginDateStr);
        }
        if (endDate == null) {
            return ToolJsonUtils.error("Enddatum konnte nicht geparst werden: " + endDateStr);
        }

        // Find calendar by name (case-insensitive)
        CalendarSetup matchedCalendar = null;
        for (CalendarSetup cs : getCachedCalendars()) {
            if (cs.getDisplayName() != null && cs.getDisplayName().equalsIgnoreCase(calendarName.trim())) {
                matchedCalendar = cs;
                break;
            }
        }
        if (matchedCalendar == null) {
            StringBuilder names = new StringBuilder();
            for (CalendarSetup cs : getCachedCalendars()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(cs.getDisplayName());
            }
            return ToolJsonUtils.error("Kalender nicht gefunden: " + calendarName + ". Verfügbare Kalender: " + names.toString());
        }

        // Validate calendar supports the event type
        if (matchedCalendar.getEventType() != eventType) {
            return ToolJsonUtils.error("Kalender '" + matchedCalendar.getDisplayName() + "' unterstützt nicht den Typ '" + typeStr + "'");
        }

        // Verify case exists
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        // Validate assignee if provided
        if (assignee != null && !assignee.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(assignee.trim())) {
                    assignee = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Benutzer nicht gefunden: " + assignee + ". Verfügbare Benutzer: " + userNames.toString());
            }
        } else {
            assignee = UserSettings.getInstance().getCurrentUser().getPrincipalId();
        }

        ArchiveFileReviewsBean review = new ArchiveFileReviewsBean();
        review.setEventType(eventType);
        review.setSummary(summary);
        review.setBeginDate(beginDate);
        review.setEndDate(endDate);
        review.setCalendarSetup(matchedCalendar);
        review.setAssignee(assignee);
        if (description != null && !description.trim().isEmpty()) {
            review.setDescription(description);
        }
        if (location != null && !location.trim().isEmpty()) {
            review.setLocation(location);
        }

        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();
        ArchiveFileReviewsBean created = calSvc.addReview(caseId, review);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"summary\": \"").append(ToolJsonUtils.escapeJson(created.getSummary())).append("\"");
        sb.append(", \"type\": \"").append(ToolJsonUtils.escapeJson(created.getEventTypeName())).append("\"");
        sb.append(", \"beginDate\": \"").append(ToolJsonUtils.formatDate(created.getBeginDate())).append("\"");
        sb.append(", \"endDate\": \"").append(ToolJsonUtils.formatDate(created.getEndDate())).append("\"");
        sb.append(", \"assignee\": \"").append(ToolJsonUtils.escapeJson(created.getAssignee())).append("\"");
        if (created.getCalendarSetup() != null) {
            sb.append(", \"calendar\": \"").append(ToolJsonUtils.escapeJson(created.getCalendarSetup().getDisplayName())).append("\"");
        }
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateNote(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String content = (String) args.get("content");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (content == null || content.trim().isEmpty()) {
            return ToolJsonUtils.error("Notizinhalt fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmm");
        String fileName = sdf.format(new Date()) + " Notiz.html";

        SimpleDateFormat displayFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String currentUser = UserSettings.getInstance().getCurrentUser().getPrincipalId();

        String html = "<html><head><meta charset=\"UTF-8\"></head><body>"
                + "<p><b>Akte:</b> " + ToolJsonUtils.escapeJson(caseBean.getFileNumber()) + " - " + ToolJsonUtils.escapeJson(caseBean.getName()) + "</p>"
                + "<p><b>Datum:</b> " + displayFmt.format(new Date()) + " | <b>Benutzer:</b> " + ToolJsonUtils.escapeJson(currentUser) + "</p>"
                + "<hr>"
                + content
                + "</body></html>";

        ArchiveFileDocumentsBean doc = svc.addDocument(caseId, fileName, html.getBytes("UTF-8"), null, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
        sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateOrGetContact(JsonObject args) throws Exception {
        String name = (String) args.get("name");
        String firstName = (String) args.get("firstName");
        String company = (String) args.get("company");
        String city = (String) args.get("city");
        String zipCode = (String) args.get("zipCode");
        String street = (String) args.get("street");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");

        if (city == null || city.trim().isEmpty()) {
            return ToolJsonUtils.error("Stadt fehlt");
        }
        if (zipCode == null || zipCode.trim().isEmpty()) {
            return ToolJsonUtils.error("Postleitzahl fehlt");
        }
        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasCompany = company != null && !company.trim().isEmpty();
        if (!hasName && !hasCompany) {
            return ToolJsonUtils.error("Name oder Firma muss angegeben werden");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();

        // Build candidate for similarity search
        AddressBean candidate = new AddressBean();
        if (hasName) {
            candidate.setName(name.trim());
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            candidate.setFirstName(firstName.trim());
        }
        if (hasCompany) {
            candidate.setCompany(company.trim());
        }
        candidate.setCity(city.trim());
        candidate.setZipCode(zipCode.trim());
        if (street != null && !street.trim().isEmpty()) {
            candidate.setStreet(street.trim());
        }

        // Similarity search with 85% threshold
        List<AddressBean> similar = addrSvc.similaritySearch(candidate, 0.85f);
        if (similar != null && !similar.isEmpty()) {
            // Return existing contact
            AddressBean existing = similar.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"wasCreated\": false");
            sb.append(", \"contact\": ");
            StringBuilder contactSb = new StringBuilder();
            appendContactJson(contactSb, existing);
            sb.append(contactSb);
            sb.append("}");
            return sb.toString();
        }

        // Create new contact
        if (email != null && !email.trim().isEmpty()) {
            candidate.setEmail(email.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            candidate.setPhone(phone.trim());
        }

        AddressBean created = addrSvc.createAddress(candidate);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"wasCreated\": true");
        sb.append(", \"contact\": ");
        StringBuilder contactSb = new StringBuilder();
        appendContactJson(contactSb, created);
        sb.append(contactSb);
        sb.append("}");
        return sb.toString();
    }

    private String executeAddPartyToCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String contactId = (String) args.get("contactId");
        String partyTypeName = (String) args.get("partyType");
        String reference = (String) args.get("reference");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID fehlt");
        }
        if (partyTypeName == null || partyTypeName.trim().isEmpty()) {
            return ToolJsonUtils.error("Beteiligtentyp fehlt");
        }

        // Validate party type (case-insensitive)
        PartyTypeBean matchedType = null;
        for (PartyTypeBean pt : getCachedPartyTypes()) {
            if (pt.getName() != null && pt.getName().equalsIgnoreCase(partyTypeName.trim())) {
                matchedType = pt;
                break;
            }
        }
        if (matchedType == null) {
            StringBuilder names = new StringBuilder();
            for (PartyTypeBean pt : getCachedPartyTypes()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(pt.getName());
            }
            return ToolJsonUtils.error("Beteiligtentyp nicht gefunden: " + partyTypeName + ". Verfügbare Typen: " + names.toString());
        }

        // Verify case and contact exist
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        AddressBean contact = addrSvc.getAddress(contactId);
        if (contact == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactId);
        }

        ArchiveFileAddressesBean party = new ArchiveFileAddressesBean();
        party.setArchiveFileKey(caseBean);
        party.setAddressKey(contact);
        party.setReferenceType(matchedType);
        if (reference != null && !reference.trim().isEmpty()) {
            party.setReference(reference.trim());
        }

        ArchiveFileAddressesBean created = svc.addAddressToCase(party);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"partyType\": \"").append(ToolJsonUtils.escapeJson(matchedType.getName())).append("\"");
        sb.append(", \"contactName\": \"").append(ToolJsonUtils.escapeJson(contact.toDisplayName())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateInvoice(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String poolName = (String) args.get("invoicePool");
        String typeName = (String) args.get("invoiceType");
        String currency = (String) args.get("currency");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (poolName == null || poolName.trim().isEmpty()) {
            return ToolJsonUtils.error("Rechnungskreis fehlt");
        }
        if (typeName == null || typeName.trim().isEmpty()) {
            return ToolJsonUtils.error("Rechnungstyp fehlt");
        }
        if (currency == null || currency.trim().isEmpty()) {
            return ToolJsonUtils.error("Währung fehlt");
        }

        // Validate invoice pool
        InvoicePool matchedPool = null;
        for (InvoicePool p : getCachedInvoicePools()) {
            if (p.getDisplayName() != null && p.getDisplayName().equalsIgnoreCase(poolName.trim())) {
                matchedPool = p;
                break;
            }
        }
        if (matchedPool == null) {
            StringBuilder names = new StringBuilder();
            for (InvoicePool p : getCachedInvoicePools()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(p.getDisplayName());
            }
            return ToolJsonUtils.error("Rechnungskreis nicht gefunden: " + poolName + ". Verfügbare Kreise: " + names.toString());
        }

        // Validate invoice type
        InvoiceType matchedType = null;
        for (InvoiceType t : getCachedInvoiceTypes()) {
            if (t.getDisplayName() != null && t.getDisplayName().equalsIgnoreCase(typeName.trim())) {
                matchedType = t;
                break;
            }
        }
        if (matchedType == null) {
            StringBuilder names = new StringBuilder();
            for (InvoiceType t : getCachedInvoiceTypes()) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(t.getDisplayName());
            }
            return ToolJsonUtils.error("Rechnungstyp nicht gefunden: " + typeName + ". Verfügbare Typen: " + names.toString());
        }

        // Verify case exists
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        Invoice created = svc.addInvoice(caseId, matchedPool, matchedType, currency.trim());

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        if (created.getInvoiceNumber() != null) {
            sb.append(", \"invoiceNumber\": \"").append(ToolJsonUtils.escapeJson(created.getInvoiceNumber())).append("\"");
        }
        if (created.getName() != null) {
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(created.getName())).append("\"");
        }
        sb.append(", \"status\": \"").append(ToolJsonUtils.escapeJson(created.getStatusString())).append("\"");
        sb.append(", \"currency\": \"").append(ToolJsonUtils.escapeJson(created.getCurrency())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateInvoicePosition(JsonObject args) throws Exception {
        String invoiceId = (String) args.get("invoiceId");
        String name = (String) args.get("name");
        String unitsStr = (String) args.get("units");
        String unitPriceStr = (String) args.get("unitPrice");
        String description = (String) args.get("description");
        String taxRateStr = (String) args.get("taxRate");

        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            return ToolJsonUtils.error("Rechnungs-ID fehlt");
        }
        if (name == null || name.trim().isEmpty()) {
            return ToolJsonUtils.error("Positionsbezeichnung fehlt");
        }
        if (unitsStr == null || unitsStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Menge fehlt");
        }
        if (unitPriceStr == null || unitPriceStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Einzelpreis fehlt");
        }

        BigDecimal units;
        BigDecimal unitPrice;
        BigDecimal taxRate;
        try {
            units = new BigDecimal(unitsStr.trim());
        } catch (NumberFormatException ex) {
            return ToolJsonUtils.error("Menge ist keine gültige Zahl: " + unitsStr);
        }
        try {
            unitPrice = new BigDecimal(unitPriceStr.trim());
        } catch (NumberFormatException ex) {
            return ToolJsonUtils.error("Einzelpreis ist keine gültige Zahl: " + unitPriceStr);
        }
        if (taxRateStr != null && !taxRateStr.trim().isEmpty()) {
            try {
                taxRate = new BigDecimal(taxRateStr.trim());
            } catch (NumberFormatException ex) {
                return ToolJsonUtils.error("Steuersatz ist keine gültige Zahl: " + taxRateStr);
            }
        } else {
            taxRate = new BigDecimal("19.0");
        }

        InvoicePosition pos = new InvoicePosition();
        pos.setName(name.trim());
        pos.setUnits(units);
        pos.setUnitPrice(unitPrice);
        pos.setTaxRate(taxRate);
        pos.setTotal(units.multiply(unitPrice));
        if (description != null && !description.trim().isEmpty()) {
            pos.setDescription(description.trim());
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        InvoicePosition created = svc.addInvoicePosition(invoiceId, pos);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(created.getName())).append("\"");
        sb.append(", \"units\": ").append(created.getUnits());
        sb.append(", \"unitPrice\": ").append(created.getUnitPrice());
        sb.append(", \"total\": ").append(created.getTotal());
        sb.append(", \"taxRate\": ").append(created.getTaxRate());
        sb.append(", \"position\": ").append(created.getPosition());
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateInstantMessage(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String content = (String) args.get("content");
        String recipient = (String) args.get("recipient");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (content == null || content.trim().isEmpty()) {
            return ToolJsonUtils.error("Nachrichteninhalt fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        // If recipient is provided, validate and prepend @mention
        String messageContent = content.trim();
        if (recipient != null && !recipient.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(recipient.trim())) {
                    recipient = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Empfänger nicht gefunden: " + recipient + ". Verfügbare Benutzer: " + userNames.toString());
            }
            messageContent = "@" + recipient + " " + messageContent;
        }

        String currentUser = UserSettings.getInstance().getCurrentUser().getPrincipalId();

        InstantMessage msg = new InstantMessage();
        msg.setCaseContext(caseBean);
        msg.setContent(messageContent);
        msg.setSender(currentUser);
        msg.setSent(new Date());

        MessagingServiceRemote msgSvc = locator.lookupMessagingServiceRemote();
        InstantMessage created = msgSvc.submitMessage(msg);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"sender\": \"").append(ToolJsonUtils.escapeJson(currentUser)).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        if (recipient != null && !recipient.trim().isEmpty()) {
            sb.append(", \"recipient\": \"").append(ToolJsonUtils.escapeJson(recipient)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateCase(JsonObject args) throws Exception {
        String name = (String) args.get("name");
        String reason = (String) args.get("reason");
        String subjectField = (String) args.get("subjectField");
        String lawyer = (String) args.get("lawyer");
        String assistant = (String) args.get("assistant");
        String notice = (String) args.get("notice");

        if (name == null || name.trim().isEmpty()) {
            return ToolJsonUtils.error("Aktenbezeichnung (name) fehlt");
        }

        // Validate lawyer if provided
        if (lawyer != null && !lawyer.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(lawyer.trim())) {
                    lawyer = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Anwalt nicht gefunden: " + lawyer + ". Verfügbare Benutzer: " + userNames.toString());
            }
        }

        // Validate assistant if provided
        if (assistant != null && !assistant.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(assistant.trim())) {
                    assistant = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Sachbearbeiter nicht gefunden: " + assistant + ". Verfügbare Benutzer: " + userNames.toString());
            }
        }

        ArchiveFileBean dto = new ArchiveFileBean();
        dto.setName(name.trim());
        if (reason != null && !reason.trim().isEmpty()) {
            dto.setReason(reason.trim());
        }
        if (subjectField != null && !subjectField.trim().isEmpty()) {
            dto.setSubjectField(subjectField.trim());
        }
        if (lawyer != null && !lawyer.trim().isEmpty()) {
            dto.setLawyer(lawyer);
        }
        if (assistant != null && !assistant.trim().isEmpty()) {
            dto.setAssistant(assistant);
        }
        if (notice != null && !notice.trim().isEmpty()) {
            dto.setNotice(notice.trim());
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean created = svc.createArchiveFile(dto);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"fileNumber\": \"").append(ToolJsonUtils.escapeJson(created.getFileNumber())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(created.getName())).append("\"");
        if (created.getReason() != null && !created.getReason().isEmpty()) {
            sb.append(", \"reason\": \"").append(ToolJsonUtils.escapeJson(created.getReason())).append("\"");
        }
        if (created.getSubjectField() != null && !created.getSubjectField().isEmpty()) {
            sb.append(", \"subjectField\": \"").append(ToolJsonUtils.escapeJson(created.getSubjectField())).append("\"");
        }
        if (created.getLawyer() != null && !created.getLawyer().isEmpty()) {
            sb.append(", \"lawyer\": \"").append(ToolJsonUtils.escapeJson(created.getLawyer())).append("\"");
        }
        if (created.getAssistant() != null && !created.getAssistant().isEmpty()) {
            sb.append(", \"assistant\": \"").append(ToolJsonUtils.escapeJson(created.getAssistant())).append("\"");
        }
        if (created.getNotice() != null && !created.getNotice().isEmpty()) {
            sb.append(", \"notice\": \"").append(ToolJsonUtils.escapeJson(created.getNotice())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeCreateContact(JsonObject args) throws Exception {
        String name = (String) args.get("name");
        String firstName = (String) args.get("firstName");
        String company = (String) args.get("company");
        String salutation = (String) args.get("salutation");
        String title = (String) args.get("title");
        String street = (String) args.get("street");
        String zipCode = (String) args.get("zipCode");
        String city = (String) args.get("city");
        String country = (String) args.get("country");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String mobile = (String) args.get("mobile");
        String fax = (String) args.get("fax");
        String website = (String) args.get("website");

        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasCompany = company != null && !company.trim().isEmpty();
        if (!hasName && !hasCompany) {
            return ToolJsonUtils.error("Name oder Firma muss angegeben werden");
        }

        AddressBean candidate = new AddressBean();
        if (hasName) {
            candidate.setName(name.trim());
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            candidate.setFirstName(firstName.trim());
        }
        if (hasCompany) {
            candidate.setCompany(company.trim());
        }
        if (salutation != null && !salutation.trim().isEmpty()) {
            candidate.setSalutation(salutation.trim());
        }
        if (title != null && !title.trim().isEmpty()) {
            candidate.setTitle(title.trim());
        }
        if (street != null && !street.trim().isEmpty()) {
            candidate.setStreet(street.trim());
        }
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            candidate.setZipCode(zipCode.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            candidate.setCity(city.trim());
        }
        if (country != null && !country.trim().isEmpty()) {
            candidate.setCountry(country.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            candidate.setEmail(email.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            candidate.setPhone(phone.trim());
        }
        if (mobile != null && !mobile.trim().isEmpty()) {
            candidate.setMobile(mobile.trim());
        }
        if (fax != null && !fax.trim().isEmpty()) {
            candidate.setFax(fax.trim());
        }
        if (website != null && !website.trim().isEmpty()) {
            candidate.setWebsite(website.trim());
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();
        AddressBean created = addrSvc.createAddress(candidate);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"contact\": ");
        StringBuilder contactSb = new StringBuilder();
        appendContactJson(contactSb, created);
        sb.append(contactSb);
        sb.append("}");
        return sb.toString();
    }

    private String executeUpdateCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        String name = (String) args.get("name");
        String reason = (String) args.get("reason");
        String subjectField = (String) args.get("subjectField");
        String lawyer = (String) args.get("lawyer");
        String assistant = (String) args.get("assistant");
        String notice = (String) args.get("notice");

        // Validate lawyer if provided
        if (lawyer != null && !lawyer.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(lawyer.trim())) {
                    lawyer = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Anwalt nicht gefunden: " + lawyer + ". Verfügbare Benutzer: " + userNames.toString());
            }
        }

        // Validate assistant if provided
        if (assistant != null && !assistant.trim().isEmpty()) {
            boolean found = false;
            for (AppUserBean u : getCachedUsers()) {
                if (u.getPrincipalId().equalsIgnoreCase(assistant.trim())) {
                    assistant = u.getPrincipalId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                StringBuilder userNames = new StringBuilder();
                for (AppUserBean u : getCachedUsers()) {
                    if (userNames.length() > 0) {
                        userNames.append(", ");
                    }
                    userNames.append(u.getPrincipalId());
                }
                return ToolJsonUtils.error("Sachbearbeiter nicht gefunden: " + assistant + ". Verfügbare Benutzer: " + userNames.toString());
            }
        }

        // Apply only provided fields
        if (name != null && !name.trim().isEmpty()) {
            caseBean.setName(name.trim());
        }
        if (reason != null) {
            caseBean.setReason(reason.trim());
        }
        if (subjectField != null) {
            caseBean.setSubjectField(subjectField.trim());
        }
        if (lawyer != null) {
            caseBean.setLawyer(lawyer.trim());
        }
        if (assistant != null) {
            caseBean.setAssistant(assistant.trim());
        }
        if (notice != null) {
            caseBean.setNotice(notice.trim());
        }

        svc.updateArchiveFile(caseBean);

        // Re-read to get server-side state
        ArchiveFileBean updated = svc.getArchiveFile(caseId);
        return buildCaseJson(updated, svc);
    }

    private String executeUpdateContact(JsonObject args) throws Exception {
        String contactId = (String) args.get("contactId");
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();
        AddressBean contact = addrSvc.getAddress(contactId);
        if (contact == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactId);
        }

        String name = (String) args.get("name");
        String firstName = (String) args.get("firstName");
        String company = (String) args.get("company");
        String salutation = (String) args.get("salutation");
        String title = (String) args.get("title");
        String street = (String) args.get("street");
        String zipCode = (String) args.get("zipCode");
        String city = (String) args.get("city");
        String country = (String) args.get("country");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String mobile = (String) args.get("mobile");
        String fax = (String) args.get("fax");
        String website = (String) args.get("website");

        // Apply only provided fields
        if (name != null) {
            contact.setName(name.trim());
        }
        if (firstName != null) {
            contact.setFirstName(firstName.trim());
        }
        if (company != null) {
            contact.setCompany(company.trim());
        }
        if (salutation != null) {
            contact.setSalutation(salutation.trim());
        }
        if (title != null) {
            contact.setTitle(title.trim());
        }
        if (street != null) {
            contact.setStreet(street.trim());
        }
        if (zipCode != null) {
            contact.setZipCode(zipCode.trim());
        }
        if (city != null) {
            contact.setCity(city.trim());
        }
        if (country != null) {
            contact.setCountry(country.trim());
        }
        if (email != null) {
            contact.setEmail(email.trim());
        }
        if (phone != null) {
            contact.setPhone(phone.trim());
        }
        if (mobile != null) {
            contact.setMobile(mobile.trim());
        }
        if (fax != null) {
            contact.setFax(fax.trim());
        }
        if (website != null) {
            contact.setWebsite(website.trim());
        }

        addrSvc.updateAddress(contact);

        // Re-read to get server-side state
        AddressBean updated = addrSvc.getAddress(contactId);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"contact\": ");
        StringBuilder contactSb = new StringBuilder();
        appendContactJson(contactSb, updated);
        sb.append(contactSb);
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // Web tool implementations
    // =========================================================================

    private String executeWebSearch(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }

        try {
            String encoded = URLEncoder.encode(query.trim(), "UTF-8");
            String searchUrl = "https://search.brave.com/search?q=" + encoded;

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(WEB_USER_AGENT)
                    .timeout(WEB_TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // Brave renders each result title inside <div class="search-snippet-title">
            // wrapped in an <a href="URL"> parent link.
            // The description follows in a sibling <div class="generic-snippet">.
            Elements titleDivs = doc.select("div.search-snippet-title");

            StringBuilder sb = new StringBuilder();
            sb.append("{\"query\": \"").append(ToolJsonUtils.escapeJson(query.trim())).append("\"");
            sb.append(", \"results\": [");
            int count = 0;
            for (Element titleDiv : titleDivs) {
                Element linkEl = titleDiv.parent();
                if (linkEl == null || !"a".equals(linkEl.tagName())) {
                    continue;
                }

                String href = linkEl.attr("href");
                if (!href.startsWith("http://") && !href.startsWith("https://")) {
                    continue;
                }

                String title = titleDiv.attr("title");
                if (title == null || title.isEmpty()) {
                    title = titleDiv.text();
                }

                // Description is in the next sibling after the <a> parent
                String snippet = "";
                Element snippetContainer = linkEl.nextElementSibling();
                if (snippetContainer != null) {
                    Element contentDiv = snippetContainer.select("div.content").first();
                    if (contentDiv != null) {
                        snippet = contentDiv.text();
                    } else {
                        snippet = snippetContainer.text();
                    }
                }

                if (count > 0) {
                    sb.append(",");
                }
                sb.append("{\"title\": \"").append(ToolJsonUtils.escapeJson(title)).append("\"");
                sb.append(", \"url\": \"").append(ToolJsonUtils.escapeJson(href)).append("\"");
                sb.append(", \"snippet\": \"").append(ToolJsonUtils.escapeJson(snippet)).append("\"");
                sb.append("}");
                count++;
                if (count >= 10) {
                    break;
                }
            }
            sb.append("], \"totalResults\": ").append(count);
            sb.append("}");
            return sb.toString();

        } catch (SocketTimeoutException ex) {
            return ToolJsonUtils.error("Zeitüberschreitung bei der Websuche");
        } catch (IOException ex) {
            log.error("Web search failed", ex);
            return ToolJsonUtils.error("Verbindungsfehler bei der Websuche: " + ex.getMessage());
        }
    }

    private String executeFetchUrl(JsonObject args) throws Exception {
        String url = (String) args.get("url");
        if (url == null || url.trim().isEmpty()) {
            return ToolJsonUtils.error("URL fehlt");
        }

        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(WEB_USER_AGENT)
                    .timeout(WEB_TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();

            int statusCode = response.statusCode();
            if (statusCode >= 400) {
                return ToolJsonUtils.error("HTTP-Fehler " + statusCode + " beim Laden von: " + url);
            }

            String contentType = response.contentType();
            if (contentType != null && !contentType.contains("html") && !contentType.contains("text") && !contentType.contains("xml") && !contentType.contains("json")) {
                return ToolJsonUtils.error("Kein Textinhalt (Content-Type: " + contentType + ")");
            }

            Document doc = response.parse();
            String title = doc.title();

            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, noscript, iframe").remove();
            // Remove common ad/cookie elements
            doc.select("[class*=cookie], [class*=Cookie], [id*=cookie], [id*=Cookie]").remove();
            doc.select("[class*=advert], [class*=Advert], [id*=advert], [id*=Advert]").remove();
            doc.select("[class*=banner], [id*=banner]").remove();

            Element body = doc.body();
            String text = (body != null) ? body.text() : doc.text();

            boolean truncated = false;
            if (text.length() > MAX_CONTENT_CHARS) {
                text = text.substring(0, MAX_CONTENT_CHARS);
                truncated = true;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"url\": \"").append(ToolJsonUtils.escapeJson(url)).append("\"");
            sb.append(", \"title\": \"").append(ToolJsonUtils.escapeJson(title)).append("\"");
            sb.append(", \"content\": \"").append(ToolJsonUtils.escapeJson(text)).append("\"");
            if (truncated) {
                sb.append(", \"truncated\": true");
            }
            sb.append("}");
            return sb.toString();

        } catch (SocketTimeoutException ex) {
            return ToolJsonUtils.error("Zeitüberschreitung beim Laden von: " + url);
        } catch (SSLException ex) {
            return ToolJsonUtils.error("SSL-Fehler beim Laden von: " + url + " - " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ToolJsonUtils.error("Ungültige URL: " + url);
        } catch (IOException ex) {
            log.error("URL fetch failed: " + url, ex);
            return ToolJsonUtils.error("Verbindungsfehler beim Laden von: " + url + " - " + ex.getMessage());
        }
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private String buildCaseJson(ArchiveFileBean caseBean, ArchiveFileServiceRemote svc) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(caseBean.getId())).append("\"");
        sb.append(", \"fileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(caseBean.getName())).append("\"");
        if (caseBean.getReason() != null) {
            sb.append(", \"reason\": \"").append(ToolJsonUtils.escapeJson(caseBean.getReason())).append("\"");
        }
        if (caseBean.getSubjectField() != null) {
            sb.append(", \"subjectField\": \"").append(ToolJsonUtils.escapeJson(caseBean.getSubjectField())).append("\"");
        }
        if (caseBean.getNotice() != null) {
            sb.append(", \"notice\": \"").append(ToolJsonUtils.escapeJson(caseBean.getNotice())).append("\"");
        }
        if (caseBean.getLawyer() != null) {
            sb.append(", \"lawyer\": \"").append(ToolJsonUtils.escapeJson(caseBean.getLawyer())).append("\"");
        }
        if (caseBean.getAssistant() != null) {
            sb.append(", \"assistant\": \"").append(ToolJsonUtils.escapeJson(caseBean.getAssistant())).append("\"");
        }
        if (caseBean.getClaimNumber() != null) {
            sb.append(", \"claimNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getClaimNumber())).append("\"");
        }
        sb.append(", \"claimValue\": ").append(caseBean.getClaimValue());
        sb.append(", \"archived\": ").append(caseBean.isArchived());

        // Add involved parties
        try {
            List<ArchiveFileAddressesBean> parties = svc.getInvolvementDetailsForCase(caseBean.getId());
            if (parties != null && !parties.isEmpty()) {
                sb.append(", \"parties\": [");
                for (int i = 0; i < parties.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    ArchiveFileAddressesBean p = parties.get(i);
                    sb.append("{");
                    if (p.getAddressKey() != null) {
                        sb.append("\"name\": \"").append(ToolJsonUtils.escapeJson(p.getAddressKey().toDisplayName())).append("\"");
                        sb.append(", \"contactId\": \"").append(ToolJsonUtils.escapeJson(p.getAddressKey().getId())).append("\"");
                    }
                    if (p.getReferenceType() != null) {
                        sb.append(", \"role\": \"").append(ToolJsonUtils.escapeJson(p.getReferenceType().getName())).append("\"");
                    }
                    sb.append("}");
                }
                sb.append("]");
            }
        } catch (Exception ex) {
            log.warn("Could not load parties for case " + caseBean.getFileNumber(), ex);
        }

        sb.append("}");
        return sb.toString();
    }

    private void appendEventJson(StringBuilder sb, ArchiveFileReviewsBean ev) {
        sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(ev.getId())).append("\"");
        sb.append(", \"type\": \"").append(ToolJsonUtils.escapeJson(ev.getEventTypeName())).append("\"");
        if (ev.getSummary() != null) {
            sb.append(", \"summary\": \"").append(ToolJsonUtils.escapeJson(ev.getSummary())).append("\"");
        }
        if (ev.getBeginDate() != null) {
            sb.append(", \"beginDate\": \"").append(ToolJsonUtils.formatDate(ev.getBeginDate())).append("\"");
        }
        if (ev.getEndDate() != null) {
            sb.append(", \"endDate\": \"").append(ToolJsonUtils.formatDate(ev.getEndDate())).append("\"");
        }
        sb.append(", \"done\": ").append(ev.isDone());
        if (ev.getAssignee() != null) {
            sb.append(", \"assignee\": \"").append(ToolJsonUtils.escapeJson(ev.getAssignee())).append("\"");
        }
        if (ev.getDescription() != null && !ev.getDescription().isEmpty()) {
            sb.append(", \"description\": \"").append(ToolJsonUtils.escapeJson(ev.getDescription())).append("\"");
        }
        if (ev.getLocation() != null && !ev.getLocation().isEmpty()) {
            sb.append(", \"location\": \"").append(ToolJsonUtils.escapeJson(ev.getLocation())).append("\"");
        }
        if (ev.getCalendarSetup() != null) {
            sb.append(", \"calendar\": \"").append(ToolJsonUtils.escapeJson(ev.getCalendarSetup().getDisplayName())).append("\"");
        }
        if (ev.getArchiveFileKey() != null) {
            sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(ev.getArchiveFileKey().getId())).append("\"");
            sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(ev.getArchiveFileKey().getFileNumber())).append("\"");
            sb.append(", \"caseName\": \"").append(ToolJsonUtils.escapeJson(ev.getArchiveFileKey().getName())).append("\"");
        }
        sb.append("}");
    }

    private void appendContactJson(StringBuilder sb, AddressBean a) {
        sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(a.getId())).append("\"");
        if (a.getName() != null) {
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(a.getName())).append("\"");
        }
        if (a.getFirstName() != null) {
            sb.append(", \"firstName\": \"").append(ToolJsonUtils.escapeJson(a.getFirstName())).append("\"");
        }
        if (a.getCompany() != null && !a.getCompany().isEmpty()) {
            sb.append(", \"company\": \"").append(ToolJsonUtils.escapeJson(a.getCompany())).append("\"");
        }
        if (a.getSalutation() != null && !a.getSalutation().isEmpty()) {
            sb.append(", \"salutation\": \"").append(ToolJsonUtils.escapeJson(a.getSalutation())).append("\"");
        }
        if (a.getTitle() != null && !a.getTitle().isEmpty()) {
            sb.append(", \"title\": \"").append(ToolJsonUtils.escapeJson(a.getTitle())).append("\"");
        }
        if (a.getStreet() != null && !a.getStreet().isEmpty()) {
            sb.append(", \"street\": \"").append(ToolJsonUtils.escapeJson(a.getStreet())).append("\"");
        }
        if (a.getZipCode() != null && !a.getZipCode().isEmpty()) {
            sb.append(", \"zipCode\": \"").append(ToolJsonUtils.escapeJson(a.getZipCode())).append("\"");
        }
        if (a.getCity() != null && !a.getCity().isEmpty()) {
            sb.append(", \"city\": \"").append(ToolJsonUtils.escapeJson(a.getCity())).append("\"");
        }
        if (a.getCountry() != null && !a.getCountry().isEmpty()) {
            sb.append(", \"country\": \"").append(ToolJsonUtils.escapeJson(a.getCountry())).append("\"");
        }
        if (a.getEmail() != null && !a.getEmail().isEmpty()) {
            sb.append(", \"email\": \"").append(ToolJsonUtils.escapeJson(a.getEmail())).append("\"");
        }
        if (a.getPhone() != null && !a.getPhone().isEmpty()) {
            sb.append(", \"phone\": \"").append(ToolJsonUtils.escapeJson(a.getPhone())).append("\"");
        }
        if (a.getMobile() != null && !a.getMobile().isEmpty()) {
            sb.append(", \"mobile\": \"").append(ToolJsonUtils.escapeJson(a.getMobile())).append("\"");
        }
        if (a.getFax() != null && !a.getFax().isEmpty()) {
            sb.append(", \"fax\": \"").append(ToolJsonUtils.escapeJson(a.getFax())).append("\"");
        }
        if (a.getWebsite() != null && !a.getWebsite().isEmpty()) {
            sb.append(", \"website\": \"").append(ToolJsonUtils.escapeJson(a.getWebsite())).append("\"");
        }
        sb.append("}");
    }

    private String extractPdfText(byte[] pdfContent) throws Exception {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfContent))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private static String escapeJson(String value) {
        return ToolJsonUtils.escapeJson(value);
    }
}
