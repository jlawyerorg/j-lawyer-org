# Web-UI Authentication Setup (JWT Bearer, additive to HTTP Basic)

This documents the server-side setup for the browser login flow of the web UI
(OpenSpec change `add-web-client`, design.md **Decision 5**). It is **additive**: existing
HTTP Basic REST clients and the EJB-based Swing client keep working unchanged.

## How it fits together

```
SPA  --POST /j-lawyer-io/rest/v8/auth/login {user,password}-->  AuthenticationEndpointV8
                                                                   | verifies via SecurityService.authenticateAndGetRoles
                                                                   | signs RS256 JWT with the PRIVATE key (j-lawyer-jwt.p12)
     <-- 200 {accessToken} + Set-Cookie: JLAWYER_REFRESH (httpOnly) --
SPA  --GET /j-lawyer-io/rest/v8/... Authorization: Bearer <jwt>-->  Undertow/Elytron
                                                                   | token-realm verifies JWT with the PUBLIC key (same keystore)
                                                                   | establishes SecurityIdentity in jlawyer-security-domain
                                                                   | => getCallerPrincipal() + @RolesAllowed work in the EJBs
```

The access token is short-lived (15 min) and kept in memory by the SPA. The refresh token
is longer-lived (8 h) and delivered as an httpOnly cookie; `POST .../auth/refresh` mints a
new access token, `POST .../auth/logout` clears the cookie.

## 1. Generate the signing keystore

The **same** PKCS#12 keystore is used by the deployment (to sign, private key) and by
Elytron (to verify, public certificate). Create it in the WildFly config directory:

```bash
cd $JBOSS_HOME/standalone/configuration
keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
  -dname "CN=j-lawyer" -validity 3650 -storetype PKCS12 \
  -keystore j-lawyer-jwt.p12 -storepass changeit
```

Defaults line up out of the box: `JwtKeyProvider` looks for
`${jboss.server.config.dir}/j-lawyer-jwt.p12`, alias `jwt`, password `changeit`, and the
Elytron `key-store jlawyer-jwt-keystore` in `standalone.xml` points at the same file.

**Production:** use a strong store password and override both sides:
- deployment: `-Djlawyer.jwt.keystore.password=<pw>` (and optionally
  `-Djlawyer.jwt.keystore` / `-Djlawyer.jwt.keystore.alias`)
- Elytron: change `<credential-reference clear-text="changeit"/>` on `jlawyer-jwt-keystore`.

## 2. Elytron configuration (already in `docker/wildfly/standalone.xml`)

The committed docker `standalone.xml` contains all of it; apply the **same** edits to your
local `$JBOSS_HOME/standalone/configuration/standalone.xml`:

1. `key-store name="jlawyer-jwt-keystore"` (PKCS12) under `elytron/tls/key-stores`.
2. `token-realm name="jlawyer-jwt-realm"` with `<jwt issuer="j-lawyer" audience="j-lawyer-web"
   key-store="jlawyer-jwt-keystore" certificate="jwt"/>` under `elytron/security-realms`.
3. Add that realm to `security-domain jlawyer-security-domain` with
   `role-decoder="from-roles-attribute"` (roles come from the token's `roles` claim).
4. `http-authentication-factory name="jlawyer-http-authentication"` offering **BASIC + BEARER_TOKEN**
   against `jlawyer-security-domain`.
5. Point the **undertow** `application-security-domain jlawyer-application-security-domain`
   at `http-authentication-factory="jlawyer-http-authentication"` with
   `override-deployment-config="true"`. Leave the **ejb3** subsystem mapping on
   `security-domain="jlawyer-security-domain"` so the identity propagates to the EJB tier.

## 3. Public login path

`web.xml` marks `/rest/v8/auth/*` as a public (no auth-constraint) path so the SPA can log
in before it has a token; all other paths stay behind the `loginRole` constraint. A failed
login returns a plain `401` with **no** `WWW-Authenticate: Basic` header, so browsers never
raise the native Basic dialog.

## 4. Verify

```bash
# login (public) — expect 200 + JLAWYER_REFRESH cookie
curl -i -c cookies.txt -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"a"}' \
  http://localhost:8080/j-lawyer-io/rest/v8/auth/login

# call a protected endpoint with the returned access token — expect 200
curl -i -H "Authorization: Bearer <accessToken>" \
  http://localhost:8080/j-lawyer-io/rest/v7/cases

# refresh (sends the cookie) — expect 200 + new access token
curl -i -b cookies.txt -X POST http://localhost:8080/j-lawyer-io/rest/v8/auth/refresh

# existing Basic clients must still work unchanged — expect 200
curl -i -u admin:a http://localhost:8080/j-lawyer-io/rest/v7/cases
```

## Known limitations (v1)

- **No server-side access-token revocation.** Tokens are stateless; logout clears the
  refresh cookie but an already-issued access token remains valid until it expires (15 min).
- **Refresh carries stale roles.** Role changes take effect on the next login (or when the
  8 h refresh token expires), not immediately on refresh.
- **2FA** (`otp` field) is accepted but not yet enforced — owned by the `add-two-factor-auth`
  change.
