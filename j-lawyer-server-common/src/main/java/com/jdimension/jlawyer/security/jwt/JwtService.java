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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Minimal, dependency-free RS256 (RSA + SHA-256) JSON Web Token issuer/verifier.
 *
 * <p>Deliberately uses only the JDK ({@link java.security.Signature}, {@link Base64}) plus
 * the json-simple library already on the classpath — no third-party JWT dependency is
 * introduced (see OpenSpec change {@code add-web-client}, design.md Decision 5 and the
 * supply-chain hardening goal). RS256 (asymmetric) is chosen over HS256 so the WildFly
 * Elytron {@code token-realm} can verify tokens with the public key/certificate while the
 * login service signs with the private key.</p>
 *
 * <p>Time is passed in explicitly (epoch seconds) rather than read from a hidden clock, so
 * issuing and verification — including expiry — are deterministically testable.</p>
 *
 * @author jens
 */
public class JwtService {

    /** JOSE algorithm identifier for RSASSA-PKCS1-v1_5 using SHA-256. */
    public static final String ALGORITHM = "RS256";
    private static final String JCA_SIGNATURE = "SHA256withRSA";

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /**
     * Issues a signed compact-serialization JWT.
     *
     * @param signingKey RSA private key used to sign (RS256)
     * @param subject    the principal ({@code sub} claim)
     * @param roles      granted roles ({@code roles} claim); may be empty, not {@code null}
     * @param issuer     the {@code iss} claim, or {@code null} to omit
     * @param audience   the {@code aud} claim, or {@code null} to omit
     * @param nowEpochSeconds current time in epoch seconds ({@code iat})
     * @param ttlSeconds  token lifetime in seconds ({@code exp = iat + ttl})
     * @param tokenId     the unique token id ({@code jti}), or {@code null} to omit
     * @return the compact-serialized token {@code header.payload.signature}
     * @throws JwtException if signing fails
     */
    public String issue(PrivateKey signingKey, String subject, List<String> roles, String issuer,
            String audience, long nowEpochSeconds, long ttlSeconds, String tokenId) throws JwtException {
        JsonObject header = new JsonObject();
        header.put("alg", ALGORITHM);
        header.put("typ", "JWT");

        JsonObject payload = new JsonObject();
        payload.put("sub", subject);
        JsonArray rolesArray = new JsonArray();
        if (roles != null) {
            rolesArray.addAll(roles);
        }
        payload.put("roles", rolesArray);
        if (issuer != null) {
            payload.put("iss", issuer);
        }
        if (audience != null) {
            payload.put("aud", audience);
        }
        payload.put("iat", nowEpochSeconds);
        payload.put("exp", nowEpochSeconds + ttlSeconds);
        if (tokenId != null) {
            payload.put("jti", tokenId);
        }

        String signingInput = base64Url(Jsoner.serialize(header)) + "." + base64Url(Jsoner.serialize(payload));
        byte[] signature = sign(signingKey, signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + ENCODER.encodeToString(signature);
    }

    /**
     * Verifies a token's signature, expiry and (if requested) issuer/audience, and returns
     * its claims. Every failure mode throws {@link JwtException}; a returned value is a
     * fully validated token.
     *
     * @param verificationKey RSA public key matching the signing key
     * @param token           the compact-serialized token
     * @param expectedIssuer  required {@code iss}, or {@code null} to skip the check
     * @param expectedAudience required {@code aud}, or {@code null} to skip the check
     * @param nowEpochSeconds current time in epoch seconds
     * @param leewaySeconds   clock-skew tolerance applied to the expiry check
     * @return the validated claims
     * @throws JwtException if the token is malformed, wrongly signed, expired, or fails a check
     */
    public JwtClaims verify(PublicKey verificationKey, String token, String expectedIssuer,
            String expectedAudience, long nowEpochSeconds, long leewaySeconds) throws JwtException {
        if (token == null) {
            throw new JwtException("token is null");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("malformed token (expected three dot-separated segments)");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] signature;
        try {
            signature = DECODER.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new JwtException("signature is not valid base64url", ex);
        }
        if (!verifySignature(verificationKey, signingInput.getBytes(StandardCharsets.US_ASCII), signature)) {
            throw new JwtException("invalid signature");
        }

        JsonObject header = decodeSegment(parts[0], "header");
        if (!ALGORITHM.equals(header.get("alg"))) {
            throw new JwtException("unexpected algorithm: " + header.get("alg"));
        }

        JsonObject payload = decodeSegment(parts[1], "payload");
        long exp = asLong(payload.get("exp"), "exp");
        if (nowEpochSeconds > exp + leewaySeconds) {
            throw new JwtException("token expired");
        }
        if (expectedIssuer != null && !expectedIssuer.equals(payload.get("iss"))) {
            throw new JwtException("issuer mismatch");
        }
        if (expectedAudience != null && !expectedAudience.equals(payload.get("aud"))) {
            throw new JwtException("audience mismatch");
        }

        List<String> roles = new ArrayList<>();
        Object rolesClaim = payload.get("roles");
        if (rolesClaim instanceof JsonArray) {
            for (Object role : (JsonArray) rolesClaim) {
                if (role != null) {
                    roles.add(String.valueOf(role));
                }
            }
        }
        return new JwtClaims(
                (String) payload.get("sub"),
                roles,
                (String) payload.get("iss"),
                (String) payload.get("aud"),
                payload.containsKey("iat") ? asLong(payload.get("iat"), "iat") : 0L,
                exp,
                (String) payload.get("jti"));
    }

    private JsonObject decodeSegment(String segment, String name) throws JwtException {
        try {
            String json = new String(DECODER.decode(segment), StandardCharsets.UTF_8);
            Object parsed = Jsoner.deserialize(json);
            if (!(parsed instanceof JsonObject)) {
                throw new JwtException(name + " is not a JSON object");
            }
            return (JsonObject) parsed;
        } catch (IllegalArgumentException | org.json.simple.DeserializationException ex) {
            throw new JwtException("cannot decode " + name, ex);
        }
    }

    private static String base64Url(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sign(PrivateKey key, byte[] data) throws JwtException {
        try {
            Signature sig = Signature.getInstance(JCA_SIGNATURE);
            sig.initSign(key);
            sig.update(data);
            return sig.sign();
        } catch (GeneralSecurityException ex) {
            throw new JwtException("unable to sign token", ex);
        }
    }

    private static boolean verifySignature(PublicKey key, byte[] data, byte[] signature) throws JwtException {
        try {
            Signature sig = Signature.getInstance(JCA_SIGNATURE);
            sig.initVerify(key);
            sig.update(data);
            return sig.verify(signature);
        } catch (GeneralSecurityException ex) {
            throw new JwtException("unable to verify signature", ex);
        }
    }

    private static long asLong(Object value, String claim) throws JwtException {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new JwtException("claim '" + claim + "' is missing or not numeric");
    }
}
