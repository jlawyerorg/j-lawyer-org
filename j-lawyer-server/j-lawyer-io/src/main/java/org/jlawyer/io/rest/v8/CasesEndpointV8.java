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

import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileHistoryBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseHistoryV8;
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
            return Response.serverError().build();
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
            return Response.serverError().build();
        }
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
            return Response.serverError().build();
        }
    }
}
