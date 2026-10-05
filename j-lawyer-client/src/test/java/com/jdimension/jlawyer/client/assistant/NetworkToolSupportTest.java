package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.client.assistant.NetworkToolSupport.CaseLinkRef;
import com.jdimension.jlawyer.client.assistant.NetworkToolSupport.CaseRef;
import com.jdimension.jlawyer.client.assistant.NetworkToolSupport.ContactRef;
import com.jdimension.jlawyer.client.assistant.NetworkToolSupport.Involvement;
import com.jdimension.jlawyer.client.assistant.NetworkToolSupport.RelationRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the traversals of the case link and contact relationship tools against an in-memory
 * graph.
 *
 * @author jens
 */
public class NetworkToolSupportTest {

    /**
     * An in-memory graph of cases, contacts, links, relationships and involvements.
     */
    private static final class Graph implements NetworkToolSupport.Source {

        final Map<String, CaseRef> cases = new HashMap<>();
        final Map<String, ContactRef> contacts = new HashMap<>();
        final Map<String, List<CaseLinkRef>> links = new HashMap<>();
        final Map<String, List<RelationRef>> relations = new HashMap<>();
        final List<Involvement> involvements = new ArrayList<>();
        int calls = 0;

        CaseRef addCase(String id, boolean archived) {
            CaseRef c = new CaseRef(id, "AZ-" + id, "Rubrum " + id, null, archived);
            cases.put(id, c);
            return c;
        }

        ContactRef addContact(String id) {
            ContactRef c = new ContactRef(id, "Kontakt " + id, null, null);
            contacts.put(id, c);
            return c;
        }

        void link(String a, String b, String description) {
            links.computeIfAbsent(a, k -> new ArrayList<>()).add(new CaseLinkRef(a + "-" + b, description, cases.get(b), null, "admin"));
            links.computeIfAbsent(b, k -> new ArrayList<>()).add(new CaseLinkRef(a + "-" + b, description, cases.get(a), null, "admin"));
        }

        void relate(String from, String to, String labelFrom, String labelTo) {
            relations.computeIfAbsent(from, k -> new ArrayList<>()).add(new RelationRef(from + "-" + to, labelFrom, "Typ", false, null, contacts.get(to)));
            relations.computeIfAbsent(to, k -> new ArrayList<>()).add(new RelationRef(from + "-" + to, labelTo, "Typ", false, null, contacts.get(from)));
        }

        void involve(String contactId, String caseId, String role) {
            involvements.add(new Involvement(cases.get(caseId), contacts.get(contactId), role, null));
        }

        @Override
        public CaseRef getCase(String caseId) {
            calls++;
            return cases.get(caseId);
        }

        @Override
        public ContactRef getContact(String contactId) {
            calls++;
            return contacts.get(contactId);
        }

        @Override
        public List<CaseLinkRef> getCaseLinks(String caseId) {
            calls++;
            return links.getOrDefault(caseId, new ArrayList<>());
        }

        @Override
        public List<RelationRef> getRelations(String contactId) {
            calls++;
            return relations.getOrDefault(contactId, new ArrayList<>());
        }

        @Override
        public List<Involvement> getCasesForContact(String contactId) {
            calls++;
            List<Involvement> result = new ArrayList<>();
            for (Involvement inv : involvements) {
                if (inv.contact.id.equals(contactId)) {
                    result.add(inv);
                }
            }
            return result;
        }

        @Override
        public List<Involvement> getPartiesOfCase(String caseId) {
            calls++;
            List<Involvement> result = new ArrayList<>();
            for (Involvement inv : involvements) {
                if (inv.caseRef.id.equals(caseId)) {
                    // the server read of a case's parties does not carry the case itself
                    result.add(new Involvement(null, inv.contact, inv.partyType, inv.reference));
                }
            }
            return result;
        }
    }

    private static JsonObject parse(String json) throws Exception {
        return (JsonObject) Jsoner.deserialize(json);
    }

    @Test
    public void caseNetworkFollowsLinksUpToTheDepthAndReportsSharedParties() throws Exception {
        Graph g = new Graph();
        g.addCase("1", false);
        g.addCase("2", false);
        g.addCase("3", true);
        g.addContact("p");
        g.link("1", "2", "Gegenakte");
        g.link("2", "3", "Folgeakte");
        g.involve("p", "1", "Mandant");
        g.involve("p", "3", "Gegner");

        JsonObject depth1 = parse(NetworkToolSupport.caseNetworkJson(g, "1", 1));
        assertEquals(2, ((JsonArray) depth1.get("cases")).size());
        assertEquals(0, ((JsonArray) depth1.get("sharedParties")).size());

        JsonObject depth2 = parse(NetworkToolSupport.caseNetworkJson(g, "1", 2));
        JsonArray cases = (JsonArray) depth2.get("cases");
        assertEquals(3, cases.size());
        JsonObject third = (JsonObject) cases.get(2);
        assertEquals("3", third.get("caseId"));
        assertEquals("2", third.get("viaCaseId"));
        assertEquals("Folgeakte", third.get("linkDescription"));
        assertEquals(true, third.get("archived"));
        assertEquals(2, ((JsonArray) depth2.get("links")).size());
        JsonArray shared = (JsonArray) depth2.get("sharedParties");
        assertEquals(1, shared.size());
        assertEquals(2, ((JsonArray) ((JsonObject) shared.get(0)).get("involvements")).size());
        assertEquals(false, depth2.get("truncated"));
    }

    @Test
    public void unknownCaseIsAnError() throws Exception {
        JsonObject result = parse(NetworkToolSupport.caseNetworkJson(new Graph(), "x", 1));
        assertNotNull(result.get("error"));
    }

    @Test
    public void contactNetworkKeepsLabelsAndAddsCases() throws Exception {
        Graph g = new Graph();
        g.addContact("m");
        g.addContact("k");
        g.addContact("e");
        g.addCase("1", false);
        g.relate("m", "k", "ist Mutter von", "ist Kind von");
        g.relate("k", "e", "ist Ehepartner von", "ist Ehepartner von");
        g.involve("k", "1", "Mandant");

        JsonObject result = parse(NetworkToolSupport.contactNetworkJson(g, "m", 1, true));
        JsonArray contacts = (JsonArray) result.get("contacts");
        assertEquals(2, contacts.size());
        JsonObject relation = (JsonObject) ((JsonArray) result.get("relations")).get(0);
        assertEquals("m", relation.get("fromContactId"));
        assertEquals("ist Mutter von", relation.get("label"));
        JsonObject child = (JsonObject) contacts.get(1);
        assertEquals(1, ((JsonArray) child.get("cases")).size());

        JsonObject depth2 = parse(NetworkToolSupport.contactNetworkJson(g, "m", 2, false));
        assertEquals(3, ((JsonArray) depth2.get("contacts")).size());
    }

    @Test
    public void partyConnectionsFlagDifferentRolesAndRelatedContacts() throws Exception {
        Graph g = new Graph();
        g.addCase("1", false);
        g.addCase("2", false);
        g.addCase("3", false);
        g.addContact("client");
        g.addContact("opponent");
        g.addContact("brother");
        g.addContact("loner");
        g.involve("client", "1", "Mandant");
        g.involve("opponent", "1", "Gegner");
        g.involve("client", "2", "Gegner");
        g.involve("client", "3", "Mandant");
        g.involve("brother", "2", "Zeuge");
        g.relate("opponent", "brother", "ist Bruder von", "ist Bruder von");
        g.relate("opponent", "loner", "ist Kollege von", "ist Kollege von");
        g.relate("opponent", "client", "ist Nachbar von", "ist Nachbar von");

        JsonObject result = parse(NetworkToolSupport.partyConnectionsJson(g, "1"));
        JsonArray parties = (JsonArray) result.get("parties");
        assertEquals(2, parties.size());

        JsonObject client = (JsonObject) parties.get(0);
        JsonArray otherCases = (JsonArray) client.get("otherCases");
        assertEquals(2, otherCases.size());
        for (Object o : otherCases) {
            JsonObject c = (JsonObject) o;
            assertEquals("2".equals(c.get("caseId")), c.get("roleDiffers"));
        }

        JsonObject opponent = (JsonObject) parties.get(1);
        JsonArray related = (JsonArray) opponent.get("relatedContacts");
        assertEquals(2, related.size());
        JsonObject brother = (JsonObject) related.get(0);
        assertEquals("brother", brother.get("contactId"));
        assertEquals("Kontakt opponent ist Bruder von Kontakt brother", brother.get("label"));
        assertEquals(false, brother.get("alsoPartyInThisCase"));
        JsonObject neighbour = (JsonObject) related.get(1);
        assertEquals(true, neighbour.get("alsoPartyInThisCase"));
        assertEquals(1, ((Number) opponent.get("relatedContactsWithoutCases")).intValue());
    }

    @Test
    public void partyConnectionsCapHubContacts() throws Exception {
        Graph g = new Graph();
        g.addContact("insurer");
        for (int i = 0; i <= NetworkToolSupport.MAX_HUB_CASES + 5; i++) {
            g.addCase("c" + i, false);
            g.involve("insurer", "c" + i, "Versicherung");
        }
        JsonObject result = parse(NetworkToolSupport.partyConnectionsJson(g, "c0"));
        JsonObject insurer = (JsonObject) ((JsonArray) result.get("parties")).get(0);
        assertEquals(NetworkToolSupport.MAX_HUB_CASES, ((JsonArray) insurer.get("otherCases")).size());
        assertEquals(NetworkToolSupport.MAX_HUB_CASES + 5, ((Number) insurer.get("otherCasesCount")).intValue());
        assertEquals(true, insurer.get("hub"));
    }

    @Test
    public void connectionFindsAPathOverRelationsInvolvementsAndLinks() throws Exception {
        Graph g = new Graph();
        g.addContact("mother");
        g.addContact("child");
        g.addContact("other");
        g.addCase("1", false);
        g.addCase("2", false);
        g.relate("mother", "child", "ist Mutter von", "ist Kind von");
        g.involve("child", "1", "Mandant");
        g.link("1", "2", "Gegenakte");
        g.involve("other", "2", "Gegner");

        JsonObject result = parse(NetworkToolSupport.connectionJson(g, "mother", "other", 4));
        assertEquals(true, result.get("found"));
        assertEquals(4, ((Number) result.get("length")).intValue());
        JsonArray steps = (JsonArray) result.get("steps");
        assertEquals("Kontakt mother ist Mutter von Kontakt child", steps.get(0));
        assertEquals("Kontakt child ist beteiligt an Akte AZ-1 (Rubrum 1) als Mandant", steps.get(1));
        assertEquals("Akte AZ-1 (Rubrum 1) ist verknüpft mit Akte AZ-2 (Rubrum 2) (Gegenakte)", steps.get(2));
        assertEquals("Kontakt other ist beteiligt an Akte AZ-2 (Rubrum 2) als Gegner", steps.get(3));
        JsonArray path = (JsonArray) result.get("path");
        assertEquals(5, path.size());
        assertEquals("mother", ((JsonObject) path.get(0)).get("contactId"));
        assertEquals("other", ((JsonObject) path.get(4)).get("contactId"));

        JsonObject tooShort = parse(NetworkToolSupport.connectionJson(g, "mother", "other", 3));
        assertEquals(false, tooShort.get("found"));
    }

    @Test
    public void connectionDoesNotWalkThroughHubContacts() throws Exception {
        Graph g = new Graph();
        g.addContact("a");
        g.addContact("b");
        g.addContact("insurer");
        g.addCase("x", false);
        g.addCase("y", false);
        g.involve("a", "x", "Mandant");
        g.involve("insurer", "x", "Versicherung");
        g.involve("insurer", "y", "Versicherung");
        g.involve("b", "y", "Mandant");
        for (int i = 0; i < NetworkToolSupport.MAX_HUB_CASES; i++) {
            g.addCase("h" + i, false);
            g.involve("insurer", "h" + i, "Versicherung");
        }

        JsonObject result = parse(NetworkToolSupport.connectionJson(g, "a", "b", 4));
        assertEquals(false, result.get("found"));
        assertEquals(1, ((JsonArray) result.get("hubsNotExpanded")).size());

        // as an endpoint the hub is expanded
        JsonObject direct = parse(NetworkToolSupport.connectionJson(g, "a", "insurer", 2));
        assertEquals(true, direct.get("found"));
        assertEquals(2, ((Number) direct.get("length")).intValue());
    }

    @Test
    public void traversalStopsAtTheCallBudget() throws Exception {
        Graph g = new Graph();
        int n = NetworkToolSupport.MAX_REMOTE_CALLS + 10;
        for (int i = 0; i < n; i++) {
            g.addContact("k" + i);
        }
        for (int i = 0; i < n - 1; i++) {
            g.relate("k" + i, "k" + (i + 1), "kennt", "kennt");
        }
        JsonObject result = parse(NetworkToolSupport.connectionJson(g, "k0", "k" + (n - 1), 4));
        assertEquals(false, result.get("found"));

        // a star around one contact runs into the budget of the party check
        Graph star = new Graph();
        star.addCase("1", false);
        for (int i = 0; i < 3; i++) {
            star.addContact("p" + i);
            star.involve("p" + i, "1", "Mandant");
            for (int j = 0; j < NetworkToolSupport.MAX_REMOTE_CALLS; j++) {
                star.addContact("r" + i + "-" + j);
                star.relate("p" + i, "r" + i + "-" + j, "kennt", "kennt");
            }
        }
        JsonObject truncated = parse(NetworkToolSupport.partyConnectionsJson(star, "1"));
        assertEquals(true, truncated.get("truncated"));
        assertTrue(star.calls <= NetworkToolSupport.MAX_REMOTE_CALLS);
    }
}
