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

import com.jdimension.jlawyer.dropscan.DropscanScanbox;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.Keyword;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.DropscanServiceLocal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.tools.RestErrorResponses;

/**
 * Dropscan integration — the REST equivalent of the desktop {@code ScannerPanel} Dropscan view. Lets
 * the authenticated user list their scanboxes and mailings, preview a scanned mailing (PDF / envelope
 * image) and request a scan or destruction. Every operation is scoped to the caller's own Dropscan
 * account (the service resolves the caller principal, decrypts their token and enforces the scanbox
 * allow-list), so all methods require only {@code loginRole}. Importing a mailing into a case reuses
 * the existing document-upload endpoint ({@code PUT /v1/cases/document/create}); no import method
 * lives here.
 *
 * @author jens
 */
@Stateless
@Path("/v8/dropscan")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Dropscan"})
public class DropscanEndpointV8 implements DropscanEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(DropscanEndpointV8.class.getName());
    private static final String LOOKUP_DROPSCAN = "java:global/j-lawyer-server/j-lawyer-server-ejb/DropscanService!com.jdimension.jlawyer.services.DropscanServiceLocal";
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";
    private static final int MAX_SUGGESTED_CASES = 15;

    private DropscanServiceLocal lookup() throws Exception {
        return (DropscanServiceLocal) new InitialContext().lookup(LOOKUP_DROPSCAN);
    }

    /**
     * Returns the scanboxes of the caller's Dropscan account.
     *
     * @response 200 The scanboxes
     */
    @Override
    @GET
    @Path("/scanboxes")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the caller's Dropscan scanboxes")
    public Response getScanboxes() {
        try {
            return Response.ok(lookup().getScanboxes()).build();
        } catch (Exception ex) {
            log.error("can not list dropscan scanboxes", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the mailings of the caller's Dropscan account. Without {@code scanboxId} all scanboxes
     * are queried; {@code status} optionally filters by mailing status.
     *
     * @param scanboxId optional scanbox id (empty = all)
     * @param status optional status filter (empty = all)
     * @response 200 The mailings
     */
    @Override
    @GET
    @Path("/mailings")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the caller's Dropscan mailings")
    public Response getMailings(@QueryParam("scanboxId") String scanboxId, @QueryParam("status") String status) {
        try {
            String st = (status == null || status.trim().isEmpty()) ? null : status.trim();
            DropscanServiceLocal ds = lookup();
            List<?> mailings = (scanboxId == null || scanboxId.trim().isEmpty())
                    ? ds.getAllMailings(st)
                    : ds.getMailings(scanboxId.trim(), st);
            return Response.ok(mailings).build();
        } catch (Exception ex) {
            log.error("can not list dropscan mailings", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the scanned PDF of a mailing (for preview and for importing into a case).
     *
     * @param scanboxId the scanbox id
     * @param uuid the mailing uuid
     * @response 200 The PDF bytes
     * @response 404 The mailing has not been scanned yet
     */
    @Override
    @GET
    @Path("/scanboxes/{scanboxId}/mailings/{uuid}/pdf")
    @Produces("application/pdf")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the scanned PDF of a Dropscan mailing")
    public Response getMailingPdf(@PathParam("scanboxId") String scanboxId, @PathParam("uuid") String uuid) {
        try {
            byte[] pdf = lookup().getMailingPdf(scanboxId, uuid);
            if (pdf == null || pdf.length == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(pdf).type("application/pdf").build();
        } catch (Exception ex) {
            log.error("can not get dropscan mailing pdf " + uuid, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the envelope image of a mailing (fallback preview before scanning).
     *
     * @param scanboxId the scanbox id
     * @param uuid the mailing uuid
     * @response 200 The image bytes
     * @response 404 No envelope image available
     */
    @Override
    @GET
    @Path("/scanboxes/{scanboxId}/mailings/{uuid}/envelope")
    @Produces("image/jpeg")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the envelope image of a Dropscan mailing")
    public Response getEnvelopeImage(@PathParam("scanboxId") String scanboxId, @PathParam("uuid") String uuid) {
        try {
            byte[] img = lookup().getEnvelopeImage(scanboxId, uuid);
            if (img == null || img.length == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(img).type("image/jpeg").build();
        } catch (Exception ex) {
            log.error("can not get dropscan envelope image " + uuid, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Requests that Dropscan scan a received mailing.
     *
     * @param scanboxId the scanbox id
     * @param uuid the mailing uuid
     * @response 200 The action request result
     */
    @Override
    @POST
    @Path("/scanboxes/{scanboxId}/mailings/{uuid}/scan")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Requests a scan of a Dropscan mailing")
    public Response requestScan(@PathParam("scanboxId") String scanboxId, @PathParam("uuid") String uuid) {
        try {
            return Response.ok(lookup().requestScan(scanboxId, uuid)).build();
        } catch (Exception ex) {
            log.error("can not request dropscan scan " + uuid, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Suggests cases for a scanned mailing, matching case/reference file numbers found in the OCR
     * plaintext (same heuristic as the e-mail/beA case suggestions). Also returns any phone numbers
     * detected in the text. Empty for mailings that have not been scanned (no OCR text).
     *
     * @param scanboxId the scanbox id
     * @param uuid the mailing uuid
     * @response 200 The case suggestions
     */
    @Override
    @GET
    @Path("/scanboxes/{scanboxId}/mailings/{uuid}/case-suggestions")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Suggests cases for a scanned Dropscan mailing", response = org.jlawyer.io.rest.v7.pojo.RestfulCaseSuggestionsV7.class)
    public Response caseSuggestions(@PathParam("scanboxId") String scanboxId, @PathParam("uuid") String uuid) {
        try {
            InitialContext ic = new InitialContext();
            DropscanServiceLocal ds = (DropscanServiceLocal) ic.lookup(LOOKUP_DROPSCAN);
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);

            org.jlawyer.io.rest.v7.pojo.RestfulCaseSuggestionsV7 result = new org.jlawyer.io.rest.v7.pojo.RestfulCaseSuggestionsV7();

            String text = "";
            try {
                text = ds.getMailingPlaintext(scanboxId, uuid);
            } catch (Exception ex) {
                log.info("no OCR text for dropscan mailing " + uuid + ": " + ex.getMessage());
            }
            if (text == null) {
                text = "";
            }
            final String lower = text.toLowerCase();

            // phone numbers from the OCR text
            java.util.List<String> phones = new java.util.ArrayList<>();
            try {
                Collection<Keyword> keywords = cases.extractKeywordsFromText(text);
                if (keywords != null) {
                    for (Keyword w : keywords) {
                        if (w.getType() == Keyword.TYPE_PHONENR && w.getValue() != null && !phones.contains(w.getValue())) {
                            phones.add(w.getValue());
                        }
                    }
                }
            } catch (Throwable t) {
                log.warn("could not extract keywords from dropscan text", t);
            }
            result.setPhoneNumbers(phones);

            Map<String, org.jlawyer.io.rest.v7.pojo.RestfulSuggestedCaseV7> byId = new LinkedHashMap<>();
            if (!lower.isEmpty()) {
                // own file numbers appearing in the OCR text
                for (String fn : cases.getAllArchiveFileNumbers(true)) {
                    if (fn != null && !fn.isEmpty() && lower.contains(fn.toLowerCase())) {
                        addCase(byId, cases.getArchiveFileByFileNumber(fn), "text");
                    }
                    if (byId.size() >= MAX_SUGGESTED_CASES) {
                        break;
                    }
                }
                // referenced (foreign) file numbers appearing in the OCR text
                if (byId.size() < MAX_SUGGESTED_CASES) {
                    for (String fn : cases.getAllReferencedFileNumbers(5, true)) {
                        if (fn != null && !fn.isEmpty() && lower.contains(fn.toLowerCase())) {
                            List<ArchiveFileAddressesBean> refs = cases.getArchiveFileAddressesByReference(fn);
                            if (refs != null) {
                                for (ArchiveFileAddressesBean aab : refs) {
                                    addCase(byId, aab.getArchiveFileKey(), "reference");
                                }
                            }
                        }
                        if (byId.size() >= MAX_SUGGESTED_CASES) {
                            break;
                        }
                    }
                }
            }
            result.getSuggestedCases().addAll(byId.values());
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not get dropscan case suggestions " + uuid, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Discovers the scanboxes available for a Dropscan API token — the REST equivalent of the desktop
     * user administration's "Test / Scanboxen ermitteln" button. If the request carries an
     * {@code apiToken} it is tested directly; otherwise the stored token of the user identified by
     * {@code principalId} is used (the token is write-only in the web UI). Requires {@code adminRole}
     * as it is part of user administration. Returns the raw, unfiltered scanbox list so the admin can
     * populate the user's scanbox configuration.
     *
     * @param request the discovery request (apiToken and/or principalId)
     * @response 200 The available scanboxes
     */
    @Override
    @POST
    @Path("/discover-scanboxes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Tests a Dropscan API token and lists its scanboxes", response = org.jlawyer.io.rest.v8.pojo.RestfulDropscanScanboxV8.class, responseContainer = "List")
    public Response discoverScanboxes(org.jlawyer.io.rest.v8.pojo.RestfulDropscanDiscoverRequestV8 request) {
        try {
            String apiToken = request == null ? null : request.getApiToken();
            String principalId = request == null ? null : request.getPrincipalId();
            List<DropscanScanbox> boxes = lookup().discoverScanboxes(apiToken, principalId);
            List<org.jlawyer.io.rest.v8.pojo.RestfulDropscanScanboxV8> result = new java.util.ArrayList<>();
            if (boxes != null) {
                for (DropscanScanbox b : boxes) {
                    result.add(new org.jlawyer.io.rest.v8.pojo.RestfulDropscanScanboxV8(b.getId(), b.getNumber()));
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not discover dropscan scanboxes", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    private static void addCase(Map<String, org.jlawyer.io.rest.v7.pojo.RestfulSuggestedCaseV7> byId, ArchiveFileBean a, String source) {
        if (a == null || a.getId() == null || byId.containsKey(a.getId())) {
            return;
        }
        org.jlawyer.io.rest.v7.pojo.RestfulSuggestedCaseV7 c = new org.jlawyer.io.rest.v7.pojo.RestfulSuggestedCaseV7();
        c.setId(a.getId());
        c.setFileNumber(a.getFileNumber());
        c.setName(a.getName());
        c.setReason(a.getReason());
        c.setArchived(a.isArchived());
        c.setSource(source);
        byId.put(a.getId(), c);
    }

    /**
     * Requests that Dropscan destroy a mailing (used to mark it done after import).
     *
     * @param scanboxId the scanbox id
     * @param uuid the mailing uuid
     * @response 200 The action request result
     */
    @Override
    @POST
    @Path("/scanboxes/{scanboxId}/mailings/{uuid}/destroy")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Requests destruction of a Dropscan mailing")
    public Response requestDestroy(@PathParam("scanboxId") String scanboxId, @PathParam("uuid") String uuid) {
        try {
            return Response.ok(lookup().requestDestroy(scanboxId, uuid)).build();
        } catch (Exception ex) {
            log.error("can not request dropscan destroy " + uuid, ex);
            return RestErrorResponses.serverError(ex);
        }
    }
}
