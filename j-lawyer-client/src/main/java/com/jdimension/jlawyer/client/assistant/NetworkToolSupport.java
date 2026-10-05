/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CaseLinkDTO;
import com.jdimension.jlawyer.services.ContactRelationDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Case link and contact relationship helpers of the assistant tools: the JSON representation of
 * links, relationships and involvements, and the bounded traversals over both graphs.
 *
 * The graphs are assembled on the client from existing server reads, one ring at a time, just
 * like the relationship graph dialog does. All reads go through a {@link Source}, so the
 * traversals can be unit tested without a server; {@link #remoteSource} binds it to the EJBs.
 * Every read the server answers already applies the caller's permissions, so a case the user may
 * not see never enters a traversal.
 *
 * A traversal is bounded by a budget of server reads and of nodes, and a contact with very many
 * cases (an insurer, a housing company) is not expanded over its cases. When a bound is hit the
 * traversal stops, returns what it found and reports why.
 *
 * @author jens
 */
public class NetworkToolSupport {

    static final int MAX_REMOTE_CALLS = 60;
    static final int MAX_NODES = 100;
    static final int MAX_HUB_CASES = 25;

    private static final String CONTACT_PREFIX = "P:";
    private static final String CASE_PREFIX = "C:";

    private NetworkToolSupport() {
    }

    /**
     * The fields of a case the tools display.
     */
    public static final class CaseRef {

        final String id;
        final String fileNumber;
        final String name;
        final String reason;
        final boolean archived;

        public CaseRef(String id, String fileNumber, String name, String reason, boolean archived) {
            this.id = id;
            this.fileNumber = fileNumber;
            this.name = name;
            this.reason = reason;
            this.archived = archived;
        }
    }

    /**
     * The fields of a contact the tools display.
     */
    public static final class ContactRef {

        final String id;
        final String displayName;
        final String company;
        final String city;

        public ContactRef(String id, String displayName, String company, String city) {
            this.id = id;
            this.displayName = displayName;
            this.company = company;
            this.city = city;
        }
    }

    /**
     * A contact being a party in a case.
     */
    public static final class Involvement {

        final CaseRef caseRef;
        final ContactRef contact;
        final String partyType;
        final String reference;

        public Involvement(CaseRef caseRef, ContactRef contact, String partyType, String reference) {
            this.caseRef = caseRef;
            this.contact = contact;
            this.partyType = partyType;
            this.reference = reference;
        }
    }

    /**
     * A case link, oriented towards the other case.
     */
    public static final class CaseLinkRef {

        final String linkId;
        final String description;
        final CaseRef otherCase;
        final Date dateCreated;
        final String createdBy;

        public CaseLinkRef(String linkId, String description, CaseRef otherCase, Date dateCreated, String createdBy) {
            this.linkId = linkId;
            this.description = description;
            this.otherCase = otherCase;
            this.dateCreated = dateCreated;
            this.createdBy = createdBy;
        }
    }

    /**
     * A contact relationship, oriented towards the other contact; the label reads from the
     * contact it was requested for.
     */
    public static final class RelationRef {

        final String relationId;
        final String label;
        final String typeName;
        final boolean symmetric;
        final String note;
        final ContactRef otherContact;

        public RelationRef(String relationId, String label, String typeName, boolean symmetric, String note, ContactRef otherContact) {
            this.relationId = relationId;
            this.label = label;
            this.typeName = typeName;
            this.symmetric = symmetric;
            this.note = note;
            this.otherContact = otherContact;
        }
    }

    /**
     * The reads the tools are built on.
     */
    public interface Source {

        /**
         * @param caseId case id
         * @return the case, or null if it does not exist
         * @throws Exception if the case cannot be read
         */
        CaseRef getCase(String caseId) throws Exception;

        /**
         * @param contactId contact id
         * @return the contact, or null if it does not exist
         * @throws Exception if the contact cannot be read
         */
        ContactRef getContact(String contactId) throws Exception;

        List<CaseLinkRef> getCaseLinks(String caseId) throws Exception;

        List<RelationRef> getRelations(String contactId) throws Exception;

        List<Involvement> getCasesForContact(String contactId) throws Exception;

        List<Involvement> getPartiesOfCase(String caseId) throws Exception;
    }

    /**
     * Binds a {@link Source} to the server.
     *
     * @param caseService case service
     * @param addressService contact service
     * @return a source reading from the server
     */
    public static Source remoteSource(ArchiveFileServiceRemote caseService, AddressServiceRemote addressService) {
        return new Source() {
            @Override
            public CaseRef getCase(String caseId) throws Exception {
                ArchiveFileBean c = caseService.getArchiveFile(caseId);
                return c == null ? null : caseRef(c);
            }

            @Override
            public ContactRef getContact(String contactId) throws Exception {
                AddressBean a = addressService.getAddress(contactId);
                return a == null ? null : contactRef(a);
            }

            @Override
            public List<CaseLinkRef> getCaseLinks(String caseId) throws Exception {
                List<CaseLinkRef> result = new ArrayList<>();
                for (CaseLinkDTO l : caseService.getCaseLinks(caseId)) {
                    CaseRef other = new CaseRef(l.getOtherCaseId(), l.getOtherCaseFileNumber(), l.getOtherCaseName(), l.getOtherCaseReason(), l.isOtherCaseArchived());
                    result.add(new CaseLinkRef(l.getLinkId(), l.getDescription(), other, l.getDateCreated(), l.getCreatedBy()));
                }
                return result;
            }

            @Override
            public List<RelationRef> getRelations(String contactId) throws Exception {
                List<RelationRef> result = new ArrayList<>();
                for (ContactRelationDTO r : addressService.getRelations(contactId)) {
                    ContactRef other = new ContactRef(r.getOtherContactId(), r.getOtherContactDisplayName(), r.getOtherContactCompany(), r.getOtherContactCity());
                    result.add(new RelationRef(r.getRelationId(), r.getLabel(), r.getTypeName(), r.isSymmetric(), r.getNote(), other));
                }
                return result;
            }

            @Override
            public List<Involvement> getCasesForContact(String contactId) throws Exception {
                return involvements(caseService.getArchiveFileAddressesForAddress(contactId));
            }

            @Override
            public List<Involvement> getPartiesOfCase(String caseId) throws Exception {
                return involvements(caseService.getInvolvementDetailsForCase(caseId));
            }
        };
    }

    private static CaseRef caseRef(ArchiveFileBean c) {
        return new CaseRef(c.getId(), c.getFileNumber(), c.getName(), c.getReason(), c.isArchived());
    }

    private static ContactRef contactRef(AddressBean a) {
        return new ContactRef(a.getId(), a.toDisplayName(), a.getCompany(), a.getCity());
    }

    private static List<Involvement> involvements(Collection<ArchiveFileAddressesBean> beans) {
        List<Involvement> result = new ArrayList<>();
        if (beans == null) {
            return result;
        }
        for (ArchiveFileAddressesBean aab : beans) {
            if (aab.getAddressKey() == null) {
                continue;
            }
            CaseRef c = aab.getArchiveFileKey() == null ? null : caseRef(aab.getArchiveFileKey());
            String partyType = aab.getReferenceType() == null ? null : aab.getReferenceType().getName();
            result.add(new Involvement(c, contactRef(aab.getAddressKey()), partyType, aab.getReference()));
        }
        return result;
    }

    // ------------------------------------------------------------------ budgeted reads

    /**
     * Reads through a {@link Source} within the budget of one tool call, each answer cached so
     * no node is read twice. A read returns null once the budget is spent.
     */
    static final class Traversal {

        private final Source source;
        private int calls = 0;
        private String truncatedReason = null;
        private final Map<String, List<CaseLinkRef>> caseLinks = new HashMap<>();
        private final Map<String, List<RelationRef>> relations = new HashMap<>();
        private final Map<String, List<Involvement>> casesForContact = new HashMap<>();
        private final Map<String, List<Involvement>> partiesOfCase = new HashMap<>();

        Traversal(Source source) {
            this.source = source;
        }

        void truncate(String reason) {
            if (this.truncatedReason == null) {
                this.truncatedReason = reason;
            }
        }

        boolean isTruncated() {
            return this.truncatedReason != null;
        }

        private boolean spend() {
            if (this.calls >= MAX_REMOTE_CALLS) {
                truncate("Maximale Anzahl von " + MAX_REMOTE_CALLS + " Serverabfragen erreicht");
                return false;
            }
            this.calls++;
            return true;
        }

        CaseRef getCase(String caseId) throws Exception {
            return spend() ? source.getCase(caseId) : null;
        }

        ContactRef getContact(String contactId) throws Exception {
            return spend() ? source.getContact(contactId) : null;
        }

        List<CaseLinkRef> caseLinks(String caseId) throws Exception {
            List<CaseLinkRef> l = this.caseLinks.get(caseId);
            if (l == null && spend()) {
                l = source.getCaseLinks(caseId);
                this.caseLinks.put(caseId, l);
            }
            return l;
        }

        List<RelationRef> relations(String contactId) throws Exception {
            List<RelationRef> l = this.relations.get(contactId);
            if (l == null && spend()) {
                l = source.getRelations(contactId);
                this.relations.put(contactId, l);
            }
            return l;
        }

        List<Involvement> casesForContact(String contactId) throws Exception {
            List<Involvement> l = this.casesForContact.get(contactId);
            if (l == null && spend()) {
                l = source.getCasesForContact(contactId);
                this.casesForContact.put(contactId, l);
            }
            return l;
        }

        List<Involvement> partiesOfCase(String caseId) throws Exception {
            List<Involvement> l = this.partiesOfCase.get(caseId);
            if (l == null && spend()) {
                l = source.getPartiesOfCase(caseId);
                this.partiesOfCase.put(caseId, l);
            }
            return l;
        }

        void appendStatus(StringBuilder sb) {
            sb.append(", \"remoteCalls\": ").append(this.calls);
            sb.append(", \"truncated\": ").append(isTruncated());
            prop(sb, "truncatedReason", this.truncatedReason);
        }
    }

    // ------------------------------------------------------------------ basic tools

    /**
     * @param caseId the case the links were read for
     * @param links its links
     * @return JSON of get_case_links
     */
    public static String caseLinksJson(String caseId, List<CaseLinkRef> links) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(caseId)).append("\", \"links\": [");
        for (int i = 0; i < links.size(); i++) {
            CaseLinkRef l = links.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"linkId\": \"").append(ToolJsonUtils.escapeJson(l.linkId)).append("\"");
            prop(sb, "description", l.description);
            prop(sb, "otherCaseId", l.otherCase.id);
            appendCaseFields(sb, l.otherCase);
            prop(sb, "dateCreated", ToolJsonUtils.formatDate(l.dateCreated));
            prop(sb, "createdBy", l.createdBy);
            sb.append("}");
        }
        sb.append("], \"totalLinks\": ").append(links.size()).append("}");
        return sb.toString();
    }

    /**
     * @param contactId the contact the relationships were read for
     * @param relations its relationships
     * @return JSON of get_contact_relations
     */
    public static String relationsJson(String contactId, List<RelationRef> relations) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(contactId)).append("\", \"relations\": [");
        for (int i = 0; i < relations.size(); i++) {
            RelationRef r = relations.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"relationId\": \"").append(ToolJsonUtils.escapeJson(r.relationId)).append("\"");
            prop(sb, "label", r.label);
            prop(sb, "typeName", r.typeName);
            sb.append(", \"symmetric\": ").append(r.symmetric);
            prop(sb, "note", r.note);
            prop(sb, "otherContactId", r.otherContact.id);
            appendContactFields(sb, r.otherContact);
            sb.append("}");
        }
        sb.append("], \"totalRelations\": ").append(relations.size()).append("}");
        return sb.toString();
    }

    /**
     * @param contactId the contact the cases were read for
     * @param involvements the contact's involvements
     * @param includeArchived false to leave out archived cases
     * @return JSON of get_cases_for_contact
     */
    public static String casesForContactJson(String contactId, List<Involvement> involvements, boolean includeArchived) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(contactId)).append("\", \"cases\": [");
        int count = 0;
        for (Involvement inv : involvements) {
            if (inv.caseRef == null || (!includeArchived && inv.caseRef.archived)) {
                continue;
            }
            if (count > 0) {
                sb.append(",");
            }
            appendInvolvement(sb, inv);
            sb.append("}");
            count++;
        }
        sb.append("], \"totalCases\": ").append(count).append("}");
        return sb.toString();
    }

    // ------------------------------------------------------------------ get_case_network

    private static final class CaseNode {

        final CaseRef caseRef;
        final int depth;
        final CaseRef via;
        final String linkDescription;

        CaseNode(CaseRef caseRef, int depth, CaseRef via, String linkDescription) {
            this.caseRef = caseRef;
            this.depth = depth;
            this.via = via;
            this.linkDescription = linkDescription;
        }
    }

    /**
     * Collects the cases reachable over case links, and the contacts that are a party in more
     * than one of them.
     *
     * @param source data source
     * @param caseId start case
     * @param maxDepth how many links away from the start case to go
     * @return JSON of get_case_network
     * @throws Exception if a read fails
     */
    public static String caseNetworkJson(Source source, String caseId, int maxDepth) throws Exception {
        Traversal t = new Traversal(source);
        CaseRef root = t.getCase(caseId);
        if (root == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }

        Map<String, CaseNode> nodes = new LinkedHashMap<>();
        nodes.put(root.id, new CaseNode(root, 0, null, null));
        Map<String, String[]> links = new LinkedHashMap<>();
        List<String> frontier = new ArrayList<>();
        frontier.add(root.id);
        for (int depth = 1; depth <= maxDepth && !frontier.isEmpty() && !t.isTruncated(); depth++) {
            List<String> next = new ArrayList<>();
            for (String id : frontier) {
                List<CaseLinkRef> caseLinks = t.caseLinks(id);
                if (caseLinks == null) {
                    break;
                }
                for (CaseLinkRef l : caseLinks) {
                    String otherId = l.otherCase.id;
                    if (!nodes.containsKey(otherId)) {
                        if (nodes.size() >= MAX_NODES) {
                            t.truncate("Maximale Anzahl von " + MAX_NODES + " Akten erreicht");
                            continue;
                        }
                        nodes.put(otherId, new CaseNode(l.otherCase, depth, nodes.get(id).caseRef, l.description));
                        next.add(otherId);
                    }
                    links.putIfAbsent(l.linkId, new String[]{id, otherId, l.description});
                }
            }
            frontier = next;
        }

        // contact id -> involvements in the network's cases
        Map<String, List<Involvement>> byContact = new LinkedHashMap<>();
        for (CaseNode node : nodes.values()) {
            List<Involvement> parties = t.partiesOfCase(node.caseRef.id);
            if (parties == null) {
                break;
            }
            for (Involvement p : parties) {
                byContact.computeIfAbsent(p.contact.id, k -> new ArrayList<>())
                        .add(new Involvement(node.caseRef, p.contact, p.partyType, p.reference));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"rootCaseId\": \"").append(ToolJsonUtils.escapeJson(root.id)).append("\"");
        sb.append(", \"maxDepth\": ").append(maxDepth);
        sb.append(", \"cases\": [");
        boolean first = true;
        for (CaseNode node : nodes.values()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(node.caseRef.id)).append("\"");
            appendCaseFields(sb, node.caseRef);
            sb.append(", \"depth\": ").append(node.depth);
            if (node.via != null) {
                prop(sb, "viaCaseId", node.via.id);
                prop(sb, "viaFileNumber", node.via.fileNumber);
                prop(sb, "linkDescription", node.linkDescription);
            }
            sb.append("}");
        }
        sb.append("], \"links\": [");
        first = true;
        for (Map.Entry<String, String[]> e : links.entrySet()) {
            String[] l = e.getValue();
            if (!nodes.containsKey(l[0]) || !nodes.containsKey(l[1])) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"linkId\": \"").append(ToolJsonUtils.escapeJson(e.getKey())).append("\"");
            prop(sb, "caseIdA", l[0]);
            prop(sb, "caseIdB", l[1]);
            prop(sb, "description", l[2]);
            sb.append("}");
        }
        sb.append("], \"sharedParties\": [");
        first = true;
        for (List<Involvement> invs : byContact.values()) {
            Set<String> caseIds = new HashSet<>();
            for (Involvement inv : invs) {
                caseIds.add(inv.caseRef.id);
            }
            if (caseIds.size() < 2) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            ContactRef c = invs.get(0).contact;
            sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(c.id)).append("\"");
            appendContactFields(sb, c);
            sb.append(", \"involvements\": [");
            for (int i = 0; i < invs.size(); i++) {
                Involvement inv = invs.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(inv.caseRef.id)).append("\"");
                prop(sb, "fileNumber", inv.caseRef.fileNumber);
                prop(sb, "partyType", inv.partyType);
                sb.append("}");
            }
            sb.append("]}");
        }
        sb.append("]");
        t.appendStatus(sb);
        sb.append("}");
        return sb.toString();
    }

    // ------------------------------------------------------------------ get_contact_network

    /**
     * Collects the contacts reachable over relationships, optionally with their cases.
     *
     * @param source data source
     * @param contactId start contact
     * @param maxDepth how many relationships away from the start contact to go
     * @param includeCases true to add each contact's cases
     * @return JSON of get_contact_network
     * @throws Exception if a read fails
     */
    public static String contactNetworkJson(Source source, String contactId, int maxDepth, boolean includeCases) throws Exception {
        Traversal t = new Traversal(source);
        ContactRef root = t.getContact(contactId);
        if (root == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactId);
        }

        Map<String, ContactRef> nodes = new LinkedHashMap<>();
        Map<String, Integer> depths = new HashMap<>();
        nodes.put(root.id, root);
        depths.put(root.id, 0);
        // relation id -> from id, to id, label as read from the from side, type, note
        Map<String, String[]> edges = new LinkedHashMap<>();
        List<String> frontier = new ArrayList<>();
        frontier.add(root.id);
        for (int depth = 1; depth <= maxDepth && !frontier.isEmpty() && !t.isTruncated(); depth++) {
            List<String> next = new ArrayList<>();
            for (String id : frontier) {
                List<RelationRef> rels = t.relations(id);
                if (rels == null) {
                    break;
                }
                for (RelationRef r : rels) {
                    String otherId = r.otherContact.id;
                    if (!nodes.containsKey(otherId)) {
                        if (nodes.size() >= MAX_NODES) {
                            t.truncate("Maximale Anzahl von " + MAX_NODES + " Kontakten erreicht");
                            continue;
                        }
                        nodes.put(otherId, r.otherContact);
                        depths.put(otherId, depth);
                        next.add(otherId);
                    }
                    edges.putIfAbsent(r.relationId, new String[]{id, otherId, r.label, r.typeName, r.note});
                }
            }
            frontier = next;
        }

        Map<String, List<Involvement>> cases = new HashMap<>();
        if (includeCases) {
            for (String id : nodes.keySet()) {
                List<Involvement> invs = t.casesForContact(id);
                if (invs == null) {
                    break;
                }
                cases.put(id, invs);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"rootContactId\": \"").append(ToolJsonUtils.escapeJson(root.id)).append("\"");
        sb.append(", \"maxDepth\": ").append(maxDepth);
        sb.append(", \"contacts\": [");
        boolean first = true;
        for (ContactRef c : nodes.values()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(c.id)).append("\"");
            appendContactFields(sb, c);
            sb.append(", \"depth\": ").append(depths.get(c.id));
            if (cases.containsKey(c.id)) {
                appendCappedInvolvements(sb, "cases", cases.get(c.id), null);
            }
            sb.append("}");
        }
        sb.append("], \"relations\": [");
        first = true;
        for (Map.Entry<String, String[]> e : edges.entrySet()) {
            String[] r = e.getValue();
            if (!nodes.containsKey(r[0]) || !nodes.containsKey(r[1])) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"relationId\": \"").append(ToolJsonUtils.escapeJson(e.getKey())).append("\"");
            prop(sb, "fromContactId", r[0]);
            prop(sb, "toContactId", r[1]);
            prop(sb, "label", r[2]);
            prop(sb, "typeName", r[3]);
            prop(sb, "note", r[4]);
            sb.append("}");
        }
        sb.append("]");
        t.appendStatus(sb);
        sb.append("}");
        return sb.toString();
    }

    // ------------------------------------------------------------------ find_party_connections

    /**
     * For every party of a case: the other cases the same contact is a party in, and the directly
     * related contacts that are a party in other cases or in this one.
     *
     * @param source data source
     * @param caseId the case to check
     * @return JSON of find_party_connections
     * @throws Exception if a read fails
     */
    public static String partyConnectionsJson(Source source, String caseId) throws Exception {
        Traversal t = new Traversal(source);
        CaseRef root = t.getCase(caseId);
        if (root == null) {
            return ToolJsonUtils.error("Akte nicht gefunden: " + caseId);
        }
        List<Involvement> parties = t.partiesOfCase(caseId);
        if (parties == null) {
            parties = new ArrayList<>();
        }

        Map<String, ContactRef> partyContacts = new LinkedHashMap<>();
        Map<String, Set<String>> rolesInCase = new HashMap<>();
        for (Involvement p : parties) {
            partyContacts.putIfAbsent(p.contact.id, p.contact);
            Set<String> roles = rolesInCase.computeIfAbsent(p.contact.id, k -> new LinkedHashSet<>());
            if (p.partyType != null) {
                roles.add(p.partyType);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(root.id)).append("\"");
        prop(sb, "fileNumber", root.fileNumber);
        prop(sb, "name", root.name);
        sb.append(", \"parties\": [");
        boolean firstParty = true;
        for (ContactRef party : partyContacts.values()) {
            if (t.isTruncated()) {
                break;
            }
            List<Involvement> invs = t.casesForContact(party.id);
            List<RelationRef> rels = t.relations(party.id);
            if (invs == null || rels == null) {
                break;
            }
            Set<String> roles = rolesInCase.get(party.id);

            if (!firstParty) {
                sb.append(",");
            }
            firstParty = false;
            sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(party.id)).append("\"");
            appendContactFields(sb, party);
            appendStrings(sb, "rolesInThisCase", roles);
            appendCappedInvolvements(sb, "otherCases", otherCases(invs, caseId), roles);

            sb.append(", \"relatedContacts\": [");
            boolean firstRelated = true;
            int withoutCases = 0;
            for (RelationRef r : rels) {
                ContactRef other = r.otherContact;
                boolean alsoParty = partyContacts.containsKey(other.id);
                List<Involvement> otherInvs = null;
                if (!alsoParty) {
                    List<Involvement> all = t.casesForContact(other.id);
                    if (all == null) {
                        break;
                    }
                    otherInvs = otherCases(all, caseId);
                    if (otherInvs.isEmpty()) {
                        withoutCases++;
                        continue;
                    }
                }
                if (!firstRelated) {
                    sb.append(",");
                }
                firstRelated = false;
                sb.append("{\"contactId\": \"").append(ToolJsonUtils.escapeJson(other.id)).append("\"");
                appendContactFields(sb, other);
                prop(sb, "label", party.displayName + " " + r.label + " " + other.displayName);
                sb.append(", \"alsoPartyInThisCase\": ").append(alsoParty);
                if (alsoParty) {
                    appendStrings(sb, "rolesInThisCase", rolesInCase.get(other.id));
                } else {
                    appendCappedInvolvements(sb, "otherCases", otherInvs, null);
                }
                sb.append("}");
            }
            sb.append("], \"relatedContactsWithoutCases\": ").append(withoutCases);
            sb.append("}");
        }
        sb.append("]");
        t.appendStatus(sb);
        sb.append("}");
        return sb.toString();
    }

    private static List<Involvement> otherCases(List<Involvement> invs, String caseId) {
        List<Involvement> result = new ArrayList<>();
        for (Involvement inv : invs) {
            if (inv.caseRef != null && !caseId.equals(inv.caseRef.id)) {
                result.add(inv);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ find_connection

    private static final class Edge {

        final String target;
        final String targetLabel;
        final String statement;

        Edge(String target, String targetLabel, String statement) {
            this.target = target;
            this.targetLabel = targetLabel;
            this.statement = statement;
        }
    }

    /**
     * One end of the bidirectional search.
     */
    private static final class Side {

        final Map<String, Integer> dist = new HashMap<>();
        final Map<String, String> parent = new HashMap<>();
        final Map<String, String> statement = new HashMap<>();
        List<String> frontier = new ArrayList<>();
        int level = 0;

        Side(String start) {
            this.dist.put(start, 0);
            this.frontier.add(start);
        }
    }

    /**
     * Searches a shortest path between two contacts over relationships, case involvements and
     * case links, from both ends at once.
     *
     * @param source data source
     * @param contactIdA one contact
     * @param contactIdB the other contact
     * @param maxDepth the maximum number of steps of the path
     * @return JSON of find_connection
     * @throws Exception if a read fails
     */
    public static String connectionJson(Source source, String contactIdA, String contactIdB, int maxDepth) throws Exception {
        if (contactIdA.equals(contactIdB)) {
            return ToolJsonUtils.error("Die beiden Kontakte sind identisch");
        }
        Traversal t = new Traversal(source);
        ContactRef a = t.getContact(contactIdA);
        if (a == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactIdA);
        }
        ContactRef b = t.getContact(contactIdB);
        if (b == null) {
            return ToolJsonUtils.error("Kontakt nicht gefunden: " + contactIdB);
        }

        String keyA = CONTACT_PREFIX + a.id;
        String keyB = CONTACT_PREFIX + b.id;
        Set<String> endpoints = new HashSet<>();
        endpoints.add(keyA);
        endpoints.add(keyB);
        Map<String, String> labels = new HashMap<>();
        labels.put(keyA, a.displayName);
        labels.put(keyB, b.displayName);
        Set<String> hubs = new LinkedHashSet<>();

        Side sideA = new Side(keyA);
        Side sideB = new Side(keyB);
        String meet = null;
        search:
        while (sideA.level + sideB.level < maxDepth) {
            Side s = sideA.frontier.size() <= sideB.frontier.size() ? sideA : sideB;
            Side other = s == sideA ? sideB : sideA;
            if (s.frontier.isEmpty()) {
                break;
            }
            List<String> next = new ArrayList<>();
            int best = Integer.MAX_VALUE;
            for (String key : s.frontier) {
                List<Edge> edges = expand(t, key, labels, endpoints, hubs);
                if (edges == null) {
                    break search;
                }
                for (Edge e : edges) {
                    if (s.dist.containsKey(e.target)) {
                        continue;
                    }
                    if (s.dist.size() + other.dist.size() >= MAX_NODES) {
                        t.truncate("Maximale Anzahl von " + MAX_NODES + " Knoten erreicht");
                        break search;
                    }
                    s.dist.put(e.target, s.level + 1);
                    s.parent.put(e.target, key);
                    s.statement.put(e.target, e.statement);
                    labels.putIfAbsent(e.target, e.targetLabel);
                    next.add(e.target);
                    Integer otherDist = other.dist.get(e.target);
                    if (otherDist != null && s.level + 1 + otherDist < best) {
                        best = s.level + 1 + otherDist;
                        meet = e.target;
                    }
                }
            }
            s.level++;
            s.frontier = next;
            if (meet != null) {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"contactIdA\": \"").append(ToolJsonUtils.escapeJson(a.id)).append("\"");
        prop(sb, "contactIdB", b.id);
        sb.append(", \"maxDepth\": ").append(maxDepth);
        sb.append(", \"found\": ").append(meet != null);
        if (meet != null) {
            List<String> path = new ArrayList<>();
            List<String> steps = new ArrayList<>();
            for (String k = meet; k != null; k = sideA.parent.get(k)) {
                path.add(0, k);
                if (sideA.statement.containsKey(k)) {
                    steps.add(0, sideA.statement.get(k));
                }
            }
            for (String k = meet; sideB.parent.containsKey(k); k = sideB.parent.get(k)) {
                steps.add(sideB.statement.get(k));
                path.add(sideB.parent.get(k));
            }
            sb.append(", \"length\": ").append(steps.size());
            sb.append(", \"steps\": [");
            for (int i = 0; i < steps.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(ToolJsonUtils.escapeJson(steps.get(i))).append("\"");
            }
            sb.append("], \"path\": [");
            for (int i = 0; i < path.size(); i++) {
                String k = path.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                boolean isCase = k.startsWith(CASE_PREFIX);
                sb.append("{\"type\": \"").append(isCase ? "case" : "contact").append("\"");
                prop(sb, isCase ? "caseId" : "contactId", k.substring(2));
                prop(sb, "label", labels.get(k));
                sb.append("}");
            }
            sb.append("]");
        }
        appendStrings(sb, "hubsNotExpanded", hubs);
        t.appendStatus(sb);
        sb.append("}");
        return sb.toString();
    }

    /**
     * The neighbours of a node in the combined graph: a contact's relationships and cases, a
     * case's links and parties. A contact with too many cases is not expanded over them unless
     * it is one of the two contacts searched for.
     *
     * @return the edges, or null once the budget is spent
     */
    private static List<Edge> expand(Traversal t, String key, Map<String, String> labels, Set<String> endpoints, Set<String> hubs) throws Exception {
        String id = key.substring(2);
        String label = labels.get(key);
        List<Edge> edges = new ArrayList<>();
        if (key.startsWith(CONTACT_PREFIX)) {
            List<RelationRef> rels = t.relations(id);
            List<Involvement> invs = t.casesForContact(id);
            if (rels == null || invs == null) {
                return null;
            }
            for (RelationRef r : rels) {
                edges.add(new Edge(CONTACT_PREFIX + r.otherContact.id, r.otherContact.displayName,
                        label + " " + r.label + " " + r.otherContact.displayName));
            }
            if (invs.size() > MAX_HUB_CASES && !endpoints.contains(key)) {
                hubs.add(label + " (" + invs.size() + " Akten)");
                return edges;
            }
            for (Involvement inv : invs) {
                if (inv.caseRef == null) {
                    continue;
                }
                String caseLabel = caseLabel(inv.caseRef);
                edges.add(new Edge(CASE_PREFIX + inv.caseRef.id, caseLabel, involvementStatement(label, caseLabel, inv.partyType)));
            }
        } else {
            List<CaseLinkRef> links = t.caseLinks(id);
            List<Involvement> parties = t.partiesOfCase(id);
            if (links == null || parties == null) {
                return null;
            }
            for (CaseLinkRef l : links) {
                String otherLabel = caseLabel(l.otherCase);
                String statement = label + " ist verknüpft mit " + otherLabel;
                if (l.description != null && !l.description.trim().isEmpty()) {
                    statement = statement + " (" + l.description + ")";
                }
                edges.add(new Edge(CASE_PREFIX + l.otherCase.id, otherLabel, statement));
            }
            for (Involvement p : parties) {
                edges.add(new Edge(CONTACT_PREFIX + p.contact.id, p.contact.displayName, involvementStatement(p.contact.displayName, label, p.partyType)));
            }
        }
        return edges;
    }

    private static String caseLabel(CaseRef c) {
        StringBuilder sb = new StringBuilder("Akte ");
        sb.append(c.fileNumber == null ? c.id : c.fileNumber);
        if (c.name != null && !c.name.trim().isEmpty()) {
            sb.append(" (").append(c.name).append(")");
        }
        if (c.archived) {
            sb.append(" [archiviert]");
        }
        return sb.toString();
    }

    private static String involvementStatement(String contactLabel, String caseLabel, String partyType) {
        String s = contactLabel + " ist beteiligt an " + caseLabel;
        if (partyType != null && !partyType.trim().isEmpty()) {
            s = s + " als " + partyType;
        }
        return s;
    }

    // ------------------------------------------------------------------ JSON helpers

    private static void prop(StringBuilder sb, String name, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(", \"").append(name).append("\": \"").append(ToolJsonUtils.escapeJson(value)).append("\"");
        }
    }

    private static void appendCaseFields(StringBuilder sb, CaseRef c) {
        prop(sb, "fileNumber", c.fileNumber);
        prop(sb, "name", c.name);
        prop(sb, "reason", c.reason);
        sb.append(", \"archived\": ").append(c.archived);
    }

    private static void appendContactFields(StringBuilder sb, ContactRef c) {
        prop(sb, "displayName", c.displayName);
        prop(sb, "company", c.company);
        prop(sb, "city", c.city);
    }

    private static void appendInvolvement(StringBuilder sb, Involvement inv) {
        sb.append("{\"caseId\": \"").append(ToolJsonUtils.escapeJson(inv.caseRef.id)).append("\"");
        appendCaseFields(sb, inv.caseRef);
        prop(sb, "partyType", inv.partyType);
        prop(sb, "reference", inv.reference);
    }

    /**
     * Appends a list of involvements under the given name, at most {@link #MAX_HUB_CASES} of
     * them, with the full count and a hub flag when there are more.
     *
     * @param rolesToCompare roles to compare each involvement's role with, setting roleDiffers;
     * null to leave the flag out
     */
    private static void appendCappedInvolvements(StringBuilder sb, String name, List<Involvement> invs, Set<String> rolesToCompare) {
        sb.append(", \"").append(name).append("\": [");
        int limit = Math.min(invs.size(), MAX_HUB_CASES);
        int count = 0;
        for (Involvement inv : invs) {
            if (inv.caseRef == null) {
                continue;
            }
            if (count == limit) {
                break;
            }
            if (count > 0) {
                sb.append(",");
            }
            appendInvolvement(sb, inv);
            if (rolesToCompare != null) {
                boolean differs = inv.partyType != null && !rolesToCompare.contains(inv.partyType);
                sb.append(", \"roleDiffers\": ").append(differs);
            }
            sb.append("}");
            count++;
        }
        sb.append("], \"").append(name).append("Count\": ").append(invs.size());
        if (invs.size() > MAX_HUB_CASES) {
            sb.append(", \"hub\": true");
        }
    }

    private static void appendStrings(StringBuilder sb, String name, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sb.append(", \"").append(name).append("\": [");
        boolean first = true;
        for (String v : values) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(ToolJsonUtils.escapeJson(v)).append("\"");
        }
        sb.append("]");
    }
}
