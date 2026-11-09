package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.client.assistant.LegalResearchToolSupport.NormCitation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.jsoup.Jsoup;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the offline parts of the legal research tools: citation parsing, URL building and the
 * mapping of sample responses of gesetze-im-internet.de, NeuRIS and Open Legal Data.
 *
 * @author jens
 */
public class LegalResearchToolSupportTest {

    @Test
    public void parsesSectionCitation() {
        NormCitation c = LegalResearchToolSupport.parseCitation("§ 573c Abs. 1 BGB");
        assertNotNull(c);
        assertNull(c.article);
        assertEquals("573c", c.section);
        assertEquals("1", c.subsection);
        assertEquals("BGB", c.law);
        assertEquals("https://www.gesetze-im-internet.de/bgb/__573c.html", LegalResearchToolSupport.giiNormUrl(c));
        assertEquals("https://dejure.org/gesetze/BGB/573c.html", LegalResearchToolSupport.dejureUrl(c));
    }

    @Test
    public void parsesSentenceAndNumberQualifiers() {
        NormCitation c = LegalResearchToolSupport.parseCitation("§ 626 Abs. 2 S. 1 Nr. 3 BGB");
        assertEquals("626", c.section);
        assertEquals("BGB", c.law);
    }

    @Test
    public void parsesArticleCitation() {
        NormCitation c = LegalResearchToolSupport.parseCitation("Art. 3 GG");
        assertEquals("3", c.article);
        assertNull(c.section);
        assertEquals("https://www.gesetze-im-internet.de/gg/art_3.html", LegalResearchToolSupport.giiNormUrl(c));
    }

    @Test
    public void parsesArticleWithSection() {
        NormCitation c = LegalResearchToolSupport.parseCitation("Art. 229 § 6 EGBGB");
        assertEquals("229", c.article);
        assertEquals("6", c.section);
        assertEquals("https://www.gesetze-im-internet.de/bgbeg/art_229__6.html", LegalResearchToolSupport.giiNormUrl(c));
    }

    @Test
    public void parsesSocialCodeWithRomanNumeral() {
        NormCitation c = LegalResearchToolSupport.parseCitation("§ 5 SGB V");
        assertEquals("5", c.section);
        assertEquals("SGB V", c.law);
        assertEquals("https://www.gesetze-im-internet.de/sgb_5/__5.html", LegalResearchToolSupport.giiNormUrl(c));
        assertEquals("sgb_9_2018", LegalResearchToolSupport.giiSlug("SGB IX"));
        assertEquals("sgb_12", LegalResearchToolSupport.giiSlug("SGB XII"));
        assertEquals("sgb_2", LegalResearchToolSupport.giiSlug("SGB 2"));
    }

    @Test
    public void parsesLawFirstAndSpacedLetter() {
        NormCitation c = LegalResearchToolSupport.parseCitation("BGB § 573 c");
        assertEquals("573c", c.section);
        assertEquals("BGB", c.law);
    }

    @Test
    public void rejectsCitationWithoutNormOrLaw() {
        assertNull(LegalResearchToolSupport.parseCitation("BGB"));
        assertNull(LegalResearchToolSupport.parseCitation("§ 573c"));
        assertNull(LegalResearchToolSupport.parseCitation(""));
        assertNull(LegalResearchToolSupport.parseCitation(null));
    }

    @Test
    public void mapsIrregularSlugs() {
        assertEquals("ao_1977", LegalResearchToolSupport.giiSlug("AO"));
        assertEquals("woeigg", LegalResearchToolSupport.giiSlug("WEG"));
        assertEquals("zpo", LegalResearchToolSupport.giiSlug("ZPO"));
        assertEquals("ao_1977", LegalResearchToolSupport.giiSlug("ao_1977"));
    }

    @Test
    public void detectsEuLaw() {
        assertEquals("32016R0679", LegalResearchToolSupport.euLawCelex("DSGVO"));
        assertNull(LegalResearchToolSupport.euLawCelex("BGB"));
    }

    @Test
    public void detectsFileNumbersAndEcli() {
        assertTrue(LegalResearchToolSupport.looksLikeFileNumber("VIII ZR 198/15"));
        assertTrue(LegalResearchToolSupport.looksLikeFileNumber("1 BvR 2347/20"));
        assertTrue(LegalResearchToolSupport.looksLikeFileNumber("5 AZR 100/19"));
        assertTrue(LegalResearchToolSupport.looksLikeFileNumber("63 S 378/14"));
        assertFalse(LegalResearchToolSupport.looksLikeFileNumber("Mietminderung Schimmel"));
        assertFalse(LegalResearchToolSupport.looksLikeFileNumber("Mietminderung 10/20 Prozent bei Schimmel"));
        assertTrue(LegalResearchToolSupport.looksLikeEcli("ECLI:DE:BGH:2016:130416UVIIIZR198.15.0"));
    }

    @Test
    public void buildsNeurisUrlForFileNumber() {
        String url = LegalResearchToolSupport.neurisCaseLawUrl("VIII ZR 198/15", "BGH", "2016-01-01", "", 2);
        assertTrue(url.contains("fileNumber=VIII+ZR+198%2F15"));
        assertTrue(url.contains("court=BGH"));
        assertTrue(url.contains("dateFrom=2016-01-01"));
        assertTrue(url.contains("pageIndex=1"));
        assertFalse(url.contains("searchTerm"));
    }

    @Test
    public void parsesGiiNorm() {
        String html = "<html><body><div class=\"jnheader\"><h1>Bürgerliches Gesetzbuch (BGB)<br />"
                + "<span class=\"jnenbez\">&#167; 573c</span>&#160;<span class=\"jnentitel\">Fristen der ordentlichen K&#252;ndigung</span></h1></div>"
                + "<div class=\"jnhtml\"><div><div class=\"jurAbsatz\">(1) Die Kündigung ist zulässig.</div>"
                + "<div class=\"jurAbsatz\">(2) Bei Wohnraum gilt anderes.</div></div></div>"
                + "<div class=\"jnfussnote\"><div class=\"jnhtml\">Fußnote zum Inkrafttreten</div></div></body></html>";
        JsonObject norm = LegalResearchToolSupport.parseGiiNorm(Jsoup.parse(html));
        assertNotNull(norm);
        assertEquals("Bürgerliches Gesetzbuch (BGB)", norm.get("lawTitle"));
        assertEquals("§ 573c", norm.get("norm"));
        assertEquals("Fristen der ordentlichen Kündigung", norm.get("heading"));
        assertEquals("(1) Die Kündigung ist zulässig.\n(2) Bei Wohnraum gilt anderes.", norm.get("text"));
        assertEquals("Fußnote zum Inkrafttreten", norm.get("footnote"));
        assertNull(LegalResearchToolSupport.parseGiiNorm(Jsoup.parse("<html><body>Fehler</body></html>")));
    }

    private static final String NEURIS_SEARCH = "{\"totalItems\":43,\"member\":[{\"item\":{\"documentNumber\":\"KORE303742016\","
            + "\"ecli\":\"ECLI:DE:BGH:2016:130416UVIIIZR198.15.0\",\"headline\":\"BGH, Urteil vom 13. April 2016 - VIII ZR 198/15\","
            + "\"titleLine\":\"(Wohnraummiete: Mietminderung)\",\"decisionDate\":\"2016-04-13\",\"fileNumbers\":[\"VIII ZR 198/15\"],"
            + "\"courtName\":\"BGH\",\"documentType\":\"Urteil\",\"judicialBody\":\"8. Zivilsenat\"},"
            + "\"textMatches\":[{\"name\":\"caseFacts\",\"text\":\"Feststellung einer <mark>Mietminderung</mark>\"},"
            + "{\"name\":\"decisionGrounds\",\"text\":\"Interesse an der <mark>Mietminderung</mark>\"},"
            + "{\"name\":\"tenor\",\"text\":\"dritter Treffer\"}]}]}";

    @Test
    public void parsesNeurisCaseLawSearch() throws Exception {
        List<JsonObject> results = LegalResearchToolSupport.parseNeurisCaseLawSearch(NEURIS_SEARCH);
        assertEquals(1, results.size());
        JsonObject r = results.get(0);
        assertEquals("neuris", r.get("source"));
        assertEquals("KORE303742016", r.get("id"));
        assertEquals("BGH, Urteil vom 13. April 2016 - VIII ZR 198/15", r.get("citation"));
        assertEquals("VIII ZR 198/15", r.get("fileNumber"));
        assertEquals("(Wohnraummiete: Mietminderung)", r.get("title"));
        JsonArray snippets = (JsonArray) r.get("snippets");
        assertEquals(2, snippets.size());
        assertEquals("Tatbestand: Feststellung einer Mietminderung", snippets.get(0));
        assertEquals("https://testphase.rechtsinformationen.bund.de/gerichtsentscheidungen/KORE303742016", r.get("url"));
        assertEquals(43, LegalResearchToolSupport.parseTotalItems(NEURIS_SEARCH));
    }

    private static final String OLDP_SEARCH = "{\"count\":62,\"results\":[{\"id\":89303,\"court\":\"AGSAARB\","
            + "\"slug\":\"ag-saarburg-2016-09-07-5a-c-19915\",\"date\":\"2016-09-07\",\"court_level_of_appeal\":\"Amtsgericht\","
            + "\"decision_type\":\"Urteil\",\"snippets\":[{\"text\":\"habe sich <em>Schimmel</em> gebildet.\"}]},"
            + "{\"id\":94097,\"court\":\"BGH\",\"slug\":\"bgh-2016-04-13-viii-zr-19815\",\"date\":\"2016-04-13\","
            + "\"decision_type\":\"Urteil\",\"snippets\":[]}]}";

    @Test
    public void parsesOldpSearchAndMergesWithoutDuplicates() throws Exception {
        List<JsonObject> oldp = LegalResearchToolSupport.parseOldpCaseSearch(OLDP_SEARCH);
        assertEquals(2, oldp.size());
        JsonObject first = oldp.get(0);
        assertEquals("oldp", first.get("source"));
        assertEquals("89303", first.get("id"));
        assertEquals("AG Saarburg", first.get("court"));
        assertEquals("AG Saarburg, Urteil vom 07.09.2016", first.get("citation"));
        assertEquals("habe sich Schimmel gebildet.", ((JsonArray) first.get("snippets")).get(0));
        assertEquals(62, LegalResearchToolSupport.parseTotalItems(OLDP_SEARCH));

        List<JsonObject> federal = LegalResearchToolSupport.parseNeurisCaseLawSearch(NEURIS_SEARCH);
        List<JsonObject> merged = LegalResearchToolSupport.mergeResults(federal, oldp);
        // the BGH decision is in both sources and must only appear once
        assertEquals(2, merged.size());
        assertEquals("neuris", merged.get(0).get("source"));
        assertEquals("89303", merged.get(1).get("id"));
    }

    @Test
    public void looksUpFileNumbersExactlyInOpenLegalData() throws Exception {
        assertEquals("https://de.openlegaldata.io/api/cases/?file_number=VIII+ZR+198%2F15&page=1&court__slug=bgh",
                LegalResearchToolSupport.oldpCaseLawUrl("VIII ZR 198/15", "BGH", 1));
        assertTrue(LegalResearchToolSupport.oldpCaseLawUrl("Mietminderung", "", 1).contains("/api/cases/search/?text=Mietminderung"));

        String json = "{\"count\":1,\"results\":[{\"id\":94097,\"slug\":\"bgh-2016-04-13-viii-zr-19815\","
                + "\"court\":{\"name\":\"Bundesgerichtshof\",\"slug\":\"bgh\"},\"file_number\":\"VIII ZR 198/15\","
                + "\"date\":\"2016-04-13\",\"type\":\"Urteil\",\"ecli\":\"ECLI:DE:BGH:2016:130416UVIIIZR198.15.0\"}]}";
        List<JsonObject> list = LegalResearchToolSupport.parseOldpCaseList(json);
        assertEquals(1, list.size());
        assertEquals("Bundesgerichtshof, Urteil vom 13.04.2016 - VIII ZR 198/15", list.get(0).get("citation"));
        assertEquals("VIII ZR 198/15", list.get(0).get("fileNumber"));
        // same decision as the federal hit - merged away
        List<JsonObject> federal = LegalResearchToolSupport.parseNeurisCaseLawSearch(NEURIS_SEARCH);
        assertEquals(1, LegalResearchToolSupport.mergeResults(federal, list).size());
    }

    @Test
    public void skipsFileNumberMatchesAsSnippets() throws Exception {
        String json = "{\"totalItems\":1,\"member\":[{\"item\":{\"documentNumber\":\"X1\",\"headline\":\"BGH\"},"
                + "\"textMatches\":[{\"name\":\"fileNumbers\",\"text\":\"VIII ZR 198/15\"}]}]}";
        JsonObject r = LegalResearchToolSupport.parseNeurisCaseLawSearch(json).get(0);
        assertTrue(((JsonArray) r.get("snippets")).isEmpty());
    }

    @Test
    public void filtersByDate() throws Exception {
        List<JsonObject> oldp = LegalResearchToolSupport.parseOldpCaseSearch(OLDP_SEARCH);
        assertEquals(1, LegalResearchToolSupport.filterByDate(oldp, "2016-05-01", "").size());
        assertEquals(1, LegalResearchToolSupport.filterByDate(oldp, null, "2016-04-13").size());
        assertEquals(2, LegalResearchToolSupport.filterByDate(oldp, "", "").size());
    }

    @Test
    public void courtFromSlug() {
        assertEquals("OLG Frankfurt", LegalResearchToolSupport.courtFromSlug("olg-frankfurt-2020-01-01-1-u-1-19"));
        assertEquals("BGH", LegalResearchToolSupport.courtFromSlug("bgh-2016-04-13-viii-zr-19815"));
        assertEquals("", LegalResearchToolSupport.courtFromSlug("kaputt"));
    }

    private static final String NEURIS_DECISION = "<!DOCTYPE HTML><html><head>"
            + "<template id=\"referenzNorm\"><ul><li>BGB, Bürgerliches Gesetzbuch § 536 Abs 1 S 1</li></ul></template>"
            + "<template id=\"vorgehendeEntscheidung\"><ul><li><a href=\"/x\">LG Berlin, Urteil vom 4. August 2015 - 63 S 378/14</a></li></ul></template>"
            + "<title>BGH, Urteil vom 13. April 2016 - VIII ZR 198/15</title></head><body>"
            + "<h1 id=\"title\">BGH, Urteil vom 13. April 2016 - VIII ZR 198/15</h1>"
            + "<section id=\"leitsatz\"><h2>Leitsatz</h2><p>Zur Mietminderung.<a class=\"border-number-link\" href=\"#randnummer-20\">Rn. 20</a></p></section>"
            + "<section id=\"tenor\"><h2>Tenor</h2><p>Die Revision wird zurückgewiesen.</p><p>Kosten trägt die Klägerin.</p></section>"
            + "<section id=\"entscheidungsgruende\"><h2>Entscheidungsgründe</h2>"
            + "<dl class=\"border-number\"><dt class=\"number\" id=\"randnummer-1\">1</dt><dd class=\"content\"><p>Die Revision hat keinen Erfolg.</p></dd></dl>"
            + "<dl class=\"border-number\"><dt class=\"number\" id=\"randnummer-2\">2</dt><dd class=\"content\"><p>Erster Satz.</p><p>Zweiter Satz.</p></dd></dl>"
            + "</section></body></html>";

    @Test
    public void parsesNeurisDecision() {
        JsonObject d = LegalResearchToolSupport.parseNeurisDecision(Jsoup.parse(NEURIS_DECISION));
        assertEquals("BGH, Urteil vom 13. April 2016 - VIII ZR 198/15", d.get("title"));
        assertEquals("BGB, Bürgerliches Gesetzbuch § 536 Abs 1 S 1", ((JsonArray) d.get("norms")).get(0));
        assertEquals("LG Berlin, Urteil vom 4. August 2015 - 63 S 378/14", ((JsonArray) d.get("priorDecisions")).get(0));
        JsonArray order = (JsonArray) d.get("sectionOrder");
        assertEquals(3, order.size());
        assertEquals("leitsatz", order.get(0));
        JsonObject sections = (JsonObject) d.get("sections");
        assertEquals("Zur Mietminderung.", sections.get("leitsatz"));
        assertEquals("Die Revision wird zurückgewiesen.\nKosten trägt die Klägerin.", sections.get("tenor"));
        assertEquals("[Rn. 1] Die Revision hat keinen Erfolg.\n[Rn. 2] Erster Satz. Zweiter Satz.", sections.get("entscheidungsgruende"));
    }

    @Test
    public void selectsSections() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("leitsatz", "L");
        sections.put("tenor", "T");
        sections.put("entscheidungsgruende", "G");
        assertEquals("G", LegalResearchToolSupport.selectSection(sections, "gruende"));
        assertEquals("G", LegalResearchToolSupport.selectSection(sections, "Gründe"));
        assertEquals("L", LegalResearchToolSupport.selectSection(sections, "Leitsätze"));
        assertNull(LegalResearchToolSupport.selectSection(sections, "tatbestand"));
        String all = LegalResearchToolSupport.selectSection(sections, "all");
        assertTrue(all.startsWith("## Leitsatz\nL"));
        assertTrue(all.contains("## Entscheidungsgründe\nG"));
    }

    @Test
    public void pagesLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("wort").append(i).append(' ');
        }
        String text = sb.toString().trim();
        JsonObject page1 = new JsonObject();
        LegalResearchToolSupport.putTextPage(page1, text, 0, 100);
        String content = (String) page1.get("content");
        assertTrue(content.length() <= 100);
        assertFalse(content.endsWith("wor"));
        int next = (Integer) page1.get("nextOffset");
        JsonObject page2 = new JsonObject();
        LegalResearchToolSupport.putTextPage(page2, text, next, 100000);
        assertEquals(text, content + page2.get("content"));
        assertNull(page2.get("nextOffset"));
        assertEquals(text.length(), page2.get("totalChars"));
    }

    @Test
    public void findsHeadingInPlainText() {
        String text = "Amtsgericht X\nTenor\nDie Klage wird abgewiesen.\nTatbestand\nDie Parteien streiten.\nEntscheidungsgründe\nDie Klage ist unbegründet.";
        assertEquals(text.indexOf("Tenor"), LegalResearchToolSupport.findHeading(text, "tenor"));
        assertEquals(text.indexOf("Entscheidungsgründe"), LegalResearchToolSupport.findHeading(text, "gruende"));
        assertEquals(-1, LegalResearchToolSupport.findHeading(text, "leitsatz"));
    }

    @Test
    public void parsesLegislationSearch() throws Exception {
        String json = "{\"totalItems\":28,\"member\":[{\"item\":{\"name\":\"Gesetz über das Wohnungseigentum und das Dauerwohnrecht\","
                + "\"legislationIdentifier\":\"eli/bund/bgbl-1/1951/s209/2024-10-10/1/deu\",\"abbreviation\":\"WEG\","
                + "\"alternateName\":\"Wohnungseigentumsgesetz\",\"legislationLegalForce\":\"InForce\","
                + "\"exampleOfWork\":{\"legislationDate\":\"1951-03-15\",\"isPartOf\":{\"name\":\"BGBl I, 1951 209\"}},"
                + "\"encoding\":[{\"contentUrl\":\"/v1/legislation/eli/x/regelungstext-1.html\"},{\"contentUrl\":\"/v1/legislation/eli/x/regelungstext-1.xml\"}]},"
                + "\"textMatches\":[{\"name\":\"§ 17 Entziehung des <mark>Wohnungseigentums</mark>\",\"text\":\"(1) Hat ein <mark>Wohnungseigentümer</mark> ...\"}]}]}";
        List<JsonObject> results = LegalResearchToolSupport.parseNeurisLegislationSearch(json);
        assertEquals(1, results.size());
        JsonObject r = results.get(0);
        assertEquals("WEG", r.get("abbreviation"));
        assertEquals("in Kraft", r.get("legalForce"));
        assertEquals("1951-03-15", r.get("date"));
        assertEquals("BGBl I, 1951 209", r.get("publication"));
        assertEquals("https://testphase.rechtsinformationen.bund.de/v1/legislation/eli/x/regelungstext-1.html", r.get("url"));
        assertEquals("§ 17 Entziehung des Wohnungseigentums: (1) Hat ein Wohnungseigentümer ...", ((JsonArray) r.get("matches")).get(0));
    }

    @Test
    public void convertsDates() {
        assertEquals("13.04.2016", LegalResearchToolSupport.germanDate("2016-04-13"));
        assertEquals("", LegalResearchToolSupport.germanDate("kaputt"));
        assertEquals("2016-04-13", LegalResearchToolSupport.isoDateOrEmpty("13.04.2016"));
        assertEquals("2016-04-13", LegalResearchToolSupport.isoDateOrEmpty("2016-04-13"));
        assertEquals("", LegalResearchToolSupport.isoDateOrEmpty("April"));
    }
}
