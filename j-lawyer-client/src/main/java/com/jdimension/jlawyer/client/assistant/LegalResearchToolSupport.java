/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Legal research helpers of the assistant tools: statutory text from gesetze-im-internet.de,
 * case law and federal legislation from the federal legal information portal (NeuRIS,
 * rechtsinformationen.bund.de) and case law of all instances from Open Legal Data.
 * <p>
 * The parsing methods work on plain strings / parsed documents and are free of network access so
 * they can be unit tested; the {@code execute*} methods do the HTTP calls.
 *
 * @author jens
 */
public class LegalResearchToolSupport {

    private static final Logger log = Logger.getLogger(LegalResearchToolSupport.class.getName());

    // NeuRIS is still in its test phase - the host will change when it goes live
    static final String NEURIS_BASE = "https://testphase.rechtsinformationen.bund.de";
    static final String OLDP_BASE = "https://de.openlegaldata.io";
    static final String GII_BASE = "https://www.gesetze-im-internet.de";
    static final String DEJURE_BASE = "https://dejure.org/gesetze";

    private static final int TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; j-lawyer.org Legal Assistant)";

    static final int SEARCH_PAGE_SIZE = 10;
    static final int MAX_SNIPPETS = 2;
    static final int MAX_SNIPPET_CHARS = 400;
    static final int MAX_DECISION_CHARS = 12000;
    static final int OVERVIEW_SECTION_CHARS = 3000;

    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Law abbreviations whose gesetze-im-internet.de directory differs from the lower-cased
     * abbreviation. Keys are upper case with single spaces.
     */
    private static final Map<String, String> GII_SLUG_ALIASES = new HashMap<>();

    /**
     * EU law is not published on gesetze-im-internet.de; maps abbreviations to CELEX numbers.
     */
    private static final Map<String, String> EU_LAW_CELEX = new HashMap<>();

    static {
        GII_SLUG_ALIASES.put("EGBGB", "bgbeg");
        GII_SLUG_ALIASES.put("EGHGB", "hgbeg");
        GII_SLUG_ALIASES.put("EGSTGB", "stgbeg");
        GII_SLUG_ALIASES.put("EGZPO", "zpoeg");
        GII_SLUG_ALIASES.put("EGSTPO", "stpoeg");
        GII_SLUG_ALIASES.put("EGGVG", "gvgeg");
        GII_SLUG_ALIASES.put("AO", "ao_1977");
        GII_SLUG_ALIASES.put("BAUGB", "bbaug");
        GII_SLUG_ALIASES.put("USTG", "ustg_1980");
        GII_SLUG_ALIASES.put("GKG", "gkg_2004");
        GII_SLUG_ALIASES.put("WEG", "woeigg");
        GII_SLUG_ALIASES.put("STVO", "stvo_2013");
        GII_SLUG_ALIASES.put("BDSG", "bdsg_2018");
        GII_SLUG_ALIASES.put("UWG", "uwg_2004");
        GII_SLUG_ALIASES.put("AUFENTHG", "aufenthg_2004");
        GII_SLUG_ALIASES.put("ASYLG", "asylvfg_1992");
        GII_SLUG_ALIASES.put("MUSCHG", "muschg_2018");
        GII_SLUG_ALIASES.put("OWIG", "owig_1968");
        GII_SLUG_ALIASES.put("BTMG", "btmg_1981");
        GII_SLUG_ALIASES.put("VVG", "vvg_2008");
        GII_SLUG_ALIASES.put("TKG", "tkg_2021");
        GII_SLUG_ALIASES.put("BBIG", "bbig_2005");
        GII_SLUG_ALIASES.put("WISTRG", "wistrg_1954");
        GII_SLUG_ALIASES.put("SGB IX", "sgb_9_2018");

        EU_LAW_CELEX.put("DSGVO", "32016R0679");
        EU_LAW_CELEX.put("DS-GVO", "32016R0679");
        EU_LAW_CELEX.put("AEUV", "12016E/TXT");
        EU_LAW_CELEX.put("EUV", "12016M/TXT");
        EU_LAW_CELEX.put("GRCH", "12016P/TXT");
        EU_LAW_CELEX.put("ROM I", "32008R0593");
        EU_LAW_CELEX.put("ROM II", "32007R0864");
        EU_LAW_CELEX.put("BRÜSSEL IA", "32012R1215");
        EU_LAW_CELEX.put("EUGVVO", "32012R1215");
    }

    private LegalResearchToolSupport() {
    }

    // =========================================================================
    // Norm citations
    // =========================================================================

    /**
     * A parsed citation of a statutory provision, e.g. "§ 573c Abs. 1 BGB" or "Art. 229 § 6 EGBGB".
     */
    public static final class NormCitation {

        /** article number ("Art."), may be null */
        public final String article;
        /** section number ("§"), may be null */
        public final String section;
        /** subsection ("Abs.") as given, may be null - informational only */
        public final String subsection;
        /** law abbreviation as given, e.g. "BGB" or "SGB V" */
        public final String law;

        NormCitation(String article, String section, String subsection, String law) {
            this.article = article;
            this.section = section;
            this.subsection = subsection;
            this.law = law;
        }

        /**
         * @return human readable form, e.g. "§ 573c BGB" or "Art. 229 § 6 EGBGB"
         */
        public String display() {
            StringBuilder sb = new StringBuilder();
            if (article != null) {
                sb.append("Art. ").append(article);
            }
            if (section != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append("§ ").append(section);
            }
            if (subsection != null) {
                sb.append(" Abs. ").append(subsection);
            }
            sb.append(' ').append(law);
            return sb.toString();
        }
    }

    private static final Pattern ART_AND_SECTION = Pattern.compile(
            "(?:(?i:art\\.?|artikel))\\s*(\\d+[a-z]?)\\s*§\\s*(\\d+(?:\\s?[a-z](?![a-zA-Z]))?)");
    private static final Pattern SINGLE_NORM = Pattern.compile(
            "(§|(?i:art\\.?|artikel))\\s*(\\d+(?:\\s?[a-z](?![a-zA-Z]))?)");
    private static final Pattern SUBSECTION = Pattern.compile("(?i)\\b(?:abs\\.?|absatz)\\s*(\\d+[a-z]?)");
    private static final Pattern QUALIFIERS = Pattern.compile(
            "(?i)\\b(?:abs\\.?|absatz|s\\.|satz|nr\\.?|nummer|lit\\.?|buchst\\.?|buchstabe|hs\\.?|halbsatz|uabs\\.?|unterabs\\.?|alt\\.?|alternative|var\\.?|variante)\\s*[0-9]+[a-z]?\\b");
    private static final Pattern TRAILING_NOISE = Pattern.compile("(?i)\\b(?:ff?\\.|a\\.\\s?f\\.|n\\.\\s?f\\.)");

    /**
     * Parses a norm citation such as "§ 573c Abs. 1 BGB", "Art. 3 GG", "§ 5 SGB V",
     * "Art. 229 § 6 EGBGB" or "BGB § 626".
     *
     * @param citation the citation
     * @return the parsed citation, or null if no § / Art. or no law could be identified
     */
    public static NormCitation parseCitation(String citation) {
        if (citation == null) {
            return null;
        }
        String s = citation.replace(' ', ' ').replace("§§", "§").trim();
        if (s.isEmpty()) {
            return null;
        }

        String article = null;
        String section = null;
        String rest;
        Matcher combo = ART_AND_SECTION.matcher(s);
        if (combo.find()) {
            article = combo.group(1);
            section = normalizeNumber(combo.group(2));
            rest = s.substring(0, combo.start()) + " " + s.substring(combo.end());
        } else {
            Matcher single = SINGLE_NORM.matcher(s);
            if (!single.find()) {
                return null;
            }
            String number = normalizeNumber(single.group(2));
            if ("§".equals(single.group(1))) {
                section = number;
            } else {
                article = number;
            }
            rest = s.substring(0, single.start()) + " " + s.substring(single.end());
        }

        String subsection = null;
        Matcher sub = SUBSECTION.matcher(rest);
        if (sub.find()) {
            subsection = sub.group(1);
        }
        String law = QUALIFIERS.matcher(rest).replaceAll(" ");
        law = TRAILING_NOISE.matcher(law).replaceAll(" ");
        law = law.replaceAll("[,;:()]", " ").replaceAll("\\s+", " ").trim();
        law = law.replaceFirst("(?i)^(des|der|dem|im|in|nach)\\s+", "").trim();
        if (law.isEmpty()) {
            return null;
        }
        return new NormCitation(article, section, subsection, law);
    }

    private static String normalizeNumber(String n) {
        return n.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Maps a law abbreviation to its directory name on gesetze-im-internet.de, e.g. "BGB" to
     * "bgb", "SGB V" to "sgb_5" and "EGBGB" to "bgbeg".
     *
     * @param law the abbreviation (or an already valid directory name)
     * @return the directory name
     */
    public static String giiSlug(String law) {
        String key = law.trim().replaceAll("\\s+", " ").toUpperCase(Locale.GERMAN);
        String alias = GII_SLUG_ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        Matcher sgb = Pattern.compile("^SGB\\s*([IVX]+|\\d+)$").matcher(key);
        if (sgb.matches()) {
            String num = sgb.group(1);
            int n = Character.isDigit(num.charAt(0)) ? Integer.parseInt(num) : romanToInt(num);
            String byNumber = GII_SLUG_ALIASES.get("SGB " + toRoman(n));
            return byNumber != null ? byNumber : "sgb_" + n;
        }
        String slug = law.trim().toLowerCase(Locale.GERMAN).replaceAll("\\s+", "_");
        return slug.replaceAll("[äöüß]", "_");
    }

    /**
     * @param law law abbreviation
     * @return CELEX number if the abbreviation denotes EU law, otherwise null
     */
    public static String euLawCelex(String law) {
        return EU_LAW_CELEX.get(law.trim().replaceAll("\\s+", " ").toUpperCase(Locale.GERMAN));
    }

    /**
     * @param c parsed citation
     * @return URL of the single provision on gesetze-im-internet.de
     */
    public static String giiNormUrl(NormCitation c) {
        String file;
        if (c.article != null && c.section != null) {
            file = "art_" + c.article + "__" + c.section;
        } else if (c.article != null) {
            file = "art_" + c.article;
        } else {
            file = "__" + c.section;
        }
        return GII_BASE + "/" + giiSlug(c.law) + "/" + file + ".html";
    }

    /**
     * @param c parsed citation
     * @return link to the provision on dejure.org (only generated, never requested)
     */
    public static String dejureUrl(NormCitation c) {
        String nr = c.section != null && c.article == null ? c.section : c.article;
        return DEJURE_BASE + "/" + c.law.replace(' ', '_') + "/" + nr + ".html";
    }

    static int romanToInt(String roman) {
        int result = 0;
        int prev = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int v;
            switch (roman.charAt(i)) {
                case 'I':
                    v = 1;
                    break;
                case 'V':
                    v = 5;
                    break;
                case 'X':
                    v = 10;
                    break;
                default:
                    v = 0;
            }
            result += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return result;
    }

    static String toRoman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    /**
     * Extracts a single provision from a gesetze-im-internet.de page.
     *
     * @param doc the parsed page
     * @return JSON object with lawTitle, norm, heading, text and (if present) footnote; null if
     * the page does not contain a provision
     */
    public static JsonObject parseGiiNorm(Document doc) {
        Element textBlock = null;
        for (Element e : doc.select("div.jnhtml")) {
            if (e.closest(".jnfussnote") == null) {
                textBlock = e;
                break;
            }
        }
        if (textBlock == null) {
            return null;
        }
        JsonObject result = new JsonObject();
        Element h1 = doc.selectFirst("div.jnheader h1");
        if (h1 != null) {
            StringBuilder title = new StringBuilder();
            for (Node n : h1.childNodes()) {
                if (n instanceof TextNode) {
                    title.append(((TextNode) n).text());
                } else if (n instanceof Element && "br".equals(((Element) n).tagName())) {
                    break;
                } else if (n instanceof Element) {
                    title.append(((Element) n).text());
                }
            }
            result.put("lawTitle", title.toString().trim());
        }
        Element enbez = doc.selectFirst("span.jnenbez");
        if (enbez != null) {
            result.put("norm", enbez.text().trim());
        }
        Element entitel = doc.selectFirst("span.jnentitel");
        if (entitel != null) {
            result.put("heading", entitel.text().trim());
        }
        result.put("text", blockText(textBlock));
        Element footnote = doc.selectFirst("div.jnfussnote");
        if (footnote != null) {
            String fn = footnote.text().trim();
            if (!fn.isEmpty()) {
                result.put("footnote", fn);
            }
        }
        return result;
    }

    // =========================================================================
    // Case law search results
    // =========================================================================

    private static final Pattern FILE_NUMBER = Pattern.compile(
            "^\\s*\\d*\\s*[IVX]*\\s*[A-Za-zÄÖÜäöü]{1,6}(?:\\s*\\([A-Za-z]+\\))?\\s*\\d+[a-z]?/\\d{2,4}(?:\\s*\\(?[A-Za-z]+\\)?)?\\s*$");

    /**
     * @param query search input
     * @return true if the input looks like a court file number such as "VIII ZR 198/15",
     * "1 BvR 2347/20" or "5 AZR 100/19"
     */
    public static boolean looksLikeFileNumber(String query) {
        return query != null && query.contains("/") && FILE_NUMBER.matcher(query).matches();
    }

    /**
     * @param query search input
     * @return true if the input is an ECLI
     */
    public static boolean looksLikeEcli(String query) {
        return query != null && query.trim().toUpperCase(Locale.ROOT).startsWith("ECLI:");
    }

    /**
     * Converts a NeuRIS case law search response into the compact result format.
     *
     * @param json the response body
     * @return list of results, each with source, id, citation, court, date, fileNumber, type,
     * ecli, title, snippets and url
     * @throws Exception if the JSON can not be parsed
     */
    public static List<JsonObject> parseNeurisCaseLawSearch(String json) throws Exception {
        JsonObject root = (JsonObject) Jsoner.deserialize(json);
        List<JsonObject> results = new ArrayList<>();
        JsonArray members = (JsonArray) root.get("member");
        if (members == null) {
            return results;
        }
        for (Object o : members) {
            JsonObject member = (JsonObject) o;
            JsonObject item = (JsonObject) member.get("item");
            if (item == null) {
                continue;
            }
            String docNr = str(item.get("documentNumber"));
            JsonObject r = new JsonObject();
            r.put("source", "neuris");
            r.put("id", docNr);
            r.put("citation", str(item.get("headline")));
            r.put("court", str(item.get("courtName")));
            String body = str(item.get("judicialBody"));
            if (!body.isEmpty()) {
                r.put("judicialBody", body);
            }
            r.put("date", str(item.get("decisionDate")));
            JsonArray fileNumbers = (JsonArray) item.get("fileNumbers");
            if (fileNumbers != null && !fileNumbers.isEmpty()) {
                r.put("fileNumber", str(fileNumbers.get(0)));
            }
            r.put("type", str(item.get("documentType")));
            String ecli = str(item.get("ecli"));
            if (!ecli.isEmpty()) {
                r.put("ecli", ecli);
            }
            String titleLine = stripTags(str(item.get("titleLine")));
            if (!titleLine.isEmpty()) {
                r.put("title", titleLine);
            }
            JsonArray snippets = new JsonArray();
            JsonArray matches = (JsonArray) member.get("textMatches");
            if (matches != null) {
                for (Object m : matches) {
                    if (snippets.size() >= MAX_SNIPPETS) {
                        break;
                    }
                    JsonObject match = (JsonObject) m;
                    String rawName = str(match.get("name"));
                    if ("fileNumbers".equals(rawName) || "ecli".equals(rawName)) {
                        // a match on the file number itself carries no content
                        continue;
                    }
                    String text = shorten(stripTags(str(match.get("text"))), MAX_SNIPPET_CHARS);
                    if (!text.isEmpty()) {
                        String name = sectionLabel(rawName);
                        snippets.add(name.isEmpty() ? text : name + ": " + text);
                    }
                }
            }
            r.put("snippets", snippets);
            r.put("url", NEURIS_BASE + "/gerichtsentscheidungen/" + docNr);
            results.add(r);
        }
        return results;
    }

    /**
     * @param json NeuRIS search response
     * @return totalItems of the response, or -1
     * @throws Exception if the JSON can not be parsed
     */
    public static long parseTotalItems(String json) throws Exception {
        JsonObject root = (JsonObject) Jsoner.deserialize(json);
        Object total = root.get("totalItems");
        if (total == null) {
            total = root.get("count");
        }
        return total instanceof Number ? ((Number) total).longValue() : -1;
    }

    /**
     * Converts an Open Legal Data case search response into the compact result format. The
     * search endpoint does not return the file number, it is available via the detail call.
     *
     * @param json the response body of /api/cases/search/
     * @return list of results
     * @throws Exception if the JSON can not be parsed
     */
    public static List<JsonObject> parseOldpCaseSearch(String json) throws Exception {
        JsonObject root = (JsonObject) Jsoner.deserialize(json);
        List<JsonObject> results = new ArrayList<>();
        JsonArray items = (JsonArray) root.get("results");
        if (items == null) {
            return results;
        }
        for (Object o : items) {
            JsonObject item = (JsonObject) o;
            String slug = str(item.get("slug"));
            String date = str(item.get("date"));
            String type = str(item.get("decision_type"));
            String court = courtFromSlug(slug);
            if (court.isEmpty()) {
                court = str(item.get("court"));
            }
            JsonObject r = new JsonObject();
            r.put("source", "oldp");
            r.put("id", str(item.get("id")));
            r.put("citation", oldpCitation(court, type, date, null));
            r.put("court", court);
            String level = str(item.get("court_level_of_appeal"));
            if (!level.isEmpty()) {
                r.put("courtLevel", level);
            }
            r.put("date", date);
            r.put("type", type);
            JsonArray snippets = new JsonArray();
            JsonArray raw = (JsonArray) item.get("snippets");
            if (raw != null) {
                for (Object s : raw) {
                    if (snippets.size() >= MAX_SNIPPETS) {
                        break;
                    }
                    String text = s instanceof JsonObject ? str(((JsonObject) s).get("text")) : str(s);
                    text = shorten(stripTags(text), MAX_SNIPPET_CHARS);
                    if (!text.isEmpty()) {
                        snippets.add(text);
                    }
                }
            }
            r.put("snippets", snippets);
            r.put("url", OLDP_BASE + "/case/" + slug);
            results.add(r);
        }
        return results;
    }

    /**
     * Derives a readable court name from an Open Legal Data slug, e.g.
     * "ag-saarburg-2016-09-07-5a-c-19915" to "AG Saarburg".
     *
     * @param slug the case slug
     * @return the court name or an empty string
     */
    public static String courtFromSlug(String slug) {
        Matcher m = Pattern.compile("^(.+?)-\\d{4}-\\d{2}-\\d{2}-").matcher(slug == null ? "" : slug);
        if (!m.find()) {
            return "";
        }
        String[] parts = m.group(1).split("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (i == 0 || p.length() <= 3) {
                sb.append(p.toUpperCase(Locale.GERMAN));
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    static String oldpCitation(String court, String type, String isoDate, String fileNumber) {
        StringBuilder sb = new StringBuilder(court);
        if (type != null && !type.isEmpty()) {
            sb.append(", ").append(type);
        }
        String d = germanDate(isoDate);
        if (!d.isEmpty()) {
            sb.append(" vom ").append(d);
        }
        if (fileNumber != null && !fileNumber.isEmpty()) {
            sb.append(" - ").append(fileNumber);
        }
        return sb.toString();
    }

    /**
     * Merges federal (NeuRIS) and Open Legal Data results: federal results first, Open Legal Data
     * results are dropped when they duplicate a federal decision (same date and file number).
     * Open Legal Data search hits carry the file number only inside the slug of their URL, e.g.
     * ".../case/bgh-2016-04-13-viii-zr-19815".
     *
     * @param federal NeuRIS results
     * @param oldp Open Legal Data results
     * @return merged list
     */
    public static List<JsonObject> mergeResults(List<JsonObject> federal, List<JsonObject> oldp) {
        List<JsonObject> merged = new ArrayList<>(federal);
        Set<String> keys = new HashSet<>();
        for (JsonObject f : federal) {
            keys.add(str(f.get("date")) + "|" + alnum(str(f.get("fileNumber"))));
        }
        for (JsonObject o : oldp) {
            String date = str(o.get("date"));
            String url = str(o.get("url"));
            int datePos = url.indexOf("-" + date + "-");
            if (!date.isEmpty() && datePos > 0) {
                String fileNumberPart = url.substring(datePos + date.length() + 2);
                if (keys.contains(date + "|" + alnum(fileNumberPart))) {
                    continue;
                }
            }
            merged.add(o);
        }
        return merged;
    }

    private static String alnum(String s) {
        return s.toLowerCase(Locale.GERMAN).replaceAll("[^a-z0-9äöü]", "");
    }

    /**
     * Filters results by decision date (ISO yyyy-MM-dd, inclusive). Used for Open Legal Data,
     * whose search endpoint has no date filter.
     *
     * @param results results with a "date" field
     * @param from lower bound or null
     * @param to upper bound or null
     * @return filtered list
     */
    public static List<JsonObject> filterByDate(List<JsonObject> results, String from, String to) {
        if ((from == null || from.isEmpty()) && (to == null || to.isEmpty())) {
            return results;
        }
        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject r : results) {
            String d = str(r.get("date"));
            if (d.isEmpty()) {
                continue;
            }
            if (from != null && !from.isEmpty() && d.compareTo(from) < 0) {
                continue;
            }
            if (to != null && !to.isEmpty() && d.compareTo(to) > 0) {
                continue;
            }
            filtered.add(r);
        }
        return filtered;
    }

    // =========================================================================
    // Decision full text
    // =========================================================================

    /**
     * Parses a NeuRIS decision page (/v1/rechtsprechung/{documentNumber}.html).
     *
     * @param doc the parsed page
     * @return JSON object with title, norms, priorDecisions and a "sections" object (section id to
     * text, in document order, e.g. leitsatz, tenor, tatbestand, entscheidungsgruende / gruende)
     */
    public static JsonObject parseNeurisDecision(Document doc) {
        JsonObject result = new JsonObject();
        Element title = doc.selectFirst("h1#title");
        result.put("title", title != null ? title.text().trim() : doc.title().trim());

        JsonArray norms = new JsonArray();
        for (Element li : doc.select("template#referenzNorm li")) {
            norms.add(li.text().trim());
        }
        if (!norms.isEmpty()) {
            result.put("norms", norms);
        }
        JsonArray prior = new JsonArray();
        for (Element li : doc.select("template#vorgehendeEntscheidung li")) {
            prior.add(li.text().trim());
        }
        if (!prior.isEmpty()) {
            result.put("priorDecisions", prior);
        }

        Map<String, String> sections = new LinkedHashMap<>();
        for (Element section : doc.select("section[id]")) {
            Element copy = section.clone();
            Element h2 = copy.selectFirst("h2");
            if (h2 != null) {
                h2.remove();
            }
            copy.select("a.border-number-link").remove();
            for (Element bn : copy.select("dl.border-number")) {
                Element number = bn.selectFirst(".number");
                Element content = bn.selectFirst(".content");
                String nr = number != null ? number.text().trim() : "";
                String text = content != null ? blockText(content) : blockText(bn);
                bn.after(new Element("p").text("[Rn. " + nr + "] " + text.replace('\n', ' ')));
                bn.remove();
            }
            sections.put(section.id(), blockText(copy));
        }
        JsonObject sectionJson = new JsonObject();
        sectionJson.putAll(sections);
        result.put("sectionOrder", new JsonArray(sections.keySet()));
        result.put("sections", sectionJson);
        return result;
    }

    /**
     * Converts the HTML content of an Open Legal Data decision to text.
     *
     * @param html the "content" field
     * @return plain text with line breaks between blocks
     */
    public static String oldpContentText(String html) {
        Document doc = Jsoup.parseBodyFragment(html == null ? "" : html);
        doc.select("script, style, img").remove();
        return blockText(doc.body());
    }

    /**
     * Picks the requested part of a decision.
     *
     * @param sections section id to text in document order
     * @param requested requested section ("leitsatz", "tenor", "tatbestand", "gruende", "all");
     * matched as substring of the section id, so "gruende" finds "entscheidungsgruende", too
     * @return the matching text, or null if no section matches
     */
    public static String selectSection(Map<String, String> sections, String requested) {
        String req = requested == null ? "all" : requested.trim().toLowerCase(Locale.GERMAN)
                .replace("ü", "ue").replace("ä", "ae").replace("ö", "oe");
        if (req.isEmpty() || "all".equals(req) || "volltext".equals(req)) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : sections.entrySet()) {
                sb.append("## ").append(sectionLabel(e.getKey())).append('\n').append(e.getValue()).append("\n\n");
            }
            return sb.toString().trim();
        }
        if ("leitsaetze".equals(req) || "leitsaetz".equals(req)) {
            req = "leitsatz";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sections.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).contains(req)) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(e.getValue());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Adds a page of a long text to a result object: content, offset, totalChars and, if more text
     * follows, nextOffset.
     *
     * @param result target object
     * @param text the complete text
     * @param offset start offset (clamped)
     * @param maxChars page size
     */
    public static void putTextPage(JsonObject result, String text, int offset, int maxChars) {
        int total = text.length();
        int start = Math.max(0, Math.min(offset, total));
        int end = Math.min(total, start + maxChars);
        if (end < total) {
            // do not cut inside a word
            int space = text.lastIndexOf(' ', end);
            if (space > start + maxChars / 2) {
                end = space;
            }
        }
        result.put("content", text.substring(start, end));
        result.put("offset", start);
        result.put("totalChars", total);
        if (end < total) {
            result.put("nextOffset", end);
        }
    }

    static String sectionLabel(String id) {
        switch (id) {
            case "leitsatz":
            case "headline":
            case "headnote":
                return "Leitsatz";
            case "orientierungssatz":
            case "otherHeadnote":
                return "Orientierungssatz";
            case "tenor":
                return "Tenor";
            case "tatbestand":
            case "caseFacts":
                return "Tatbestand";
            case "entscheidungsgruende":
            case "decisionGrounds":
                return "Entscheidungsgründe";
            case "gruende":
            case "grounds":
                return "Gründe";
            default:
                return id;
        }
    }

    // =========================================================================
    // Legislation search
    // =========================================================================

    /**
     * Converts a NeuRIS legislation search response into the compact result format.
     *
     * @param json the response body
     * @return list of results with name, abbreviation, alternateName, legalForce, date, eli,
     * matches (norm and snippet) and url
     * @throws Exception if the JSON can not be parsed
     */
    public static List<JsonObject> parseNeurisLegislationSearch(String json) throws Exception {
        JsonObject root = (JsonObject) Jsoner.deserialize(json);
        List<JsonObject> results = new ArrayList<>();
        JsonArray members = (JsonArray) root.get("member");
        if (members == null) {
            return results;
        }
        for (Object o : members) {
            JsonObject member = (JsonObject) o;
            JsonObject item = (JsonObject) member.get("item");
            if (item == null) {
                continue;
            }
            JsonObject r = new JsonObject();
            r.put("name", str(item.get("name")));
            String abbr = str(item.get("abbreviation"));
            if (!abbr.isEmpty()) {
                r.put("abbreviation", abbr);
            }
            String alt = str(item.get("alternateName"));
            if (!alt.isEmpty()) {
                r.put("alternateName", alt);
            }
            r.put("legalForce", "InForce".equals(str(item.get("legislationLegalForce"))) ? "in Kraft" : str(item.get("legislationLegalForce")));
            JsonObject work = (JsonObject) item.get("exampleOfWork");
            if (work != null) {
                r.put("date", str(work.get("legislationDate")));
                JsonObject issue = (JsonObject) work.get("isPartOf");
                if (issue != null) {
                    r.put("publication", str(issue.get("name")));
                }
            }
            r.put("eli", str(item.get("legislationIdentifier")));
            JsonArray encodings = (JsonArray) item.get("encoding");
            if (encodings != null) {
                for (Object e : encodings) {
                    String url = str(((JsonObject) e).get("contentUrl"));
                    if (url.endsWith(".html")) {
                        r.put("url", NEURIS_BASE + url);
                        break;
                    }
                }
            }
            JsonArray matches = new JsonArray();
            JsonArray raw = (JsonArray) member.get("textMatches");
            if (raw != null) {
                for (Object m : raw) {
                    if (matches.size() >= 3) {
                        break;
                    }
                    JsonObject match = (JsonObject) m;
                    String name = stripTags(str(match.get("name")));
                    String text = shorten(stripTags(str(match.get("text"))), 300);
                    if (name.isEmpty() && text.isEmpty()) {
                        continue;
                    }
                    matches.add(name.isEmpty() ? text : name + ": " + text);
                }
            }
            if (!matches.isEmpty()) {
                r.put("matches", matches);
            }
            results.add(r);
        }
        return results;
    }

    // =========================================================================
    // Tool execution (network)
    // =========================================================================

    /**
     * Tool get_law_norm.
     *
     * @param args tool arguments: citation, or law + section
     * @return JSON result
     */
    public static String executeGetLawNorm(JsonObject args) {
        String citation = str(args.get("citation"));
        if (citation.isEmpty()) {
            String law = str(args.get("law"));
            String section = str(args.get("section"));
            if (!law.isEmpty() && !section.isEmpty()) {
                if (!section.contains("§") && !section.toLowerCase(Locale.ROOT).startsWith("art")) {
                    section = "§ " + section;
                }
                citation = section + " " + law;
            }
        }
        if (citation.isEmpty()) {
            return ToolJsonUtils.error("Normangabe fehlt (z. B. citation = \"§ 573c BGB\")");
        }
        NormCitation c = parseCitation(citation);
        if (c == null) {
            return ToolJsonUtils.error("Normangabe nicht erkannt: '" + citation + "'. Erwartet z. B. \"§ 573c BGB\", \"Art. 3 GG\" oder \"§ 5 SGB V\".");
        }
        String celex = euLawCelex(c.law);
        if (celex != null) {
            return ToolJsonUtils.error(c.law + " ist EU-Recht und nicht auf gesetze-im-internet.de veröffentlicht. "
                    + "Verwende fetch_url mit https://eur-lex.europa.eu/legal-content/DE/TXT/HTML/?uri=CELEX:" + celex);
        }

        String url = giiNormUrl(c);
        try {
            Connection.Response response = connect(url).execute();
            if (response.statusCode() == 404) {
                return ToolJsonUtils.error("Norm " + c.display() + " nicht gefunden (" + url + "). "
                        + "Entweder existiert die Vorschrift nicht oder das Gesetz hat auf gesetze-im-internet.de eine andere Kennung "
                        + "(z. B. ao_1977 für AO). Prüfe Abkürzung bzw. Gesetz mit search_legislation und gib ggf. law als Kennung an. "
                        + "Landesrecht ist dort nicht enthalten.");
            }
            if (response.statusCode() >= 400) {
                return ToolJsonUtils.error("HTTP-Fehler " + response.statusCode() + " beim Laden von " + url);
            }
            JsonObject norm = parseGiiNorm(response.parse());
            if (norm == null) {
                return ToolJsonUtils.error("Kein Normtext gefunden unter " + url);
            }
            norm.put("citation", c.display());
            if (c.subsection != null) {
                norm.put("hint", "Angefragt war Abs. " + c.subsection + "; geliefert wird die gesamte Vorschrift.");
            }
            norm.put("source", "gesetze-im-internet.de (amtliche Fassung, Bundesministerium der Justiz)");
            norm.put("url", url);
            norm.put("dejureUrl", dejureUrl(c));
            return norm.toJson();
        } catch (SocketTimeoutException ex) {
            return ToolJsonUtils.error("Zeitüberschreitung beim Laden von " + url);
        } catch (IOException ex) {
            log.error("Loading norm failed: " + url, ex);
            return ToolJsonUtils.error("Verbindungsfehler beim Laden von " + url + ": " + ex.getMessage());
        }
    }

    /**
     * Tool search_case_law.
     *
     * @param args tool arguments: query, court, fromDate, toDate, source, page
     * @return JSON result
     */
    public static String executeSearchCaseLaw(JsonObject args) {
        String query = str(args.get("query"));
        if (query.isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }
        String court = str(args.get("court"));
        String fromDate = isoDateOrEmpty(str(args.get("fromDate")));
        String toDate = isoDateOrEmpty(str(args.get("toDate")));
        String source = str(args.get("source")).toLowerCase(Locale.ROOT);
        if (source.isEmpty()) {
            source = "all";
        }
        Integer pageArg = ToolJsonUtils.toInteger(args.get("page"));
        int page = pageArg != null && pageArg > 0 ? pageArg : 1;

        boolean useFederal = !"oldp".equals(source);
        boolean useOldp = !"federal".equals(source) && !"neuris".equals(source);

        String neurisUrl = neurisCaseLawUrl(query, court, fromDate, toDate, page);
        // a file number or ECLI is looked up exactly - a full text search would mostly return
        // decisions that merely cite it
        boolean exactLookup = looksLikeFileNumber(query) || looksLikeEcli(query);
        String oldpUrl = oldpCaseLawUrl(query, court, page);

        CompletableFuture<String> federalFuture = useFederal
                ? CompletableFuture.supplyAsync(() -> fetchQuietly(neurisUrl)) : CompletableFuture.completedFuture(null);
        CompletableFuture<String> oldpFuture = useOldp
                ? CompletableFuture.supplyAsync(() -> fetchQuietly(oldpUrl)) : CompletableFuture.completedFuture(null);

        JsonObject result = new JsonObject();
        result.put("query", query);
        result.put("page", page);
        JsonArray errors = new JsonArray();
        List<JsonObject> federal = new ArrayList<>();
        List<JsonObject> oldp = new ArrayList<>();
        boolean hasMore = false;

        String federalJson = federalFuture.join();
        if (useFederal) {
            if (federalJson == null || federalJson.startsWith("ERROR:")) {
                errors.add("Rechtsprechung Bund (NeuRIS) nicht erreichbar" + errorSuffix(federalJson));
            } else {
                try {
                    federal = parseNeurisCaseLawSearch(federalJson);
                    long total = parseTotalItems(federalJson);
                    result.put("totalFederal", total);
                    hasMore |= total > (long) page * SEARCH_PAGE_SIZE;
                } catch (Exception ex) {
                    errors.add("Antwort von NeuRIS nicht lesbar: " + ex.getMessage());
                }
            }
        }
        String oldpJson = oldpFuture.join();
        if (useOldp) {
            if (oldpJson == null || oldpJson.startsWith("ERROR:")) {
                errors.add("Open Legal Data nicht erreichbar" + errorSuffix(oldpJson));
            } else {
                try {
                    oldp = filterByDate(exactLookup ? parseOldpCaseList(oldpJson) : parseOldpCaseSearch(oldpJson), fromDate, toDate);
                    long total = parseTotalItems(oldpJson);
                    result.put("totalOpenLegalData", total);
                    hasMore |= total > (long) page * SEARCH_PAGE_SIZE;
                } catch (Exception ex) {
                    errors.add("Antwort von Open Legal Data nicht lesbar: " + ex.getMessage());
                }
            }
        }

        List<JsonObject> merged = mergeResults(federal, oldp);
        if (merged.isEmpty() && errors.size() == (useFederal ? 1 : 0) + (useOldp ? 1 : 0)) {
            return ToolJsonUtils.error("Rechtsprechungssuche fehlgeschlagen: " + String.join("; ", errorsAsStrings(errors)));
        }
        JsonArray resultArray = new JsonArray();
        resultArray.addAll(merged);
        result.put("results", resultArray);
        result.put("hasMore", hasMore);
        if (!errors.isEmpty()) {
            result.put("warnings", errors);
        }
        if (useOldp && (!fromDate.isEmpty() || !toDate.isEmpty())) {
            result.put("hint", "Der Datumsfilter wird bei Open Legal Data nur auf die aktuelle Ergebnisseite angewendet.");
        }
        return result.toJson();
    }

    static String neurisCaseLawUrl(String query, String court, String fromDate, String toDate, int page) {
        StringBuilder url = new StringBuilder(NEURIS_BASE).append("/v1/rechtsprechung?size=").append(SEARCH_PAGE_SIZE)
                .append("&pageIndex=").append(page - 1);
        if (looksLikeEcli(query)) {
            url.append("&ecli=").append(enc(query.trim()));
        } else if (looksLikeFileNumber(query)) {
            url.append("&fileNumber=").append(enc(query.trim()));
        } else {
            url.append("&searchTerm=").append(enc(query.trim()));
        }
        if (!court.isEmpty()) {
            url.append("&court=").append(enc(court));
        }
        if (!fromDate.isEmpty()) {
            url.append("&dateFrom=").append(fromDate);
        }
        if (!toDate.isEmpty()) {
            url.append("&dateTo=").append(toDate);
        }
        return url.toString();
    }

    /**
     * Tool get_court_decision.
     *
     * @param args tool arguments: source + id, or fileNumber; section, offset
     * @return JSON result
     */
    public static String executeGetCourtDecision(JsonObject args) {
        String source = str(args.get("source")).toLowerCase(Locale.ROOT);
        String id = str(args.get("id"));
        String fileNumber = str(args.get("fileNumber"));
        String section = str(args.get("section"));
        Integer offsetArg = ToolJsonUtils.toInteger(args.get("offset"));
        int offset = offsetArg != null ? offsetArg : 0;

        try {
            if (id.isEmpty()) {
                if (fileNumber.isEmpty()) {
                    return ToolJsonUtils.error("Entweder source + id (aus search_case_law) oder fileNumber angeben");
                }
                // resolve the file number: federal courts first, then Open Legal Data
                List<JsonObject> hits = new ArrayList<>();
                try {
                    hits = parseNeurisCaseLawSearch(fetch(neurisCaseLawUrl(fileNumber, "", "", "", 1)));
                } catch (Exception ex) {
                    log.warn("Resolving file number via NeuRIS failed, trying Open Legal Data: " + ex.getMessage());
                }
                if (hits.size() > 1) {
                    return candidates(fileNumber, hits);
                }
                if (hits.size() == 1) {
                    source = "neuris";
                    id = str(hits.get(0).get("id"));
                } else {
                    List<JsonObject> results = parseOldpCaseList(fetch(OLDP_BASE + "/api/cases/?file_number=" + enc(fileNumber)));
                    if (results.isEmpty()) {
                        return ToolJsonUtils.error("Keine Entscheidung mit Aktenzeichen '" + fileNumber + "' gefunden. Ggf. search_case_law verwenden "
                                + "oder web_search mit site:openjur.de.");
                    }
                    if (results.size() > 1) {
                        return candidates(fileNumber, results);
                    }
                    source = "oldp";
                    id = str(results.get(0).get("id"));
                }
            }
            if (source.isEmpty()) {
                source = id.matches("\\d+") ? "oldp" : "neuris";
            }

            if ("oldp".equals(source)) {
                return oldpDecision(id, section, offset);
            }
            return neurisDecision(id, section, offset);
        } catch (SocketTimeoutException ex) {
            return ToolJsonUtils.error("Zeitüberschreitung beim Laden der Entscheidung");
        } catch (IOException ex) {
            log.error("Loading court decision failed", ex);
            return ToolJsonUtils.error("Verbindungsfehler beim Laden der Entscheidung: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Parsing court decision failed", ex);
            return ToolJsonUtils.error("Entscheidung konnte nicht gelesen werden: " + ex.getMessage());
        }
    }

    private static String neurisDecision(String id, String section, int offset) throws IOException {
        if (!id.matches("[A-Za-z0-9]+")) {
            return ToolJsonUtils.error("Ungültige Dokumentnummer: " + id);
        }
        String apiUrl = NEURIS_BASE + "/v1/rechtsprechung/" + id + ".html";
        Connection.Response response = connect(apiUrl).execute();
        if (response.statusCode() == 404) {
            return ToolJsonUtils.error("Entscheidung " + id + " nicht gefunden");
        }
        if (response.statusCode() >= 400) {
            return ToolJsonUtils.error("HTTP-Fehler " + response.statusCode() + " beim Laden von " + apiUrl);
        }
        JsonObject parsed = parseNeurisDecision(response.parse());
        JsonObject sectionsJson = (JsonObject) parsed.remove("sections");
        JsonArray order = (JsonArray) parsed.remove("sectionOrder");
        Map<String, String> sections = new LinkedHashMap<>();
        for (Object key : order) {
            sections.put(str(key), str(sectionsJson.get(str(key))));
        }

        JsonObject result = new JsonObject();
        result.putAll(parsed);
        result.put("source", "neuris");
        result.put("id", id);
        result.put("url", NEURIS_BASE + "/gerichtsentscheidungen/" + id);
        JsonArray available = new JsonArray();
        for (Map.Entry<String, String> e : sections.entrySet()) {
            available.add(e.getKey() + " (" + e.getValue().length() + " Zeichen)");
        }
        result.put("availableSections", available);

        if (section.isEmpty()) {
            // overview: guiding principles and operative part, then the caller picks what to read
            JsonObject overview = new JsonObject();
            for (Map.Entry<String, String> e : sections.entrySet()) {
                String key = e.getKey();
                if (key.contains("leitsatz") || key.contains("orientierungssatz") || key.equals("tenor")) {
                    overview.put(key, shorten(e.getValue(), OVERVIEW_SECTION_CHARS));
                }
            }
            result.put("overview", overview);
            result.put("hint", "Für den Volltext section angeben (z. B. gruende, tatbestand, all); lange Abschnitte mit offset weiterblättern.");
            return result.toJson();
        }
        String text = selectSection(sections, section);
        if (text == null) {
            return ToolJsonUtils.error("Abschnitt '" + section + "' nicht vorhanden. Verfügbar: " + String.join(", ", sections.keySet()));
        }
        result.put("section", section);
        putTextPage(result, text, offset, MAX_DECISION_CHARS);
        return result.toJson();
    }

    private static String oldpDecision(String id, String section, int offset) throws Exception {
        if (!id.matches("\\d+")) {
            return ToolJsonUtils.error("Ungültige Open-Legal-Data-ID: " + id);
        }
        String json = fetch(OLDP_BASE + "/api/cases/" + id + "/");
        JsonObject item = (JsonObject) Jsoner.deserialize(json);
        JsonObject result = oldpDetailSummary(item);
        String text = oldpContentText(str(item.get("content")));
        if (!section.isEmpty() && !"all".equalsIgnoreCase(section) && offset == 0) {
            // no structured sections - jump to the heading if it can be found in the text
            int pos = findHeading(text, section);
            if (pos >= 0) {
                offset = pos;
            } else {
                result.put("hint", "Abschnitt '" + section + "' nicht eindeutig erkennbar, Text ab Anfang.");
            }
        }
        putTextPage(result, text, offset, MAX_DECISION_CHARS);
        return result.toJson();
    }

    static int findHeading(String text, String section) {
        String s = section.trim().toLowerCase(Locale.GERMAN);
        String[] headings;
        if (s.startsWith("leits")) {
            headings = new String[]{"Leitsatz", "Leitsätze", "Orientierungssatz"};
        } else if (s.startsWith("tenor")) {
            headings = new String[]{"Tenor"};
        } else if (s.startsWith("tatbestand")) {
            headings = new String[]{"Tatbestand"};
        } else if (s.contains("gruende") || s.contains("gründe")) {
            headings = new String[]{"Entscheidungsgründe", "Gründe"};
        } else {
            return -1;
        }
        for (String h : headings) {
            Matcher m = Pattern.compile("(^|\\n)\\s*" + Pattern.quote(h) + "\\s*:?\\s*\\n").matcher(text);
            if (m.find()) {
                return m.start() == 0 ? 0 : m.start() + 1;
            }
        }
        return -1;
    }

    static String oldpCaseLawUrl(String query, String court, int page) {
        String q = query.trim();
        String courtFilter = court.isEmpty() ? "" : "&court__slug=" + enc(court.toLowerCase(Locale.ROOT));
        if (looksLikeEcli(q)) {
            return OLDP_BASE + "/api/cases/?ecli=" + enc(q) + "&page=" + page + courtFilter;
        }
        if (looksLikeFileNumber(q)) {
            return OLDP_BASE + "/api/cases/?file_number=" + enc(q) + "&page=" + page + courtFilter;
        }
        return OLDP_BASE + "/api/cases/search/?text=" + enc(q) + "&page=" + page
                + (court.isEmpty() ? "" : "&court=" + enc(court));
    }

    /**
     * Converts an Open Legal Data case list response (/api/cases/?file_number=... or ?ecli=...)
     * into the compact result format, including the file number.
     *
     * @param json the response body
     * @return list of results
     * @throws Exception if the JSON can not be parsed
     */
    public static List<JsonObject> parseOldpCaseList(String json) throws Exception {
        JsonObject root = (JsonObject) Jsoner.deserialize(json);
        List<JsonObject> results = new ArrayList<>();
        JsonArray items = (JsonArray) root.get("results");
        if (items != null) {
            for (Object o : items) {
                results.add(oldpDetailSummary((JsonObject) o));
            }
        }
        return results;
    }

    private static JsonObject oldpDetailSummary(JsonObject item) {
        JsonObject result = new JsonObject();
        String court = "";
        Object courtObj = item.get("court");
        if (courtObj instanceof JsonObject) {
            court = str(((JsonObject) courtObj).get("name"));
        }
        String fileNumber = str(item.get("file_number"));
        String type = str(item.get("type"));
        String date = str(item.get("date"));
        result.put("source", "oldp");
        result.put("id", str(item.get("id")));
        result.put("citation", oldpCitation(court, type, date, fileNumber));
        result.put("court", court);
        result.put("date", date);
        result.put("fileNumber", fileNumber);
        result.put("type", type);
        String ecli = str(item.get("ecli"));
        if (!ecli.isEmpty()) {
            result.put("ecli", ecli);
        }
        result.put("url", OLDP_BASE + "/case/" + str(item.get("slug")));
        String sourceUrl = str(item.get("source_url"));
        if (!sourceUrl.isEmpty()) {
            result.put("originalUrl", sourceUrl);
        }
        return result;
    }

    private static String candidates(String fileNumber, List<JsonObject> hits) {
        JsonObject result = new JsonObject();
        result.put("message", "Mehrere Entscheidungen mit Aktenzeichen '" + fileNumber + "' gefunden - bitte mit source + id erneut aufrufen.");
        JsonArray list = new JsonArray();
        list.addAll(hits);
        result.put("candidates", list);
        return result.toJson();
    }

    /**
     * Tool search_legislation.
     *
     * @param args tool arguments: query, page
     * @return JSON result
     */
    public static String executeSearchLegislation(JsonObject args) {
        String query = str(args.get("query"));
        if (query.isEmpty()) {
            return ToolJsonUtils.error("Suchbegriff fehlt");
        }
        Integer pageArg = ToolJsonUtils.toInteger(args.get("page"));
        int page = pageArg != null && pageArg > 0 ? pageArg : 1;
        String url = NEURIS_BASE + "/v1/legislation?size=" + SEARCH_PAGE_SIZE + "&pageIndex=" + (page - 1)
                + "&searchTerm=" + enc(query);
        try {
            String json = fetch(url);
            List<JsonObject> hits = parseNeurisLegislationSearch(json);
            long total = parseTotalItems(json);
            JsonObject result = new JsonObject();
            result.put("query", query);
            result.put("page", page);
            result.put("totalResults", total);
            result.put("hasMore", total > (long) page * SEARCH_PAGE_SIZE);
            JsonArray list = new JsonArray();
            list.addAll(hits);
            result.put("results", list);
            result.put("hint", "Den Wortlaut einer Vorschrift liefert get_law_norm (z. B. \"§ 17 WEG\").");
            return result.toJson();
        } catch (SocketTimeoutException ex) {
            return ToolJsonUtils.error("Zeitüberschreitung bei der Gesetzessuche");
        } catch (IOException ex) {
            log.error("Legislation search failed", ex);
            return ToolJsonUtils.error("Verbindungsfehler bei der Gesetzessuche: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Legislation search failed", ex);
            return ToolJsonUtils.error("Antwort der Gesetzessuche nicht lesbar: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0);
    }

    private static String fetch(String url) throws IOException {
        Connection.Response response = connect(url).header("Accept", "application/json").execute();
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String fetchQuietly(String url) {
        try {
            return fetch(url);
        } catch (SocketTimeoutException ex) {
            return "ERROR:Zeitüberschreitung";
        } catch (IOException ex) {
            log.warn("Request failed: " + url + ": " + ex.getMessage());
            return "ERROR:" + ex.getMessage();
        }
    }

    private static String errorSuffix(String fetchResult) {
        return fetchResult != null && fetchResult.startsWith("ERROR:") ? " (" + fetchResult.substring(6) + ")" : "";
    }

    private static List<String> errorsAsStrings(JsonArray errors) {
        List<String> list = new ArrayList<>();
        for (Object o : errors) {
            list.add(str(o));
        }
        return list;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    static String stripTags(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Jsoup.parse(s).text().trim();
    }

    static String shorten(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        int space = s.lastIndexOf(' ', max);
        return (space > max / 2 ? s.substring(0, space) : s.substring(0, max)) + " …";
    }

    static String germanDate(String iso) {
        try {
            return LocalDate.parse(iso).format(GERMAN_DATE);
        } catch (DateTimeParseException | NullPointerException ex) {
            return "";
        }
    }

    static String isoDateOrEmpty(String s) {
        if (s.isEmpty()) {
            return "";
        }
        try {
            return LocalDate.parse(s).toString();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(s, GERMAN_DATE).toString();
            } catch (DateTimeParseException ex2) {
                return "";
            }
        }
    }

    private static final Set<String> BLOCK_TAGS = new HashSet<>(Arrays.asList(
            "p", "div", "br", "li", "dt", "dd", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "section", "table", "ul", "ol", "dl", "blockquote"));

    /**
     * Extracts the text of an element, keeping a line break between block elements.
     *
     * @param root element
     * @return text with collapsed white space
     */
    static String blockText(Element root) {
        StringBuilder sb = new StringBuilder();
        appendBlockText(root, sb);
        return sb.toString()
                .replaceAll("[ \\t\\x0B\\f\\r\\u00a0]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

    private static void appendBlockText(Node node, StringBuilder sb) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                sb.append(((TextNode) child).text());
            } else if (child instanceof Element) {
                Element e = (Element) child;
                boolean block = BLOCK_TAGS.contains(e.tagName());
                if (block) {
                    sb.append('\n');
                }
                if ("td".equals(e.tagName()) || "th".equals(e.tagName())) {
                    sb.append(' ');
                }
                appendBlockText(e, sb);
                if (block) {
                    sb.append('\n');
                }
            }
        }
    }
}
