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

import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.security.jwt.JwtClaims;
import com.jdimension.jlawyer.security.jwt.JwtException;
import com.jdimension.jlawyer.security.jwt.JwtService;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.security.PermitAll;
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

/**
 * WOPI host for in-browser Office editing (OpenSpec add-web-client, Decision 6). The external
 * document server (Collabora / OnlyOffice) calls these endpoints server-to-server to read and write
 * a document; there is no j-lawyer user session, so every request is authorised by a short-lived
 * {@code access_token} (a JWT bound to the document via its {@code aud} claim and to the acting user
 * via {@code sub}) that was minted — after a full ACL check — by {@code /v8/office/editor-config}.
 * These paths are public in web.xml ({@code /rest/wopi/*}); the token IS the authorisation, which is
 * why the underlying EJB calls use the {@code *Unrestricted} methods.
 *
 * @author jens
 */
@Stateless
@Path("/wopi/files")
@PermitAll
public class WopiEndpoint {

    private static final Logger log = Logger.getLogger(WopiEndpoint.class.getName());
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";

    // The WOPI access token must pass WildFly's Elytron token-realm (standalone.xml:
    // issuer "j-lawyer", audience "j-lawyer-web") because Collabora sends it as an
    // Authorization: Bearer header — the container's BEARER_TOKEN mechanism would otherwise
    // reject it with 401 before this endpoint runs. The token therefore carries the login
    // issuer/audience but NO roles (so, if replayed elsewhere, it establishes an identity that
    // is authorised for nothing — every protected endpoint needs a role). The document binding
    // is carried in the jti claim (checked here), not in aud.
    static final String TOKEN_ISSUER = "j-lawyer";
    static final String TOKEN_AUDIENCE = "j-lawyer-web";
    /** Access-token lifetime — long enough to cover an editing session. */
    static final long WOPI_TTL_SECONDS = 12 * 60 * 60L;
    private static final long LEEWAY_SECONDS = 60L;

    private final JwtService jwt = new JwtService();

    /** WOPI CheckFileInfo: metadata + the acting user's permissions. */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFileInfo(@PathParam("id") String id, @QueryParam("access_token") String token) {
        try {
            JwtClaims claims = validate(token, id);
            ArchiveFileDocumentsBean doc = cases().getDocumentUnrestricted(id);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("BaseFileName", doc.getName());
            info.put("Size", doc.getSize());
            info.put("Version", String.valueOf(doc.getVersion()));
            info.put("OwnerId", "j-lawyer");
            info.put("UserId", claims.getSubject());
            info.put("UserFriendlyName", claims.getSubject());
            // A valid token is only ever minted after a writeArchiveFileRole + case-ACL check.
            info.put("UserCanWrite", true);
            info.put("SupportsUpdate", true);
            info.put("SupportsLocks", false);
            info.put("PostMessageOrigin", "*");
            return Response.ok(info).build();
        } catch (JwtException je) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (Exception ex) {
            log.error("WOPI CheckFileInfo failed for " + id, ex);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /** WOPI GetFile: the raw document bytes. */
    @GET
    @Path("/{id}/contents")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@PathParam("id") String id, @QueryParam("access_token") String token) {
        try {
            validate(token, id);
            byte[] bytes = cases().getDocumentContentUnrestricted(id);
            return Response.ok(bytes).build();
        } catch (JwtException je) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (Exception ex) {
            log.error("WOPI GetFile failed for " + id, ex);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /** WOPI PutFile: store the edited bytes (records the token's user in the case history). */
    @POST
    @Path("/{id}/contents")
    @Consumes(MediaType.WILDCARD)
    public Response putFile(@PathParam("id") String id, @QueryParam("access_token") String token, byte[] content) {
        try {
            JwtClaims claims = validate(token, id);
            cases().setDocumentContentUnrestricted(id, content, claims.getSubject());
            return Response.ok().build();
        } catch (JwtException je) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (Exception ex) {
            log.error("WOPI PutFile failed for " + id, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Verifies the access token and that it was issued for exactly this document ({@code aud}=id).
     *
     * @throws JwtException if the token is missing, wrongly signed, expired, or for another document
     */
    private JwtClaims validate(String token, String documentId) throws JwtException {
        long now = Instant.now().getEpochSecond();
        JwtClaims claims = jwt.verify(JwtKeyProvider.getPublicKey(), token, TOKEN_ISSUER, TOKEN_AUDIENCE, now, LEEWAY_SECONDS);
        // Document binding: the token was minted with jti = the document id it is valid for.
        if (documentId == null || !documentId.equals(claims.getTokenId())) {
            throw new JwtException("token is not valid for this document");
        }
        return claims;
    }

    private ArchiveFileServiceLocal cases() throws Exception {
        return (ArchiveFileServiceLocal) new InitialContext().lookup(LOOKUP_CASES);
    }

}
