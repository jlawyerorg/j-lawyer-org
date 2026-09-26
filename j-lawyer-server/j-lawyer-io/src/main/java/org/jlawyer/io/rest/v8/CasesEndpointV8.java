/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v8;
import org.jlawyer.io.rest.tools.RestErrorResponses;

import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.persistence.ArchiveFileHistoryBean;
import com.jdimension.jlawyer.persistence.ArchiveFileTagsBean;
import com.jdimension.jlawyer.persistence.DocumentTagsBean;
import com.jdimension.jlawyer.documents.DocumentPreview;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.CaseLinkDTO;
import com.jdimension.jlawyer.services.CaseLinkExistsException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentContentUpdateV8;
import com.jdimension.jlawyer.services.DocumentMetadata;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentMetadataPatchV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentMetadataV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentParentV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentTagV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentV8;
import org.jlawyer.io.rest.v7.pojo.RestfulInstantMessageV7;
import com.jdimension.jlawyer.persistence.InstantMessage;
import com.jdimension.jlawyer.services.MessagingServiceLocal;
import org.jlawyer.io.rest.v8.pojo.RestfulTaggedDocumentV8;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseHistoryV8;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseLinkRequestV8;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseLinkV8;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseOverviewV8;
import org.jlawyer.io.rest.v8.pojo.RestfulCasePageV8;

/**
 * v8 cases list endpoints returning a richer overview than v1 (adds subject field, lawyer,
 * assistant, claim value and the archived flag) so a case-list UI can render columns and
 * filter (e.g. mine / open / closed) without a per-row detail fetch — OpenSpec change
 * {@code add-web-client}, task 2.4. Additive: v1 {@code /list} is unchanged.
 *
 * @author jens
 */
@Stateless
@Path("/v8/cases")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Cases"})
public class CasesEndpointV8 implements CasesEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(CasesEndpointV8.class.getName());
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";

    /**
     * Lists all cases as a richer overview.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/list")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Lists all cases with a richer overview", response = RestfulCaseOverviewV8.class, responseContainer = "List")
    public Response listCases() {
        return list(false);
    }

    /**
     * Lists all active (non-archived) cases as a richer overview.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/list/active")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Lists all active (non-archived) cases with a richer overview", response = RestfulCaseOverviewV8.class, responseContainer = "List")
    public Response listActiveCases() {
        return list(true);
    }

    /**
     * Returns one server-paginated, filtered page of cases for the caller.
     *
     * @param offset 0-based row offset (default 0)
     * @param limit  page size (default 50, clamped server-side)
     * @param filter one of {@code all} | {@code open} (non-archived) | {@code closed} (archived)
     * @param q      optional case-insensitive search over name/file number/reason/subject/lawyer
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/page")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a server-paginated, filtered page of cases", response = RestfulCasePageV8.class)
    public Response listPage(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("q") String q) {
        try {
            Boolean archived = null;
            if ("open".equalsIgnoreCase(filter)) {
                archived = Boolean.FALSE;
            } else if ("closed".equalsIgnoreCase(filter)) {
                archived = Boolean.TRUE;
            }
            String search = (q == null || q.trim().isEmpty()) ? null : q.trim();

            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            long total = cases.countManagedCases(search, archived);
            List<ArchiveFileBean> page = cases.getManagedCasesPage(search, archived, offset, limit);

            ArrayList<RestfulCaseOverviewV8> items = new ArrayList<>();
            for (ArchiveFileBean afb : page) {
                items.add(RestfulCaseOverviewV8.fromArchiveFile(afb));
            }
            return Response.ok(new RestfulCasePageV8(total, offset, limit, items)).build();
        } catch (Exception ex) {
            log.error("Can not list cases page", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the change history (audit trail) of a case, most recent first. The underlying
     * service performs the ACL check for the calling user.
     *
     * @param id case id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/history")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the change history of a case (most recent first)", response = RestfulCaseHistoryV8.class, responseContainer = "List")
    public Response getHistory(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            // null "since" returns the full history; the service enforces the ACL for the caller.
            ArchiveFileHistoryBean[] history = cases.getHistoryForArchiveFile(id, null);

            ArrayList<RestfulCaseHistoryV8> result = new ArrayList<>();
            if (history != null) {
                for (ArchiveFileHistoryBean h : history) {
                    result.add(RestfulCaseHistoryV8.fromBean(h));
                }
            }
            // most recent first
            result.sort((a, b) -> Long.compare(b.getChangeDate(), a.getChangeDate()));
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get history for case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the links (Aktenverknüpfungen) of a case. Every entry describes the case at the
     * other end of the link as seen from the requested case, so a client never has to work out
     * which of the two stored case references is "the other one". The underlying service enforces
     * the ACL for the caller and silently omits links whose other case the caller may not see.
     *
     * @param id case id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/links")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the links of a case (each with the case at the other end)", response = RestfulCaseLinkV8.class, responseContainer = "List")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response getCaseLinks(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            if (cases.getArchiveFile(id) == null) {
                log.warn("case with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<CaseLinkDTO> links = cases.getCaseLinks(id);
            ArrayList<RestfulCaseLinkV8> result = new ArrayList<>();
            for (CaseLinkDTO link : links) {
                result.add(RestfulCaseLinkV8.fromCaseLinkDTO(link));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get links for case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Links a case to another case. The link is symmetric and stored once, so it is immediately
     * visible from either case and linking A to B is the same as linking B to A. Requires write
     * permission on the given case and read permission on the case to link to; both cases get a
     * history entry naming the other one.
     *
     * Two cases that are already linked are reported through the uniform error envelope, whose
     * {@code error} is {@code CaseLinkExistsException} and whose {@code message} names both cases -
     * so a client can tell that harmless situation from a real failure.
     *
     * @param id   case id
     * @param body the case to link to ({@code linkedCaseId}) and an optional {@code description}
     * @response 400 Missing linked case id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/links")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Links a case to another case (symmetric, stored once)", response = RestfulCaseLinkV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"), @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response createCaseLink(@PathParam("id") String id, @io.swagger.annotations.ApiParam RestfulCaseLinkRequestV8 body) {
        try {
            if (body == null || body.getLinkedCaseId() == null || body.getLinkedCaseId().trim().isEmpty()) {
                log.warn("linked case id is required for case " + id);
                return Response.status(Response.Status.BAD_REQUEST).entity("Linked case id is required").build();
            }

            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            if (cases.getArchiveFile(id) == null) {
                log.warn("case with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            CaseLinkDTO link = cases.linkCases(id, body.getLinkedCaseId().trim(), body.getDescription());
            return Response.ok(RestfulCaseLinkV8.fromCaseLinkDTO(link)).build();
        } catch (CaseLinkExistsException ex) {
            // a harmless situation the user only needs to be told about - the error envelope carries
            // the exception type, so a client can tell "already linked" from a real failure
            log.warn("cases " + id + " and " + (body == null ? null : body.getLinkedCaseId()) + " are already linked");
            return RestErrorResponses.serverError(ex);
        } catch (Exception ex) {
            log.error("Can not link case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Changes the free-text description of one of a case's links. The linked case itself can not
     * be changed - a link is symmetric and stored once, so a different linked case means a
     * different link. No history entry is written for a description change.
     *
     * @param id     case id
     * @param linkId id of the link, which must be a link of that case
     * @param body   the new {@code description} (may be empty to clear it)
     * @response 400 Missing request body
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found, or the link does not belong to that case
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/links/{linkId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Changes the description of a case link", response = RestfulCaseLinkV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"), @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response updateCaseLink(@PathParam("id") String id, @PathParam("linkId") String linkId, @io.swagger.annotations.ApiParam RestfulCaseLinkRequestV8 body) {
        try {
            if (body == null) {
                log.warn("link data is required for link " + linkId);
                return Response.status(Response.Status.BAD_REQUEST).entity("Link data is required").build();
            }

            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            if (cases.getArchiveFile(id) == null) {
                log.warn("case with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            CaseLinkDTO link = findLinkInCase(cases, id, linkId);
            if (link == null) {
                log.warn("link with id " + linkId + " does not exist in case " + id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            cases.updateCaseLinkDescription(linkId, body.getDescription());
            link.setDescription(body.getDescription());
            return Response.ok(RestfulCaseLinkV8.fromCaseLinkDTO(link)).build();
        } catch (Exception ex) {
            log.error("Can not update link " + linkId + " of case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Removes one of a case's links. Either side of a symmetric link may remove it; both cases get
     * a history entry recording the removal.
     *
     * @param id     case id
     * @param linkId id of the link, which must be a link of that case
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found, or the link does not belong to that case
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/links/{linkId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Removes a link between two cases")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response deleteCaseLink(@PathParam("id") String id, @PathParam("linkId") String linkId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            if (cases.getArchiveFile(id) == null) {
                log.warn("case with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (findLinkInCase(cases, id, linkId) == null) {
                log.warn("link with id " + linkId + " does not exist in case " + id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            cases.unlinkCases(linkId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("Can not remove link " + linkId + " of case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns cases carrying the given tag as a richer v8 overview (adds subject field, lawyer,
     * assistant, archived flag) so a dashboard can filter by responsible user — the v7 equivalent
     * only returns the leaner v1 overview.
     *
     * @param tag   the tag (label) name
     * @param value optional multi-value tag value
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/bytag/{tag}")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns cases with a given tag as a richer v8 overview", response = RestfulCaseOverviewV8.class, responseContainer = "List")
    public Response getCasesByTag(@PathParam("tag") String tag, @QueryParam("value") @DefaultValue("") String value) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<ArchiveFileBean> matches;
            if (value != null && !value.isEmpty()) {
                HashMap<String, String[]> tagValues = new HashMap<>();
                tagValues.put(tag, new String[]{value});
                matches = cases.getTagged(new String[]{tag}, null, Integer.MAX_VALUE, tagValues, null);
            } else {
                matches = cases.getTagged(new String[]{tag}, null, Integer.MAX_VALUE);
            }
            ArrayList<RestfulCaseOverviewV8> result = new ArrayList<>();
            ArrayList<String> ids = new ArrayList<>();
            if (matches != null) {
                for (ArchiveFileBean afb : matches) {
                    result.add(RestfulCaseOverviewV8.fromArchiveFile(afb));
                    ids.add(afb.getId());
                }
            }
            // one bulk call to attach every case's full tag list (so the UI can show all labels)
            if (!ids.isEmpty()) {
                HashMap<String, ArrayList<ArchiveFileTagsBean>> tagMap = cases.getTags(ids);
                for (RestfulCaseOverviewV8 o : result) {
                    ArrayList<ArchiveFileTagsBean> t = tagMap == null ? null : tagMap.get(o.getId());
                    if (t != null) {
                        ArrayList<String> names = new ArrayList<>();
                        for (ArchiveFileTagsBean b : t) {
                            if (b != null && b.getTagName() != null) {
                                names.add(b.getTagName());
                            }
                        }
                        o.setTags(names);
                    }
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get cases by tag " + tag, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns documents carrying the given tag (with their containing case id and all their tags)
     * for the dashboard "Nach Etikett" widget — the v7 document-by-tag endpoint returns no tags.
     *
     * @param tag   the tag (label) name
     * @param value optional multi-value tag value
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/bytag/{tag}")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns documents with a given tag (with case id + tags)", response = RestfulTaggedDocumentV8.class, responseContainer = "List")
    public Response getDocumentsByTag(@PathParam("tag") String tag, @QueryParam("value") @DefaultValue("") String value) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<ArchiveFileDocumentsBean> matches = cases.getTaggedDocuments(new String[]{tag}, Integer.MAX_VALUE);
            ArrayList<RestfulTaggedDocumentV8> result = new ArrayList<>();
            ArrayList<String> ids = new ArrayList<>();
            if (matches != null) {
                for (ArchiveFileDocumentsBean doc : matches) {
                    RestfulTaggedDocumentV8 d = new RestfulTaggedDocumentV8();
                    d.setId(doc.getId());
                    d.setName(doc.getName());
                    if (doc.getArchiveFileKey() != null) {
                        d.setCaseId(doc.getArchiveFileKey().getId());
                    }
                    result.add(d);
                    ids.add(doc.getId());
                }
            }
            if (!ids.isEmpty()) {
                HashMap<String, ArrayList<DocumentTagsBean>> tagMap = cases.getDocumentTags(ids);
                for (RestfulTaggedDocumentV8 d : result) {
                    ArrayList<DocumentTagsBean> t = tagMap == null ? null : tagMap.get(d.getId());
                    if (t != null) {
                        ArrayList<String> names = new ArrayList<>();
                        for (DocumentTagsBean b : t) {
                            if (b != null && b.getTagName() != null) {
                                names.add(b.getTagName());
                            }
                        }
                        d.setTags(names);
                    }
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get documents by tag " + tag, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Replaces the content of an existing case document with new Base64-encoded bytes — the
     * server side of the web client's "download → edit locally → re-upload" Office fallback. Only
     * the content changes (name/extension/folder/tags stay); the service creates a history entry
     * for the change. The caller's access to the document's containing case is verified.
     *
     * @param id   the document id
     * @param body the new content ({@code base64content})
     * @response 400 Missing or invalid content
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document (or its case) not found / not accessible
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/document/{id}/content")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Replaces the content of an existing case document (creates a history entry)")
    public Response updateDocumentContent(@PathParam("id") String id, RestfulDocumentContentUpdateV8 body) {
        try {
            if (body == null || body.getBase64content() == null || body.getBase64content().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            byte[] content;
            try {
                content = Base64.getDecoder().decode(body.getBase64content().replaceAll("\\s", ""));
            } catch (IllegalArgumentException iae) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);

            // getDocument throws when the document does not exist and enforces the caller's ACL on
            // the containing case (checkGroupsForCase) — so a missing or inaccessible document maps
            // to 404 (without leaking which of the two it is).
            try {
                cases.getDocument(id);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            cases.setDocumentContent(id, content);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("Can not update content of document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns a Base64-encoded PDF rendering of a document for in-browser preview. Office formats
     * (docx/odt/xls/xlsx/ppt/pptx/rtf/…) are converted server-side (StirlingPDF via the preview
     * generator); the client renders the result like any PDF. 415 when no PDF preview is available.
     *
     * @param id the document id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     * @response 415 No PDF preview available for this document
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/document/{id}/preview-pdf")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a Base64 PDF rendering of a document for preview (Office formats via server conversion)")
    public Response getDocumentPreviewPdf(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            ArchiveFileDocumentsBean doc;
            try {
                doc = cases.getDocument(id); // enforces the per-case ACL for the caller
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("fileName", doc.getName());
            // Prefer the (best-fidelity) cached PDF rendering; it exists only where StirlingPDF is
            // configured and the async generation ran. Otherwise fall back to the always-available
            // Tika text preview (generated synchronously on every document change).
            DocumentPreview pdf = null;
            try {
                pdf = cases.getDocumentPreview(id, DocumentPreview.TYPE_PDF);
            } catch (Exception noPdf) {
                // no cached PDF preview — fall back to text below
            }
            if (pdf != null && pdf.getBytes() != null) {
                out.put("kind", "pdf");
                out.put("base64content", Base64.getEncoder().encodeToString(pdf.getBytes()));
            } else {
                DocumentPreview txt = null;
                try {
                    txt = cases.getDocumentPreview(id, DocumentPreview.TYPE_TEXT);
                } catch (Exception noText) {
                    // neither preview available
                }
                out.put("kind", "text");
                out.put("text", (txt != null && txt.getText() != null) ? txt.getText() : "");
            }
            return Response.ok(out).build();
        } catch (Exception ex) {
            log.error("Can not build PDF preview for document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Structured preview of an e-mail document (.eml): parses the MIME message and returns
     * headers, an HTML or plain-text body and the attachment list. Mirrors the Swing e-mail
     * viewer. Case documents only.
     *
     * @param id the document id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/document/{id}/eml")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a structured preview of an e-mail (.eml) document")
    public Response getDocumentEmlPreview(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            try {
                cases.getDocument(id); // enforces the per-case ACL for the caller
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            byte[] content = cases.getDocumentContent(id);

            javax.mail.Session session = javax.mail.Session.getInstance(new java.util.Properties());
            javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session,
                    new java.io.ByteArrayInputStream(content == null ? new byte[0] : content));

            StringBuilder html = new StringBuilder();
            StringBuilder text = new StringBuilder();
            List<Map<String, Object>> atts = new ArrayList<>();
            walkMimePart(msg, html, text, atts);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("subject", nullSafe(decodeMime(msg.getSubject())));
            out.put("from", addressList(msg.getFrom()));
            out.put("to", addressList(msg.getRecipients(javax.mail.Message.RecipientType.TO)));
            out.put("cc", addressList(msg.getRecipients(javax.mail.Message.RecipientType.CC)));
            out.put("date", msg.getSentDate() != null ? formatDate(msg.getSentDate()) : "");
            out.put("htmlBody", html.toString());
            out.put("textBody", html.length() > 0 ? "" : text.toString());
            out.put("attachments", atts);
            return Response.ok(out).build();
        } catch (Exception ex) {
            log.error("Can not build EML preview for document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Structured preview of a beA message document (.bea): unmarshals the stored JAXB
     * representation and returns headers, the message body and the attachment list. Mirrors the
     * Swing beA viewer. Case documents only.
     *
     * @param id the document id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/document/{id}/bea")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a structured preview of a beA (.bea) message document")
    public Response getDocumentBeaPreview(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            try {
                cases.getDocument(id); // enforces the per-case ACL for the caller
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            byte[] content = cases.getDocumentContent(id);
            if (content == null) {
                content = new byte[0];
            }

            com.jdimension.jlawyer.services.bea.rest.BeaMessage msg = unmarshalBeaMessage(content);

            StringBuilder from = new StringBuilder();
            if (msg.getSenderName() != null) {
                from.append(msg.getSenderName());
            }
            if (msg.getSenderSafeId() != null && !msg.getSenderSafeId().isEmpty()) {
                from.append(from.length() > 0 ? " (" : "(").append(msg.getSenderSafeId()).append(")");
            }
            StringBuilder to = new StringBuilder();
            if (msg.getRecipients() != null) {
                for (com.jdimension.jlawyer.services.bea.rest.BeaRecipient r : msg.getRecipients()) {
                    if (to.length() > 0) {
                        to.append(", ");
                    }
                    if (r.getName() != null) {
                        to.append(r.getName());
                    }
                    if (r.getSafeId() != null && !r.getSafeId().isEmpty()) {
                        to.append(to.length() > 0 && r.getName() != null ? " (" : "(").append(r.getSafeId()).append(")");
                    }
                }
            }
            java.util.Date sent = msg.getReceptionTime() != null ? msg.getReceptionTime() : msg.getCreatedTime();
            List<Map<String, Object>> atts = new ArrayList<>();
            if (msg.getAttachments() != null) {
                for (com.jdimension.jlawyer.services.bea.rest.BeaAttachment a : msg.getAttachments()) {
                    String name = a.getName() != null ? a.getName() : a.getAlias();
                    atts.add(attachment(name, a.getSize()));
                }
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("subject", nullSafe(msg.getSubject()));
            out.put("from", from.toString());
            out.put("to", to.toString());
            out.put("sent", sent != null ? formatDate(sent) : "");
            out.put("caseNumber", nullSafe(msg.getReferenceNumber()));
            out.put("justizReference", nullSafe(msg.getReferenceJustice()));
            out.put("body", nullSafe(msg.getBody()));
            out.put("attachments", atts);
            return Response.ok(out).build();
        } catch (Exception ex) {
            log.error("Can not build beA preview for document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the given case's link with that id, or null if the case has no such link - so a link
     * id of another case (or one the caller may not see) maps to a 404 instead of acting on it.
     */
    private static CaseLinkDTO findLinkInCase(ArchiveFileServiceLocal cases, String caseId, String linkId) throws Exception {
        if (linkId == null) {
            return null;
        }
        for (CaseLinkDTO link : cases.getCaseLinks(caseId)) {
            if (linkId.equals(link.getLinkId())) {
                return link;
            }
        }
        return null;
    }

    /** Unmarshals a stored .bea document (JAXB-serialised {@link com.jdimension.jlawyer.services.bea.rest.BeaMessage}). */
    private static com.jdimension.jlawyer.services.bea.rest.BeaMessage unmarshalBeaMessage(byte[] xml) throws Exception {
        javax.xml.bind.JAXBContext ctx = javax.xml.bind.JAXBContext.newInstance(
                com.jdimension.jlawyer.services.bea.rest.BeaMessage.class);
        javax.xml.bind.Unmarshaller u = ctx.createUnmarshaller();
        try {
            return (com.jdimension.jlawyer.services.bea.rest.BeaMessage) u.unmarshal(new java.io.ByteArrayInputStream(xml));
        } catch (javax.xml.bind.UnmarshalException ex) {
            // fallback: data may be encoded in ISO-8859-1 instead of UTF-8 (mirrors the Swing client)
            java.io.InputStreamReader reader = new java.io.InputStreamReader(
                    new java.io.ByteArrayInputStream(xml), java.nio.charset.StandardCharsets.ISO_8859_1);
            return (com.jdimension.jlawyer.services.bea.rest.BeaMessage) u.unmarshal(new javax.xml.transform.stream.StreamSource(reader));
        }
    }

    /**
     * Recursively walks a MIME part, collecting the HTML body, the plain-text body and any
     * attachments (parts that carry a file name). Prefers HTML over plain text for display.
     */
    private void walkMimePart(javax.mail.Part part, StringBuilder html, StringBuilder text,
            List<Map<String, Object>> atts) throws Exception {
        String fileName = null;
        try {
            fileName = part.getFileName();
        } catch (Exception ignore) {
            // some malformed parts throw on getFileName — treat as bodyless
        }
        if (fileName != null && !fileName.isEmpty()) {
            atts.add(attachment(decodeMime(fileName), (long) part.getSize()));
            return;
        }
        if (part.isMimeType("multipart/*")) {
            javax.mail.Multipart mp = (javax.mail.Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                walkMimePart(mp.getBodyPart(i), html, text, atts);
            }
        } else if (part.isMimeType("text/html")) {
            html.append(String.valueOf(part.getContent()));
        } else if (part.isMimeType("text/plain")) {
            text.append(String.valueOf(part.getContent()));
        }
        // other single parts without a file name (e.g. inline images without a name) are ignored
    }

    /** Builds an {name,size} attachment map for the preview payload. */
    private static Map<String, Object> attachment(String name, long size) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", nullSafe(name));
        a.put("size", size);
        return a;
    }

    /** Joins mail addresses to a display string, MIME-decoding personal names. */
    private static String addressList(javax.mail.Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (javax.mail.Address a : addresses) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (a instanceof javax.mail.internet.InternetAddress) {
                sb.append(((javax.mail.internet.InternetAddress) a).toUnicodeString());
            } else {
                sb.append(decodeMime(a.toString()));
            }
        }
        return sb.toString();
    }

    /** MIME-decodes an encoded-word header value, falling back to the raw value on error. */
    private static String decodeMime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return javax.mail.internet.MimeUtility.decodeText(value);
        } catch (Exception ex) {
            return value;
        }
    }

    private static String formatDate(java.util.Date d) {
        return new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm").format(d);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private Response list(boolean activeOnly) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            ArrayList<String> ids = cases.getAllArchiveFileIds();
            ArrayList<RestfulCaseOverviewV8> result = new ArrayList<>();
            for (String id : ids) {
                ArchiveFileBean afb;
                try {
                    afb = cases.getArchiveFile(id);
                } catch (Throwable t) {
                    log.error("Case not accessible: " + id, t);
                    continue;
                }
                if (afb == null || (activeOnly && afb.isArchived())) {
                    continue;
                }
                result.add(RestfulCaseOverviewV8.fromArchiveFile(afb));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not list cases", ex);
            return RestErrorResponses.serverError(ex);
        }
    }
    private static final String LOOKUP_MESSAGING = "java:global/j-lawyer-server/j-lawyer-server-ejb/MessagingService!com.jdimension.jlawyer.services.MessagingServiceLocal";

    /**
     * Returns the documents of a case with all of their metadata, tags and the number of linked
     * instant messages.
     *
     * @param id case ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/documents")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the documents of a case with metadata, tags and message counts", response = RestfulDocumentV8.class, responseContainer = "List")
    public Response getCaseDocuments(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            MessagingServiceLocal messaging = (MessagingServiceLocal) ic.lookup(LOOKUP_MESSAGING);
            try {
                if (cases.getArchiveFile(id) == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            ArrayList<RestfulDocumentV8> result = new ArrayList<>();
            ArrayList<String> ids = new ArrayList<>();
            for (ArchiveFileDocumentsBean doc : cases.getDocuments(id)) {
                result.add(RestfulDocumentV8.fromDocumentsBean(doc));
                ids.add(doc.getId());
            }
            Map<String, Integer> messageCounts = messaging.getMessageCountsForDocuments(id);
            HashMap<String, ArrayList<DocumentTagsBean>> tagMap = ids.isEmpty() ? new HashMap<>() : cases.getDocumentTags(ids);
            for (RestfulDocumentV8 d : result) {
                d.setMessageCount(messageCounts.getOrDefault(d.getId(), 0));
                d.setTags(toRestTags(tagMap == null ? null : tagMap.get(d.getId())));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get documents of case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    private static List<RestfulDocumentTagV8> toRestTags(List<DocumentTagsBean> tags) {
        ArrayList<RestfulDocumentTagV8> result = new ArrayList<>();
        if (tags != null) {
            for (DocumentTagsBean t : tags) {
                if (t != null && t.getTagName() != null) {
                    result.add(new RestfulDocumentTagV8(t.getTagName(), t.getTagValue()));
                }
            }
        }
        return result;
    }

    private RestfulDocumentV8 toRestDocument(ArchiveFileServiceLocal cases, MessagingServiceLocal messaging, ArchiveFileDocumentsBean doc) throws Exception {
        RestfulDocumentV8 d = RestfulDocumentV8.fromDocumentsBean(doc);
        d.setMessageCount(messaging.getMessagesForDocument(doc.getId()).size());
        ArrayList<DocumentTagsBean> tags = new ArrayList<>(cases.getDocumentTags(doc.getId()));
        d.setTags(toRestTags(tags));
        return d;
    }

    /**
     * Returns a single case document with all of its metadata.
     *
     * @param id document ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/{id}")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a case document with all of its metadata", response = RestfulDocumentV8.class)
    public Response getDocumentWithMetadata(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            MessagingServiceLocal messaging = (MessagingServiceLocal) ic.lookup(LOOKUP_MESSAGING);
            ArchiveFileDocumentsBean doc;
            try {
                doc = cases.getDocument(id);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (doc == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(toRestDocument(cases, messaging, doc)).build();
        } catch (Exception ex) {
            log.error("Can not get document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Replaces the metadata of a case document (title, keywords, received date, correspondent,
     * parent). The document content, version and change date stay unchanged.
     *
     * @param id document ID
     * @param body the new metadata
     * @response 400 Missing body
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     * @response 500 Invalid parent (other case, cycle)
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/{id}/metadata")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Replaces the metadata of a case document", response = RestfulDocumentV8.class)
    public Response updateDocumentMetadata(@PathParam("id") String id, RestfulDocumentMetadataV8 body) {
        try {
            if (body == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            MessagingServiceLocal messaging = (MessagingServiceLocal) ic.lookup(LOOKUP_MESSAGING);
            try {
                cases.getDocument(id);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ArchiveFileDocumentsBean doc = cases.updateDocumentMetadata(id, body.toDocumentMetadata());
            return Response.ok(toRestDocument(cases, messaging, doc)).build();
        } catch (Exception ex) {
            log.error("Can not update metadata of document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Changes the metadata of several documents at once. Only the activated fields are changed.
     *
     * @param body the documents and the change
     * @response 400 Missing body or unknown keyword operation
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/metadata")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Changes the metadata of several case documents", response = RestfulDocumentV8.class, responseContainer = "List")
    public Response updateDocumentsMetadata(RestfulDocumentMetadataPatchV8 body) {
        try {
            if (body == null || body.getDocumentIds() == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            com.jdimension.jlawyer.services.DocumentMetadataPatch patch;
            try {
                patch = body.toPatch();
            } catch (IllegalArgumentException iae) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            ArrayList<RestfulDocumentV8> result = new ArrayList<>();
            for (ArchiveFileDocumentsBean doc : cases.updateDocumentsMetadata(body.getDocumentIds(), patch)) {
                result.add(RestfulDocumentV8.fromDocumentsBean(doc));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not update metadata of documents", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Sets or removes the parent of a case document.
     *
     * @param id document ID
     * @param body the parent id, null to remove the parent
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     * @response 500 Invalid parent (other case, deleted, cycle)
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/{id}/parent")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Sets or removes the parent of a case document", response = RestfulDocumentV8.class)
    public Response setDocumentParent(@PathParam("id") String id, RestfulDocumentParentV8 body) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            try {
                cases.getDocument(id);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ArchiveFileDocumentsBean doc = cases.setDocumentParent(id, body == null ? null : body.getParentId());
            return Response.ok(RestfulDocumentV8.fromDocumentsBean(doc)).build();
        } catch (Exception ex) {
            log.error("Can not set parent of document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns all keywords used by the documents of a case, for auto completion.
     *
     * @param id case ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/documents/keywords")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the keywords used by the documents of a case", response = String.class, responseContainer = "List")
    public Response getDocumentKeywordsForCase(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            try {
                return Response.ok(cases.getDocumentKeywordsForCase(id)).build();
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception ex) {
            log.error("Can not get document keywords of case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the instant messages linked to a case document, oldest first.
     *
     * @param id document ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/documents/{id}/messages")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the instant messages linked to a case document", response = RestfulInstantMessageV7.class, responseContainer = "List")
    public Response getDocumentMessages(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            MessagingServiceLocal messaging = (MessagingServiceLocal) ic.lookup(LOOKUP_MESSAGING);
            List<InstantMessage> messages;
            try {
                messages = messaging.getMessagesForDocument(id);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ArrayList<RestfulInstantMessageV7> result = new ArrayList<>();
            for (InstantMessage im : messages) {
                result.add(RestfulInstantMessageV7.fromInstantMessage(im));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get messages of document " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Resolves the correspondent ("Von/An") of a document from a message address, the same way
     * the desktop client does when saving a message to a case: the parties of the case are
     * searched first, then all contacts. If no contact matches, the display name (or the key) is
     * returned as free text without a contact reference. Nothing is stored.
     *
     * @param id case ID
     * @param keyType the kind of the key: "email", "safeid" or "fax"
     * @param key the e-mail address, beA SafeId or fax number
     * @param name the display name given by the message, optional
     * @param direction 1 (incoming) or 2 (outgoing)
     * @response 400 Unknown key type
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Case not found / not accessible
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/documents/correspondent")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Resolves the correspondent of a document from a message address", response = RestfulDocumentMetadataV8.class)
    public Response resolveDocumentCorrespondent(@PathParam("id") String id, @QueryParam("keyType") @DefaultValue("email") String keyType,
            @QueryParam("key") @DefaultValue("") String key, @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("direction") @DefaultValue("1") int direction) {
        try {
            int type;
            switch (keyType == null ? "" : keyType.toLowerCase()) {
                case "email":
                    type = DocumentMetadata.KEY_EMAIL;
                    break;
                case "safeid":
                    type = DocumentMetadata.KEY_BEA_SAFEID;
                    break;
                case "fax":
                    type = DocumentMetadata.KEY_FAX;
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST).build();
            }
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            DocumentMetadata md;
            try {
                md = cases.resolveCorrespondent(id, type, key, name, direction);
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            RestfulDocumentMetadataV8 result = new RestfulDocumentMetadataV8();
            if (md != null) {
                result.setCorrespondentId(md.getCorrespondentId());
                result.setCorrespondentName(md.getCorrespondentName());
                result.setCorrespondentDirection(md.getCorrespondentDirection());
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not resolve correspondent for case " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

}
