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

import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.services.AddressServiceLocal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulContactOverviewV8;
import org.jlawyer.io.rest.v8.pojo.RestfulContactPageV8;

/**
 * v8 contacts list endpoint returning a richer, server-paginated overview than v1 (adds title,
 * salutation, country, mobile, e-mail and website) so a contact-list UI can render columns,
 * distinguish people from companies and filter/search server-side without a per-row detail
 * fetch — OpenSpec change {@code add-web-client}. Additive: v1 {@code /list} is unchanged. The
 * full contact detail is served by the existing {@code GET /v1/contacts/{id}}.
 *
 * @author jens
 */
@Stateless
@Path("/v8/contacts")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Contacts"})
public class ContactsEndpointV8 implements ContactsEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(ContactsEndpointV8.class.getName());
    private static final String LOOKUP_ADDRESSES = "java:global/j-lawyer-server/j-lawyer-server-ejb/AddressService!com.jdimension.jlawyer.services.AddressServiceLocal";

    /**
     * Returns one server-paginated, filtered page of contacts.
     *
     * @param offset 0-based row offset (default 0)
     * @param limit  page size (default 50, clamped server-side)
     * @param filter one of {@code all} | {@code people} (no company) | {@code companies} (has a company)
     * @param q      optional case-insensitive search over name/first name/company/city/zip/e-mail
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/page")
    @RolesAllowed({"readAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns a server-paginated, filtered page of contacts", response = RestfulContactPageV8.class)
    public Response listPage(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("q") String q) {
        try {
            String kind = null;
            if ("people".equalsIgnoreCase(filter)) {
                kind = "people";
            } else if ("companies".equalsIgnoreCase(filter)) {
                kind = "companies";
            }
            String search = (q == null || q.trim().isEmpty()) ? null : q.trim();

            InitialContext ic = new InitialContext();
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESSES);
            long total = addresses.countContacts(search, kind);
            List<AddressBean> page = addresses.getContactsPage(search, kind, offset, limit);

            ArrayList<RestfulContactOverviewV8> items = new ArrayList<>();
            for (AddressBean a : page) {
                items.add(RestfulContactOverviewV8.fromAddressBean(a));
            }
            return Response.ok(new RestfulContactPageV8(total, offset, limit, items)).build();
        } catch (Exception ex) {
            log.error("Can not list contacts page", ex);
            return RestErrorResponses.serverError(ex);
        }
    }
}
