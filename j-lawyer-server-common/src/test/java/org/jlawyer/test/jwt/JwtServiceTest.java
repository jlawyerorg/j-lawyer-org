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
package org.jlawyer.test.jwt;

import com.jdimension.jlawyer.security.jwt.JwtClaims;
import com.jdimension.jlawyer.security.jwt.JwtException;
import com.jdimension.jlawyer.security.jwt.JwtService;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Round-trip and failure-mode tests for {@link JwtService}. Uses a freshly generated RSA
 * key pair; time is injected so expiry is deterministic.
 *
 * @author jens
 */
public class JwtServiceTest {

    private static final String ISSUER = "j-lawyer";
    private static final String AUDIENCE = "j-lawyer-web";
    private static final long NOW = 1_700_000_000L;

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;
    private final JwtService jwt = new JwtService();

    @BeforeClass
    public static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        otherKeyPair = gen.generateKeyPair();
    }

    @Test
    public void issuedTokenVerifiesAndCarriesClaims() throws Exception {
        String token = jwt.issue(keyPair.getPrivate(), "admin",
                Arrays.asList("loginRole", "adminRole"), ISSUER, AUDIENCE, NOW, 900, "tok-1");

        assertEquals("compact serialization has three segments", 3, token.split("\\.").length);

        JwtClaims claims = jwt.verify(keyPair.getPublic(), token, ISSUER, AUDIENCE, NOW + 10, 30);
        assertEquals("admin", claims.getSubject());
        assertEquals(Arrays.asList("loginRole", "adminRole"), claims.getRoles());
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals(AUDIENCE, claims.getAudience());
        assertEquals(NOW, claims.getIssuedAt());
        assertEquals(NOW + 900, claims.getExpiresAt());
        assertEquals("tok-1", claims.getTokenId());
    }

    @Test
    public void expiredTokenIsRejectedButLeewayIsHonoured() throws Exception {
        String token = jwt.issue(keyPair.getPrivate(), "admin",
                Collections.singletonList("loginRole"), ISSUER, AUDIENCE, NOW, 60, "tok-2");

        // within leeway: still accepted one second past expiry
        jwt.verify(keyPair.getPublic(), token, ISSUER, AUDIENCE, NOW + 61, 30);

        // beyond expiry + leeway: rejected
        expectFailure(token, ISSUER, AUDIENCE, NOW + 200, 30, "expired");
    }

    @Test
    public void tamperedPayloadBreaksSignature() throws Exception {
        String token = jwt.issue(keyPair.getPrivate(), "admin",
                Collections.singletonList("loginRole"), ISSUER, AUDIENCE, NOW, 900, "tok-3");
        String[] parts = token.split("\\.");
        // forge a payload claiming a different subject, re-encoded but not re-signed
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"attacker\",\"roles\":[\"adminRole\"],\"exp\":9999999999}".getBytes("UTF-8"));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];
        expectFailure(forged, ISSUER, AUDIENCE, NOW + 10, 30, "invalid signature");
    }

    @Test
    public void wrongKeyIsRejected() throws Exception {
        String token = jwt.issue(keyPair.getPrivate(), "admin",
                Collections.singletonList("loginRole"), ISSUER, AUDIENCE, NOW, 900, "tok-4");
        try {
            jwt.verify(otherKeyPair.getPublic(), token, ISSUER, AUDIENCE, NOW + 10, 30);
            fail("expected verification with the wrong public key to fail");
        } catch (JwtException expected) {
            assertTrue(expected.getMessage().contains("invalid signature"));
        }
    }

    @Test
    public void issuerAndAudienceMismatchAreRejected() throws Exception {
        String token = jwt.issue(keyPair.getPrivate(), "admin",
                Collections.singletonList("loginRole"), ISSUER, AUDIENCE, NOW, 900, "tok-5");
        expectFailure(token, "someone-else", AUDIENCE, NOW + 10, 30, "issuer");
        expectFailure(token, ISSUER, "some-other-app", NOW + 10, 30, "audience");
    }

    @Test
    public void malformedTokenIsRejected() {
        expectFailure("not-a-jwt", ISSUER, AUDIENCE, NOW, 30, "malformed");
    }

    private void expectFailure(String token, String issuer, String audience, long now, long leeway, String messageFragment) {
        try {
            jwt.verify(keyPair.getPublic(), token, issuer, audience, now, leeway);
            fail("expected JwtException containing '" + messageFragment + "'");
        } catch (JwtException expected) {
            assertTrue("message should mention '" + messageFragment + "' but was: " + expected.getMessage(),
                    expected.getMessage().contains(messageFragment));
        }
    }
}
