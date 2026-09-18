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
import com.jdimension.jlawyer.services.ContactRelationDTO;
import com.jdimension.jlawyer.services.ContactRelationExistsException;
import java.util.ArrayList;
import java.util.List;
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
import org.jlawyer.io.rest.v8.pojo.RestfulContactOverviewV8;
import org.jlawyer.io.rest.v8.pojo.RestfulContactPageV8;
import org.jlawyer.io.rest.v8.pojo.RestfulContactRelationRequestV8;
import org.jlawyer.io.rest.v8.pojo.RestfulContactRelationV8;

/**
 * v8 contacts list endpoint returning a richer, server-paginated overview than v1 (adds title,
 * salutation, country, mobile, e-mail and website) so a contact-list UI can render columns,
 * distinguish people from companies and filter/search server-side without a per-row detail
 * fetch — OpenSpec change {@code add-web-client}. Additive: v1 {@code /list} is unchanged. The
 * full contact detail is served by the existing {@code GET /v1/contacts/{id}}.
 *
 * It also serves the relationships of a contact (Kontaktbeziehungen) — OpenSpec change
 * {@code add-contact-relationships}. Relationships are never part of a contact: they form a
 * graph, so they are read only by their own call, one contact at a time. The type catalogue they
 * refer to is administered under {@code /v8/configuration/contact-relation-types}.
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

    /**
     * Returns the relationships (Kontaktbeziehungen) of a contact. Every entry describes the
     * contact at the other end as seen from the requested contact and carries the label that
     * applies from that side, so a client never has to work out which of the two stored contact
     * references is "the other one" nor which of the type's two labels to render.
     *
     * The related contacts themselves are not transported, only their identifying fields; the
     * full contact is fetched via {@code GET /v1/contacts/{id}} when the user navigates to it.
     *
     * @param id contact id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Contact not found
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/relations")
    @RolesAllowed({"readAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the relationships of a contact (each with the contact at the other end)", response = RestfulContactRelationV8.class, responseContainer = "List")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response getRelations(@PathParam("id") String id) {
        try {
            InitialContext ic = new InitialContext();
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESSES);
            if (addresses.getAddress(id) == null) {
                log.warn("contact with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<ContactRelationDTO> relations = addresses.getRelations(id);
            ArrayList<RestfulContactRelationV8> result = new ArrayList<>();
            for (ContactRelationDTO relation : relations) {
                result.add(RestfulContactRelationV8.fromContactRelationDTO(relation));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("Can not get relations for contact " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Records a relationship between the given contact and another one. The relationship is a
     * directed statement: the contact in the path is what the type's forward label says of it, the
     * contact in the body what the reverse label says. For a symmetric type both directions are
     * the same statement, so the pair is normalised and the mirrored entry is refused. The
     * relationship is returned as seen from the contact in the path and is immediately visible
     * from the other contact as well.
     *
     * The three rejections are reported through the uniform error envelope and can be told apart
     * by a client: an already recorded relationship arrives with {@code error} set to
     * {@code ContactRelationExistsException} and a {@code message} naming the type and both
     * contacts - a harmless situation the user only needs to be told about; a relationship of a
     * contact to itself and an unknown type both arrive as a plain {@code Exception} whose
     * {@code message} states which of the two it is.
     *
     * @param id   contact id, the contact the type's forward label describes
     * @param body the contact at the other end ({@code otherContactId}), the relationship type
     *             ({@code typeId}) and an optional {@code note}
     * @response 400 Missing other contact id or type id
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Contact not found
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/relations")
    @RolesAllowed({"writeAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Records a relationship between two contacts", response = RestfulContactRelationV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"), @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response createRelation(@PathParam("id") String id, @io.swagger.annotations.ApiParam RestfulContactRelationRequestV8 body) {
        try {
            if (body == null || body.getOtherContactId() == null || body.getOtherContactId().trim().isEmpty()) {
                log.warn("other contact id is required for contact " + id);
                return Response.status(Response.Status.BAD_REQUEST).entity("Other contact id is required").build();
            }
            if (body.getTypeId() == null || body.getTypeId().trim().isEmpty()) {
                log.warn("relation type id is required for contact " + id);
                return Response.status(Response.Status.BAD_REQUEST).entity("Relation type id is required").build();
            }

            InitialContext ic = new InitialContext();
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESSES);
            if (addresses.getAddress(id) == null) {
                log.warn("contact with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            ContactRelationDTO relation = addresses.addRelation(id, body.getOtherContactId().trim(),
                    body.getTypeId().trim(), body.getNote());
            return Response.ok(RestfulContactRelationV8.fromContactRelationDTO(relation)).build();
        } catch (ContactRelationExistsException ex) {
            // a harmless situation the user only needs to be told about - the error envelope carries
            // the exception type, so a client can tell "already recorded" from a real failure
            log.warn("contacts " + id + " and " + (body == null ? null : body.getOtherContactId())
                    + " already have that relationship");
            return RestErrorResponses.serverError(ex);
        } catch (Exception ex) {
            // self-relation and unknown type land here, each with its own message
            log.error("Can not add relation for contact " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Changes the free-text remark of one of a contact's relationships. Neither the other contact
     * nor the type can be changed - a different type or direction is a different statement, so it
     * is a different relationship.
     *
     * @param id         contact id
     * @param relationId id of the relationship, which must be a relationship of that contact
     * @param body       the new {@code note} (may be empty to clear it)
     * @response 400 Missing request body
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Contact not found, or the relationship does not belong to that contact
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/relations/{relationId}")
    @RolesAllowed({"writeAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Changes the note of a contact relationship", response = RestfulContactRelationV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"), @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response updateRelation(@PathParam("id") String id, @PathParam("relationId") String relationId, @io.swagger.annotations.ApiParam RestfulContactRelationRequestV8 body) {
        try {
            if (body == null) {
                log.warn("relation data is required for relation " + relationId);
                return Response.status(Response.Status.BAD_REQUEST).entity("Relation data is required").build();
            }

            InitialContext ic = new InitialContext();
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESSES);
            if (addresses.getAddress(id) == null) {
                log.warn("contact with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            ContactRelationDTO relation = findRelationOfContact(addresses, id, relationId);
            if (relation == null) {
                log.warn("relation with id " + relationId + " does not exist for contact " + id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            addresses.updateRelationNote(relationId, body.getNote());
            relation.setNote(body.getNote());
            return Response.ok(RestfulContactRelationV8.fromContactRelationDTO(relation)).build();
        } catch (Exception ex) {
            log.error("Can not update relation " + relationId + " of contact " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Removes one of a contact's relationships. Either end may remove it; removing it affects
     * neither contact and is immediately reflected at the other end.
     *
     * @param id         contact id
     * @param relationId id of the relationship, which must be a relationship of that contact
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Contact not found, or the relationship does not belong to that contact
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{id}/relations/{relationId}")
    @RolesAllowed({"writeAddressRole"})
    @io.swagger.annotations.ApiOperation(value = "Removes a relationship between two contacts")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found")})
    public Response deleteRelation(@PathParam("id") String id, @PathParam("relationId") String relationId) {
        try {
            InitialContext ic = new InitialContext();
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESSES);
            if (addresses.getAddress(id) == null) {
                log.warn("contact with id " + id + " does not exist");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (findRelationOfContact(addresses, id, relationId) == null) {
                log.warn("relation with id " + relationId + " does not exist for contact " + id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            addresses.removeRelation(relationId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("Can not remove relation " + relationId + " of contact " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the given contact's relationship with that id, or null if the contact has no such
     * relationship - so a relationship id of two other contacts maps to a 404 instead of being
     * acted on.
     */
    private static ContactRelationDTO findRelationOfContact(AddressServiceLocal addresses, String contactId, String relationId) throws Exception {
        if (relationId == null) {
            return null;
        }
        for (ContactRelationDTO relation : addresses.getRelations(contactId)) {
            if (relationId.equals(relation.getRelationId())) {
                return relation;
            }
        }
        return null;
    }
}
