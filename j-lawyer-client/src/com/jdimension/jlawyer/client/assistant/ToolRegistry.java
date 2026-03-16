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
import com.jdimension.jlawyer.persistence.Group;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.persistence.CaseFolder;
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
import com.jdimension.jlawyer.persistence.Timesheet;
import com.jdimension.jlawyer.persistence.TimesheetPosition;
import com.jdimension.jlawyer.persistence.AppOptionGroupBean;
import com.jdimension.jlawyer.persistence.ArchiveFileTagsBean;
import com.jdimension.jlawyer.persistence.DocumentFolderTemplate;
import com.jdimension.jlawyer.persistence.DocumentTagsBean;
import com.jdimension.jlawyer.pojo.PartiesTriplet;
import com.jdimension.jlawyer.server.constants.OptionConstants;
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.FormsServiceRemote;
import com.jdimension.jlawyer.services.SystemManagementRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CalendarServiceRemote;
import com.jdimension.jlawyer.services.InvoiceServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import com.jdimension.jlawyer.services.MessagingServiceRemote;
import org.jlawyer.data.tree.GenericNode;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import javax.net.ssl.SSLException;
import com.jdimension.jlawyer.client.events.EventBroker;
import com.jdimension.jlawyer.client.events.CasesChangedEvent;
import com.jdimension.jlawyer.client.events.ContactUpdatedEvent;
import com.jdimension.jlawyer.client.events.DocumentAddedEvent;
import com.jdimension.jlawyer.client.events.DocumentRemovedEvent;
import com.jdimension.jlawyer.client.events.ReviewAddedEvent;
import com.jdimension.jlawyer.client.events.InvoicePositionAddedEvent;
import com.jdimension.jlawyer.client.events.NewInstantMessagesEvent;
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
    private List<Group> cachedMyGroups;

    static {
        // Existing tools
        TOOLS.add(new ToolDefinition("search_cases", "Sucht nach Akten anhand eines Suchbegriffs. Gibt eine Liste von Akten mit Aktenzeichen, Kurzrubrum und Sachgebiet zurück.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Aktensuche", true))));

        TOOLS.add(new ToolDefinition("get_case", "Ruft Details einer Akte ab, einschließlich Beteiligte und Aktennotiz.",
                Arrays.asList(new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true))));

        TOOLS.add(new ToolDefinition("search_contacts", "Sucht nach Kontakten/Adressen anhand eines Suchbegriffs.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Kontaktsuche", true))));

        TOOLS.add(new ToolDefinition("list_case_documents", "Listet Dokumente einer Akte seitenweise auf (20 pro Seite). Gibt totalDocuments, page, totalPages und hasMore zurück.",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("list_case_documents_by_date",
                "Listet Dokumente einer Akte, die innerhalb eines Zeitraums erstellt wurden. Gibt die Dokumente sortiert nach Erstellungsdatum (neueste zuerst) zurück.",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("fromDate", "string", "Startdatum im Format yyyy-MM-dd", true),
                        new ToolParameter("toDate", "string", "Enddatum im Format yyyy-MM-dd", true))));

        TOOLS.add(new ToolDefinition("search_case_documents", "Durchsucht Dokumente einer Akte anhand des Dateinamens (case-insensitive, Teilübereinstimmung). Ergebnisse sind seitenweise (20 pro Seite).",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("query", "string", "Suchbegriff für den Dateinamen", true),
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

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

        TOOLS.add(new ToolDefinition("get_all_open_events", "Gibt alle offenen Kalenderereignisse zurück. Optional nach Typ filterbar (Wiedervorlage, Frist, Termin).",
                Arrays.asList(
                        new ToolParameter("eventType", "string", "Ereignistyp zum Filtern: Wiedervorlage, Frist oder Termin (optional, Standard: alle)", false))));

        TOOLS.add(new ToolDefinition("get_all_open_events_between_dates", "Gibt alle offenen Kalenderereignisse zwischen zwei Daten zurück. Optional nach Typ filterbar.",
                Arrays.asList(
                        new ToolParameter("fromDate", "string", "Startdatum im ISO-8601-Format (z.B. 2025-03-01T00:00:00)", true),
                        new ToolParameter("toDate", "string", "Enddatum im ISO-8601-Format (z.B. 2025-03-31T23:59:59)", true),
                        new ToolParameter("eventType", "string", "Ereignistyp zum Filtern: Wiedervorlage, Frist oder Termin (optional, Standard: alle)", false))));

        TOOLS.add(new ToolDefinition("list_event_types",
                "Gibt die verfügbaren Kalenderereignis-Typen zurück (Wiedervorlage, Frist, Termin). Nützlich um den eventType-Parameter für get_all_open_events oder get_all_open_events_between_dates zu ermitteln.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("find_free_slots",
                "Findet freie Zeitfenster im Kalender eines Benutzers. Gibt verfügbare Slots zurück, die für neue Termine genutzt werden können. Nur Typ 'Termin' (eventType=30) wird als blockierend betrachtet.",
                Arrays.asList(
                        new ToolParameter("fromDate", "string", "Startdatum im Format yyyy-MM-dd", true),
                        new ToolParameter("toDate", "string", "Enddatum im Format yyyy-MM-dd", true),
                        new ToolParameter("durationMinutes", "integer", "Gewünschte Mindestdauer des Slots in Minuten (Standard: 60)", false),
                        new ToolParameter("assignee", "string", "Benutzername des Kalenderinhabers (optional, Standard: angemeldeter Benutzer)", false),
                        new ToolParameter("workStartHour", "integer", "Beginn der Arbeitszeit als Stunde (0-23, Standard: 8)", false),
                        new ToolParameter("workEndHour", "integer", "Ende der Arbeitszeit als Stunde (0-23, Standard: 18)", false))));

        TOOLS.add(new ToolDefinition("get_all_open_invoices", "Gibt alle offenen Rechnungen seitenweise zurück (20 pro Seite). Gibt totalInvoices, page, totalPages und hasMore zurück.",
                Arrays.asList(
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("search_invoices", "Sucht offene Rechnungen per Textsuche (case-insensitiv, contains) in Rechnungsnummer, Name, Vorname und Firma des Kontakts. Ergebnisse auf 50 begrenzt.",
                Arrays.asList(
                        new ToolParameter("query", "string", "Suchbegriff", true))));

        TOOLS.add(new ToolDefinition("search_invoices_by_date", "Sucht offene Rechnungen, deren Erstellungsdatum in einem Zeitraum liegt. Seitenweise Ausgabe (20 pro Seite). Gibt totalInvoices, page, totalPages und hasMore zurück.",
                Arrays.asList(
                        new ToolParameter("fromDate", "string", "Startdatum im Format yyyy-MM-dd", true),
                        new ToolParameter("toDate", "string", "Enddatum im Format yyyy-MM-dd", true),
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("get_document_content", "Gibt den Inhalt eines Dokuments als Base64-kodierten String zurück.",
                Arrays.asList(new ToolParameter("documentId", "string", "ID des Dokuments", true))));

        TOOLS.add(new ToolDefinition("rename_document",
                "Benennt ein Dokument in einer Akte um.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments", true),
                        new ToolParameter("newName", "string", "Neuer Dateiname des Dokuments", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("delete_document",
                "Löscht ein Dokument aus einer Akte (Papierkorb). Das Dokument kann vom Benutzer wiederhergestellt werden.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments", true)),
                ToolDefinition.RISK_HIGH));

        TOOLS.add(new ToolDefinition("list_calendars",
                "Listet alle verfügbaren Kalender auf. Nützlich um die Kalender-ID für create_event zu ermitteln.",
                Arrays.asList()));

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
                        new ToolParameter("streetNumber", "string", "Hausnummer (optional)", false),
                        new ToolParameter("email", "string", "E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Telefon (optional)", false),
                        new ToolParameter("gender", "string", "Geschlecht (optional, Werte: MALE, FEMALE, OTHER, LEGALENTITY, UNDEFINED)", false),
                        new ToolParameter("salutation", "string", "Anrede (optional, z.B. Herr, Frau)", false),
                        new ToolParameter("complimentaryClose", "string", "Grußformel (optional, z.B. Mit freundlichen Grüßen)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("add_party_to_case", "Fügt einen bestehenden Kontakt als Beteiligten zu einer Akte hinzu.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("contactId", "string", "ID des Kontakts", true),
                        new ToolParameter("partyType", "string", "Beteiligtentyp (z.B. Mandant, Gegner)", true),
                        new ToolParameter("reference", "string", "Aktenzeichen des Beteiligten (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("list_invoice_pools",
                "Listet alle verfügbaren Rechnungsnummernkreise auf. Nützlich um die Pool-ID für create_invoice zu ermitteln.",
                Arrays.asList()));

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
                        new ToolParameter("notice", "string", "Aktennotiz (optional)", false),
                        new ToolParameter("group", "string", "Name der Gruppe für Berechtigungen (optional)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_contact", "Erstellt einen neuen Kontakt. Führt keine Ähnlichkeitssuche durch — dafür gibt es create_or_get_contact.",
                Arrays.asList(
                        new ToolParameter("name", "string", "Nachname (erforderlich wenn keine Firma angegeben)", false),
                        new ToolParameter("firstName", "string", "Vorname (optional)", false),
                        new ToolParameter("company", "string", "Firma (erforderlich wenn kein Name angegeben)", false),
                        new ToolParameter("salutation", "string", "Anrede (optional, z.B. Herr, Frau)", false),
                        new ToolParameter("title", "string", "Titel (optional, z.B. Dr., Prof.)", false),
                        new ToolParameter("street", "string", "Straße (optional)", false),
                        new ToolParameter("streetNumber", "string", "Hausnummer (optional)", false),
                        new ToolParameter("zipCode", "string", "Postleitzahl (optional)", false),
                        new ToolParameter("city", "string", "Stadt (optional)", false),
                        new ToolParameter("country", "string", "Land (optional)", false),
                        new ToolParameter("email", "string", "E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Telefon (optional)", false),
                        new ToolParameter("mobile", "string", "Mobiltelefon (optional)", false),
                        new ToolParameter("fax", "string", "Fax (optional)", false),
                        new ToolParameter("website", "string", "Webseite (optional)", false),
                        new ToolParameter("gender", "string", "Geschlecht (optional, Werte: MALE, FEMALE, OTHER, LEGALENTITY, UNDEFINED)", false),
                        new ToolParameter("complimentaryClose", "string", "Grußformel (optional, z.B. Mit freundlichen Grüßen)", false)),
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
                        new ToolParameter("streetNumber", "string", "Neue Hausnummer (optional)", false),
                        new ToolParameter("zipCode", "string", "Neue Postleitzahl (optional)", false),
                        new ToolParameter("city", "string", "Neue Stadt (optional)", false),
                        new ToolParameter("country", "string", "Neues Land (optional)", false),
                        new ToolParameter("email", "string", "Neue E-Mail (optional)", false),
                        new ToolParameter("phone", "string", "Neues Telefon (optional)", false),
                        new ToolParameter("mobile", "string", "Neues Mobiltelefon (optional)", false),
                        new ToolParameter("fax", "string", "Neues Fax (optional)", false),
                        new ToolParameter("website", "string", "Neue Webseite (optional)", false),
                        new ToolParameter("gender", "string", "Neues Geschlecht (optional, Werte: MALE, FEMALE, OTHER, LEGALENTITY, UNDEFINED)", false),
                        new ToolParameter("complimentaryClose", "string", "Neue Grußformel (optional, z.B. Mit freundlichen Grüßen)", false)),
                ToolDefinition.RISK_MEDIUM));

        // Timesheet tools
        TOOLS.add(new ToolDefinition("get_all_open_timesheets", "Gibt alle offenen Timesheets zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("get_open_timesheets_for_case", "Gibt alle offenen Timesheets einer Akte zurück.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_timesheet_positions", "Gibt alle erfassten Zeiteinträge eines Timesheets zurück.",
                Arrays.asList(new ToolParameter("timesheetId", "string", "ID des Timesheets", true))));

        TOOLS.add(new ToolDefinition("create_timesheet_position", "Erstellt einen neuen Zeiteintrag in einem Timesheet.",
                Arrays.asList(
                        new ToolParameter("timesheetId", "string", "ID des Timesheets", true),
                        new ToolParameter("name", "string", "Bezeichnung/Tätigkeit des Zeiteintrags", true),
                        new ToolParameter("startDate", "string", "Startdatum und -zeit im ISO-8601-Format (z.B. 2025-03-01T09:00:00)", true),
                        new ToolParameter("stopDate", "string", "Enddatum und -zeit im ISO-8601-Format (z.B. 2025-03-01T10:30:00)", true),
                        new ToolParameter("unitPrice", "string", "Stundensatz als Dezimalzahl (z.B. 150.00)", true),
                        new ToolParameter("taxRate", "string", "Steuersatz in Prozent (Standard: 19.0)", false),
                        new ToolParameter("description", "string", "Beschreibung (optional)", false),
                        new ToolParameter("principal", "string", "Benutzername der buchenden Person (optional, Standard: angemeldeter Benutzer)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("list_users",
                "Listet alle Benutzer der Installation auf. Gibt Benutzername, Anzeigename und E-Mail zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("get_my_groups",
                "Gibt die Gruppen zurück, in denen der aktuell angemeldete Benutzer Mitglied ist. Diese Gruppen steuern z.B. die Berechtigungen an Akten.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("list_case_folders",
                "Gibt die Ordnerstruktur einer Akte zurück. Jeder Ordner hat eine ID, einen Namen und ggf. Unterordner.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "ID der Akte", true))));

        TOOLS.add(new ToolDefinition("move_document_to_folder",
                "Verschiebt ein Dokument in einen Ordner innerhalb derselben Akte.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments", true),
                        new ToolParameter("folderId", "string", "ID des Zielordners", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("list_folder_templates",
                "Listet alle verfügbaren Ordnerstruktur-Vorlagen auf. Gibt die Namen und IDs der Vorlagen zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("apply_folder_template",
                "Wendet eine Ordnerstruktur-Vorlage auf eine Akte an. Die Ordner aus der Vorlage werden zur bestehenden Ordnerstruktur hinzugefügt.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("templateName", "string", "Name der Ordnervorlage", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("list_document_tags",
                "Gibt alle verfügbaren Etiketten (Tags) für Dokumente zurück, inkl. Listenetiketten mit ihren möglichen Werten.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("list_case_tags",
                "Gibt alle verfügbaren Etiketten (Tags) für Akten zurück, inkl. Listenetiketten mit ihren möglichen Werten.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("set_document_tag",
                "Setzt ein Etikett auf ein Dokument. Für Listenetiketten muss zusätzlich ein tagValue angegeben werden. Zum Entfernen active=false setzen.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments", true),
                        new ToolParameter("tagName", "string", "Name des Etiketts", true),
                        new ToolParameter("tagValue", "string", "Wert bei Listenetiketten (optional, null für einfache Etiketten)", false),
                        new ToolParameter("active", "string", "true zum Setzen, false zum Entfernen (Standard: true)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("set_case_tag",
                "Setzt ein Etikett auf eine Akte. Für Listenetiketten muss zusätzlich ein tagValue angegeben werden. Zum Entfernen active=false setzen.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "ID der Akte", true),
                        new ToolParameter("tagName", "string", "Name des Etiketts", true),
                        new ToolParameter("tagValue", "string", "Wert bei Listenetiketten (optional, null für einfache Etiketten)", false),
                        new ToolParameter("active", "string", "true zum Setzen, false zum Entfernen (Standard: true)", false)),
                ToolDefinition.RISK_MEDIUM));

        // Template tools
//        TOOLS.add(new ToolDefinition("list_templates",
//                "Listet alle verfügbaren Dokumentvorlagen und ihre Ordnerstruktur auf.",
//                Arrays.asList()));

        TOOLS.add(new ToolDefinition("search_templates",
                "Sucht Dokumentvorlagen anhand eines Suchbegriffs (Teilübereinstimmung, Groß-/Kleinschreibung wird ignoriert). Gibt nur passende Vorlagen mit Ordnerpfad zurück.",
                Arrays.asList(
                        new ToolParameter("query", "string", "Suchbegriff für den Vorlagennamen", true))));

        TOOLS.add(new ToolDefinition("create_document_from_template",
                "Erstellt ein Dokument in einer Akte aus einer Dokumentvorlage. Platzhalter werden automatisch aus den Aktendaten befüllt. Verwende zuerst list_templates um verfügbare Vorlagen zu sehen.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("templateFolder", "string", "Ordnerpfad der Vorlage (z.B. / oder /Vertragsrecht)", true),
                        new ToolParameter("templateName", "string", "Dateiname der Vorlage (z.B. Vollmacht.odt)", true),
                        new ToolParameter("fileName", "string", "Dateiname des neuen Dokuments ohne Erweiterung (z.B. Vollmacht Mueller)", true),
                        new ToolParameter("generatedText", "string", "Vom Assistenten generierter Text, der als Platzhalter {{INGO_TEXT}} in die Vorlage eingefügt wird (optional)", false)),
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
                case "list_case_documents_by_date":
                    return executeListCaseDocumentsByDate(args);
                case "search_case_documents":
                    return executeSearchCaseDocuments(args);
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
                case "list_event_types":
                    return executeListEventTypes(args);
                case "find_free_slots":
                    return executeFindFreeSlots(args);
                case "get_all_open_invoices":
                    return executeGetAllOpenInvoices(args);
                case "search_invoices":
                    return executeSearchInvoices(args);
                case "search_invoices_by_date":
                    return executeSearchInvoicesByDate(args);
                case "get_document_content":
                    return executeGetDocumentContent(args);
                case "rename_document":
                    return executeRenameDocument(args);
                case "delete_document":
                    return executeDeleteDocument(args);
                case "list_calendars":
                    return executeListCalendars(args);
                case "create_event":
                    return executeCreateEvent(args);
                case "create_note":
                    return executeCreateNote(args);
                case "create_or_get_contact":
                    return executeCreateOrGetContact(args);
                case "add_party_to_case":
                    return executeAddPartyToCase(args);
                case "list_invoice_pools":
                    return executeListInvoicePools(args);
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
                case "get_all_open_timesheets":
                    return executeGetAllOpenTimesheets(args);
                case "get_open_timesheets_for_case":
                    return executeGetOpenTimesheetsForCase(args);
                case "get_timesheet_positions":
                    return executeGetTimesheetPositions(args);
                case "create_timesheet_position":
                    return executeCreateTimesheetPosition(args);
                case "list_users":
                    return executeListUsers(args);
                case "get_my_groups":
                    return executeGetMyGroups(args);
                case "list_case_folders":
                    return executeListCaseFolders(args);
                case "move_document_to_folder":
                    return executeMoveDocumentToFolder(args);
                case "list_folder_templates":
                    return executeListFolderTemplates(args);
                case "apply_folder_template":
                    return executeApplyFolderTemplate(args);
                case "list_document_tags":
                    return executeListDocumentTags(args);
                case "list_case_tags":
                    return executeListCaseTags(args);
                case "set_document_tag":
                    return executeSetDocumentTag(args);
                case "set_case_tag":
                    return executeSetCaseTag(args);
//                case "list_templates":
//                    return executeListTemplates(args);
                case "search_templates":
                    return executeSearchTemplates(args);
                case "create_document_from_template":
                    return executeCreateDocumentFromTemplate(args);
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

    public ToolDefinition getToolDefinition(String toolId) {
        for (ToolDefinition td : TOOLS) {
            if (td.getId().equals(toolId)) {
                return td;
            }
        }
        return null;
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
                    return "Dokumentenliste: " + args.getOrDefault("fileNumber", "") + " (Seite " + args.getOrDefault("page", "1") + ")";
                case "list_case_documents_by_date":
                    return "Dokumentenliste: " + args.getOrDefault("fileNumber", "") + " (" + args.getOrDefault("fromDate", "") + " bis " + args.getOrDefault("toDate", "") + ")";
                case "search_case_documents":
                    return "Dokumentensuche: '" + args.getOrDefault("query", "") + "' in " + args.getOrDefault("fileNumber", "") + " (Seite " + args.getOrDefault("page", "1") + ")";
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
                case "list_event_types":
                    return "Verfügbare Ereignistypen auflisten";
                case "find_free_slots":
                    return "Freie Termine suchen: " + args.getOrDefault("fromDate", "") + " - " + args.getOrDefault("toDate", "");
                case "get_all_open_invoices":
                    return "Offene Rechnungen (Seite " + args.getOrDefault("page", "1") + ")";
                case "search_invoices":
                    return "Rechnungssuche: '" + args.getOrDefault("query", "") + "'";
                case "search_invoices_by_date":
                    return "Rechnungssuche: " + args.getOrDefault("fromDate", "") + " - " + args.getOrDefault("toDate", "") + " (Seite " + args.getOrDefault("page", "1") + ")";
                case "get_document_content":
                    return "Dokumentinhalt (Base64): " + args.getOrDefault("documentId", "");
                case "rename_document":
                    return "Dokument umbenennen: " + args.getOrDefault("newName", "");
                case "delete_document":
                    return "Dokument löschen: " + args.getOrDefault("documentId", "");
                case "list_calendars":
                    return "Verfügbare Kalender auflisten";
                case "create_event":
                    return "Termin erstellen: " + args.getOrDefault("summary", "");
                case "create_note":
                    return "Notiz erstellen in Akte: " + args.getOrDefault("caseId", "");
                case "create_or_get_contact":
                    return "Kontakt erstellen/suchen: " + args.getOrDefault("name", args.getOrDefault("company", ""));
                case "add_party_to_case":
                    return "Beteiligten hinzufügen: " + args.getOrDefault("partyType", "");
                case "list_invoice_pools":
                    return "Rechnungsnummernkreise auflisten";
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
                case "get_all_open_timesheets":
                    return "Alle offenen Timesheets";
                case "get_open_timesheets_for_case":
                    return "Offene Timesheets der Akte: " + args.getOrDefault("caseId", "");
                case "get_timesheet_positions":
                    return "Zeiteinträge: " + args.getOrDefault("timesheetId", "");
                case "create_timesheet_position":
                    return "Zeiteintrag erstellen: " + args.getOrDefault("name", "");
                case "list_users":
                    return "Benutzer auflisten";
                case "get_my_groups":
                    return "Meine Gruppen auflisten";
                case "list_case_folders":
                    return "Ordner der Akte: " + args.getOrDefault("caseId", "");
                case "move_document_to_folder":
                    return "Dokument in Ordner verschieben: " + args.getOrDefault("documentId", "");
                case "list_folder_templates":
                    return "Verfügbare Ordnervorlagen auflisten";
                case "apply_folder_template":
                    return "Ordnervorlage anwenden: " + args.getOrDefault("templateName", "");
                case "list_document_tags":
                    return "Verfügbare Dokument-Etiketten auflisten";
                case "list_case_tags":
                    return "Verfügbare Akten-Etiketten auflisten";
                case "set_document_tag":
                    return "Dokument-Etikett setzen: " + args.getOrDefault("tagName", "");
                case "set_case_tag":
                    return "Akten-Etikett setzen: " + args.getOrDefault("tagName", "");
//                case "list_templates":
//                    return "Dokumentvorlagen auflisten";
                case "search_templates":
                    return "Vorlagensuche: '" + args.getOrDefault("query", "") + "'";
                case "create_document_from_template":
                    return "Dokument aus Vorlage erstellen: " + args.getOrDefault("templateName", "");
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

    private List<Group> getCachedMyGroups() throws Exception {
        if (cachedMyGroups == null) {
            String principalId = UserSettings.getInstance().getCurrentUser().getPrincipalId();
            cachedMyGroups = ToolJsonUtils.getLocator().lookupSecurityServiceRemote().getGroupsForUser(principalId);
        }
        return cachedMyGroups;
    }

    // =========================================================================
    // New tool implementations
    // =========================================================================

    private String executeRenameDocument(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        String newName = (String) args.get("newName");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }
        if (newName == null || newName.trim().isEmpty()) {
            return ToolJsonUtils.error("Neuer Dateiname fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        ArchiveFileDocumentsBean doc = svc.getDocument(documentId);
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        // Dateiendung des Originals beibehalten
        String originalName = doc.getName();
        String originalExt = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0) {
            originalExt = originalName.substring(dotIdx);
        }

        String trimmedNew = newName.trim();
        String newExt = "";
        int newDotIdx = trimmedNew.lastIndexOf('.');
        if (newDotIdx >= 0) {
            newExt = trimmedNew.substring(newDotIdx);
        }

        if (!originalExt.isEmpty()) {
            if (newExt.equalsIgnoreCase(originalExt)) {
                // Endung stimmt überein — nichts tun
            } else if (!newExt.isEmpty()) {
                // Falsche Endung — ersetzen
                trimmedNew = trimmedNew.substring(0, newDotIdx) + originalExt;
            } else {
                // Keine Endung — anhängen
                trimmedNew = trimmedNew + originalExt;
            }
        }
        newName = trimmedNew;

        boolean success = svc.renameDocument(documentId, newName);
        if (success) {
            EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
            doc.setName(newName.trim());
            EventBroker.getInstance().publishEvent(new DocumentAddedEvent(doc));
            return "{\"success\": true, \"documentId\": \"" + ToolJsonUtils.escapeJson(documentId)
                    + "\", \"newName\": \"" + ToolJsonUtils.escapeJson(newName.trim()) + "\"}";
        } else {
            return ToolJsonUtils.error("Dokument konnte nicht umbenannt werden");
        }
    }

    private String executeDeleteDocument(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        ArchiveFileDocumentsBean doc = svc.getDocument(documentId);
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        String docName = doc.getName();
        svc.removeDocument(documentId);
        EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
        return "{\"success\": true, \"documentId\": \"" + ToolJsonUtils.escapeJson(documentId)
                + "\", \"deletedName\": \"" + ToolJsonUtils.escapeJson(docName) + "\"}";
    }

    private String executeListCalendars(JsonObject args) throws Exception {
        List<CalendarSetup> calendars = getCachedCalendars();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"calendars\": [");
        for (int i = 0; i < calendars.size(); i++) {
            CalendarSetup cs = calendars.get(i);
            if (i > 0) sb.append(", ");
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(cs.getId())).append("\"");
            sb.append(", \"displayName\": \"").append(ToolJsonUtils.escapeJson(cs.getDisplayName())).append("\"");
            sb.append(", \"eventType\": ").append(cs.getEventType());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeListInvoicePools(JsonObject args) throws Exception {
        List<InvoicePool> pools = getCachedInvoicePools();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"invoicePools\": [");
        for (int i = 0; i < pools.size(); i++) {
            InvoicePool p = pools.get(i);
            if (i > 0) sb.append(", ");
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(p.getId())).append("\"");
            sb.append(", \"displayName\": \"").append(ToolJsonUtils.escapeJson(p.getDisplayName())).append("\"");
            sb.append(", \"pattern\": \"").append(ToolJsonUtils.escapeJson(p.getPattern())).append("\"");
            sb.append(", \"lastIndex\": ").append(p.getLastIndex());
            sb.append(", \"paymentTerm\": ").append(p.getPaymentTerm());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeListUsers(JsonObject args) throws Exception {
        List<AppUserBean> users = getCachedUsers();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"users\": [");
        for (int i = 0; i < users.size(); i++) {
            AppUserBean u = users.get(i);
            if (i > 0) sb.append(", ");
            sb.append("{\"principalId\": \"").append(ToolJsonUtils.escapeJson(u.getPrincipalId())).append("\"");
            sb.append(", \"displayName\": \"").append(ToolJsonUtils.escapeJson(u.getDisplayName())).append("\"");
            if (u.getEmail() != null) {
                sb.append(", \"email\": \"").append(ToolJsonUtils.escapeJson(u.getEmail())).append("\"");
            }
            if (u.getAbbreviation() != null) {
                sb.append(", \"abbreviation\": \"").append(ToolJsonUtils.escapeJson(u.getAbbreviation())).append("\"");
            }
            sb.append(", \"lawyer\": ").append(u.isLawyer());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeGetMyGroups(JsonObject args) throws Exception {
        String principalId = UserSettings.getInstance().getCurrentUser().getPrincipalId();
        List<Group> groups = getCachedMyGroups();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"principalId\": \"").append(ToolJsonUtils.escapeJson(principalId)).append("\"");
        sb.append(", \"groups\": [");
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            if (i > 0) sb.append(", ");
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(g.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(g.getName())).append("\"");
            if (g.getAbbreviation() != null) {
                sb.append(", \"abbreviation\": \"").append(ToolJsonUtils.escapeJson(g.getAbbreviation())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeListCaseFolders(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId.trim());
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        CaseFolder root = caseBean.getRootFolder();
        if (root == null) {
            return "{\"caseId\": \"" + ToolJsonUtils.escapeJson(caseId) + "\", \"folders\": []}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\"");
        sb.append(", \"folders\": [");
        appendFolderChildren(root.getChildren(), sb, true);
        sb.append("]}");
        return sb.toString();
    }

    private void appendFolderChildren(List<CaseFolder> folders, StringBuilder sb, boolean isFirst) {
        if (folders == null) return;
        for (CaseFolder f : folders) {
            if (!isFirst) sb.append(", ");
            isFirst = false;
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(f.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(f.getName())).append("\"");
            if (f.getChildren() != null && !f.getChildren().isEmpty()) {
                sb.append(", \"children\": [");
                appendFolderChildren(f.getChildren(), sb, true);
                sb.append("]");
            }
            sb.append("}");
        }
    }

    private String executeMoveDocumentToFolder(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        String folderId = (String) args.get("folderId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID (documentId) fehlt");
        }
        if (folderId == null || folderId.trim().isEmpty()) {
            return ToolJsonUtils.error("Ordner-ID (folderId) fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();

        ArchiveFileDocumentsBean doc = svc.getDocument(documentId.trim());
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        svc.moveDocumentsToFolder(Collections.singletonList(documentId.trim()), folderId.trim());

        EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
        // re-fetch document to get updated folder information
        ArchiveFileDocumentsBean updatedDoc = svc.getDocument(documentId.trim());
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(updatedDoc));

        return "{\"success\": true, \"documentId\": \"" + ToolJsonUtils.escapeJson(documentId.trim())
                + "\", \"folderId\": \"" + ToolJsonUtils.escapeJson(folderId.trim()) + "\"}";
    }

    private String executeListFolderTemplates(JsonObject args) throws Exception {
        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        List<DocumentFolderTemplate> templates = svc.getAllFolderTemplates();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"folderTemplates\": [");
        for (int i = 0; i < templates.size(); i++) {
            DocumentFolderTemplate t = templates.get(i);
            if (i > 0) sb.append(", ");
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(t.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(t.getName())).append("\"");
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeApplyFolderTemplate(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String templateName = (String) args.get("templateName");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }
        if (templateName == null || templateName.trim().isEmpty()) {
            return ToolJsonUtils.error("Vorlagenname (templateName) fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        CaseFolder newRoot = svc.applyFolderTemplate(caseId.trim(), templateName.trim());

        EventBroker.getInstance().publishEvent(new CasesChangedEvent());

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId.trim())).append("\"");
        sb.append(", \"templateName\": \"").append(ToolJsonUtils.escapeJson(templateName.trim())).append("\"");
        if (newRoot != null) {
            sb.append(", \"rootFolderName\": \"").append(ToolJsonUtils.escapeJson(newRoot.getName())).append("\"");
            sb.append(", \"rootFolderId\": \"").append(ToolJsonUtils.escapeJson(newRoot.getId())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeFindFreeSlots(JsonObject args) throws Exception {
        String fromDateStr = (String) args.get("fromDate");
        String toDateStr = (String) args.get("toDate");
        if (fromDateStr == null || toDateStr == null) {
            return ToolJsonUtils.error("fromDate und toDate sind erforderlich");
        }

        int durationMinutes = 60;
        if (args.get("durationMinutes") != null) {
            durationMinutes = ((Number) args.get("durationMinutes")).intValue();
        }
        if (durationMinutes < 15) {
            durationMinutes = 15;
        }

        String assignee = (String) args.get("assignee");
        if (assignee == null || assignee.trim().isEmpty()) {
            assignee = UserSettings.getInstance().getCurrentUser().getPrincipalId();
        }

        int workStartHour = 8;
        int workEndHour = 18;
        if (args.get("workStartHour") != null) {
            workStartHour = ((Number) args.get("workStartHour")).intValue();
        }
        if (args.get("workEndHour") != null) {
            workEndHour = ((Number) args.get("workEndHour")).intValue();
        }
        if (workStartHour < 0 || workStartHour > 23) {
            return ToolJsonUtils.error("workStartHour muss zwischen 0 und 23 liegen, angegeben: " + workStartHour);
        }
        if (workEndHour < 0 || workEndHour > 23) {
            return ToolJsonUtils.error("workEndHour muss zwischen 0 und 23 liegen, angegeben: " + workEndHour);
        }
        if (workStartHour >= workEndHour) {
            return ToolJsonUtils.error("workStartHour (" + workStartHour + ") muss kleiner als workEndHour (" + workEndHour + ") sein");
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
        Date fromDate = dateFmt.parse(fromDateStr);
        Date toDate = dateFmt.parse(toDateStr);

        long diffDays = (toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24);
        if (diffDays > 14) {
            return ToolJsonUtils.error("Zeitraum darf maximal 14 Tage betragen");
        }

        CalendarServiceRemote calSvc = ToolJsonUtils.getLocator().lookupCalendarServiceRemote();
        Collection<ArchiveFileReviewsBean> allEvents = calSvc.searchReviews(0, -1, fromDate, toDate);

        final String targetAssignee = assignee;
        List<ArchiveFileReviewsBean> blockingEvents = new ArrayList<>();
        for (ArchiveFileReviewsBean ev : allEvents) {
            if (ev.getEventType() == EventTypes.EVENTTYPE_EVENT
                    && targetAssignee.equals(ev.getAssignee())) {
                blockingEvents.add(ev);
            }
        }

        Collections.sort(blockingEvents, (a, b) -> a.getBeginDate().compareTo(b.getBeginDate()));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"assignee\": \"").append(ToolJsonUtils.escapeJson(assignee)).append("\"");
        sb.append(", \"fromDate\": \"").append(fromDateStr).append("\"");
        sb.append(", \"toDate\": \"").append(toDateStr).append("\"");
        sb.append(", \"durationMinutes\": ").append(durationMinutes);
        sb.append(", \"workingHours\": \"").append(String.format("%02d:00-%02d:00", workStartHour, workEndHour)).append("\"");
        sb.append(", \"freeSlots\": [");

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(fromDate);
        int slotCount = 0;
        boolean first = true;

        while (!cal.getTime().after(toDate) && slotCount < 50) {
            int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
            if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                continue;
            }

            java.util.Calendar dayCal = java.util.Calendar.getInstance();
            dayCal.setTime(cal.getTime());
            dayCal.set(java.util.Calendar.HOUR_OF_DAY, workStartHour);
            dayCal.set(java.util.Calendar.MINUTE, 0);
            dayCal.set(java.util.Calendar.SECOND, 0);
            Date workStart = dayCal.getTime();

            dayCal.set(java.util.Calendar.HOUR_OF_DAY, workEndHour);
            Date workEnd = dayCal.getTime();

            List<long[]> busyRanges = new ArrayList<>();
            for (ArchiveFileReviewsBean ev : blockingEvents) {
                if (ev.getEndDate() != null && ev.getBeginDate() != null) {
                    long evStart = Math.max(ev.getBeginDate().getTime(), workStart.getTime());
                    long evEnd = Math.min(ev.getEndDate().getTime(), workEnd.getTime());
                    if (evStart < evEnd) {
                        busyRanges.add(new long[]{evStart, evEnd});
                    }
                }
            }

            Collections.sort(busyRanges, (a, b) -> Long.compare(a[0], b[0]));
            List<long[]> merged = new ArrayList<>();
            for (long[] range : busyRanges) {
                if (!merged.isEmpty() && range[0] <= merged.get(merged.size() - 1)[1]) {
                    merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], range[1]);
                } else {
                    merged.add(new long[]{range[0], range[1]});
                }
            }

            long cursor = workStart.getTime();
            for (long[] busy : merged) {
                if (busy[0] > cursor) {
                    int gapMinutes = (int) ((busy[0] - cursor) / (1000 * 60));
                    if (gapMinutes >= durationMinutes && slotCount < 50) {
                        if (!first) {
                            sb.append(", ");
                        }
                        first = false;
                        sb.append("{\"date\": \"").append(dateFmt.format(new Date(cursor))).append("\"");
                        sb.append(", \"start\": \"").append(timeFmt.format(new Date(cursor))).append("\"");
                        sb.append(", \"end\": \"").append(timeFmt.format(new Date(busy[0]))).append("\"");
                        sb.append(", \"durationMinutes\": ").append(gapMinutes);
                        sb.append("}");
                        slotCount++;
                    }
                }
                cursor = Math.max(cursor, busy[1]);
            }
            if (cursor < workEnd.getTime()) {
                int gapMinutes = (int) ((workEnd.getTime() - cursor) / (1000 * 60));
                if (gapMinutes >= durationMinutes && slotCount < 50) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append("{\"date\": \"").append(dateFmt.format(new Date(cursor))).append("\"");
                    sb.append(", \"start\": \"").append(timeFmt.format(new Date(cursor))).append("\"");
                    sb.append(", \"end\": \"").append(timeFmt.format(workEnd)).append("\"");
                    sb.append(", \"durationMinutes\": ").append(gapMinutes);
                    sb.append("}");
                    slotCount++;
                }
            }

            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        sb.append("], \"totalSlots\": ").append(slotCount).append("}");
        return sb.toString();
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

        int page = 1;
        Object pageObj = args.get("page");
        if (pageObj != null) {
            page = ((Number) pageObj).intValue();
            if (page < 1) {
                page = 1;
            }
        }
        final int PAGE_SIZE = 20;

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

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());

        // Filter deleted documents and sort by creation date descending
        List<ArchiveFileDocumentsBean> filteredDocs = new ArrayList<>();
        for (ArchiveFileDocumentsBean doc : allDocs) {
            if (!doc.isDeleted()) {
                filteredDocs.add(doc);
            }
        }
        filteredDocs.sort((a, b) -> {
            if (a.getCreationDate() == null && b.getCreationDate() == null) return 0;
            if (a.getCreationDate() == null) return 1;
            if (b.getCreationDate() == null) return -1;
            return b.getCreationDate().compareTo(a.getCreationDate());
        });

        int totalDocuments = filteredDocs.size();
        int totalPages = (int) Math.ceil((double) totalDocuments / PAGE_SIZE);
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalDocuments);
        List<ArchiveFileDocumentsBean> pageDocs = filteredDocs.subList(fromIndex, toIndex);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : pageDocs) {
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
        sb.append("], \"totalDocuments\": ").append(totalDocuments);
        sb.append(", \"page\": ").append(page);
        sb.append(", \"totalPages\": ").append(totalPages);
        sb.append(", \"hasMore\": ").append(page < totalPages);
        sb.append("}");
        return sb.toString();
    }

    private String executeListCaseDocumentsByDate(JsonObject args) throws Exception {
        String fileNumber = (String) args.get("fileNumber");
        if (fileNumber == null || fileNumber.trim().isEmpty()) {
            return ToolJsonUtils.error("Aktenzeichen fehlt");
        }

        String fromDateStr = (String) args.get("fromDate");
        String toDateStr = (String) args.get("toDate");
        if (fromDateStr == null || fromDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Startdatum (fromDate) fehlt");
        }
        if (toDateStr == null || toDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Enddatum (toDate) fehlt");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        Date fromDate;
        Date toDate;
        try {
            fromDate = sdf.parse(fromDateStr.trim());
        } catch (Exception ex) {
            return ToolJsonUtils.error("Ungültiges Startdatum (erwartet yyyy-MM-dd): " + fromDateStr);
        }
        try {
            toDate = sdf.parse(toDateStr.trim());
        } catch (Exception ex) {
            return ToolJsonUtils.error("Ungültiges Enddatum (erwartet yyyy-MM-dd): " + toDateStr);
        }
        // Set toDate to end of day (23:59:59.999)
        toDate = new Date(toDate.getTime() + 24L * 60 * 60 * 1000 - 1);

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

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());

        // Filter: not deleted, creationDate within range
        List<ArchiveFileDocumentsBean> filteredDocs = new ArrayList<>();
        for (ArchiveFileDocumentsBean doc : allDocs) {
            if (doc.isDeleted()) {
                continue;
            }
            Date created = doc.getCreationDate();
            if (created == null) {
                continue;
            }
            if (!created.before(fromDate) && !created.after(toDate)) {
                filteredDocs.add(doc);
            }
        }

        // Sort by creation date descending
        filteredDocs.sort((a, b) -> {
            if (a.getCreationDate() == null && b.getCreationDate() == null) return 0;
            if (a.getCreationDate() == null) return 1;
            if (b.getCreationDate() == null) return -1;
            return b.getCreationDate().compareTo(a.getCreationDate());
        });

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"fromDate\": \"").append(ToolJsonUtils.escapeJson(fromDateStr.trim())).append("\"");
        sb.append(", \"toDate\": \"").append(ToolJsonUtils.escapeJson(toDateStr.trim())).append("\"");
        sb.append(", \"totalDocuments\": ").append(filteredDocs.size());
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : filteredDocs) {
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
        sb.append("]}");
        return sb.toString();
    }

    private String executeSearchCaseDocuments(JsonObject args) throws Exception {
        String fileNumber = (String) args.get("fileNumber");
        if (fileNumber == null || fileNumber.trim().isEmpty()) {
            return ToolJsonUtils.error("Aktenzeichen fehlt");
        }

        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }
        String queryLower = query.toLowerCase();

        int page = 1;
        Object pageObj = args.get("page");
        if (pageObj != null) {
            page = ((Number) pageObj).intValue();
            if (page < 1) {
                page = 1;
            }
        }
        final int PAGE_SIZE = 20;

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

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());

        // Filter deleted documents, match by filename, sort by creation date descending
        List<ArchiveFileDocumentsBean> filteredDocs = new ArrayList<>();
        for (ArchiveFileDocumentsBean doc : allDocs) {
            if (!doc.isDeleted() && doc.getName() != null && doc.getName().toLowerCase().contains(queryLower)) {
                filteredDocs.add(doc);
            }
        }
        filteredDocs.sort((a, b) -> {
            if (a.getCreationDate() == null && b.getCreationDate() == null) return 0;
            if (a.getCreationDate() == null) return 1;
            if (b.getCreationDate() == null) return -1;
            return b.getCreationDate().compareTo(a.getCreationDate());
        });

        int totalDocuments = filteredDocs.size();
        int totalPages = (int) Math.ceil((double) totalDocuments / PAGE_SIZE);
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalDocuments);
        List<ArchiveFileDocumentsBean> pageDocs = filteredDocs.subList(fromIndex, toIndex);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"query\": \"").append(ToolJsonUtils.escapeJson(query)).append("\"");
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : pageDocs) {
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
        sb.append("], \"totalDocuments\": ").append(totalDocuments);
        sb.append(", \"page\": ").append(page);
        sb.append(", \"totalPages\": ").append(totalPages);
        sb.append(", \"hasMore\": ").append(page < totalPages);
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

    private String executeListEventTypes(JsonObject args) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"eventTypes\": [");
        sb.append("{\"name\": \"Wiedervorlage\", \"value\": ").append(EventTypes.EVENTTYPE_FOLLOWUP).append("}");
        sb.append(", {\"name\": \"Frist\", \"value\": ").append(EventTypes.EVENTTYPE_RESPITE).append("}");
        sb.append(", {\"name\": \"Termin\", \"value\": ").append(EventTypes.EVENTTYPE_EVENT).append("}");
        sb.append("]}");
        return sb.toString();
    }

    private int parseEventType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return -1;
        }
        switch (typeStr.trim().toLowerCase()) {
            case "wiedervorlage": return EventTypes.EVENTTYPE_FOLLOWUP;
            case "frist": return EventTypes.EVENTTYPE_RESPITE;
            case "termin": return EventTypes.EVENTTYPE_EVENT;
            default: return -2;
        }
    }

    private String executeGetAllOpenEvents(JsonObject args) throws Exception {
        String eventTypeStr = (String) args.get("eventType");
        int eventTypeFilter = parseEventType(eventTypeStr);
        if (eventTypeFilter == -2) {
            return ToolJsonUtils.error("Unbekannter Ereignistyp: " + eventTypeStr + ". Erlaubt: Wiedervorlage, Frist, Termin");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();
        Collection<ArchiveFileReviewsBean> events = calSvc.getAllOpenReviews();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"events\": [");
        int count = 0;
        int total = 0;
        for (ArchiveFileReviewsBean ev : events) {
            if (eventTypeFilter != -1 && ev.getEventType() != eventTypeFilter) {
                continue;
            }
            total++;
            if (count < 50) {
                if (count > 0) sb.append(",");
                appendEventJson(sb, ev);
                count++;
            }
        }
        sb.append("], \"totalEvents\": ").append(total);
        if (total > 50) {
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

        String eventTypeStr = (String) args.get("eventType");
        int eventTypeFilter = parseEventType(eventTypeStr);
        if (eventTypeFilter == -2) {
            return ToolJsonUtils.error("Unbekannter Ereignistyp: " + eventTypeStr + ". Erlaubt: Wiedervorlage, Frist, Termin");
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
        // status 0 = open, eventTypeFilter -1 = all types
        Collection<ArchiveFileReviewsBean> events = calSvc.searchReviews(0, eventTypeFilter, fromDate, toDate);

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
        int page = 1;
        Object pageObj = args.get("page");
        if (pageObj != null) {
            page = ((Number) pageObj).intValue();
            if (page < 1) {
                page = 1;
            }
        }
        final int PAGE_SIZE = 20;

        List<Invoice> invoices = getOpenInvoices();

        int totalInvoices = invoices.size();
        int totalPages = (int) Math.ceil((double) totalInvoices / PAGE_SIZE);
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalInvoices);
        List<Invoice> pageInvoices = invoices.subList(fromIndex, toIndex);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"invoices\": [");
        for (int i = 0; i < pageInvoices.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendInvoiceJson(sb, pageInvoices.get(i), sdf);
        }
        sb.append("], \"totalInvoices\": ").append(totalInvoices);
        sb.append(", \"page\": ").append(page);
        sb.append(", \"totalPages\": ").append(totalPages);
        sb.append(", \"hasMore\": ").append(page < totalPages);
        sb.append("}");
        return sb.toString();
    }

    private void appendInvoiceJson(StringBuilder sb, Invoice inv, SimpleDateFormat sdf) {
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

    private List<Invoice> getOpenInvoices() throws Exception {
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        InvoiceServiceRemote invSvc = locator.lookupInvoiceServiceRemote();
        return invSvc.getInvoicesByStatus(
                Invoice.STATUS_OPEN, Invoice.STATUS_OPEN_REMINDER1, Invoice.STATUS_OPEN_REMINDER2,
                Invoice.STATUS_OPEN_REMINDER3, Invoice.STATUS_OPEN_NONENFORCEABLE);
    }

    private String executeSearchInvoices(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }
        String queryLower = query.trim().toLowerCase();

        List<Invoice> invoices = getOpenInvoices();

        List<Invoice> filtered = new ArrayList<>();
        for (Invoice inv : invoices) {
            if (filtered.size() >= 50) {
                break;
            }
            // search in invoice number
            if (inv.getInvoiceNumber() != null && inv.getInvoiceNumber().toLowerCase().contains(queryLower)) {
                filtered.add(inv);
                continue;
            }
            // search in contact fields
            if (inv.getContact() != null) {
                AddressBean contact = inv.getContact();
                if (contact.getName() != null && contact.getName().toLowerCase().contains(queryLower)) {
                    filtered.add(inv);
                    continue;
                }
                if (contact.getFirstName() != null && contact.getFirstName().toLowerCase().contains(queryLower)) {
                    filtered.add(inv);
                    continue;
                }
                if (contact.getCompany() != null && contact.getCompany().toLowerCase().contains(queryLower)) {
                    filtered.add(inv);
                    continue;
                }
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\": \"").append(ToolJsonUtils.escapeJson(query)).append("\"");
        sb.append(", \"invoices\": [");
        for (int i = 0; i < filtered.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendInvoiceJson(sb, filtered.get(i), sdf);
        }
        sb.append("], \"totalInvoices\": ").append(filtered.size());
        sb.append("}");
        return sb.toString();
    }

    private String executeSearchInvoicesByDate(JsonObject args) throws Exception {
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

        int page = 1;
        Object pageObj = args.get("page");
        if (pageObj != null) {
            page = ((Number) pageObj).intValue();
            if (page < 1) {
                page = 1;
            }
        }
        final int PAGE_SIZE = 20;

        List<Invoice> invoices = getOpenInvoices();

        List<Invoice> filtered = new ArrayList<>();
        for (Invoice inv : invoices) {
            if (inv.getCreationDate() != null && !inv.getCreationDate().before(fromDate) && !inv.getCreationDate().after(toDate)) {
                filtered.add(inv);
            }
        }

        int totalInvoices = filtered.size();
        int totalPages = (int) Math.ceil((double) totalInvoices / PAGE_SIZE);
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalInvoices);
        List<Invoice> pageInvoices = filtered.subList(fromIndex, toIndex);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"fromDate\": \"").append(ToolJsonUtils.escapeJson(fromDateStr)).append("\"");
        sb.append(", \"toDate\": \"").append(ToolJsonUtils.escapeJson(toDateStr)).append("\"");
        sb.append(", \"invoices\": [");
        for (int i = 0; i < pageInvoices.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendInvoiceJson(sb, pageInvoices.get(i), sdf);
        }
        sb.append("], \"totalInvoices\": ").append(totalInvoices);
        sb.append(", \"page\": ").append(page);
        sb.append(", \"totalPages\": ").append(totalPages);
        sb.append(", \"hasMore\": ").append(page < totalPages);
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
        EventBroker.getInstance().publishEvent(new ReviewAddedEvent(created));

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
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(doc));

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
        String streetNumber = (String) args.get("streetNumber");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String gender = (String) args.get("gender");
        String salutation = (String) args.get("salutation");
        String complimentaryClose = (String) args.get("complimentaryClose");

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
        if (streetNumber != null && !streetNumber.trim().isEmpty()) {
            candidate.setStreetNumber(streetNumber.trim());
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
        if (gender != null && !gender.trim().isEmpty()) {
            candidate.setGender(gender.trim().toUpperCase());
        }
        if (salutation != null && !salutation.trim().isEmpty()) {
            candidate.setSalutation(salutation.trim());
        }
        if (complimentaryClose != null && !complimentaryClose.trim().isEmpty()) {
            candidate.setComplimentaryClose(complimentaryClose.trim());
        }

        AddressBean created = addrSvc.createAddress(candidate);
        EventBroker.getInstance().publishEvent(new ContactUpdatedEvent(created));

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
        EventBroker.getInstance().publishEvent(new CasesChangedEvent());

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
        EventBroker.getInstance().publishEvent(new InvoicePositionAddedEvent(invoiceId, created));

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
        EventBroker.getInstance().publishEvent(new NewInstantMessagesEvent(Collections.singletonList(created)));

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
        String groupName = (String) args.get("group");

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

        // Validate group if provided
        Group matchedGroup = null;
        if (groupName != null && !groupName.trim().isEmpty()) {
            List<Group> myGroups = getCachedMyGroups();
            for (Group g : myGroups) {
                if (g.getName().equalsIgnoreCase(groupName.trim())) {
                    matchedGroup = g;
                    break;
                }
            }
            if (matchedGroup == null) {
                StringBuilder groupNames = new StringBuilder();
                for (Group g : myGroups) {
                    if (groupNames.length() > 0) {
                        groupNames.append(", ");
                    }
                    groupNames.append(g.getName());
                }
                return ToolJsonUtils.error("Gruppe nicht gefunden: " + groupName + ". Verfügbare Gruppen: " + groupNames.toString());
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
        if (matchedGroup != null) {
            dto.setGroup(matchedGroup);
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean created = svc.createArchiveFile(dto);
        EventBroker.getInstance().publishEvent(new CasesChangedEvent());

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
        if (created.getGroup() != null) {
            sb.append(", \"group\": \"").append(ToolJsonUtils.escapeJson(created.getGroup().getName())).append("\"");
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
        String streetNumber = (String) args.get("streetNumber");
        String zipCode = (String) args.get("zipCode");
        String city = (String) args.get("city");
        String country = (String) args.get("country");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String mobile = (String) args.get("mobile");
        String fax = (String) args.get("fax");
        String website = (String) args.get("website");
        String gender = (String) args.get("gender");
        String complimentaryClose = (String) args.get("complimentaryClose");

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
        if (streetNumber != null && !streetNumber.trim().isEmpty()) {
            candidate.setStreetNumber(streetNumber.trim());
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
        if (gender != null && !gender.trim().isEmpty()) {
            candidate.setGender(gender.trim().toUpperCase());
        }
        if (complimentaryClose != null && !complimentaryClose.trim().isEmpty()) {
            candidate.setComplimentaryClose(complimentaryClose.trim());
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        AddressServiceRemote addrSvc = locator.lookupAddressServiceRemote();
        AddressBean created = addrSvc.createAddress(candidate);
        EventBroker.getInstance().publishEvent(new ContactUpdatedEvent(created));

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
        EventBroker.getInstance().publishEvent(new CasesChangedEvent());

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
        String streetNumber = (String) args.get("streetNumber");
        String zipCode = (String) args.get("zipCode");
        String city = (String) args.get("city");
        String country = (String) args.get("country");
        String email = (String) args.get("email");
        String phone = (String) args.get("phone");
        String mobile = (String) args.get("mobile");
        String fax = (String) args.get("fax");
        String website = (String) args.get("website");
        String gender = (String) args.get("gender");
        String complimentaryClose = (String) args.get("complimentaryClose");

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
        if (streetNumber != null) {
            contact.setStreetNumber(streetNumber.trim());
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
        if (gender != null) {
            contact.setGender(gender.trim().toUpperCase());
        }
        if (complimentaryClose != null) {
            contact.setComplimentaryClose(complimentaryClose.trim());
        }

        addrSvc.updateAddress(contact);

        // Re-read to get server-side state
        AddressBean updated = addrSvc.getAddress(contactId);
        EventBroker.getInstance().publishEvent(new ContactUpdatedEvent(updated));
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"contact\": ");
        StringBuilder contactSb = new StringBuilder();
        appendContactJson(contactSb, updated);
        sb.append(contactSb);
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // Template tool implementations
    // =========================================================================

    // potentially large list, could overwhelm context window - use search of templates instead
//    private String executeListTemplates(JsonObject args) throws Exception {
//        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
//        GenericNode root = sys.getAllTemplatesTree(SystemManagementRemote.TEMPLATE_TYPE_BODY);
//        String rootId = root.getId();
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\"folders\": [");
//        boolean[] first = {true};
//        collectTemplatesFromTree(root, rootId, sys, sb, first);
//        sb.append("]}");
//        return sb.toString();
//    }

    private String executeSearchTemplates(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff (query) fehlt");
        }
        String queryLower = query.trim().toLowerCase();

        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        GenericNode root = sys.getAllTemplatesTree(SystemManagementRemote.TEMPLATE_TYPE_BODY);
        String rootId = root.getId();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\": \"").append(ToolJsonUtils.escapeJson(query.trim())).append("\"");
        sb.append(", \"results\": [");
        boolean[] first = {true};
        collectMatchingTemplatesFromTree(root, rootId, sys, sb, first, queryLower);
        sb.append("]}");
        return sb.toString();
    }

    private void collectMatchingTemplatesFromTree(GenericNode node, String rootId,
            SystemManagementRemote sys, StringBuilder sb, boolean[] first, String queryLower) throws Exception {
        List<String> templates = sys.getTemplatesInFolder(
                SystemManagementRemote.TEMPLATE_TYPE_BODY, node);
        if (templates != null) {
            String displayPath = node.getId().replace(rootId, "");
            if (displayPath.isEmpty()) {
                displayPath = "/";
            }
            for (String tpl : templates) {
                if (tpl.toLowerCase().contains(queryLower)) {
                    if (!first[0]) sb.append(", ");
                    first[0] = false;
                    sb.append("{\"folderPath\": \"").append(ToolJsonUtils.escapeJson(displayPath)).append("\"");
                    sb.append(", \"templateName\": \"").append(ToolJsonUtils.escapeJson(tpl)).append("\"}");
                }
            }
        }
        if (node.getChildren() != null) {
            for (GenericNode child : node.getChildren()) {
                collectMatchingTemplatesFromTree(child, rootId, sys, sb, first, queryLower);
            }
        }
    }

    private void collectTemplatesFromTree(GenericNode node, String rootId,
            SystemManagementRemote sys, StringBuilder sb, boolean[] first) throws Exception {
        List<String> templates = sys.getTemplatesInFolder(
                SystemManagementRemote.TEMPLATE_TYPE_BODY, node);
        if (templates != null && !templates.isEmpty()) {
            if (!first[0]) sb.append(", ");
            first[0] = false;
            String displayPath = node.getId().replace(rootId, "");
            if (displayPath.isEmpty()) {
                displayPath = "/";
            }
            sb.append("{\"folderPath\": \"").append(ToolJsonUtils.escapeJson(displayPath)).append("\"");
            sb.append(", \"templates\": [");
            for (int i = 0; i < templates.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(ToolJsonUtils.escapeJson(templates.get(i))).append("\"");
            }
            sb.append("]}");
        }
        if (node.getChildren() != null) {
            for (GenericNode child : node.getChildren()) {
                collectTemplatesFromTree(child, rootId, sys, sb, first);
            }
        }
    }

    private String executeCreateDocumentFromTemplate(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String templateFolder = (String) args.get("templateFolder");
        String templateName = (String) args.get("templateName");
        String fileName = (String) args.get("fileName");
        String generatedText = (String) args.get("generatedText");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }
        if (templateFolder == null || templateFolder.trim().isEmpty()) {
            templateFolder = "/";
        }
        if (!templateFolder.startsWith("/")) {
            templateFolder = "/" + templateFolder;
        }
        if (templateName == null || templateName.trim().isEmpty()) {
            return ToolJsonUtils.error("Vorlagenname (templateName) fehlt");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            return ToolJsonUtils.error("Dateiname (fileName) fehlt");
        }

        // Validate template exists by checking the template tree
        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        GenericNode root = sys.getAllTemplatesTree(SystemManagementRemote.TEMPLATE_TYPE_BODY);
        String rootId = root.getId();
        GenericNode matchedFolder = findTemplateFolder(root, rootId, templateFolder);
        if (matchedFolder == null) {
            return ToolJsonUtils.error("Vorlagenordner nicht gefunden: " + templateFolder + ". Verwende search_templates um verfügbare Ordner zu sehen.");
        }
        List<String> templatesInFolder = sys.getTemplatesInFolder(SystemManagementRemote.TEMPLATE_TYPE_BODY, matchedFolder);
        boolean templateFound = false;
        if (templatesInFolder != null) {
            for (String t : templatesInFolder) {
                if (t.equals(templateName.trim())) {
                    templateFound = true;
                    break;
                }
            }
        }
        if (!templateFound) {
            StringBuilder available = new StringBuilder();
            if (templatesInFolder != null) {
                for (int i = 0; i < templatesInFolder.size(); i++) {
                    if (i > 0) available.append(", ");
                    available.append(templatesInFolder.get(i));
                }
            }
            return ToolJsonUtils.error("Vorlage '" + templateName.trim() + "' nicht gefunden in Ordner " + templateFolder
                    + ". Verfügbare Vorlagen: " + available.toString());
        }

        // Use the matched folder node (has correct server ID) for all subsequent calls
        GenericNode folderNode = matchedFolder;

        ArchiveFileServiceRemote archiveSvc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        FormsServiceRemote formsSvc = ToolJsonUtils.getLocator().lookupFormsServiceRemote();

        // Validate case exists
        ArchiveFileBean caseBean = archiveSvc.getArchiveFile(caseId.trim());
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        // Get form placeholders for case
        Collection<String> formPlaceHolders = formsSvc.getPlaceHoldersForCase(caseId.trim());
        HashMap<String, String> formPlaceHolderValues = formsSvc.getPlaceHolderValuesForCase(caseId.trim());

        // Get placeholders in template, filter out system placeholders
        List<String> phInTemplate = sys.getPlaceHoldersForTemplate(
                SystemManagementRemote.TEMPLATE_TYPE_BODY, folderNode, templateName.trim(), formPlaceHolders);
        HashMap<String, Object> phMap = new HashMap<>();
        if (phInTemplate != null) {
            for (String ph : phInTemplate) {
                if (!ph.startsWith("[[")) {
                    phMap.put(ph, "");
                }
            }
        }

        // Gather parties
        List<ArchiveFileAddressesBean> involvements = archiveSvc.getInvolvementDetailsForCase(caseId.trim());
        List<PartiesTriplet> parties = new ArrayList<>();
        if (involvements != null) {
            for (ArchiveFileAddressesBean aab : involvements) {
                parties.add(new PartiesTriplet(aab.getAddressKey(), aab.getReferenceType(), aab));
            }
        }

        // Resolve lawyer and assistant
        AppUserBean userLawyer = null;
        if (caseBean.getLawyer() != null && !caseBean.getLawyer().isEmpty()) {
            try {
                userLawyer = sys.getUser(caseBean.getLawyer());
            } catch (Exception e) {
                log.warn("Could not resolve lawyer: " + caseBean.getLawyer(), e);
            }
        }
        AppUserBean userAssistant = null;
        if (caseBean.getAssistant() != null && !caseBean.getAssistant().isEmpty()) {
            try {
                userAssistant = sys.getUser(caseBean.getAssistant());
            } catch (Exception e) {
                log.warn("Could not resolve assistant: " + caseBean.getAssistant(), e);
            }
        }

        // Resolve system placeholders
        String ingoText = (generatedText != null && !generatedText.trim().isEmpty()) ? generatedText.trim() : null;
        phMap = sys.getPlaceHolderValues(phMap, caseBean, parties, "", null,
                formPlaceHolderValues, userLawyer, userAssistant, null, null, null, null, null, null, null, ingoText);

        // Create document from template
        ArchiveFileDocumentsBean newDoc = archiveSvc.addDocumentFromTemplate(
                caseId.trim(), fileName.trim(), null, folderNode, templateName.trim(), phMap, "", null);

        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(newDoc));

        // Build response
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(newDoc.getId())).append("\"");
        sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(newDoc.getName())).append("\"");
        sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId.trim())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private GenericNode findTemplateFolder(GenericNode node, String rootId, String targetPath) {
        String displayPath = node.getId().replace(rootId, "");
        if (displayPath.isEmpty()) {
            displayPath = "/";
        } else if (!displayPath.startsWith("/")) {
            displayPath = "/" + displayPath;
        }
        if (displayPath.equals(targetPath)) {
            return node;
        }
        if (node.getChildren() != null) {
            for (GenericNode child : node.getChildren()) {
                GenericNode found = findTemplateFolder(child, rootId, targetPath);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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



    private String executeListDocumentTags(JsonObject args) throws Exception {
        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();

        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_DOCUMENTTAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);

        return buildTagListJson(boolTags, mvGroups, OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);
    }

    private String executeListCaseTags(JsonObject args) throws Exception {
        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();

        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX);

        return buildTagListJson(boolTags, mvGroups, OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX);
    }

    private String buildTagListJson(AppOptionGroupBean[] boolTags,
                                    HashMap<String, AppOptionGroupBean[]> mvGroups,
                                    String mvPrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"booleanTags\": [");
        if (boolTags != null) {
            for (int i = 0; i < boolTags.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(ToolJsonUtils.escapeJson(boolTags[i].getValue())).append("\"");
            }
        }
        sb.append("], \"multiValueTags\": [");
        if (mvGroups != null) {
            boolean first = true;
            for (Map.Entry<String, AppOptionGroupBean[]> entry : mvGroups.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                String tagName = entry.getKey().substring(mvPrefix.length());
                sb.append("{\"name\": \"").append(ToolJsonUtils.escapeJson(tagName)).append("\"");
                sb.append(", \"values\": [");
                AppOptionGroupBean[] vals = entry.getValue();
                if (vals != null) {
                    for (int i = 0; i < vals.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("\"").append(ToolJsonUtils.escapeJson(vals[i].getValue())).append("\"");
                    }
                }
                sb.append("]}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String validateTag(String tagName, String tagValue,
                               AppOptionGroupBean[] boolTags,
                               HashMap<String, AppOptionGroupBean[]> mvGroups,
                               String mvPrefix) {
        // Check if it's a boolean tag
        if (boolTags != null) {
            for (AppOptionGroupBean b : boolTags) {
                if (tagName.equals(b.getValue())) {
                    if (tagValue != null && !tagValue.trim().isEmpty()) {
                        return "{\"error\": \"Etikett '" + ToolJsonUtils.escapeJson(tagName)
                                + "' ist ein einfaches Etikett und akzeptiert keinen Wert (tagValue). tagValue weglassen oder null setzen.\"}";
                    }
                    return null;
                }
            }
        }

        // Check if it's a multivalue tag
        if (mvGroups != null) {
            String mvKey = mvPrefix + tagName;
            AppOptionGroupBean[] allowedVals = mvGroups.get(mvKey);
            if (allowedVals != null) {
                if (tagValue == null || tagValue.trim().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"error\": \"Etikett '").append(ToolJsonUtils.escapeJson(tagName));
                    sb.append("' ist ein Mehrwert-Etikett und benötigt einen tagValue.\", \"allowedValues\": [");
                    for (int i = 0; i < allowedVals.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("\"").append(ToolJsonUtils.escapeJson(allowedVals[i].getValue())).append("\"");
                    }
                    sb.append("]}");
                    return sb.toString();
                }
                for (AppOptionGroupBean v : allowedVals) {
                    if (tagValue.trim().equals(v.getValue())) {
                        return null;
                    }
                }
                // tagValue not in allowed values
                StringBuilder sb = new StringBuilder();
                sb.append("{\"error\": \"Wert '").append(ToolJsonUtils.escapeJson(tagValue.trim()));
                sb.append("' ist nicht erlaubt für Etikett '").append(ToolJsonUtils.escapeJson(tagName));
                sb.append("'.\", \"allowedValues\": [");
                for (int i = 0; i < allowedVals.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(ToolJsonUtils.escapeJson(allowedVals[i].getValue())).append("\"");
                }
                sb.append("]}");
                return sb.toString();
            }
        }

        // Tag name not found at all — return available tags
        String availableTags = buildTagListJson(boolTags, mvGroups, mvPrefix);
        return "{\"error\": \"Etikett '" + ToolJsonUtils.escapeJson(tagName)
                + "' existiert nicht.\", \"availableTags\": " + availableTags + "}";
    }

    private String executeSetDocumentTag(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        String tagName = (String) args.get("tagName");
        String tagValue = (String) args.get("tagValue");
        String activeStr = (String) args.get("active");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID (documentId) fehlt");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            return ToolJsonUtils.error("Etikett-Name (tagName) fehlt");
        }
        boolean active = (activeStr == null || !"false".equalsIgnoreCase(activeStr.trim()));

        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_DOCUMENTTAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);

        String validationError = validateTag(tagName.trim(), tagValue, boolTags, mvGroups, OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);
        if (validationError != null) {
            return validationError;
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();

        ArchiveFileDocumentsBean doc = svc.getDocument(documentId.trim());
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        DocumentTagsBean tag = new DocumentTagsBean();
        tag.setTagName(tagName.trim());
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            tag.setTagValue(tagValue.trim());
        }

        svc.setDocumentTag(documentId.trim(), tag, active);

        EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(doc));

        StringBuilder sbResult = new StringBuilder();
        sbResult.append("{\"success\": true, \"documentId\": \"").append(ToolJsonUtils.escapeJson(documentId.trim()));
        sbResult.append("\", \"tagName\": \"").append(ToolJsonUtils.escapeJson(tagName.trim()));
        sbResult.append("\", \"active\": ").append(active);
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            sbResult.append(", \"tagValue\": \"").append(ToolJsonUtils.escapeJson(tagValue.trim())).append("\"");
        }
        sbResult.append("}");
        return sbResult.toString();
    }

    private String executeSetCaseTag(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String tagName = (String) args.get("tagName");
        String tagValue = (String) args.get("tagValue");
        String activeStr = (String) args.get("active");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            return ToolJsonUtils.error("Etikett-Name (tagName) fehlt");
        }
        boolean active = (activeStr == null || !"false".equalsIgnoreCase(activeStr.trim()));

        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX);

        String validationError = validateTag(tagName.trim(), tagValue, boolTags, mvGroups, OptionConstants.OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX);
        if (validationError != null) {
            return validationError;
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();

        ArchiveFileBean caseBean = svc.getArchiveFile(caseId.trim());
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        ArchiveFileTagsBean tag = new ArchiveFileTagsBean();
        tag.setTagName(tagName.trim());
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            tag.setTagValue(tagValue.trim());
        }

        svc.setTag(caseId.trim(), tag, active);

        EventBroker.getInstance().publishEvent(new CasesChangedEvent());

        StringBuilder sbResult = new StringBuilder();
        sbResult.append("{\"success\": true, \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId.trim()));
        sbResult.append("\", \"tagName\": \"").append(ToolJsonUtils.escapeJson(tagName.trim()));
        sbResult.append("\", \"active\": ").append(active);
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            sbResult.append(", \"tagValue\": \"").append(ToolJsonUtils.escapeJson(tagValue.trim())).append("\"");
        }
        sbResult.append("}");
        return sbResult.toString();
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

    private String executeGetAllOpenTimesheets(JsonObject args) throws Exception {
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        List<Timesheet> timesheets = svc.getOpenTimesheets();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"timesheets\": [");
        int limit = Math.min(timesheets.size(), 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendTimesheetJson(sb, timesheets.get(i));
        }
        sb.append("], \"totalTimesheets\": ").append(timesheets.size());
        if (timesheets.size() > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetOpenTimesheetsForCase(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        List<Timesheet> timesheets = svc.getOpenTimesheets(caseId);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"timesheets\": [");
        for (int i = 0; i < timesheets.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendTimesheetJson(sb, timesheets.get(i));
        }
        sb.append("], \"totalTimesheets\": ").append(timesheets.size()).append("}");
        return sb.toString();
    }

    private String executeGetTimesheetPositions(JsonObject args) throws Exception {
        String timesheetId = (String) args.get("timesheetId");
        if (timesheetId == null || timesheetId.trim().isEmpty()) {
            return ToolJsonUtils.error("Timesheet-ID fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        Timesheet ts = svc.getTimesheet(timesheetId);
        if (ts == null) {
            return ToolJsonUtils.error("Timesheet nicht gefunden: " + timesheetId);
        }

        List<TimesheetPosition> positions = svc.getTimesheetPositions(timesheetId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"timesheetId\": \"").append(ToolJsonUtils.escapeJson(timesheetId)).append("\"");
        sb.append(", \"timesheetName\": \"").append(ToolJsonUtils.escapeJson(ts.getName())).append("\"");
        sb.append(", \"positions\": [");
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            TimesheetPosition pos = positions.get(i);
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(pos.getId())).append("\"");
            if (pos.getName() != null) {
                sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(pos.getName())).append("\"");
            }
            if (pos.getDescription() != null) {
                sb.append(", \"description\": \"").append(ToolJsonUtils.escapeJson(pos.getDescription())).append("\"");
            }
            if (pos.getStarted() != null) {
                sb.append(", \"started\": \"").append(sdf.format(pos.getStarted())).append("\"");
            }
            if (pos.getStopped() != null) {
                sb.append(", \"stopped\": \"").append(sdf.format(pos.getStopped())).append("\"");
            }
            sb.append(", \"running\": ").append(pos.isRunning());
            if (pos.getUnitPrice() != null) {
                sb.append(", \"unitPrice\": ").append(pos.getUnitPrice());
            }
            if (pos.getTaxRate() != null) {
                sb.append(", \"taxRate\": ").append(pos.getTaxRate());
            }
            if (pos.getTotal() != null) {
                sb.append(", \"total\": ").append(pos.getTotal());
            }
            if (pos.getPrincipal() != null) {
                sb.append(", \"principal\": \"").append(ToolJsonUtils.escapeJson(pos.getPrincipal())).append("\"");
            }
            sb.append("}");
        }
        sb.append("], \"totalPositions\": ").append(positions.size()).append("}");
        return sb.toString();
    }

    private String executeCreateTimesheetPosition(JsonObject args) throws Exception {
        String timesheetId = (String) args.get("timesheetId");
        String name = (String) args.get("name");
        String startDateStr = (String) args.get("startDate");
        String stopDateStr = (String) args.get("stopDate");
        String unitPriceStr = (String) args.get("unitPrice");
        String taxRateStr = (String) args.get("taxRate");
        String description = (String) args.get("description");
        String principal = (String) args.get("principal");

        if (timesheetId == null || timesheetId.trim().isEmpty()) {
            return ToolJsonUtils.error("Timesheet-ID fehlt");
        }
        if (name == null || name.trim().isEmpty()) {
            return ToolJsonUtils.error("Bezeichnung fehlt");
        }
        if (startDateStr == null || startDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Startdatum fehlt");
        }
        if (stopDateStr == null || stopDateStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Enddatum fehlt");
        }
        if (unitPriceStr == null || unitPriceStr.trim().isEmpty()) {
            return ToolJsonUtils.error("Stundensatz fehlt");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date startDate;
        Date stopDate;
        try {
            startDate = sdf.parse(startDateStr.trim());
        } catch (Exception ex) {
            return ToolJsonUtils.error("Startdatum ist kein gültiges Datum: " + startDateStr);
        }
        try {
            stopDate = sdf.parse(stopDateStr.trim());
        } catch (Exception ex) {
            return ToolJsonUtils.error("Enddatum ist kein gültiges Datum: " + stopDateStr);
        }

        BigDecimal unitPrice;
        BigDecimal taxRate;
        try {
            unitPrice = new BigDecimal(unitPriceStr.trim());
        } catch (NumberFormatException ex) {
            return ToolJsonUtils.error("Stundensatz ist keine gültige Zahl: " + unitPriceStr);
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

        if (principal != null && !principal.trim().isEmpty()) {
            principal = principal.trim();
        } else {
            principal = UserSettings.getInstance().getCurrentUser().getPrincipalId();
        }

        TimesheetPosition pos = new TimesheetPosition();
        pos.setName(name.trim());
        pos.setStarted(startDate);
        pos.setStopped(stopDate);
        pos.setUnitPrice(unitPrice);
        pos.setTaxRate(taxRate);
        pos.setPrincipal(principal);
        if (description != null && !description.trim().isEmpty()) {
            pos.setDescription(description.trim());
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        TimesheetPosition created = svc.timesheetPositionAdd(timesheetId, pos);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(created.getName())).append("\"");
        if (created.getStarted() != null) {
            sb.append(", \"started\": \"").append(sdf.format(created.getStarted())).append("\"");
        }
        if (created.getStopped() != null) {
            sb.append(", \"stopped\": \"").append(sdf.format(created.getStopped())).append("\"");
        }
        if (created.getUnitPrice() != null) {
            sb.append(", \"unitPrice\": ").append(created.getUnitPrice());
        }
        if (created.getTotal() != null) {
            sb.append(", \"total\": ").append(created.getTotal());
        }
        sb.append(", \"taxRate\": ").append(created.getTaxRate());
        sb.append("}");
        return sb.toString();
    }

    private void appendTimesheetJson(StringBuilder sb, Timesheet ts) {
        sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(ts.getId())).append("\"");
        if (ts.getName() != null) {
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(ts.getName())).append("\"");
        }
        if (ts.getDescription() != null) {
            sb.append(", \"description\": \"").append(ToolJsonUtils.escapeJson(ts.getDescription())).append("\"");
        }
        sb.append(", \"status\": \"").append(ToolJsonUtils.escapeJson(ts.getStatusString())).append("\"");
        sb.append(", \"intervalMinutes\": ").append(ts.getInterval());
        sb.append(", \"limited\": ").append(ts.isLimited());
        if (ts.getLimit() != null) {
            sb.append(", \"limitNet\": ").append(ts.getLimit());
        }
        sb.append(", \"percentageDone\": ").append(ts.getPercentageDone());
        if (ts.getArchiveFileKey() != null) {
            sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(ts.getArchiveFileKey().getId())).append("\"");
            sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(ts.getArchiveFileKey().getFileNumber())).append("\"");
            if (ts.getArchiveFileKey().getName() != null) {
                sb.append(", \"caseName\": \"").append(ToolJsonUtils.escapeJson(ts.getArchiveFileKey().getName())).append("\"");
            }
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
