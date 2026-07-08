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
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import java.util.ArrayList;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulCaseOverviewV8;

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
