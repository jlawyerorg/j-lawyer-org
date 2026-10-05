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
import com.jdimension.jlawyer.persistence.AddressTagsBean;
import com.jdimension.jlawyer.persistence.AppUserBean;
import com.jdimension.jlawyer.persistence.Group;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileFormsBean;
import com.jdimension.jlawyer.persistence.FormTypeBean;
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
import com.jdimension.jlawyer.services.DocumentMetadata;
import com.jdimension.jlawyer.services.DocumentMetadataPatch;
import com.jdimension.jlawyer.client.editors.files.DocumentOrigin;
import com.jdimension.jlawyer.client.mail.MailDocumentOrigins;
import com.jdimension.jlawyer.services.FormsServiceRemote;
import com.jdimension.jlawyer.services.SystemManagementRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CalendarServiceRemote;
import com.jdimension.jlawyer.services.InvoiceServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import com.jdimension.jlawyer.services.MessagingServiceRemote;
import com.jdimension.jlawyer.services.EmailServiceRemote;
import com.jdimension.jlawyer.services.MailAttachmentDTO;
import com.jdimension.jlawyer.services.MailFolderDTO;
import com.jdimension.jlawyer.services.MailMessageDTO;
import com.jdimension.jlawyer.persistence.MailboxSetup;
import com.jdimension.jlawyer.email.CommonMailUtils;
import com.jdimension.jlawyer.persistence.DocumentNameTemplate;
import com.jdimension.jlawyer.client.utils.FileUtils;
import com.jdimension.jlawyer.client.utils.TemplatesUtil;
import org.jlawyer.data.tree.GenericNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.jdimension.jlawyer.client.events.ReviewUpdatedEvent;
import com.jdimension.jlawyer.client.events.InvoicePositionAddedEvent;
import com.jdimension.jlawyer.client.events.NewInstantMessagesEvent;
import com.jdimension.jlawyer.client.events.PartyAddedEvent;
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

    private static final int MAX_MAIL_RESULTS = 50;
    private static final int MAX_SEARCH_FOLDERS = 15;
    private static final int MAX_MAIL_BODY_CHARS = 30000;
    private static final int MAX_DOCUMENT_NAME_CHARS = 250;
    private static final int MAX_NAME_CLASH_RETRIES = 5;

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

        TOOLS.add(new ToolDefinition("list_case_documents", "Listet Dokumente einer Akte seitenweise auf (20 pro Seite). Je Dokument, soweit gesetzt: Bezeichnung (title), Schlagworte, Eingang (receivedDate), Von/An (correspondent mit direction in/out und contactId), Eltern-Dokument (parentId, parentTitle) und Anzahl Anlagen. Etiketten und Nachrichten liefert get_document_details. Gibt totalDocuments, page, totalPages und hasMore zurück.",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("list_case_documents_by_date",
                "Listet Dokumente einer Akte, deren Erstelldatum (Standard) oder Eingangsdatum innerhalb eines Zeitraums liegt, neueste zuerst. Je Dokument dieselben Metadaten wie list_case_documents.",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("fromDate", "string", "Startdatum im Format yyyy-MM-dd", true),
                        new ToolParameter("toDate", "string", "Enddatum im Format yyyy-MM-dd", true),
                        new ToolParameter("dateField", "string", "created (Erstelldatum, Standard) oder received (Eingangsdatum, z. B. für eingegangene Post)", false))));

        TOOLS.add(new ToolDefinition("search_case_documents", "Durchsucht Dokumente einer Akte in Dateiname, Bezeichnung, Schlagworten und Von/An (case-insensitive, Teilübereinstimmung). Ergebnisse sind seitenweise (20 pro Seite), je Dokument mit denselben Metadaten wie list_case_documents.",
                Arrays.asList(
                        new ToolParameter("fileNumber", "string", "Aktenzeichen der Akte", true),
                        new ToolParameter("query", "string", "Suchbegriff für Dateiname, Bezeichnung, Schlagworte oder Von/An", true),
                        new ToolParameter("page", "integer", "Seitennummer (1-basiert, Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("get_document_text", "Extrahiert den Textinhalt eines Dokuments (PDF oder Textdatei).",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments. Es darf kein Dokumentname als Parameter übergeben werden.", true),
                        new ToolParameter("maxChars", "integer", "Optional: maximale Anzahl der vom Dokumentanfang zurückzugebenden Zeichen. Nützlich, wenn nur der Anfang benötigt wird (z. B. Kontaktdaten von Seite 1). Standard und Obergrenze: 30000.", false))));

        TOOLS.add(new ToolDefinition("get_document_details",
                "Gibt alle Metadaten eines Dokuments zurück: Bezeichnung, Schlagworte, Eingang, Von/An, Etiketten mit Werten, Eltern-Dokument, Anlagen (Kind-Dokumente) und die mit dem Dokument verknüpften Sofortnachrichten.",
                Arrays.asList(new ToolParameter("documentId", "string", "ID des Dokuments", true))));

        TOOLS.add(new ToolDefinition("update_document_metadata",
                "Ändert Metadaten eines oder mehrerer Dokumente: Bezeichnung, Schlagworte, Eingangsdatum und Von/An. Nur die übergebenen Felder werden geändert. Die Bezeichnung ist der angezeigte Titel und unabhängig vom Dateinamen (dafür rename_document).",
                Arrays.asList(
                        new ToolParameter("documentIds", "string", "Kommagetrennte IDs der Dokumente (eine oder mehrere)", true),
                        new ToolParameter("title", "string", "Neue Bezeichnung (optional; leerer Text entfernt die Bezeichnung)", false),
                        new ToolParameter("keywords", "string", "Kommagetrennte Schlagworte (optional)", false),
                        new ToolParameter("keywordOperation", "string", "add (Standard: ergänzen), remove (entfernen) oder set (ersetzen)", false),
                        new ToolParameter("receivedDate", "string", "Eingangsdatum yyyy-MM-dd oder yyyy-MM-dd HH:mm (optional; leerer Text entfernt es)", false),
                        new ToolParameter("correspondentContactId", "string", "ID eines Kontakts als Von/An (optional, aus search_contacts oder get_parties_for_case)", false),
                        new ToolParameter("correspondentName", "string", "Von/An als Freitext bzw. abweichender Anzeigename (optional)", false),
                        new ToolParameter("correspondentDirection", "string", "in (Von, Standard), out (An) oder none (Von/An entfernen)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("set_document_parent",
                "Ordnet ein Dokument als Anlage einem anderen Dokument derselben Akte zu (z. B. Anhang einer E-Mail) oder hebt die Zuordnung auf.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments, das Anlage werden soll", true),
                        new ToolParameter("parentId", "string", "ID des übergeordneten Dokuments; leer lassen, um die Zuordnung aufzuheben", false)),
                ToolDefinition.RISK_MEDIUM));

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

        // Case links and contact relationships (read-only)
        TOOLS.add(new ToolDefinition("get_case_links",
                "Gibt die Aktenverknüpfungen einer Akte zurück (z.B. Gegenakte, Folgeakte), je Verknüpfung mit Beschreibung und Aktenzeichen, Kurzrubrum, Grund und Archivstatus der verknüpften Akte. Akten ohne Leseberechtigung fehlen. Die Akten-ID liefern search_cases bzw. get_case.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("get_contact_relations",
                "Gibt die Kontaktbeziehungen eines Kontakts zurück (z.B. Mutter/Kind, Geschäftsführer/Gesellschaft), je Beziehung mit Bezeichnung aus Sicht dieses Kontakts (label, z.B. \"ist Mutter von\"), Beziehungstyp, Notiz und dem verbundenen Kontakt. Die Kontakt-ID liefern search_contacts bzw. get_parties_for_case.",
                Arrays.asList(new ToolParameter("contactId", "string", "Interne ID des Kontakts", true))));

        TOOLS.add(new ToolDefinition("get_cases_for_contact",
                "Gibt alle Akten zurück, in denen ein Kontakt beteiligt ist, je Akte mit Beteiligtentyp (z.B. Mandant, Gegner) und Zeichen des Beteiligten. Akten ohne Leseberechtigung fehlen.",
                Arrays.asList(
                        new ToolParameter("contactId", "string", "Interne ID des Kontakts", true),
                        new ToolParameter("includeArchived", "string", "false, um archivierte Akten auszulassen (Standard: true)", false))));

        TOOLS.add(new ToolDefinition("get_case_network",
                "Ermittelt alle Akten, die über Aktenverknüpfungen (auch über mehrere Stufen) mit einer Akte zusammenhängen, je Akte mit Abstand (depth) und über welche Akte/Verknüpfung sie erreicht wurde. "
                + "Liefert zusätzlich sharedParties: Kontakte, die in mehr als einer dieser Akten beteiligt sind, mit ihrer Rolle je Akte. "
                + "Begrenzt auf " + NetworkToolSupport.MAX_NODES + " Akten und " + NetworkToolSupport.MAX_REMOTE_CALLS + " Serverabfragen; bei Erreichen ist truncated=true.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Ausgangsakte", true),
                        new ToolParameter("maxDepth", "integer", "Maximale Anzahl Verknüpfungsstufen, 1 bis 3 (Standard: 1)", false))));

        TOOLS.add(new ToolDefinition("get_contact_network",
                "Ermittelt das Beziehungsumfeld eines Kontakts über Kontaktbeziehungen (auch über mehrere Stufen): Kontakte mit Abstand (depth) und die Beziehungen zwischen ihnen (label aus Sicht von fromContactId). "
                + "Mit includeCases=true zusätzlich je Kontakt die Akten, in denen er beteiligt ist, samt Rolle (höchstens " + NetworkToolSupport.MAX_HUB_CASES + " je Kontakt, sonst hub=true). "
                + "Begrenzt auf " + NetworkToolSupport.MAX_NODES + " Kontakte und " + NetworkToolSupport.MAX_REMOTE_CALLS + " Serverabfragen; bei Erreichen ist truncated=true.",
                Arrays.asList(
                        new ToolParameter("contactId", "string", "Interne ID des Ausgangskontakts", true),
                        new ToolParameter("maxDepth", "integer", "Maximale Anzahl Beziehungsstufen, 1 oder 2 (Standard: 1)", false),
                        new ToolParameter("includeCases", "string", "true, um die Akten jedes Kontakts mitzuliefern (Standard: false)", false))));

        TOOLS.add(new ToolDefinition("find_party_connections",
                "Prüft die Beteiligten einer Akte auf Verbindungen zu anderen Akten: je Beteiligtem (a) die anderen Akten, in denen derselbe Kontakt beteiligt ist, mit Rolle und roleDiffers=true, wenn die Rolle von der in dieser Akte abweicht, "
                + "und (b) direkt verbundene Kontakte (Kontaktbeziehungen), die in anderen Akten oder in dieser Akte beteiligt sind. "
                + "Das Ergebnis ist ein Hinweis für eine Kollisionsprüfung, keine abschließende Prüfung: Es berücksichtigt nur erfasste Beteiligungen und Beziehungen und nur Akten, die der Nutzer sehen darf. "
                + "Kontakte mit mehr als " + NetworkToolSupport.MAX_HUB_CASES + " Akten werden gekürzt (hub=true). Begrenzt auf " + NetworkToolSupport.MAX_REMOTE_CALLS + " Serverabfragen; bei Erreichen ist truncated=true.",
                Arrays.asList(new ToolParameter("caseId", "string", "Interne ID der Akte", true))));

        TOOLS.add(new ToolDefinition("find_connection",
                "Sucht den kürzesten Zusammenhang zwischen zwei Kontakten über Kontaktbeziehungen, Beteiligungen an Akten und Aktenverknüpfungen. "
                + "Liefert bei found=true den Pfad als lesbare Schritte (steps) und Knoten (path), sonst found=false. "
                + "Kontakte mit mehr als " + NetworkToolSupport.MAX_HUB_CASES + " Akten werden unterwegs nicht über ihre Akten weiterverfolgt (hubsNotExpanded). "
                + "Begrenzt auf " + NetworkToolSupport.MAX_NODES + " Knoten und " + NetworkToolSupport.MAX_REMOTE_CALLS + " Serverabfragen; bei Erreichen ist truncated=true.",
                Arrays.asList(
                        new ToolParameter("contactIdA", "string", "Interne ID des ersten Kontakts", true),
                        new ToolParameter("contactIdB", "string", "Interne ID des zweiten Kontakts", true),
                        new ToolParameter("maxDepth", "integer", "Maximale Anzahl Schritte des Pfads, 1 bis 4 (Standard: 3)", false))));

        TOOLS.add(new ToolDefinition("get_all_open_events", "Gibt alle offenen Kalenderereignisse zurück. Optional nach Typ und/oder Verantwortlichem filterbar.",
                Arrays.asList(
                        new ToolParameter("eventType", "string", "Ereignistyp zum Filtern: Wiedervorlage, Frist oder Termin (optional, Standard: alle)", false),
                        new ToolParameter("assignee", "string", "Benutzername des Verantwortlichen zum Filtern (optional, Standard: alle)", false))));

        TOOLS.add(new ToolDefinition("get_all_open_events_between_dates", "Gibt alle offenen Kalenderereignisse zwischen zwei Daten zurück. Optional nach Typ und/oder Verantwortlichem filterbar.",
                Arrays.asList(
                        new ToolParameter("fromDate", "string", "Startdatum im ISO-8601-Format (z.B. 2025-03-01T00:00:00)", true),
                        new ToolParameter("toDate", "string", "Enddatum im ISO-8601-Format (z.B. 2025-03-31T23:59:59)", true),
                        new ToolParameter("eventType", "string", "Ereignistyp zum Filtern: Wiedervorlage, Frist oder Termin (optional, Standard: alle)", false),
                        new ToolParameter("assignee", "string", "Benutzername des Verantwortlichen zum Filtern (optional, Standard: alle)", false))));

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
                "Ändert den Dateinamen eines Dokuments in einer Akte. Für die angezeigte Bezeichnung (Titel) stattdessen update_document_metadata verwenden.",
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
                        new ToolParameter("location", "string", "Ort (optional)", false),
                        new ToolParameter("reminderMinutes", "integer", "Erinnerung in Minuten vor Beginn (optional, nur für Typ 'Termin'; 0 = bei Beginn, max. 1440 = 1 Tag, -1 = keine Erinnerung)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("update_event", "Aktualisiert ein bestehendes Kalenderereignis. Nur summary, beginDate, endDate, assignee, description, location und reminderMinutes können geändert werden.",
                Arrays.asList(
                        new ToolParameter("eventId", "string", "ID des zu ändernden Ereignisses", true),
                        new ToolParameter("summary", "string", "Neue Zusammenfassung/Betreff (optional)", false),
                        new ToolParameter("beginDate", "string", "Neues Startdatum im ISO-8601-Format (optional)", false),
                        new ToolParameter("endDate", "string", "Neues Enddatum im ISO-8601-Format (optional)", false),
                        new ToolParameter("assignee", "string", "Neuer Verantwortlicher Benutzername (optional)", false),
                        new ToolParameter("description", "string", "Neue Beschreibung (optional)", false),
                        new ToolParameter("location", "string", "Neuer Ort (optional)", false),
                        new ToolParameter("reminderMinutes", "integer", "Neue Erinnerung in Minuten vor Beginn (optional, nur für Typ 'Termin'; 0 = bei Beginn, max. 1440 = 1 Tag, -1 = keine Erinnerung)", false)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("create_note", "Erstellt eine Aktennotiz als HTML-Dokument in einer Akte.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("content", "string", "Inhalt der Notiz (HTML erlaubt: b, i, br, ul, li)", true),
                        new ToolParameter("title", "string", "Bezeichnung des Notiz-Dokuments (optional)", false),
                        new ToolParameter("keywords", "string", "Kommagetrennte Schlagworte (optional)", false)),
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
                        new ToolParameter("recipient", "string", "Benutzername des Empfängers (optional, wird als @Erwähnung hinzugefügt)", false),
                        new ToolParameter("documentId", "string", "ID eines Dokuments der Akte, auf das sich die Nachricht bezieht (optional)", false)),
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

        TOOLS.add(new ToolDefinition("create_case_folder",
                "Erstellt einen neuen Ordner in einer Akte. Ohne parentFolderId wird der Ordner auf oberster Ebene erstellt. Für Unterordner zuerst list_case_folders aufrufen, um die parentFolderId zu ermitteln.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "ID der Akte", true),
                        new ToolParameter("parentFolderId", "string", "ID des übergeordneten Ordners (optional, ohne Angabe wird der Ordner auf oberster Ebene erstellt)", false),
                        new ToolParameter("name", "string", "Name des neuen Ordners", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("move_document_to_folder",
                "Verschiebt ein Dokument in einen Ordner innerhalb derselben Akte.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments", true),
                        new ToolParameter("folderId", "string", "ID des Zielordners", true)),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("move_document_to_case",
                "Verschiebt ein oder mehrere Dokumente in eine andere Akte. Die Dokumente werden mit Etiketten und allen Metadaten (Bezeichnung, Schlagworte, Eingang, Von/An) in die Zielakte kopiert und in der Quellakte in den Papierkorb gelegt. Werden ein Dokument und seine Anlagen gemeinsam verschoben, bleibt die Zuordnung erhalten. Optional kann ein Zielordner und - bei einem einzelnen Dokument - ein neuer Dateiname angegeben werden.",
                Arrays.asList(
                        new ToolParameter("documentId", "string", "ID des Dokuments (oder documentIds verwenden)", false),
                        new ToolParameter("documentIds", "string", "Kommagetrennte IDs mehrerer Dokumente, z. B. eine E-Mail mit ihren Anlagen (optional statt documentId)", false),
                        new ToolParameter("targetCaseId", "string", "ID der Zielakte", true),
                        new ToolParameter("targetFolderId", "string", "ID des Zielordners in der Zielakte (optional)", false),
                        new ToolParameter("newFileName", "string", "Neuer Dateiname (optional, ohne Angabe wird der bisherige Name verwendet)", false)),
                ToolDefinition.RISK_HIGH));

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

        TOOLS.add(new ToolDefinition("list_contact_tags",
                "Gibt alle verfügbaren Etiketten (Tags) für Kontakte zurück, inkl. Listenetiketten mit ihren möglichen Werten.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("set_contact_tag",
                "Setzt ein Etikett auf einen Kontakt. Für Listenetiketten muss zusätzlich ein tagValue angegeben werden. Zum Entfernen active=false setzen.",
                Arrays.asList(
                        new ToolParameter("contactId", "string", "Interne ID des Kontakts", true),
                        new ToolParameter("tagName", "string", "Name des Etiketts", true),
                        new ToolParameter("tagValue", "string", "Wert bei Listenetiketten (optional, null für einfache Etiketten)", false),
                        new ToolParameter("active", "string", "true zum Setzen, false zum Entfernen (Standard: true)", false)),
                ToolDefinition.RISK_MEDIUM));

        // Template tools

        TOOLS.add(new ToolDefinition("search_templates",
                "Sucht Dokumentvorlagen anhand eines Suchbegriffs (Teilübereinstimmung, Groß-/Kleinschreibung wird ignoriert). Gibt nur passende Vorlagen mit Ordnerpfad zurück.",
                Arrays.asList(
                        new ToolParameter("query", "string", "Suchbegriff für den Vorlagennamen", true))));

        TOOLS.add(new ToolDefinition("list_letter_heads",
                "Listet alle verfügbaren Briefköpfe auf.",
                Arrays.asList(),
                ToolDefinition.RISK_LOW));

        TOOLS.add(new ToolDefinition("create_document_from_template",
                "Erstellt ein Dokument in einer Akte aus einer Dokumentvorlage. Platzhalter werden automatisch aus den Aktendaten befüllt. Verwende zuerst search_templates um verfügbare Vorlagen zu sehen.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("templateFolder", "string", "Ordnerpfad der Vorlage (z.B. / oder /Vertragsrecht)", true),
                        new ToolParameter("templateName", "string", "Dateiname der Vorlage (z.B. Vollmacht.odt)", true),
                        new ToolParameter("fileName", "string", "Dateiname des neuen Dokuments ohne Erweiterung (z.B. Vollmacht Mueller)", true),
                        new ToolParameter("generatedText", "string", "Vom Assistenten generierter Text, der als Platzhalter {{INGO_TEXT}} in die Vorlage eingefügt wird (optional)", false),
                        new ToolParameter("letterHead", "string", "Name des Briefkopfs (optional). Verwende list_letter_heads um verfügbare Briefköpfe zu sehen.", false),
                        new ToolParameter("title", "string", "Bezeichnung des neuen Dokuments (optional)", false),
                        new ToolParameter("keywords", "string", "Kommagetrennte Schlagworte (optional)", false)),
                ToolDefinition.RISK_MEDIUM));


        // Web tools (read-only, RISK_LOW)
        TOOLS.add(new ToolDefinition("web_search",
                "Führt eine Websuche durch und gibt eine Liste von Ergebnissen zurück (Titel, URL, Textausschnitt). Nützlich für aktuelle Informationen, Rechtsprechung, oder Fakten die nicht in den Akten enthalten sind.",
                Arrays.asList(new ToolParameter("query", "string", "Suchbegriff für die Websuche", true))));

        TOOLS.add(new ToolDefinition("fetch_url",
                "Lädt den Textinhalt einer Webseite herunter. Gibt den extrahierten Text ohne HTML-Tags zurück. Nützlich um Details einer Webseite zu lesen, die z.B. aus einer Websuche stammt.",
                Arrays.asList(new ToolParameter("url", "string", "Die URL der Webseite", true))));

        TOOLS.add(new ToolDefinition("search_instant_messages",
                "Sucht Sofortnachrichten/Verfügungen. Mindestens caseId, documentId oder fromDate muss angegeben werden. Nachrichten mit Dokumentbezug enthalten documentId und documentName. Ergebnisse auf 100 begrenzt.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte (optional)", false),
                        new ToolParameter("documentId", "string", "Nur Nachrichten zu diesem Dokument (optional)", false),
                        new ToolParameter("sender", "string", "Benutzername des Absenders (optional)", false),
                        new ToolParameter("fromDate", "string", "Startdatum im ISO-Format yyyy-MM-dd (optional)", false),
                        new ToolParameter("toDate", "string", "Enddatum im ISO-Format yyyy-MM-dd (optional)", false)
                )));

        TOOLS.add(new ToolDefinition("list_form_types",
                "Listet alle verfügbaren/installierten Falldatenblätter (Formulare) auf. Gibt ID, Name, Platzhalterpräfix und Version zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("create_case_form",
                "Erstellt ein Falldatenblatt in einer Akte. Der Platzhalterpräfix muss innerhalb der Akte eindeutig sein.",
                Arrays.asList(
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("formTypeId", "string", "ID des Falldatenblatt-Typs (aus list_form_types)", true),
                        new ToolParameter("placeHolder", "string", "Platzhalterpräfix für die Formularfelder (aus list_form_types, muss in der Akte eindeutig sein)", true),
                        new ToolParameter("description", "string", "Beschreibung des Falldatenblatts", false)
                ),
                ToolDefinition.RISK_MEDIUM));

        TOOLS.add(new ToolDefinition("list_mailboxes",
                "Listet alle E-Mail-Postfächer auf, auf die der angemeldete Benutzer Zugriff hat. Gibt ID, Anzeigename und E-Mail-Adresse zurück.",
                Arrays.asList()));

        TOOLS.add(new ToolDefinition("search_emails",
                "Durchsucht E-Mail-Postfächer. Der Suchbegriff wird in Betreff, Absender, Empfänger und Nachrichtentext gesucht. WICHTIG: Es wird ausschließlich wörtlich nach der übergebenen Zeichenfolge gesucht (Teilübereinstimmung, Groß-/Kleinschreibung wird ignoriert). Der Suchtext darf Leerzeichen enthalten und wird dann als zusammenhängende Zeichenfolge gesucht ('jens müller' findet 'Jens Müller'). Suchoperatoren wie OR/AND/NOT, Anführungszeichen und Platzhalter werden NICHT unterstützt, sondern als Teil des Suchtexts gesucht - 'rechnung OR invoice' liefert deshalb keine Treffer. Für Synonyme oder Wortvarianten sind mehrere getrennte Aufrufe nötig; ein kurzer Wortstamm ('Rechnung') findet auch längere Formen ('Rechnungen'). Standardmäßig wird nur der Posteingang aller zugänglichen Postfächer durchsucht. Gibt eine Trefferliste ohne Nachrichtentext zurück (max. 50 Treffer); den Volltext einer Nachricht liefert get_email.",
                Arrays.asList(
                        new ToolParameter("query", "string", "Suchtext, der wörtlich in Betreff, Absender, Empfänger und Nachrichtentext gesucht wird. Darf Leerzeichen enthalten und wird dann als zusammenhängende Zeichenfolge gesucht. Keine Suchoperatoren, keine Platzhalter, keine Anführungszeichen.", true),
                        new ToolParameter("mailboxId", "string", "ID des zu durchsuchenden Postfachs (aus list_mailboxes). Optional, Standard: alle zugänglichen Postfächer", false),
                        new ToolParameter("folder", "string", "Anzeigename des zu durchsuchenden Ordners, z. B. 'Gesendet'. Optional, Standard: Posteingang", false),
                        new ToolParameter("scope", "string", "'inbox' (Standard) oder 'all', um alle Ordner zu durchsuchen. 'all' ist deutlich langsamer.", false),
                        new ToolParameter("fromDate", "string", "Nur Nachrichten ab diesem Datum, Format yyyy-MM-dd (optional)", false),
                        new ToolParameter("unreadOnly", "string", "'true', um nur ungelesene Nachrichten zu berücksichtigen (optional)", false),
                        new ToolParameter("maxResults", "integer", "Maximale Trefferzahl. Standard und Obergrenze: 50", false))));

        TOOLS.add(new ToolDefinition("get_email",
                "Lädt eine einzelne E-Mail mit Nachrichtentext und Liste der Anhänge. mailboxId und messageRef stammen aus search_emails oder aus den Kopfdaten einer im Kontext übergebenen E-Mail.",
                Arrays.asList(
                        new ToolParameter("mailboxId", "string", "ID des Postfachs", true),
                        new ToolParameter("messageRef", "string", "Referenz der Nachricht aus search_emails oder aus dem Kontext (messageRef)", true),
                        new ToolParameter("maxChars", "integer", "Optional: maximale Zeichenzahl des Nachrichtentexts. Standard und Obergrenze: 30000", false))));

        TOOLS.add(new ToolDefinition("save_email_to_case",
                "Speichert eine E-Mail als .eml-Datei in einer Akte. mailboxId und messageRef stammen aus search_emails oder aus den Kopfdaten einer im Kontext übergebenen E-Mail, die caseId aus search_cases. Der Dateiname wird aus dem Betreff nach der konfigurierten Dateinamensvorschrift der Kanzlei gebildet; existiert der Name bereits, wird automatisch eine Nummer angehängt. Wie beim Speichern im Client erhält das Dokument den Betreff als Bezeichnung, das Datum der E-Mail als Eingang und Absender bzw. Empfänger als Von/An (einem Kontakt zugeordnet, soweit vorhanden). Anhänge bleiben in der .eml enthalten; mit saveAttachments=true werden sie zusätzlich als Anlagen der E-Mail abgelegt.",
                Arrays.asList(
                        new ToolParameter("mailboxId", "string", "ID des Postfachs", true),
                        new ToolParameter("messageRef", "string", "Referenz der Nachricht aus search_emails oder aus dem Kontext (messageRef)", true),
                        new ToolParameter("caseId", "string", "Interne ID der Akte", true),
                        new ToolParameter("fileName", "string", "Dateiname übersteuern (optional). Die Endung .eml wird immer erzwungen, die Dateinamensvorschrift wird trotzdem angewendet.", false),
                        new ToolParameter("folderId", "string", "ID des Zielordners in der Akte (optional, aus list_case_folders). Ohne Angabe landet die Datei im Wurzelordner der Akte.", false),
                        new ToolParameter("tags", "string", "Kommagetrennte Dokument-Etiketten für die E-Mail (optional, aus list_document_tags). Nur einfache Etiketten ohne Wert; Mehrwert-Etiketten über set_document_tag setzen.", false),
                        new ToolParameter("keywords", "string", "Kommagetrennte Schlagworte für die E-Mail (optional)", false),
                        new ToolParameter("saveAttachments", "string", "true, um die Anhänge (ohne eingebettete Bilder) zusätzlich als eigene Dokumente abzulegen, die der E-Mail als Anlagen zugeordnet sind (Standard: false)", false)),
                ToolDefinition.RISK_MEDIUM));
    }

    public boolean hasAiAgentRole() {
        String principalId = UserSettings.getInstance().getCurrentUser().getPrincipalId();
        List<String> roles = UserSettings.getInstance().getUserRoles(principalId);
        return roles.contains("aiAgentRole");
    }

    public List<ToolDefinition> getToolDefinitions() {
        if (!hasAiAgentRole()) {
            return Collections.emptyList();
        }
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
        if (!hasAiAgentRole()) {
            return "{\"error\": \"Zugriff verweigert: Keine Berechtigung für KI-Agentenfunktionen.\"}";
        }
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
                case "get_document_details":
                    return executeGetDocumentDetails(args);
                case "update_document_metadata":
                    return executeUpdateDocumentMetadata(args);
                case "set_document_parent":
                    return executeSetDocumentParent(args);
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
                case "get_case_links":
                    return executeGetCaseLinks(args);
                case "get_contact_relations":
                    return executeGetContactRelations(args);
                case "get_cases_for_contact":
                    return executeGetCasesForContact(args);
                case "get_case_network":
                    return executeGetCaseNetwork(args);
                case "get_contact_network":
                    return executeGetContactNetwork(args);
                case "find_party_connections":
                    return executeFindPartyConnections(args);
                case "find_connection":
                    return executeFindConnection(args);
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
                case "update_event":
                    return executeUpdateEvent(args);
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
                case "create_case_folder":
                    return executeCreateCaseFolder(args);
                case "move_document_to_folder":
                    return executeMoveDocumentToFolder(args);
                case "move_document_to_case":
                    return executeMoveDocumentToCase(args);
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
                case "list_contact_tags":
                    return executeListContactTags(args);
                case "set_contact_tag":
                    return executeSetContactTag(args);
                case "search_templates":
                    return executeSearchTemplates(args);
                case "list_letter_heads":
                    return executeListLetterHeads(args);
                case "create_document_from_template":
                    return executeCreateDocumentFromTemplate(args);
                case "web_search":
                    return executeWebSearch(args);
                case "fetch_url":
                    return executeFetchUrl(args);
                case "search_instant_messages":
                    return executeSearchInstantMessages(args);
                case "list_form_types":
                    return executeListFormTypes(args);
                case "create_case_form":
                    return executeCreateCaseForm(args);
                case "list_mailboxes":
                    return executeListMailboxes(args);
                case "search_emails":
                    return executeSearchEmails(args);
                case "get_email":
                    return executeGetEmail(args);
                case "save_email_to_case":
                    return executeSaveEmailToCase(args);
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

    /**
     * Revokes the "always allow" permission for the given tool.
     * @param toolId the tool identifier
     */
    public void revokeAlways(String toolId) {
        UserSettings.getInstance().removeSetting(SETTINGS_PREFIX + toolId);
    }

    /**
     * Checks whether the given tool has been permanently approved (not just for the current session).
     * @param toolId the tool identifier
     * @return true if the tool has been permanently approved
     */
    public boolean isAlwaysApproved(String toolId) {
        String setting = UserSettings.getInstance().getSetting(SETTINGS_PREFIX + toolId, "false");
        return "true".equalsIgnoreCase(setting);
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

    private static String formatReminderSummary(Object rawValue) {
        Integer reminderMinutes = ToolJsonUtils.toInteger(rawValue);
        if (reminderMinutes == null) {
            return "";
        }
        if (reminderMinutes < 0) {
            return " (ohne Erinnerung)";
        }
        if (reminderMinutes == 0) {
            return " (Erinnerung: bei Beginn)";
        }
        return " (Erinnerung: " + reminderMinutes + " Min. vorher)";
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
                case "get_document_details":
                    return "Dokumentdetails: " + args.getOrDefault("documentId", "");
                case "update_document_metadata":
                    return "Dokument-Metadaten ändern: " + DocumentToolSupport.splitIds((String) args.get("documentIds")).size() + " Dokument(e)" + describeMetadataChange(args);
                case "set_document_parent": {
                    Object parent = args.get("parentId");
                    return (parent == null || parent.toString().isBlank())
                            ? "Anlagen-Zuordnung aufheben: " + args.getOrDefault("documentId", "")
                            : "Dokument als Anlage zuordnen: " + args.getOrDefault("documentId", "") + " zu " + parent;
                }
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
                case "get_case_links":
                    return "Aktenverknüpfungen: " + args.getOrDefault("caseId", "");
                case "get_contact_relations":
                    return "Kontaktbeziehungen: " + args.getOrDefault("contactId", "");
                case "get_cases_for_contact":
                    return "Akten des Kontakts: " + args.getOrDefault("contactId", "");
                case "get_case_network":
                    return "Aktennetz: " + args.getOrDefault("caseId", "");
                case "get_contact_network":
                    return "Beziehungsumfeld: " + args.getOrDefault("contactId", "");
                case "find_party_connections":
                    return "Verbindungen der Beteiligten: " + args.getOrDefault("caseId", "");
                case "find_connection":
                    return "Zusammenhang: " + args.getOrDefault("contactIdA", "") + " / " + args.getOrDefault("contactIdB", "");
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
                    return "Termin erstellen: " + args.getOrDefault("summary", "") + formatReminderSummary(args.get("reminderMinutes"));
                case "update_event":
                    return "Termin aktualisieren: " + args.getOrDefault("eventId", "") + formatReminderSummary(args.get("reminderMinutes"));
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
                case "create_case_folder":
                    return "Ordner erstellen: " + args.getOrDefault("name", "");
                case "move_document_to_folder":
                    return "Dokument in Ordner verschieben: " + args.getOrDefault("documentId", "");
                case "move_document_to_case": {
                    List<String> moveIds = DocumentToolSupport.splitIds((String) args.get("documentIds"));
                    String what = moveIds.size() > 1 ? moveIds.size() + " Dokumente" : "Dokument " + args.getOrDefault("documentId", moveIds.isEmpty() ? "" : moveIds.get(0));
                    return what + " in andere Akte verschieben nach Akte " + args.getOrDefault("targetCaseId", "");
                }
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
                case "list_contact_tags":
                    return "Verfügbare Kontakt-Etiketten auflisten";
                case "set_contact_tag":
                    return "Kontakt-Etikett setzen: " + args.getOrDefault("tagName", "");
                case "search_templates":
                    return "Vorlagensuche: '" + args.getOrDefault("query", "") + "'";
                case "list_letter_heads":
                    return "Verfügbare Briefköpfe auflisten";
                case "create_document_from_template":
                    return "Dokument aus Vorlage erstellen: " + args.getOrDefault("templateName", "");
                case "web_search":
                    return "Websuche: '" + args.getOrDefault("query", "") + "'";
                case "fetch_url":
                    return "Webseite laden: " + args.getOrDefault("url", "");
                case "search_instant_messages":
                    return "Sofortnachrichten suchen" + (args.containsKey("caseId") ? " (Akte: " + args.get("caseId") + ")" : "") + (args.containsKey("sender") ? " (Absender: " + args.get("sender") + ")" : "");
                case "list_form_types":
                    return "Verfügbare Falldatenblätter auflisten";
                case "create_case_form":
                    return "Falldatenblatt erstellen: " + args.getOrDefault("placeHolder", "");
                case "list_mailboxes":
                    return "E-Mail-Postfächer auflisten";
                case "search_emails":
                    return "E-Mails durchsuchen: '" + args.getOrDefault("query", "") + "'"
                            + (args.containsKey("folder") ? " (Ordner: " + args.get("folder") + ")" : "")
                            + ("all".equalsIgnoreCase(String.valueOf(args.getOrDefault("scope", ""))) ? " (alle Ordner)" : "");
                case "get_email":
                    return "E-Mail lesen";
                case "save_email_to_case":
                    return "E-Mail in Akte speichern: " + args.getOrDefault("caseId", "");
                default:
                    return tc.getToolName() + ": " + tc.getArguments();
            }
        } catch (Exception ex) {
            return tc.getToolName() + ": " + tc.getArguments();
        }
    }

    private String executeSearchInstantMessages(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String sender = (String) args.get("sender");
        String fromDateStr = (String) args.get("fromDate");
        String toDateStr = (String) args.get("toDate");

        String documentId = (String) args.get("documentId");
        boolean hasDocumentId = documentId != null && !documentId.trim().isEmpty();
        boolean hasCaseId = caseId != null && !caseId.trim().isEmpty();
        boolean hasFromDate = fromDateStr != null && !fromDateStr.trim().isEmpty();

        if (!hasCaseId && !hasFromDate && !hasDocumentId) {
            return ToolJsonUtils.error("Mindestens caseId, documentId oder fromDate muss angegeben werden");
        }

        Date fromDate = hasFromDate ? ToolJsonUtils.parseIsoDate(fromDateStr) : null;
        Date toDate = toDateStr != null && !toDateStr.trim().isEmpty() ? ToolJsonUtils.parseIsoDate(toDateStr) : null;
        if (hasFromDate && fromDate == null) {
            return ToolJsonUtils.error("fromDate konnte nicht geparst werden: " + fromDateStr);
        }
        if (toDate != null) {
            // set toDate to end of day
            Calendar cal = Calendar.getInstance();
            cal.setTime(toDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            toDate = cal.getTime();
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        MessagingServiceRemote msgSvc = locator.lookupMessagingServiceRemote();

        List<InstantMessage> messages;
        if (hasDocumentId) {
            messages = msgSvc.getMessagesForDocument(documentId.trim());
            if (messages == null) {
                messages = new ArrayList<>();
            }
            if (fromDate != null || toDate != null) {
                Date fDate = fromDate;
                Date tDate = toDate;
                messages.removeIf(m -> m.getSent() == null || (fDate != null && m.getSent().before(fDate)) || (tDate != null && m.getSent().after(tDate)));
            }
        } else if (hasCaseId) {
            messages = msgSvc.getMessagesForCase(caseId.trim());
            if (messages == null) {
                messages = new ArrayList<>();
            }
            // client-side date filtering
            if (fromDate != null || toDate != null) {
                Date fDate = fromDate;
                Date tDate = toDate;
                messages.removeIf(m -> {
                    if (m.getSent() == null) return true;
                    if (fDate != null && m.getSent().before(fDate)) return true;
                    if (tDate != null && m.getSent().after(tDate)) return true;
                    return false;
                });
            }
        } else {
            messages = msgSvc.getMessagesSince(fromDate);
            if (messages == null) {
                messages = new ArrayList<>();
            }
            if (toDate != null) {
                Date tDate = toDate;
                messages.removeIf(m -> m.getSent() == null || m.getSent().after(tDate));
            }
        }

        // client-side sender filtering
        if (sender != null && !sender.trim().isEmpty()) {
            String senderFilter = sender.trim();
            messages.removeIf(m -> m.getSender() == null || !m.getSender().equalsIgnoreCase(senderFilter));
        }

        int total = messages.size();
        int limit = Math.min(total, 100);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            InstantMessage m = messages.get(i);
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(m.getId())).append("\"");
            sb.append(", \"sender\": \"").append(ToolJsonUtils.escapeJson(m.getSender())).append("\"");
            sb.append(", \"sent\": \"").append(ToolJsonUtils.formatDate(m.getSent())).append("\"");
            sb.append(", \"content\": \"").append(ToolJsonUtils.escapeJson(m.getContent())).append("\"");
            if (m.getCaseContext() != null) {
                sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(m.getCaseContext().getId())).append("\"");
                sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(m.getCaseContext().getFileNumber())).append("\"");
            }
            if (m.getDocumentContext() != null) {
                sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(m.getDocumentContext().getId())).append("\"");
                sb.append(", \"documentName\": \"").append(ToolJsonUtils.escapeJson(m.getDocumentContext().getDisplayTitle())).append("\"");
            }
            sb.append("}");
        }
        sb.append("], \"totalResults\": ").append(total);
        if (total > limit) {
            sb.append(", \"truncated\": true");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeListFormTypes(JsonObject args) throws Exception {
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        FormsServiceRemote formsSvc = locator.lookupFormsServiceRemote();
        List<FormTypeBean> formTypes = formsSvc.getAllFormTypes();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < formTypes.size(); i++) {
            if (i > 0) sb.append(",");
            FormTypeBean ft = formTypes.get(i);
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(ft.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(ft.getName())).append("\"");
            sb.append(", \"placeHolder\": \"").append(ToolJsonUtils.escapeJson(ft.getPlaceHolder())).append("\"");
            if (ft.getVersion() != null) {
                sb.append(", \"version\": \"").append(ToolJsonUtils.escapeJson(ft.getVersion())).append("\"");
            }
            sb.append("}");
        }
        sb.append("], \"totalResults\": ").append(formTypes.size()).append("}");
        return sb.toString();
    }

    private String executeCreateCaseForm(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String formTypeId = (String) args.get("formTypeId");
        String placeHolder = (String) args.get("placeHolder");
        String description = (String) args.get("description");

        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        if (formTypeId == null || formTypeId.trim().isEmpty()) {
            return ToolJsonUtils.error("Falldatenblatt-Typ-ID fehlt");
        }
        if (placeHolder == null || placeHolder.trim().isEmpty()) {
            return ToolJsonUtils.error("Platzhalterpräfix fehlt");
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();

        ArchiveFileServiceRemote afSvc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = afSvc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        FormsServiceRemote formsSvc = locator.lookupFormsServiceRemote();
        List<FormTypeBean> formTypes = formsSvc.getAllFormTypes();
        FormTypeBean matchedType = null;
        for (FormTypeBean ft : formTypes) {
            if (ft.getId().equals(formTypeId.trim())) {
                matchedType = ft;
                break;
            }
        }
        if (matchedType == null) {
            return ToolJsonUtils.error("Falldatenblatt-Typ nicht gefunden: " + formTypeId);
        }

        ArchiveFileFormsBean affb = new ArchiveFileFormsBean();
        affb.setFormType(matchedType);
        affb.setArchiveFileKey(caseBean);
        affb.setPlaceHolder(placeHolder.trim());
        if (description != null && !description.trim().isEmpty()) {
            affb.setDescription(description.trim());
        }

        affb = formsSvc.addForm(caseId, affb);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(affb.getId())).append("\"");
        sb.append(", \"formTypeName\": \"").append(ToolJsonUtils.escapeJson(matchedType.getName())).append("\"");
        sb.append(", \"placeHolder\": \"").append(ToolJsonUtils.escapeJson(affb.getPlaceHolder())).append("\"");
        sb.append(", \"creationDate\": \"").append(ToolJsonUtils.formatDate(affb.getCreationDate())).append("\"");
        sb.append("}");
        return sb.toString();
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

    private String executeCreateCaseFolder(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String parentFolderId = (String) args.get("parentFolderId");
        String name = (String) args.get("name");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }
        if (name == null || name.trim().isEmpty()) {
            return ToolJsonUtils.error("Ordnername (name) fehlt");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        ArchiveFileBean caseBean = svc.getArchiveFile(caseId.trim());
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        CaseFolder root = caseBean.getRootFolder();
        if (root == null) {
            return ToolJsonUtils.error("Akte hat keine Ordnerstruktur");
        }

        // Ohne parentFolderId wird der Root-Ordner verwendet
        String effectiveParentId;
        if (parentFolderId != null && !parentFolderId.trim().isEmpty()) {
            if (!folderExistsInTree(root, parentFolderId.trim())) {
                return ToolJsonUtils.error("Übergeordneter Ordner nicht gefunden: " + parentFolderId + ". Ordner-IDs können über list_case_folders ermittelt werden.");
            }
            effectiveParentId = parentFolderId.trim();
        } else {
            effectiveParentId = root.getId();
        }

        CaseFolder created = svc.createCaseFolder(effectiveParentId, name.trim());

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"id\": \"").append(ToolJsonUtils.escapeJson(created.getId())).append("\"");
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(created.getName())).append("\"");
        sb.append(", \"parentFolderId\": \"").append(ToolJsonUtils.escapeJson(parentFolderId.trim())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private boolean folderExistsInTree(CaseFolder folder, String folderId) {
        if (folder.getId().equals(folderId)) {
            return true;
        }
        if (folder.getChildren() != null) {
            for (CaseFolder child : folder.getChildren()) {
                if (folderExistsInTree(child, folderId)) {
                    return true;
                }
            }
        }
        return false;
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

    private String executeMoveDocumentToCase(JsonObject args) throws Exception {
        List<String> documentIds = DocumentToolSupport.splitIds((String) args.get("documentIds"));
        String documentId = (String) args.get("documentId");
        if (documentId != null && !documentId.trim().isEmpty() && !documentIds.contains(documentId.trim())) {
            documentIds.add(0, documentId.trim());
        }
        String targetCaseId = (String) args.get("targetCaseId");
        String targetFolderId = (String) args.get("targetFolderId");
        String newFileName = (String) args.get("newFileName");

        if (documentIds.isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID (documentId oder documentIds) fehlt");
        }
        if (targetCaseId == null || targetCaseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Zielakten-ID (targetCaseId) fehlt");
        }
        boolean rename = newFileName != null && !newFileName.trim().isEmpty();
        if (rename && documentIds.size() > 1) {
            return ToolJsonUtils.error("newFileName ist nur beim Verschieben eines einzelnen Dokuments möglich");
        }

        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();

        ArchiveFileBean targetCase = svc.getArchiveFile(targetCaseId.trim());
        if (targetCase == null) {
            return ToolJsonUtils.error("Zielakte nicht gefunden: " + targetCaseId);
        }
        if (targetCase.isArchived()) {
            return ToolJsonUtils.error("Zielakte ist archiviert (abgelegt). Bitte die Akte zuerst reaktivieren.");
        }

        List<ArchiveFileDocumentsBean> sources = new ArrayList<>();
        for (String id : documentIds) {
            ArchiveFileDocumentsBean doc = svc.getDocument(id);
            if (doc == null) {
                return ToolJsonUtils.error("Dokument nicht gefunden: " + id);
            }
            if (doc.getArchiveFileKey() != null && targetCaseId.trim().equals(doc.getArchiveFileKey().getId())) {
                return ToolJsonUtils.error("Das Dokument " + doc.getName() + " befindet sich bereits in der Zielakte. Verwende move_document_to_folder um es innerhalb der Akte zu verschieben.");
            }
            sources.add(doc);
        }

        // one server call: content, tags, metadata and relations within the set are copied,
        // the sources go to the recycle bin
        HashMap<String, String> names = new HashMap<>();
        for (ArchiveFileDocumentsBean doc : sources) {
            names.put(doc.getId(), rename ? newFileName.trim() : doc.getName());
        }
        String folderId = (targetFolderId != null && !targetFolderId.trim().isEmpty()) ? targetFolderId.trim() : null;
        HashMap<String, ArchiveFileDocumentsBean> moved = svc.moveDocumentsToCase(new ArrayList<>(documentIds), targetCaseId.trim(), folderId, names);

        StringBuilder docs = new StringBuilder();
        for (ArchiveFileDocumentsBean doc : sources) {
            ArchiveFileDocumentsBean newDoc = moved.get(doc.getId());
            if (newDoc == null) {
                continue;
            }
            EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
            ArchiveFileDocumentsBean updatedNewDoc = svc.getDocument(newDoc.getId());
            EventBroker.getInstance().publishEvent(new DocumentAddedEvent(updatedNewDoc != null ? updatedNewDoc : newDoc));
            if (docs.length() > 0) {
                docs.append(", ");
            }
            docs.append("{\"oldDocumentId\": \"").append(ToolJsonUtils.escapeJson(doc.getId()))
                    .append("\", \"newDocumentId\": \"").append(ToolJsonUtils.escapeJson(newDoc.getId()))
                    .append("\", \"fileName\": \"").append(ToolJsonUtils.escapeJson(newDoc.getName())).append("\"");
            if (newDoc.getParentId() != null) {
                docs.append(", \"parentId\": \"").append(ToolJsonUtils.escapeJson(newDoc.getParentId())).append("\"");
            }
            docs.append("}");
        }

        StringBuilder sb = new StringBuilder("{\"success\": true");
        sb.append(", \"targetCaseId\": \"").append(ToolJsonUtils.escapeJson(targetCaseId.trim())).append("\"");
        if (sources.size() == 1 && moved.get(sources.get(0).getId()) != null) {
            // kept for single documents, as before
            ArchiveFileDocumentsBean newDoc = moved.get(sources.get(0).getId());
            sb.append(", \"oldDocumentId\": \"").append(ToolJsonUtils.escapeJson(sources.get(0).getId())).append("\"");
            sb.append(", \"newDocumentId\": \"").append(ToolJsonUtils.escapeJson(newDoc.getId())).append("\"");
            sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(newDoc.getName())).append("\"");
        }
        sb.append(", \"documents\": [").append(docs).append("]}");
        return sb.toString();
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
        SimpleDateFormat weekdayFmt = new SimpleDateFormat("EEEE", java.util.Locale.GERMAN);
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
                        sb.append(", \"weekday\": \"").append(ToolJsonUtils.escapeJson(weekdayFmt.format(new Date(cursor)))).append("\"");
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
                    sb.append(", \"weekday\": \"").append(ToolJsonUtils.escapeJson(weekdayFmt.format(new Date(cursor)))).append("\"");
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
            if (fileNumber.equals(r.getFileNumber()) || fileNumber.equals(r.getFileNumberMain())) {
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
            if (fileNumber.equals(r.getFileNumber()) || fileNumber.equals(r.getFileNumberMain())) {
                caseBean = r;
                break;
            }
        }
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + fileNumber);
        }

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());
        Map<String, ArchiveFileDocumentsBean> byId = DocumentToolSupport.byId(allDocs);
        Map<String, Integer> attachmentCounts = DocumentToolSupport.countAttachments(allDocs);

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

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : pageDocs) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append(DocumentToolSupport.documentJson(doc, byId.get(doc.getParentId()), attachmentCounts.getOrDefault(doc.getId(), 0)));
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

        String dateField = (String) args.get("dateField");
        boolean byReceived = dateField != null && "received".equalsIgnoreCase(dateField.trim());
        if (dateField != null && !dateField.trim().isEmpty() && !byReceived && !"created".equalsIgnoreCase(dateField.trim())) {
            return ToolJsonUtils.error("Ungültiges dateField (erlaubt: created, received): " + dateField);
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean[] results = svc.searchEnhanced(fileNumber, false, new String[]{}, new String[]{});
        ArchiveFileBean caseBean = null;
        for (ArchiveFileBean r : results) {
            if (fileNumber.equals(r.getFileNumber()) || fileNumber.equals(r.getFileNumberMain())) {
                caseBean = r;
                break;
            }
        }
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + fileNumber);
        }

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());
        Map<String, ArchiveFileDocumentsBean> byId = DocumentToolSupport.byId(allDocs);
        Map<String, Integer> attachmentCounts = DocumentToolSupport.countAttachments(allDocs);

        // Filter: not deleted, creation or received date within range
        java.util.function.Function<ArchiveFileDocumentsBean, Date> dateOf = byReceived ? ArchiveFileDocumentsBean::getReceivedDate : ArchiveFileDocumentsBean::getCreationDate;
        List<ArchiveFileDocumentsBean> filteredDocs = new ArrayList<>();
        for (ArchiveFileDocumentsBean doc : allDocs) {
            if (doc.isDeleted()) {
                continue;
            }
            Date d = dateOf.apply(doc);
            if (d == null) {
                continue;
            }
            if (!d.before(fromDate) && !d.after(toDate)) {
                filteredDocs.add(doc);
            }
        }

        // Sort by the filtered date descending
        filteredDocs.sort((a, b) -> dateOf.apply(b).compareTo(dateOf.apply(a)));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"fromDate\": \"").append(ToolJsonUtils.escapeJson(fromDateStr.trim())).append("\"");
        sb.append(", \"toDate\": \"").append(ToolJsonUtils.escapeJson(toDateStr.trim())).append("\"");
        sb.append(", \"dateField\": \"").append(byReceived ? "received" : "created").append("\"");
        sb.append(", \"totalDocuments\": ").append(filteredDocs.size());
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : filteredDocs) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append(DocumentToolSupport.documentJson(doc, byId.get(doc.getParentId()), attachmentCounts.getOrDefault(doc.getId(), 0)));
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
            if (fileNumber.equals(r.getFileNumber()) || fileNumber.equals(r.getFileNumberMain())) {
                caseBean = r;
                break;
            }
        }
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + fileNumber);
        }

        Collection<ArchiveFileDocumentsBean> allDocs = svc.getDocuments(caseBean.getId());
        Map<String, ArchiveFileDocumentsBean> byId = DocumentToolSupport.byId(allDocs);
        Map<String, Integer> attachmentCounts = DocumentToolSupport.countAttachments(allDocs);

        // Filter deleted documents, match by file name, title, keywords and correspondent, sort by creation date descending
        List<ArchiveFileDocumentsBean> filteredDocs = new ArrayList<>();
        for (ArchiveFileDocumentsBean doc : allDocs) {
            if (!doc.isDeleted() && DocumentToolSupport.matches(doc, query)) {
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

        StringBuilder sb = new StringBuilder();
        sb.append("{\"fileNumber\": \"").append(ToolJsonUtils.escapeJson(fileNumber)).append("\"");
        sb.append(", \"query\": \"").append(ToolJsonUtils.escapeJson(query)).append("\"");
        sb.append(", \"documents\": [");
        int count = 0;
        for (ArchiveFileDocumentsBean doc : pageDocs) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append(DocumentToolSupport.documentJson(doc, byId.get(doc.getParentId()), attachmentCounts.getOrDefault(doc.getId(), 0)));
            count++;
        }
        sb.append("], \"totalDocuments\": ").append(totalDocuments);
        sb.append(", \"page\": ").append(page);
        sb.append(", \"totalPages\": ").append(totalPages);
        sb.append(", \"hasMore\": ").append(page < totalPages);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Describes the fields an update_document_metadata call changes, for the approval prompt.
     */
    private static String describeMetadataChange(JsonObject args) {
        List<String> parts = new ArrayList<>();
        if (args.containsKey("title")) {
            Object t = args.get("title");
            parts.add(t == null || t.toString().isBlank() ? "Bezeichnung entfernen" : "Bezeichnung '" + t + "'");
        }
        if (args.containsKey("keywords")) {
            String op = args.get("keywordOperation") == null ? "add" : args.get("keywordOperation").toString();
            parts.add("Schlagworte " + op + " '" + args.get("keywords") + "'");
        }
        if (args.containsKey("receivedDate")) {
            parts.add("Eingang '" + args.get("receivedDate") + "'");
        }
        if (args.containsKey("correspondentContactId") || args.containsKey("correspondentName") || args.containsKey("correspondentDirection")) {
            parts.add("Von/An");
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private String executeGetDocumentDetails(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();
        ArchiveFileDocumentsBean doc = svc.getDocument(documentId.trim());
        if (doc == null) {
            return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
        }

        Map<String, ArchiveFileDocumentsBean> byId = new HashMap<>();
        List<ArchiveFileDocumentsBean> attachments = new ArrayList<>();
        ArchiveFileBean caseBean = doc.getArchiveFileKey();
        if (caseBean != null) {
            Collection<ArchiveFileDocumentsBean> caseDocs = svc.getDocuments(caseBean.getId());
            byId = DocumentToolSupport.byId(caseDocs);
            for (ArchiveFileDocumentsBean d : byId.values()) {
                if (doc.getId().equals(d.getParentId())) {
                    attachments.add(d);
                }
            }
            attachments.sort((a, b) -> a.getDisplayTitle().compareToIgnoreCase(b.getDisplayTitle()));
        }
        ArchiveFileDocumentsBean parent = doc.getParentId() == null ? null : byId.get(doc.getParentId());

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
        // "name" next to "id" lets the chat panel offer to open the document
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        if (caseBean != null) {
            sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseBean.getId())).append("\"");
            sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        }
        sb.append(", \"size\": ").append(doc.getSize());
        if (doc.getCreationDate() != null) {
            sb.append(", \"creationDate\": \"").append(ToolJsonUtils.formatDate(doc.getCreationDate())).append("\"");
        }
        if (doc.getChangeDate() != null) {
            sb.append(", \"changeDate\": \"").append(ToolJsonUtils.formatDate(doc.getChangeDate())).append("\"");
        }
        if (doc.getFolder() != null) {
            sb.append(", \"folder\": \"").append(ToolJsonUtils.escapeJson(doc.getFolder().getName())).append("\"");
        }
        if (doc.isFavorite()) {
            sb.append(", \"favorite\": true");
        }
        if (doc.getDictateSign() != null && !doc.getDictateSign().isBlank()) {
            sb.append(", \"dictateSign\": \"").append(ToolJsonUtils.escapeJson(doc.getDictateSign())).append("\"");
        }
        if (doc.isDeleted()) {
            sb.append(", \"deleted\": true");
        }
        DocumentToolSupport.appendMetadata(sb, doc, parent, attachments.size());

        sb.append(", \"tags\": [");
        Collection<DocumentTagsBean> tags = svc.getDocumentTags(doc.getId());
        int i = 0;
        if (tags != null) {
            for (DocumentTagsBean t : tags) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append("{\"name\": \"").append(ToolJsonUtils.escapeJson(t.getTagName())).append("\"");
                if (t.getTagValue() != null && !t.getTagValue().isBlank()) {
                    sb.append(", \"value\": \"").append(ToolJsonUtils.escapeJson(t.getTagValue())).append("\"");
                }
                sb.append("}");
            }
        }
        sb.append("]");

        sb.append(", \"attachments\": [");
        for (int a = 0; a < attachments.size(); a++) {
            ArchiveFileDocumentsBean d = attachments.get(a);
            if (a > 0) {
                sb.append(", ");
            }
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(d.getId())).append("\"");
            sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(d.getName())).append("\"");
            if (d.getTitle() != null && !d.getTitle().isBlank()) {
                sb.append(", \"title\": \"").append(ToolJsonUtils.escapeJson(d.getTitle())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");

        sb.append(", \"messages\": [");
        List<InstantMessage> messages = locator.lookupMessagingServiceRemote().getMessagesForDocument(doc.getId());
        int m = 0;
        if (messages != null) {
            for (InstantMessage im : messages) {
                if (m++ > 0) {
                    sb.append(", ");
                }
                sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(im.getId())).append("\"");
                sb.append(", \"sender\": \"").append(ToolJsonUtils.escapeJson(im.getSender())).append("\"");
                if (im.getSent() != null) {
                    sb.append(", \"sent\": \"").append(ToolJsonUtils.formatDate(im.getSent())).append("\"");
                }
                sb.append(", \"content\": \"").append(ToolJsonUtils.escapeJson(im.getContent())).append("\"}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeUpdateDocumentMetadata(JsonObject args) throws Exception {
        List<String> ids = DocumentToolSupport.splitIds((String) args.get("documentIds"));
        if (ids.isEmpty()) {
            return ToolJsonUtils.error("Dokument-IDs (documentIds) fehlen");
        }
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote svc = locator.lookupArchiveFileServiceRemote();

        DocumentMetadataPatch patch = new DocumentMetadataPatch();
        try {
            if (args.containsKey("title")) {
                Object title = args.get("title");
                patch.setTitle(title == null ? null : title.toString());
            }
            if (args.containsKey("keywords")) {
                Object keywords = args.get("keywords");
                patch.setKeywords(DocumentToolSupport.parseKeywordOperation((String) args.get("keywordOperation")), keywords == null ? "" : keywords.toString());
            }
            if (args.containsKey("receivedDate")) {
                Object received = args.get("receivedDate");
                patch.setReceivedDate(DocumentToolSupport.parseDateTime(received == null ? null : received.toString()));
            }
            boolean hasContact = args.get("correspondentContactId") != null && !args.get("correspondentContactId").toString().isBlank();
            boolean hasName = args.get("correspondentName") != null && !args.get("correspondentName").toString().isBlank();
            if (hasContact || hasName || args.containsKey("correspondentDirection")) {
                int direction = DocumentToolSupport.parseDirection((String) args.get("correspondentDirection"));
                if (direction == ArchiveFileDocumentsBean.CORRESPONDENT_NONE) {
                    patch.setCorrespondent(null, null, ArchiveFileDocumentsBean.CORRESPONDENT_NONE);
                } else {
                    String contactId = hasContact ? args.get("correspondentContactId").toString().trim() : null;
                    String name = hasName ? args.get("correspondentName").toString().trim() : null;
                    if (contactId != null) {
                        AddressBean contact = locator.lookupAddressServiceRemote().getAddress(contactId);
                        if (contact == null) {
                            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactId);
                        }
                        if (name == null) {
                            name = contact.toDisplayName();
                        }
                    }
                    if (name == null) {
                        return ToolJsonUtils.error("Für Von/An ist correspondentContactId oder correspondentName nötig");
                    }
                    patch.setCorrespondent(contactId, name, direction);
                }
            }
        } catch (IllegalArgumentException iae) {
            return ToolJsonUtils.error(iae.getMessage());
        }
        if (patch.isEmpty()) {
            return ToolJsonUtils.error("Keine Änderung angegeben (title, keywords, receivedDate oder Von/An)");
        }

        List<ArchiveFileDocumentsBean> updated = svc.updateDocumentsMetadata(ids, patch);
        for (ArchiveFileDocumentsBean d : updated) {
            publishDocumentChanged(d);
        }

        StringBuilder sb = new StringBuilder("{\"success\": true, \"updatedDocuments\": ").append(updated.size());
        sb.append(", \"documents\": [");
        for (int i = 0; i < updated.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(DocumentToolSupport.documentJson(updated.get(i), null, 0));
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeSetDocumentParent(JsonObject args) throws Exception {
        String documentId = (String) args.get("documentId");
        if (documentId == null || documentId.trim().isEmpty()) {
            return ToolJsonUtils.error("Dokument-ID fehlt");
        }
        String parentId = (String) args.get("parentId");
        if (parentId != null && parentId.trim().isEmpty()) {
            parentId = null;
        }
        ArchiveFileServiceRemote svc = ToolJsonUtils.getLocator().lookupArchiveFileServiceRemote();
        ArchiveFileDocumentsBean updated;
        try {
            // the server refuses cycles, deleted parents and parents of other cases
            updated = svc.setDocumentParent(documentId.trim(), parentId == null ? null : parentId.trim());
        } catch (Exception ex) {
            return ToolJsonUtils.error("Zuordnung nicht möglich: " + ex.getMessage());
        }
        publishDocumentChanged(updated);
        StringBuilder sb = new StringBuilder("{\"success\": true, \"document\": ");
        sb.append(DocumentToolSupport.documentJson(updated, parentId == null ? null : svc.getDocument(parentId.trim()), 0));
        return sb.append("}").toString();
    }

    /**
     * Lets open case views show the changed document, the same way rename_document does.
     */
    private void publishDocumentChanged(ArchiveFileDocumentsBean doc) {
        if (doc == null) {
            return;
        }
        EventBroker.getInstance().publishEvent(new DocumentRemovedEvent(doc));
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(doc));
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

        final int HARD_CAP = 30000;
        int effectiveMax = HARD_CAP;
        if (args.get("maxChars") != null) {
            int maxChars = ((Number) args.get("maxChars")).intValue();
            if (maxChars <= 0) {
                return ToolJsonUtils.error("maxChars muss größer als 0 sein, angegeben: " + maxChars);
            }
            effectiveMax = Math.min(maxChars, HARD_CAP);
        }

        boolean truncated = false;
        if (text.length() > effectiveMax) {
            text = text.substring(0, effectiveMax);
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

    private String executeGetCaseLinks(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        NetworkToolSupport.Source source = NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote());
        return NetworkToolSupport.caseLinksJson(caseId, source.getCaseLinks(caseId));
    }

    private String executeGetContactRelations(JsonObject args) throws Exception {
        String contactId = (String) args.get("contactId");
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID fehlt");
        }
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        NetworkToolSupport.Source source = NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote());
        return NetworkToolSupport.relationsJson(contactId, source.getRelations(contactId));
    }

    private String executeGetCasesForContact(JsonObject args) throws Exception {
        String contactId = (String) args.get("contactId");
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID fehlt");
        }
        boolean includeArchived = booleanArg(args, "includeArchived", true);
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        NetworkToolSupport.Source source = NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote());
        return NetworkToolSupport.casesForContactJson(contactId, source.getCasesForContact(contactId), includeArchived);
    }

    private String executeGetCaseNetwork(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        int maxDepth = clampedIntArg(args, "maxDepth", 1, 1, 3);
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        return NetworkToolSupport.caseNetworkJson(NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote()), caseId, maxDepth);
    }

    private String executeGetContactNetwork(JsonObject args) throws Exception {
        String contactId = (String) args.get("contactId");
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID fehlt");
        }
        int maxDepth = clampedIntArg(args, "maxDepth", 1, 1, 2);
        boolean includeCases = booleanArg(args, "includeCases", false);
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        return NetworkToolSupport.contactNetworkJson(NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote()), contactId, maxDepth, includeCases);
    }

    private String executeFindPartyConnections(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID fehlt");
        }
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        return NetworkToolSupport.partyConnectionsJson(NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote()), caseId);
    }

    private String executeFindConnection(JsonObject args) throws Exception {
        String contactIdA = (String) args.get("contactIdA");
        String contactIdB = (String) args.get("contactIdB");
        if (contactIdA == null || contactIdA.trim().isEmpty() || contactIdB == null || contactIdB.trim().isEmpty()) {
            return ToolJsonUtils.error("Beide Kontakt-IDs (contactIdA, contactIdB) sind erforderlich");
        }
        int maxDepth = clampedIntArg(args, "maxDepth", 3, 1, 4);
        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        return NetworkToolSupport.connectionJson(NetworkToolSupport.remoteSource(locator.lookupArchiveFileServiceRemote(), locator.lookupAddressServiceRemote()), contactIdA.trim(), contactIdB.trim(), maxDepth);
    }

    private static boolean booleanArg(JsonObject args, String name, boolean defaultValue) {
        Object value = args.get(name);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.toString().trim());
    }

    private static int clampedIntArg(JsonObject args, String name, int defaultValue, int min, int max) {
        Integer value = ToolJsonUtils.toInteger(args.get(name));
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
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
                if (addr.getStreetNumber() != null && !addr.getStreetNumber().isEmpty()) {
                    sb.append(", \"streetNumber\": \"").append(ToolJsonUtils.escapeJson(addr.getStreetNumber())).append("\"");
                }
                if (addr.getZipCode() != null && !addr.getZipCode().isEmpty()) {
                    sb.append(", \"zipCode\": \"").append(ToolJsonUtils.escapeJson(addr.getZipCode())).append("\"");
                }
                if (addr.getEmail() != null && !addr.getEmail().isEmpty()) {
                    sb.append(", \"email\": \"").append(ToolJsonUtils.escapeJson(addr.getEmail())).append("\"");
                }
                if (addr.getEmailHome() != null && !addr.getEmailHome().isEmpty()) {
                    sb.append(", \"emailHome\": \"").append(ToolJsonUtils.escapeJson(addr.getEmailHome())).append("\"");
                }
                if (addr.getEmailMisc() != null && !addr.getEmailMisc().isEmpty()) {
                    sb.append(", \"emailMisc\": \"").append(ToolJsonUtils.escapeJson(addr.getEmailMisc())).append("\"");
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

        String assigneeFilter = (String) args.get("assignee");

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
            if (assigneeFilter != null && !assigneeFilter.trim().isEmpty() && !assigneeFilter.trim().equalsIgnoreCase(ev.getAssignee())) {
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

        String assigneeFilter = (String) args.get("assignee");

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
            if (assigneeFilter != null && !assigneeFilter.trim().isEmpty() && !assigneeFilter.trim().equalsIgnoreCase(ev.getAssignee())) {
                continue;
            }
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

    /**
     * Validates the optional reminderMinutes argument of create_event and
     * update_event. Allowed are -1 (no reminder) and 0 to 1440 minutes - 1440
     * is the maximum because ReminderNotificationTimerTask only looks ahead 24
     * hours.
     *
     * @param rawValue the raw argument as delivered by the AI backend, null if absent
     * @param parsedValue the result of ToolJsonUtils.toInteger(rawValue)
     * @return an error JSON string if the value is present but invalid, null otherwise
     */
    private String validateReminderMinutes(Object rawValue, Integer parsedValue) {
        if (rawValue == null) {
            return null;
        }
        if (parsedValue == null) {
            if (rawValue.toString().trim().isEmpty()) {
                // leerer String wird wie "nicht angegeben" behandelt
                return null;
            }
            return ToolJsonUtils.error("Ungültige Erinnerung: " + rawValue + ". Erlaubt: -1 (keine Erinnerung) oder 0 bis 1440 Minuten");
        }
        if (parsedValue < -1 || parsedValue > 1440) {
            return ToolJsonUtils.error("Ungültige Erinnerung: " + parsedValue + ". Erlaubt: -1 (keine Erinnerung) oder 0 bis 1440 Minuten");
        }
        return null;
    }

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
        Object reminderArg = args.get("reminderMinutes");
        Integer reminderMinutes = ToolJsonUtils.toInteger(reminderArg);

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

        String reminderError = validateReminderMinutes(reminderArg, reminderMinutes);
        if (reminderError != null) {
            return reminderError;
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
        // Erinnerungen sind nur fuer den Typ Termin wirksam - bei Wiedervorlage
        // und Frist wird der Wert still ignoriert (wie in der Oberflaeche auch)
        if (reminderMinutes != null && eventType == EventTypes.EVENTTYPE_EVENT) {
            review.setReminderMinutes(reminderMinutes);
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
        sb.append(", \"reminderMinutes\": ").append(created.getReminderMinutes());
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String executeUpdateEvent(JsonObject args) throws Exception {
        String eventId = (String) args.get("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            return ToolJsonUtils.error("Ereignis-ID fehlt");
        }

        String summary = (String) args.get("summary");
        String beginDateStr = (String) args.get("beginDate");
        String endDateStr = (String) args.get("endDate");
        String assignee = (String) args.get("assignee");
        String description = (String) args.get("description");
        String location = (String) args.get("location");
        Object reminderArg = args.get("reminderMinutes");
        Integer reminderMinutes = ToolJsonUtils.toInteger(reminderArg);

        // Mindestens ein Feld muss angegeben sein
        boolean hasUpdate = (summary != null && !summary.trim().isEmpty())
                || (beginDateStr != null && !beginDateStr.trim().isEmpty())
                || (endDateStr != null && !endDateStr.trim().isEmpty())
                || (assignee != null && !assignee.trim().isEmpty())
                || (description != null)
                || (location != null)
                || (reminderMinutes != null);
        if (!hasUpdate) {
            return ToolJsonUtils.error("Mindestens ein zu änderndes Feld muss angegeben werden (summary, beginDate, endDate, assignee, description, location, reminderMinutes)");
        }

        String reminderError = validateReminderMinutes(reminderArg, reminderMinutes);
        if (reminderError != null) {
            return reminderError;
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        CalendarServiceRemote calSvc = locator.lookupCalendarServiceRemote();

        // Bestehendes Event laden
        ArchiveFileReviewsBean review = calSvc.getReview(eventId);
        if (review == null) {
            return ToolJsonUtils.error("Ereignis nicht gefunden: " + eventId);
        }

        // Alte Daten merken fuer ReviewUpdatedEvent
        Date oldBeginDate = review.getBeginDate();
        Date oldEndDate = review.getEndDate();

        // Felder aktualisieren
        if (summary != null && !summary.trim().isEmpty()) {
            review.setSummary(summary.trim());
        }
        if (beginDateStr != null && !beginDateStr.trim().isEmpty()) {
            Date beginDate = ToolJsonUtils.parseIsoDate(beginDateStr);
            if (beginDate == null) {
                return ToolJsonUtils.error("Startdatum konnte nicht geparst werden: " + beginDateStr);
            }
            review.setBeginDate(beginDate);
        }
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            Date endDate = ToolJsonUtils.parseIsoDate(endDateStr);
            if (endDate == null) {
                return ToolJsonUtils.error("Enddatum konnte nicht geparst werden: " + endDateStr);
            }
            review.setEndDate(endDate);
        }
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
            review.setAssignee(assignee);
        }
        if (description != null) {
            review.setDescription(description.trim());
        }
        if (location != null) {
            review.setLocation(location.trim());
        }
        // Erinnerungen sind nur fuer den Typ Termin wirksam - bei Wiedervorlage
        // und Frist wird der Wert still ignoriert (wie in der Oberflaeche auch)
        if (reminderMinutes != null && review.hasEndDateAndTime()) {
            review.setReminderMinutes(reminderMinutes);
        }

        String archiveFileId = review.getArchiveFileKey().getId();
        ArchiveFileReviewsBean updated = calSvc.updateReview(archiveFileId, review);
        EventBroker.getInstance().publishEvent(new ReviewUpdatedEvent(oldBeginDate, oldEndDate, updated));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"event\": ");
        appendEventJson(sb, updated);
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

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setTitle((String) args.get("title"));
        metadata.setKeywords((String) args.get("keywords"));
        ArchiveFileDocumentsBean doc = svc.addDocument(caseId, fileName, html.getBytes("UTF-8"), null, null, metadata);
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(doc));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
        sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(doc.getName())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        DocumentToolSupport.appendMetadata(sb, doc, null, 0);
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
        EventBroker.getInstance().publishEvent(new PartyAddedEvent(created, contact));
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

        ArchiveFileDocumentsBean documentContext = null;
        String documentId = (String) args.get("documentId");
        if (documentId != null && !documentId.trim().isEmpty()) {
            documentContext = svc.getDocument(documentId.trim());
            if (documentContext == null || documentContext.isDeleted()) {
                return ToolJsonUtils.error("Dokument nicht gefunden: " + documentId);
            }
            if (documentContext.getArchiveFileKey() == null || !caseBean.getId().equals(documentContext.getArchiveFileKey().getId())) {
                return ToolJsonUtils.error("Das Dokument " + documentContext.getName() + " gehört nicht zur Akte " + caseBean.getFileNumber());
            }
        }

        String currentUser = UserSettings.getInstance().getCurrentUser().getPrincipalId();

        InstantMessage msg = new InstantMessage();
        msg.setCaseContext(caseBean);
        if (documentContext != null) {
            msg.setDocumentContext(documentContext);
        }
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
        if (documentContext != null) {
            sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(documentContext.getId())).append("\"");
            sb.append(", \"documentName\": \"").append(ToolJsonUtils.escapeJson(documentContext.getName())).append("\"");
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

        // truncate to database column sizes
        name = name.trim();
        if (name.length() > 250) {
            name = name.substring(0, 250);
        }
        if (reason != null) {
            reason = reason.trim();
            if (reason.length() > 250) {
                reason = reason.substring(0, 250);
            }
        }
        if (subjectField != null) {
            subjectField = subjectField.trim();
            if (subjectField.length() > 100) {
                subjectField = subjectField.substring(0, 100);
            }
        }
        if (notice != null) {
            notice = notice.trim();
            if (notice.length() > 2500) {
                notice = notice.substring(0, 2500);
            }
        }

        ArchiveFileBean dto = new ArchiveFileBean();
        dto.setName(name);
        if (reason != null && !reason.isEmpty()) {
            dto.setReason(reason);
        }
        if (subjectField != null && !subjectField.isEmpty()) {
            dto.setSubjectField(subjectField);
        }
        if (lawyer != null && !lawyer.trim().isEmpty()) {
            dto.setLawyer(lawyer);
        }
        if (assistant != null && !assistant.trim().isEmpty()) {
            dto.setAssistant(assistant);
        }
        if (notice != null && !notice.isEmpty()) {
            dto.setNotice(notice);
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

    private String executeListLetterHeads(JsonObject args) throws Exception {
        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        List<String> heads = sys.getTemplatesInFolder(SystemManagementRemote.TEMPLATE_TYPE_HEAD, new GenericNode(null, null, "/"));

        StringBuilder sb = new StringBuilder();
        sb.append("{\"letterHeads\": [");
        if (heads != null) {
            for (int i = 0; i < heads.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(ToolJsonUtils.escapeJson(heads.get(i))).append("\"");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeCreateDocumentFromTemplate(JsonObject args) throws Exception {
        String caseId = (String) args.get("caseId");
        String templateFolder = (String) args.get("templateFolder");
        String templateName = (String) args.get("templateName");
        String fileName = (String) args.get("fileName");
        String generatedText = (String) args.get("generatedText");
        String letterHead = (String) args.get("letterHead");

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

        // Validate letterHead if provided
        if (letterHead != null && !letterHead.trim().isEmpty()) {
            letterHead = letterHead.trim();
            List<String> availableHeads = sys.getTemplatesInFolder(SystemManagementRemote.TEMPLATE_TYPE_HEAD, new GenericNode(null, null, "/"));
            boolean headFound = false;
            if (availableHeads != null) {
                for (String h : availableHeads) {
                    if (h.equals(letterHead)) {
                        headFound = true;
                        break;
                    }
                }
            }
            if (!headFound) {
                StringBuilder availHeads = new StringBuilder();
                if (availableHeads != null) {
                    for (int i = 0; i < availableHeads.size(); i++) {
                        if (i > 0) availHeads.append(", ");
                        availHeads.append(availableHeads.get(i));
                    }
                }
                return ToolJsonUtils.error("Briefkopf '" + letterHead + "' nicht gefunden. Verfügbare Briefköpfe: " + availHeads.toString());
            }
        } else {
            letterHead = null;
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
                caseId.trim(), fileName.trim(), letterHead, folderNode, templateName.trim(), phMap, "", null);

        // title and keywords are set in a second step - documents from templates have no
        // variant with metadata
        String title = (String) args.get("title");
        String keywords = (String) args.get("keywords");
        boolean hasTitle = title != null && !title.trim().isEmpty();
        boolean hasKeywords = keywords != null && !keywords.trim().isEmpty();
        if (hasTitle || hasKeywords) {
            DocumentMetadataPatch patch = new DocumentMetadataPatch();
            if (hasTitle) {
                patch.setTitle(title.trim());
            }
            if (hasKeywords) {
                patch.setKeywords(DocumentMetadataPatch.KeywordOperation.SET, keywords);
            }
            List<ArchiveFileDocumentsBean> updated = archiveSvc.updateDocumentsMetadata(Arrays.asList(newDoc.getId()), patch);
            if (updated != null && !updated.isEmpty()) {
                newDoc = updated.get(0);
            }
        }

        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(newDoc));

        // Build response
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(newDoc.getId())).append("\"");
        sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(newDoc.getName())).append("\"");
        sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId.trim())).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        DocumentToolSupport.appendMetadata(sb, newDoc, null, 0);
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

    private String executeListContactTags(JsonObject args) throws Exception {
        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();

        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_ADDRESSTAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_ADDRESSTAGS_MV_PREFIX);

        return buildTagListJson(boolTags, mvGroups, OptionConstants.OPTIONGROUP_ADDRESSTAGS_MV_PREFIX);
    }

    private String executeSetContactTag(JsonObject args) throws Exception {
        String contactId = (String) args.get("contactId");
        String tagName = (String) args.get("tagName");
        String tagValue = (String) args.get("tagValue");
        String activeStr = (String) args.get("active");
        if (contactId == null || contactId.trim().isEmpty()) {
            return ToolJsonUtils.error("Kontakt-ID (contactId) fehlt");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            return ToolJsonUtils.error("Etikett-Name (tagName) fehlt");
        }
        boolean active = (activeStr == null || !"false".equalsIgnoreCase(activeStr.trim()));

        SystemManagementRemote sys = ToolJsonUtils.getLocator().lookupSystemManagementRemote();
        AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_ADDRESSTAGS);
        HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_ADDRESSTAGS_MV_PREFIX);

        String validationError = validateTag(tagName.trim(), tagValue, boolTags, mvGroups, OptionConstants.OPTIONGROUP_ADDRESSTAGS_MV_PREFIX);
        if (validationError != null) {
            return validationError;
        }

        AddressServiceRemote addrSvc = ToolJsonUtils.getLocator().lookupAddressServiceRemote();

        AddressBean contact = addrSvc.getAddress(contactId.trim());
        if (contact == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactId);
        }

        AddressTagsBean tag = new AddressTagsBean();
        tag.setTagName(tagName.trim());
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            tag.setTagValue(tagValue.trim());
        }

        addrSvc.setTag(contactId.trim(), tag, active);

        EventBroker.getInstance().publishEvent(new ContactUpdatedEvent(contact));

        StringBuilder sbResult = new StringBuilder();
        sbResult.append("{\"success\": true, \"contactId\": \"").append(ToolJsonUtils.escapeJson(contactId.trim()));
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
        sb.append(", \"reminderMinutes\": ").append(ev.getReminderMinutes());
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
        if (a.getStreetNumber() != null && !a.getStreetNumber().isEmpty()) {
            sb.append(", \"streetNumber\": \"").append(ToolJsonUtils.escapeJson(a.getStreetNumber())).append("\"");
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
        if (a.getEmailHome() != null && !a.getEmailHome().isEmpty()) {
            sb.append(", \"emailHome\": \"").append(ToolJsonUtils.escapeJson(a.getEmailHome())).append("\"");
        }
        if (a.getEmailMisc() != null && !a.getEmailMisc().isEmpty()) {
            sb.append(", \"emailMisc\": \"").append(ToolJsonUtils.escapeJson(a.getEmailMisc())).append("\"");
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

    // =========================================================================
    // E-Mail tool implementations
    // =========================================================================

    private String executeListMailboxes(JsonObject args) throws Exception {
        List<MailboxSetup> mailboxes = getAccessibleMailboxes();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"mailboxes\": [");
        for (int i = 0; i < mailboxes.size(); i++) {
            MailboxSetup mb = mailboxes.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("{\"id\": \"").append(ToolJsonUtils.escapeJson(mb.getId())).append("\"");
            sb.append(", \"displayName\": \"").append(ToolJsonUtils.escapeJson(mb.getDisplayName())).append("\"");
            sb.append(", \"emailAddress\": \"").append(ToolJsonUtils.escapeJson(mb.getEmailAddress())).append("\"");
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeSearchEmails(JsonObject args) throws Exception {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }
        query = query.trim();

        int maxResults = MAX_MAIL_RESULTS;
        Integer requestedMax = ToolJsonUtils.toInteger(args.get("maxResults"));
        if (requestedMax != null) {
            if (requestedMax <= 0) {
                return ToolJsonUtils.error("maxResults muss größer als 0 sein, angegeben: " + requestedMax);
            }
            maxResults = Math.min(requestedMax, MAX_MAIL_RESULTS);
        }

        String fromDateStr = (String) args.get("fromDate");
        Date fromDate = null;
        if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
            fromDate = ToolJsonUtils.parseIsoDate(fromDateStr);
            if (fromDate == null) {
                return ToolJsonUtils.error("fromDate konnte nicht geparst werden: " + fromDateStr);
            }
        }

        boolean unreadOnly = args.get("unreadOnly") != null
                && Boolean.parseBoolean(String.valueOf(args.get("unreadOnly")).trim());
        String folderName = (String) args.get("folder");
        boolean allFolders = "all".equalsIgnoreCase(String.valueOf(args.getOrDefault("scope", "inbox")).trim());

        List<MailboxSetup> mailboxes;
        String mailboxId = (String) args.get("mailboxId");
        if (mailboxId != null && !mailboxId.trim().isEmpty()) {
            MailboxSetup mailbox = findAccessibleMailbox(mailboxId.trim());
            if (mailbox == null) {
                return ToolJsonUtils.error("Postfach nicht gefunden oder kein Zugriff: " + mailboxId);
            }
            mailboxes = Arrays.asList(mailbox);
        } else {
            mailboxes = getAccessibleMailboxes();
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        EmailServiceRemote svc = locator.lookupEmailServiceRemote();

        List<MailHit> hits = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (mailboxes.isEmpty()) {
            warnings.add("Für den angemeldeten Benutzer ist kein E-Mail-Postfach freigeschaltet.");
        }
        int searchedFolders = 0;
        boolean foldersTruncated = false;

        for (MailboxSetup mb : mailboxes) {
            List<MailFolderDTO> folders;
            try {
                folders = svc.listFolders(mb.getId());
            } catch (Exception ex) {
                log.warn("Unable to list folders of mailbox " + mb.getId(), ex);
                warnings.add("Ordner des Postfachs " + mb.getEmailAddress() + " konnten nicht gelesen werden: " + ex.getMessage());
                continue;
            }

            List<MailFolderDTO> selected = selectSearchFolders(folders, folderName, allFolders);
            if (selected.isEmpty()) {
                warnings.add("Im Postfach " + mb.getEmailAddress() + " wurde kein passender Ordner gefunden.");
                continue;
            }
            if (selected.size() > MAX_SEARCH_FOLDERS) {
                selected = selected.subList(0, MAX_SEARCH_FOLDERS);
                foldersTruncated = true;
            }

            for (MailFolderDTO folder : selected) {
                try {
                    List<MailMessageDTO> messages = svc.listMessages(mb.getId(), folder.getFolderId(), maxResults, 0, fromDate, unreadOnly, query);
                    searchedFolders++;
                    if (messages == null) {
                        continue;
                    }
                    for (MailMessageDTO m : messages) {
                        hits.add(new MailHit(mb, folder.getDisplayName(), m));
                    }
                } catch (Exception ex) {
                    // a single unreachable folder must not abort the entire search
                    log.warn("Unable to search folder " + folder.getDisplayName() + " of mailbox " + mb.getId(), ex);
                    warnings.add("Ordner '" + folder.getDisplayName() + "' im Postfach " + mb.getEmailAddress() + " konnte nicht durchsucht werden: " + ex.getMessage());
                }
            }
        }

        hits.sort((h1, h2) -> {
            Date d1 = h1.message.getDate();
            Date d2 = h2.message.getDate();
            if (d1 == null && d2 == null) {
                return 0;
            }
            if (d1 == null) {
                return 1;
            }
            if (d2 == null) {
                return -1;
            }
            return d2.compareTo(d1);
        });

        int total = hits.size();
        int limit = Math.min(total, maxResults);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"results\": [");
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }
            MailHit hit = hits.get(i);
            MailMessageDTO m = hit.message;
            sb.append("{\"mailboxId\": \"").append(ToolJsonUtils.escapeJson(hit.mailbox.getId())).append("\"");
            sb.append(", \"mailbox\": \"").append(ToolJsonUtils.escapeJson(hit.mailbox.getEmailAddress())).append("\"");
            sb.append(", \"folder\": \"").append(ToolJsonUtils.escapeJson(hit.folderName)).append("\"");
            sb.append(", \"messageRef\": \"").append(ToolJsonUtils.escapeJson(m.getMessageRef())).append("\"");
            sb.append(", \"subject\": \"").append(ToolJsonUtils.escapeJson(m.getSubject())).append("\"");
            sb.append(", \"from\": \"").append(ToolJsonUtils.escapeJson(m.getFrom())).append("\"");
            appendStringArrayJson(sb, "to", m.getTo());
            if (m.getDate() != null) {
                sb.append(", \"date\": \"").append(ToolJsonUtils.formatDate(m.getDate())).append("\"");
            }
            sb.append(", \"read\": ").append(m.isRead());
            sb.append(", \"hasAttachments\": ").append(m.isHasAttachments());
            sb.append("}");
        }
        sb.append("], \"totalResults\": ").append(total);
        sb.append(", \"searchedFolders\": ").append(searchedFolders);
        if (total > limit) {
            sb.append(", \"truncated\": true");
        }
        if (foldersTruncated) {
            sb.append(", \"foldersTruncated\": true");
        }
        if (!warnings.isEmpty()) {
            sb.append(", \"warnings\": [");
            for (int i = 0; i < warnings.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(ToolJsonUtils.escapeJson(warnings.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String executeGetEmail(JsonObject args) throws Exception {
        String mailboxId = (String) args.get("mailboxId");
        if (mailboxId == null || mailboxId.trim().isEmpty()) {
            return ToolJsonUtils.error("Postfach-ID fehlt");
        }
        String messageRef = (String) args.get("messageRef");
        if (messageRef == null || messageRef.trim().isEmpty()) {
            return ToolJsonUtils.error("Nachrichtenreferenz fehlt");
        }

        MailboxSetup mailbox = findAccessibleMailbox(mailboxId.trim());
        if (mailbox == null) {
            return ToolJsonUtils.error("Postfach nicht gefunden oder kein Zugriff: " + mailboxId);
        }

        int effectiveMax = MAX_MAIL_BODY_CHARS;
        Integer maxChars = ToolJsonUtils.toInteger(args.get("maxChars"));
        if (maxChars != null) {
            if (maxChars <= 0) {
                return ToolJsonUtils.error("maxChars muss größer als 0 sein, angegeben: " + maxChars);
            }
            effectiveMax = Math.min(maxChars, MAX_MAIL_BODY_CHARS);
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        EmailServiceRemote svc = locator.lookupEmailServiceRemote();
        MailMessageDTO m = svc.getMessage(mailbox.getId(), messageRef.trim(), true);
        if (m == null) {
            return ToolJsonUtils.error("Nachricht nicht gefunden: " + messageRef);
        }

        String body = m.getBody();
        if (body == null) {
            body = "";
        } else if (m.getBodyContentType() != null && m.getBodyContentType().toLowerCase().contains("html")) {
            body = CommonMailUtils.html2Text(body);
        }
        boolean truncated = false;
        if (body.length() > effectiveMax) {
            body = body.substring(0, effectiveMax);
            truncated = true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"mailboxId\": \"").append(ToolJsonUtils.escapeJson(mailbox.getId())).append("\"");
        sb.append(", \"messageRef\": \"").append(ToolJsonUtils.escapeJson(m.getMessageRef())).append("\"");
        sb.append(", \"subject\": \"").append(ToolJsonUtils.escapeJson(m.getSubject())).append("\"");
        sb.append(", \"from\": \"").append(ToolJsonUtils.escapeJson(m.getFrom())).append("\"");
        appendStringArrayJson(sb, "to", m.getTo());
        appendStringArrayJson(sb, "cc", m.getCc());
        if (m.getDate() != null) {
            sb.append(", \"date\": \"").append(ToolJsonUtils.formatDate(m.getDate())).append("\"");
        }
        sb.append(", \"read\": ").append(m.isRead());
        sb.append(", \"hasAttachments\": ").append(m.isHasAttachments());
        sb.append(", \"body\": \"").append(ToolJsonUtils.escapeJson(body)).append("\"");
        if (truncated) {
            sb.append(", \"truncated\": true");
        }
        sb.append(", \"attachments\": [");
        List<MailAttachmentDTO> attachments = m.getAttachments();
        if (attachments != null) {
            for (int i = 0; i < attachments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                MailAttachmentDTO a = attachments.get(i);
                sb.append("{\"attachmentId\": \"").append(ToolJsonUtils.escapeJson(a.getAttachmentId())).append("\"");
                sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(a.getName())).append("\"");
                sb.append(", \"contentType\": \"").append(ToolJsonUtils.escapeJson(a.getContentType())).append("\"");
                sb.append(", \"size\": ").append(a.getSize());
                sb.append("}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String executeSaveEmailToCase(JsonObject args) throws Exception {
        String mailboxId = (String) args.get("mailboxId");
        if (mailboxId == null || mailboxId.trim().isEmpty()) {
            return ToolJsonUtils.error("Postfach-ID fehlt");
        }
        String messageRef = (String) args.get("messageRef");
        if (messageRef == null || messageRef.trim().isEmpty()) {
            return ToolJsonUtils.error("Nachrichtenreferenz fehlt");
        }
        String caseId = (String) args.get("caseId");
        if (caseId == null || caseId.trim().isEmpty()) {
            return ToolJsonUtils.error("Akten-ID (caseId) fehlt");
        }
        mailboxId = mailboxId.trim();
        messageRef = messageRef.trim();
        caseId = caseId.trim();

        MailboxSetup mailbox = findAccessibleMailbox(mailboxId);
        if (mailbox == null) {
            return ToolJsonUtils.error("Postfach nicht gefunden oder kein Zugriff: " + mailboxId);
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        ArchiveFileServiceRemote archiveSvc = locator.lookupArchiveFileServiceRemote();

        ArchiveFileBean caseBean = archiveSvc.getArchiveFile(caseId);
        if (caseBean == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }
        if (caseBean.isArchived()) {
            return ToolJsonUtils.error("Akte ist archiviert (abgelegt). Bitte die Akte zuerst reaktivieren.");
        }

        // validate the tags before anything is written, so that a typo does not
        // leave a stored document behind with a failed tag call
        List<String> tagNames = new ArrayList<>();
        String tagsArg = (String) args.get("tags");
        if (tagsArg != null && !tagsArg.trim().isEmpty()) {
            SystemManagementRemote sys = locator.lookupSystemManagementRemote();
            AppOptionGroupBean[] boolTags = sys.getOptionGroup(OptionConstants.OPTIONGROUP_DOCUMENTTAGS);
            HashMap<String, AppOptionGroupBean[]> mvGroups = sys.getOptionGroupsByPrefix(OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);
            for (String tagName : tagsArg.split(",")) {
                tagName = tagName.trim();
                if (tagName.isEmpty()) {
                    continue;
                }
                String validationError = validateTag(tagName, null, boolTags, mvGroups, OptionConstants.OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX);
                if (validationError != null) {
                    return validationError;
                }
                tagNames.add(tagName);
            }
        }

        EmailServiceRemote emailSvc = locator.lookupEmailServiceRemote();
        MailMessageDTO message = emailSvc.getMessage(mailboxId, messageRef);
        if (message == null) {
            return ToolJsonUtils.error("Nachricht nicht gefunden: " + messageRef);
        }
        byte[] eml = emailSvc.getMessageAsEml(mailboxId, messageRef);
        if (eml == null || eml.length == 0) {
            return ToolJsonUtils.error("E-Mail konnte nicht als .eml gelesen werden: " + messageRef);
        }

        String rawName = (String) args.get("fileName");
        if (rawName == null || rawName.trim().isEmpty()) {
            rawName = message.getSubject();
        }
        if (rawName == null || rawName.trim().isEmpty()) {
            rawName = "E-Mail ohne Betreff";
        }
        Date documentDate = message.getDate() != null ? message.getDate() : new Date();
        String docName = buildEmlDocumentName(caseBean, rawName, documentDate);

        // resolve naming clashes the same way the mailbox scanner does
        String uniqueName = docName;
        int index = 2;
        while (archiveSvc.doesDocumentExist(caseId, uniqueName) && index <= MAX_NAME_CLASH_RETRIES) {
            uniqueName = appendNameIndex(docName, index);
            index++;
        }
        if (archiveSvc.doesDocumentExist(caseId, uniqueName)) {
            return ToolJsonUtils.error("Es existiert bereits ein Dokument mit dem Namen " + docName
                    + " in der Akte " + caseBean.getFileNumber() + " - bitte einen abweichenden fileName angeben.");
        }

        // title, received date and sender / recipient like saving from the client
        DocumentOrigin origin = MailDocumentOrigins.fromMessage(message.getSubject(), message.getDate(), message.getFrom(), message.getTo());
        Map<String, DocumentMetadata> correspondentCache = DocumentOrigin.newCache();
        DocumentMetadata metadata = origin.toMetadata(archiveSvc, caseId, correspondentCache);
        String keywordsArg = (String) args.get("keywords");
        if (keywordsArg != null && !keywordsArg.trim().isEmpty()) {
            metadata.setKeywords(keywordsArg);
        }

        ArchiveFileDocumentsBean doc;
        try {
            doc = archiveSvc.addDocument(caseId, uniqueName, eml, "", null, metadata);
        } catch (Exception ex) {
            // doesDocumentExist ignores the recycle bin while the server side check
            // of addDocument does not, so a clash can still surface here
            log.warn("Unable to store email " + messageRef + " in case " + caseId, ex);
            return ToolJsonUtils.error("Dokument konnte nicht in der Akte angelegt werden: " + ex.getMessage());
        }

        String folderId = (String) args.get("folderId");
        boolean hasFolder = folderId != null && !folderId.trim().isEmpty();
        if (hasFolder) {
            archiveSvc.moveDocumentsToFolder(Arrays.asList(doc.getId()), folderId.trim());
        }

        for (String tagName : tagNames) {
            DocumentTagsBean tag = new DocumentTagsBean();
            tag.setTagName(tagName);
            archiveSvc.setDocumentTag(doc.getId(), tag, true);
        }

        // re-read the document, the bean returned by addDocument does not know
        // about the folder the document was just moved into
        ArchiveFileDocumentsBean storedDoc = archiveSvc.getDocument(doc.getId());
        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(storedDoc != null ? storedDoc : doc));

        // attachments as documents of their own, linked to the e-mail document
        List<ArchiveFileDocumentsBean> savedAttachments = new ArrayList<>();
        List<String> failedAttachments = new ArrayList<>();
        if ("true".equalsIgnoreCase(String.valueOf(args.get("saveAttachments")).trim())) {
            MailMessageDTO withAttachments = emailSvc.getMessage(mailboxId, messageRef, true);
            List<MailAttachmentDTO> attachments = withAttachments == null ? null : withAttachments.getAttachments();
            if (attachments != null) {
                DocumentMetadata attachmentMetadata = origin.withoutTitle().toMetadata(archiveSvc, caseId, correspondentCache);
                attachmentMetadata.setParentId(doc.getId());
                for (MailAttachmentDTO a : attachments) {
                    // only embedded images (inline with a Content-ID) are skipped - an inline
                    // disposition alone does not make a part part of the body
                    if (a.isInline() && a.getContentId() != null && !a.getContentId().trim().isEmpty()) {
                        continue;
                    }
                    String attachmentName = FileUtils.sanitizeFileName(a.getName() == null || a.getName().isBlank() ? "Anhang" : a.getName());
                    try {
                        MailAttachmentDTO full = emailSvc.getAttachmentContent(mailboxId, messageRef, a.getAttachmentId());
                        if (full == null || full.getContent() == null) {
                            failedAttachments.add(attachmentName);
                            continue;
                        }
                        String uniqueAttachmentName = limitDocumentNameLength(attachmentName);
                        int attachmentIndex = 2;
                        while (archiveSvc.doesDocumentExist(caseId, uniqueAttachmentName) && attachmentIndex <= MAX_NAME_CLASH_RETRIES) {
                            uniqueAttachmentName = appendNameIndex(limitDocumentNameLength(attachmentName), attachmentIndex);
                            attachmentIndex++;
                        }
                        ArchiveFileDocumentsBean attachmentDoc = archiveSvc.addDocument(caseId, uniqueAttachmentName, full.getContent(), "", null, attachmentMetadata);
                        if (hasFolder) {
                            archiveSvc.moveDocumentsToFolder(Arrays.asList(attachmentDoc.getId()), folderId.trim());
                        }
                        ArchiveFileDocumentsBean storedAttachment = archiveSvc.getDocument(attachmentDoc.getId());
                        savedAttachments.add(storedAttachment != null ? storedAttachment : attachmentDoc);
                        EventBroker.getInstance().publishEvent(new DocumentAddedEvent(storedAttachment != null ? storedAttachment : attachmentDoc));
                    } catch (Exception ex) {
                        log.warn("Unable to store attachment " + attachmentName + " of email " + messageRef + " in case " + caseId, ex);
                        failedAttachments.add(attachmentName);
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true");
        sb.append(", \"documentId\": \"").append(ToolJsonUtils.escapeJson(doc.getId())).append("\"");
        // "name" next to "documentId" is what makes the chat panel offer the
        // "open document" button, see AssistantChatPanel.extractDocumentReferences
        sb.append(", \"name\": \"").append(ToolJsonUtils.escapeJson(uniqueName)).append("\"");
        sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(uniqueName)).append("\"");
        sb.append(", \"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\"");
        sb.append(", \"caseFileNumber\": \"").append(ToolJsonUtils.escapeJson(caseBean.getFileNumber())).append("\"");
        if (hasFolder) {
            sb.append(", \"folderId\": \"").append(ToolJsonUtils.escapeJson(folderId.trim())).append("\"");
        }
        if (!tagNames.isEmpty()) {
            sb.append(", \"tags\": [");
            for (int i = 0; i < tagNames.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(ToolJsonUtils.escapeJson(tagNames.get(i))).append("\"");
            }
            sb.append("]");
        }
        DocumentToolSupport.appendMetadata(sb, storedDoc != null ? storedDoc : doc, null, savedAttachments.size());
        if (!savedAttachments.isEmpty()) {
            sb.append(", \"attachments\": [");
            for (int i = 0; i < savedAttachments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("{\"documentId\": \"").append(ToolJsonUtils.escapeJson(savedAttachments.get(i).getId())).append("\"");
                sb.append(", \"fileName\": \"").append(ToolJsonUtils.escapeJson(savedAttachments.get(i).getName())).append("\"}");
            }
            sb.append("]");
        }
        if (!failedAttachments.isEmpty()) {
            sb.append(", \"failedAttachments\": ").append(DocumentToolSupport.stringArray(failedAttachments));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Builds the document name for a stored e-mail: sanitized subject with an
     * .eml extension, run through the configured document name template exactly
     * like BulkSaveEntry.setNameTemplate does for the manual filing dialog.
     */
    private String buildEmlDocumentName(ArchiveFileBean caseBean, String rawName, Date documentDate) throws Exception {
        String docName = FileUtils.sanitizeFileName(rawName.trim());
        if (!docName.toLowerCase().endsWith(".eml")) {
            docName = docName + ".eml";
        }

        JLawyerServiceLocator locator = ToolJsonUtils.getLocator();
        SystemManagementRemote sys = locator.lookupSystemManagementRemote();
        DocumentNameTemplate nameTemplate = sys.getDefaultDocumentNameTemplate();
        if (nameTemplate == null) {
            return limitDocumentNameLength(docName);
        }

        ArchiveFileServiceRemote archiveSvc = locator.lookupArchiveFileServiceRemote();
        FormsServiceRemote formsSvc = locator.lookupFormsServiceRemote();

        String templatedName = archiveSvc.getNewDocumentName(docName, documentDate, nameTemplate);

        List<PartiesTriplet> parties = new ArrayList<>();
        List<ArchiveFileAddressesBean> involvements = archiveSvc.getInvolvementDetailsForCase(caseBean.getId());
        if (involvements != null) {
            for (ArchiveFileAddressesBean aab : involvements) {
                parties.add(new PartiesTriplet(aab.getAddressKey(), aab.getReferenceType(), aab));
            }
        }
        Collection<String> formPlaceHolders = formsSvc.getPlaceHoldersForCase(caseBean.getId());
        HashMap<String, String> formPlaceHolderValues = formsSvc.getPlaceHolderValuesForCase(caseBean.getId());

        HashMap<String, Object> placeHolders = TemplatesUtil.getPlaceHolderValues(templatedName, caseBean, parties,
                null, null, getCachedPartyTypes(), formPlaceHolders, formPlaceHolderValues,
                resolveUserQuietly(sys, caseBean.getLawyer()), resolveUserQuietly(sys, caseBean.getAssistant()));
        templatedName = TemplatesUtil.replacePlaceHolders(templatedName, placeHolders);
        templatedName = FileUtils.sanitizeFileName(templatedName);
        // the template may put the extension anywhere in the name, so strip it and add it back
        templatedName = templatedName.replace(".eml", "");
        return limitDocumentNameLength(FileUtils.preserveExtension(docName, templatedName));
    }

    private AppUserBean resolveUserQuietly(SystemManagementRemote sys, String principalId) {
        if (principalId == null || principalId.trim().isEmpty()) {
            return null;
        }
        try {
            return sys.getUser(principalId);
        } catch (Exception ex) {
            log.warn("Could not resolve user: " + principalId, ex);
            return null;
        }
    }

    /**
     * The documents.name column holds 250 characters, and a long mail subject
     * exceeds that easily. Shorten the stem, always keep the extension.
     */
    private String limitDocumentNameLength(String docName) {
        if (docName.length() <= MAX_DOCUMENT_NAME_CHARS) {
            return docName;
        }
        String extension = "";
        int dotIndex = docName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = docName.substring(dotIndex);
        }
        return docName.substring(0, MAX_DOCUMENT_NAME_CHARS - extension.length()) + extension;
    }

    private String appendNameIndex(String docName, int index) {
        String extension = "";
        String stem = docName;
        int dotIndex = docName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = docName.substring(dotIndex);
            stem = docName.substring(0, dotIndex);
        }
        String suffix = " (" + index + ")";
        int maxStem = MAX_DOCUMENT_NAME_CHARS - extension.length() - suffix.length();
        if (stem.length() > maxStem) {
            stem = stem.substring(0, maxStem);
        }
        return stem + suffix + extension;
    }

    /**
     * Returns the mailboxes the currently logged in user has been granted
     * access to. This implicitly enforces the mailbox ACL for all e-mail tools.
     */
    private List<MailboxSetup> getAccessibleMailboxes() {
        List<MailboxSetup> mailboxes = UserSettings.getInstance().getMailboxes(UserSettings.getInstance().getCurrentUser().getPrincipalId());
        if (mailboxes == null) {
            return new ArrayList<>();
        }
        return mailboxes;
    }

    private MailboxSetup findAccessibleMailbox(String mailboxId) {
        for (MailboxSetup mb : getAccessibleMailboxes()) {
            if (mailboxId.equals(mb.getId())) {
                return mb;
            }
        }
        return null;
    }

    private List<MailFolderDTO> selectSearchFolders(List<MailFolderDTO> folders, String folderName, boolean allFolders) {
        List<MailFolderDTO> selected = new ArrayList<>();
        if (folders == null) {
            return selected;
        }
        if (folderName != null && !folderName.trim().isEmpty()) {
            for (MailFolderDTO f : folders) {
                if (folderName.trim().equalsIgnoreCase(f.getDisplayName())) {
                    selected.add(f);
                }
            }
        } else if (allFolders) {
            // MAX_SEARCH_FOLDERS truncates this list, and listFolders returns the
            // server-defined (usually alphabetical) order - so the well-known
            // folders are put up front to make sure the cap never drops them
            for (MailFolderDTO f : folders) {
                if (f.isInbox()) {
                    selected.add(f);
                }
            }
            for (MailFolderDTO f : folders) {
                if (f.isSent()) {
                    selected.add(f);
                }
            }
            for (MailFolderDTO f : folders) {
                if (!f.isTrash() && !f.isInbox() && !f.isSent()) {
                    selected.add(f);
                }
            }
        } else {
            for (MailFolderDTO f : folders) {
                if (f.isInbox()) {
                    selected.add(f);
                }
            }
        }
        return selected;
    }

    private void appendStringArrayJson(StringBuilder sb, String name, String[] values) {
        sb.append(", \"").append(name).append("\": [");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(ToolJsonUtils.escapeJson(values[i])).append("\"");
            }
        }
        sb.append("]");
    }

    /**
     * A single search hit, keeping the message together with the mailbox and
     * folder it was found in.
     */
    private static class MailHit {

        private final MailboxSetup mailbox;
        private final String folderName;
        private final MailMessageDTO message;

        MailHit(MailboxSetup mailbox, String folderName, MailMessageDTO message) {
            this.mailbox = mailbox;
            this.folderName = folderName;
            this.message = message;
        }
    }

    private static String escapeJson(String value) {
        return ToolJsonUtils.escapeJson(value);
    }
}
