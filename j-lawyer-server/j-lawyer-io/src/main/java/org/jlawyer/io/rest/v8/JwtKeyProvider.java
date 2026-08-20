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

import com.jdimension.jlawyer.security.jwt.JwtException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import org.jboss.logging.Logger;

/**
 * Loads the RSA key pair used to sign and verify j-lawyer web-UI JSON Web Tokens from a
 * PKCS#12 keystore. The <b>same</b> keystore's public certificate is referenced by the
 * WildFly Elytron {@code token-realm} so it can verify access tokens the server issued
 * (see OpenSpec change {@code add-web-client}, Decision 5).
 *
 * <p>Location and credentials are resolved from system properties, falling back to the
 * WildFly config directory:</p>
 * <ul>
 *   <li>{@code jlawyer.jwt.keystore} — path to the PKCS#12 file
 *       (default {@code ${jboss.server.config.dir}/j-lawyer-jwt.p12})</li>
 *   <li>{@code jlawyer.jwt.keystore.password} — keystore/key password (default {@code changeit})</li>
 *   <li>{@code jlawyer.jwt.keystore.alias} — key alias (default {@code jwt})</li>
 * </ul>
 *
 * <p>Keys are loaded once and cached. Generate the keystore with:</p>
 * <pre>keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
 *   -dname "CN=j-lawyer" -validity 3650 -storetype PKCS12 \
 *   -keystore j-lawyer-jwt.p12 -storepass changeit</pre>
 *
 * @author jens
 */
public final class JwtKeyProvider {

    private static final Logger log = Logger.getLogger(JwtKeyProvider.class.getName());
    private static final String DEFAULT_PASSWORD = "changeit";
    private static final String DEFAULT_ALIAS = "jwt";

    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;

    private JwtKeyProvider() {
    }

    /** RSA private key used to sign tokens; loads lazily on first use. */
    public static PrivateKey getPrivateKey() throws JwtException {
        ensureLoaded();
        return privateKey;
    }

    /** RSA public key used to verify the server's own (refresh) tokens; loads lazily. */
    public static PublicKey getPublicKey() throws JwtException {
        ensureLoaded();
        return publicKey;
    }

    private static void ensureLoaded() throws JwtException {
        if (privateKey != null && publicKey != null) {
            return;
        }
        synchronized (JwtKeyProvider.class) {
            if (privateKey != null && publicKey != null) {
                return;
            }
            load();
        }
    }

    private static void load() throws JwtException {
        String path = System.getProperty("jlawyer.jwt.keystore");
        if (path == null || path.trim().isEmpty()) {
            String configDir = System.getProperty("jboss.server.config.dir", ".");
            path = configDir + File.separator + "j-lawyer-jwt.p12";
        }
        String password = System.getProperty("jlawyer.jwt.keystore.password", DEFAULT_PASSWORD);
        String alias = System.getProperty("jlawyer.jwt.keystore.alias", DEFAULT_ALIAS);
        if (DEFAULT_PASSWORD.equals(password)) {
            log.warn("JWT keystore is using the default password; set -Djlawyer.jwt.keystore.password for production");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream in = new FileInputStream(path)) {
                keyStore.load(in, password.toCharArray());
            }
            Key key = keyStore.getKey(alias, password.toCharArray());
            if (!(key instanceof PrivateKey)) {
                throw new JwtException("keystore alias '" + alias + "' does not hold an RSA private key");
            }
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                throw new JwtException("keystore alias '" + alias + "' has no certificate / public key");
            }
            privateKey = (PrivateKey) key;
            publicKey = certificate.getPublicKey();
            log.info("loaded JWT signing key from " + path + " (alias '" + alias + "')");
        } catch (JwtException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtException("unable to load JWT keystore from " + path, ex);
        }
    }
}
