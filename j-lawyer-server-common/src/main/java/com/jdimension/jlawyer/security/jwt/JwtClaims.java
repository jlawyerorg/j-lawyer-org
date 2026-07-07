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
package com.jdimension.jlawyer.security.jwt;

import java.util.Collections;
import java.util.List;

/**
 * Immutable view of the validated payload of a JSON Web Token. Returned by
 * {@link JwtService#verify}; all fields have already passed signature, expiry and
 * issuer/audience checks.
 *
 * @author jens
 */
public final class JwtClaims {

    private final String subject;
    private final List<String> roles;
    private final String issuer;
    private final String audience;
    private final long issuedAt;
    private final long expiresAt;
    private final String tokenId;

    public JwtClaims(String subject, List<String> roles, String issuer, String audience,
            long issuedAt, long expiresAt, String tokenId) {
        this.subject = subject;
        this.roles = roles == null ? Collections.emptyList() : Collections.unmodifiableList(roles);
        this.issuer = issuer;
        this.audience = audience;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.tokenId = tokenId;
    }

    /** The authenticated principal (the {@code sub} claim). */
    public String getSubject() {
        return subject;
    }

    /** Granted roles carried in the {@code roles} claim (never {@code null}). */
    public List<String> getRoles() {
        return roles;
    }

    /** The {@code iss} claim, or {@code null} if absent. */
    public String getIssuer() {
        return issuer;
    }

    /** The {@code aud} claim, or {@code null} if absent. */
    public String getAudience() {
        return audience;
    }

    /** Issued-at time as epoch seconds ({@code iat}). */
    public long getIssuedAt() {
        return issuedAt;
    }

    /** Expiry time as epoch seconds ({@code exp}). */
    public long getExpiresAt() {
        return expiresAt;
    }

    /** The unique token id ({@code jti}), or {@code null} if absent. */
    public String getTokenId() {
        return tokenId;
    }
}
