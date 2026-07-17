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

import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.AddressDocumentsBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.server.services.settings.ServerSettingsKeys;
import com.jdimension.jlawyer.persistence.ServerSettingsBean;
import com.jdimension.jlawyer.services.AddressDocumentServiceLocal;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.SystemManagementLocal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulBinDocumentV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentBinV8;

/**
 * The document recycle bin ("Papierkorb") — the web equivalent of the desktop {@code DocumentsBinDialog}.
 * Lists soft-deleted case and address documents, restores or permanently deletes them, and exposes the
 * retention period (auto-purge after N days). Reading needs {@code loginRole} (each sub-list is still
 * gated by the caller's read role for cases/addresses and skipped when absent); restore/delete require
 * the matching write role; changing the retention period requires {@code adminRole}.
 *
 * @author jens
 */
@Stateless
@Path("/v8/documents/bin")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Documents"})
public class DocumentsBinEndpointV8 implements DocumentsBinEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(DocumentsBinEndpointV8.class.getName());
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";
    private static final String LOOKUP_ADDRESSDOCS = "java:global/j-lawyer-server/j-lawyer-server-ejb/AddressDocumentService!com.jdimension.jlawyer.services.AddressDocumentServiceLocal";
    private static final String LOOKUP_SYSMAN = "java:global/j-lawyer-server/j-lawyer-server-ejb/SystemManagement!com.jdimension.jlawyer.services.SystemManagementLocal";

    /**
     * Returns the recycle bin: retention days, total size and all soft-deleted case and address
     * documents. A caller lacking the case- or address-read role simply gets fewer entries.
     *
     * @response 200 The recycle bin
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the document recycle bin", response = RestfulDocumentBinV8.class)
    public Response getBin() {
        try {
            InitialContext ic = new InitialContext();
            RestfulDocumentBinV8 result = new RestfulDocumentBinV8();
            long total = 0L;

            try {
                ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
                Collection<ArchiveFileDocumentsBean> deleted = cases.getDocumentsBin();
                for (ArchiveFileDocumentsBean d : deleted) {
                    RestfulBinDocumentV8 dto = new RestfulBinDocumentV8();
                    dto.setType("case");
                    dto.setId(d.getId());
                    dto.setName(d.getName());
                    dto.setDeletionDate(d.getDeletionDate() != null ? d.getDeletionDate().getTime() : 0L);
                    dto.setDeletedBy(d.getDeletedBy());
                    dto.setSize(d.getSize());
                    ArchiveFileBean af = d.getArchiveFileKey();
                    if (af != null) {
                        dto.setOwnerId(af.getId());
                        dto.setOwnerLabel(af.getName());
                        dto.setOwnerReference(af.getFileNumber());
                    }
                    total += d.getSize();
                    result.getDocuments().add(dto);
                }
            } catch (Exception ex) {
                log.info("case documents bin not available for caller: " + ex.getMessage());
            }

            try {
                AddressDocumentServiceLocal addrDocs = (AddressDocumentServiceLocal) ic.lookup(LOOKUP_ADDRESSDOCS);
                Collection<AddressDocumentsBean> deleted = addrDocs.getDocumentsBin();
                for (AddressDocumentsBean d : deleted) {
                    RestfulBinDocumentV8 dto = new RestfulBinDocumentV8();
                    dto.setType("address");
                    dto.setId(d.getId());
                    dto.setName(d.getName());
                    dto.setDeletionDate(d.getDeletionDate() != null ? d.getDeletionDate().getTime() : 0L);
                    dto.setDeletedBy(d.getDeletedBy());
                    dto.setSize(d.getSize());
                    AddressBean a = d.getAddressKey();
                    if (a != null) {
                        dto.setOwnerId(a.getId());
                        dto.setOwnerLabel(contactLabel(a));
                    }
                    total += d.getSize();
                    result.getDocuments().add(dto);
                }
            } catch (Exception ex) {
                log.info("address documents bin not available for caller: " + ex.getMessage());
            }

            result.setTotalBytes(total);
            result.setRetentionDays(readRetentionDays(ic));
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not read documents bin", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Restores a soft-deleted case document from the recycle bin.
     *
     * @param id the document id
     * @response 200 Restored
     */
    @Override
    @POST
    @Path("/case/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Restores a case document from the recycle bin")
    public Response restoreCaseDocument(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            cases.restoreDocumentFromBin(id);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not restore case document " + id, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Permanently deletes a soft-deleted case document.
     *
     * @param id the document id
     * @response 200 Deleted
     */
    @Override
    @DELETE
    @Path("/case/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Permanently deletes a case document from the recycle bin")
    public Response deleteCaseDocument(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            cases.removeDocumentFromBin(id);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete case document " + id, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Restores a soft-deleted address (contact) document from the recycle bin.
     *
     * @param id the document id
     * @response 200 Restored
     */
    @Override
    @POST
    @Path("/address/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"writeAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Restores an address document from the recycle bin")
    public Response restoreAddressDocument(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            AddressDocumentServiceLocal addrDocs = (AddressDocumentServiceLocal) ic.lookup(LOOKUP_ADDRESSDOCS);
            addrDocs.restoreDocumentFromBin(id);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not restore address document " + id, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Permanently deletes a soft-deleted address (contact) document.
     *
     * @param id the document id
     * @response 200 Deleted
     */
    @Override
    @DELETE
    @Path("/address/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"writeAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Permanently deletes an address document from the recycle bin")
    public Response deleteAddressDocument(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            AddressDocumentServiceLocal addrDocs = (AddressDocumentServiceLocal) ic.lookup(LOOKUP_ADDRESSDOCS);
            addrDocs.removeDocumentFromBin(id);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete address document " + id, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Sets the recycle-bin retention period (days after which soft-deleted documents are purged
     * automatically). A value of 0 or less disables automatic purging.
     *
     * @param body the new retention period (field {@code retentionDays})
     * @response 200 Stored
     */
    @Override
    @PUT
    @Path("/retention")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Sets the recycle-bin retention period (days)")
    public Response setRetention(@io.swagger.annotations.ApiParam RestfulDocumentBinV8 body) {
        try {
            if (body == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);
            system.setSetting(ServerSettingsKeys.SERVERCONF_DOCUMENTS_BIN_RETENTIONDAYS, Integer.toString(body.getRetentionDays()));
            return Response.ok(body).build();
        } catch (Exception ex) {
            log.error("can not set bin retention days", ex);
            return Response.serverError().build();
        }
    }

    private int readRetentionDays(InitialContext ic) {
        try {
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);
            ServerSettingsBean b = system.getSetting(ServerSettingsKeys.SERVERCONF_DOCUMENTS_BIN_RETENTIONDAYS);
            if (b == null || b.getSettingValue() == null || b.getSettingValue().trim().isEmpty()) {
                return 7;
            }
            return Integer.parseInt(b.getSettingValue().trim());
        } catch (Exception ex) {
            return 7;
        }
    }

    private String contactLabel(AddressBean a) {
        StringBuilder sb = new StringBuilder();
        if (a.getFirstName() != null && !a.getFirstName().isEmpty()) {
            sb.append(a.getFirstName());
        }
        if (a.getName() != null && !a.getName().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(a.getName());
        }
        if (a.getCompany() != null && !a.getCompany().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(a.getCompany());
        }
        return sb.toString();
    }
}
