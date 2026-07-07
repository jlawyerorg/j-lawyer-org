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

import com.jdimension.jlawyer.security.jwt.JwtClaims;
import com.jdimension.jlawyer.security.jwt.JwtException;
import com.jdimension.jlawyer.security.jwt.JwtService;
import com.jdimension.jlawyer.services.SecurityServiceLocal;
import java.util.List;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.CookieParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.LoginRequestV8;
import org.jlawyer.io.rest.v8.pojo.TokenResponseV8;

/**
 * Browser-friendly authentication endpoints for the web UI (OpenSpec change
 * {@code add-web-client}, Decision 5). This is the <b>additive</b> login path that sits
 * next to the unchanged HTTP Basic auth used by existing REST clients and the EJB-based
 * Swing client.
 *
 * <p>On login the server validates the credentials against the same user/role store as
 * HTTP Basic (via {@code SecurityService.authenticateAndGetRoles}), then issues a
 * short-lived RS256 <b>access token</b> (returned in the body, sent by the SPA as
 * {@code Authorization: Bearer}) and a longer-lived <b>refresh token</b> delivered as an
 * httpOnly cookie. WildFly Elytron's {@code token-realm} verifies the access token on
 * protected endpoints and establishes the caller identity, so EJB-level {@code @RolesAllowed}
 * and {@code getCallerPrincipal()} work exactly as with Basic.</p>
 *
 * <p>These three operations are the only public ({@code @PermitAll}-equivalent) paths;
 * {@code /rest/v8/auth/*} is excluded from the war's security-constraint so the login can be
 * reached without prior authentication. A 401 here carries no {@code WWW-Authenticate: Basic}
 * header, so browsers do not raise the native Basic dialog.</p>
 *
 * @author jens
 */
@Stateless
@Path("/v8/auth")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Authentication"})
public class AuthenticationEndpointV8 implements AuthenticationEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(AuthenticationEndpointV8.class.getName());
    private static final String LOOKUP_SECURITY = "java:global/j-lawyer-server/j-lawyer-server-ejb/SecurityService!com.jdimension.jlawyer.services.SecurityServiceLocal";

    private static final String ISSUER = "j-lawyer";
    /** Audience of access tokens — must match the WildFly Elytron token-realm configuration. */
    private static final String AUDIENCE_ACCESS = "j-lawyer-web";
    /** Audience of refresh tokens — verified only by this endpoint, never by Elytron. */
    private static final String AUDIENCE_REFRESH = "j-lawyer-web-refresh";
    private static final long ACCESS_TTL_SECONDS = 15 * 60L;
    private static final long REFRESH_TTL_SECONDS = 8 * 60 * 60L;
    private static final long CLOCK_SKEW_SECONDS = 30L;

    private static final String REFRESH_COOKIE = "JLAWYER_REFRESH";
    private static final String COOKIE_PATH = "/j-lawyer-io/rest/v8/auth";

    @Context
    private SecurityContext securityContext;

    private final JwtService jwt = new JwtService();

    /**
     * Authenticates a user and issues an access token plus a refresh cookie.
     *
     * @param request the credentials ({@code username}, {@code password}, optional {@code otp})
     * @response 200 Authenticated; body holds the access token, cookie holds the refresh token
     * @response 400 Missing username or password
     * @response 401 Invalid credentials
     */
    @Override
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @io.swagger.annotations.ApiOperation(value = "Authenticates a user and issues a JWT access token plus a refresh cookie", response = TokenResponseV8.class)
    public Response login(LoginRequestV8 request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return error(Response.Status.BAD_REQUEST, "missing_credentials");
        }
        try {
            InitialContext ic = new InitialContext();
            SecurityServiceLocal security = (SecurityServiceLocal) ic.lookup(LOOKUP_SECURITY);
            List<String> roles = security.authenticateAndGetRoles(request.getUsername(), request.getPassword());
            if (roles == null) {
                return error(Response.Status.UNAUTHORIZED, "invalid_credentials");
            }

            // NOTE: request.getOtp() (second factor) is accepted but not yet enforced — it is
            // wired in by the add-two-factor-auth change, which owns the verification logic.

            return issueTokens(request.getUsername(), roles);
        } catch (Exception ex) {
            log.error("login failed for principal " + request.getUsername(), ex);
            return Response.serverError().build();
        }
    }

    /**
     * Issues a fresh access token from a valid refresh cookie (rotating the refresh token).
     *
     * @param refreshToken the refresh token from the httpOnly cookie
     * @response 200 New access token issued; refresh cookie rotated
     * @response 401 Missing, expired or invalid refresh token
     */
    @Override
    @POST
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @io.swagger.annotations.ApiOperation(value = "Issues a new access token from the refresh cookie", response = TokenResponseV8.class)
    public Response refresh(@CookieParam(REFRESH_COOKIE) String refreshToken) {
        if (isBlank(refreshToken)) {
            return error(Response.Status.UNAUTHORIZED, "missing_refresh_token");
        }
        try {
            long now = epochSeconds();
            JwtClaims claims = jwt.verify(JwtKeyProvider.getPublicKey(), refreshToken, ISSUER,
                    AUDIENCE_REFRESH, now, CLOCK_SKEW_SECONDS);
            // roles are carried over from the refresh token (stateless); a revoked account
            // remains valid until the refresh token expires (see design.md Risks).
            return issueTokens(claims.getSubject(), claims.getRoles());
        } catch (JwtException ex) {
            log.info("refresh rejected: " + ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .header("Set-Cookie", clearingCookie())
                    .entity("{\"error\":\"invalid_refresh_token\"}")
                    .type(MediaType.APPLICATION_JSON + ";charset=utf-8")
                    .build();
        } catch (Exception ex) {
            log.error("refresh failed", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Ends the session by clearing the refresh cookie. The short-lived access token is not
     * server-side revocable and simply expires.
     *
     * @response 204 Refresh cookie cleared
     */
    @Override
    @POST
    @Path("/logout")
    @io.swagger.annotations.ApiOperation(value = "Clears the refresh cookie")
    public Response logout() {
        return Response.noContent().header("Set-Cookie", clearingCookie()).build();
    }

    private Response issueTokens(String principal, List<String> roles) throws JwtException {
        long now = epochSeconds();
        String accessToken = jwt.issue(JwtKeyProvider.getPrivateKey(), principal, roles, ISSUER,
                AUDIENCE_ACCESS, now, ACCESS_TTL_SECONDS, UUID.randomUUID().toString());
        String refreshToken = jwt.issue(JwtKeyProvider.getPrivateKey(), principal, roles, ISSUER,
                AUDIENCE_REFRESH, now, REFRESH_TTL_SECONDS, UUID.randomUUID().toString());

        TokenResponseV8 body = new TokenResponseV8(accessToken, ACCESS_TTL_SECONDS, principal, roles);
        return Response.ok(body)
                .header("Set-Cookie", refreshCookie(refreshToken, REFRESH_TTL_SECONDS))
                .build();
    }

    private String refreshCookie(String value, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(REFRESH_COOKIE).append('=').append(value);
        cookie.append("; Path=").append(COOKIE_PATH);
        cookie.append("; Max-Age=").append(maxAgeSeconds);
        cookie.append("; HttpOnly");
        cookie.append("; SameSite=Lax");
        if (securityContext != null && securityContext.isSecure()) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    private String clearingCookie() {
        return refreshCookie("", 0L);
    }

    private Response error(Response.Status status, String code) {
        // deliberately no WWW-Authenticate header, so browsers do not show the native Basic dialog
        return Response.status(status)
                .entity("{\"error\":\"" + code + "\"}")
                .type(MediaType.APPLICATION_JSON + ";charset=utf-8")
                .build();
    }

    private static long epochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
