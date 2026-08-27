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
import com.jdimension.jlawyer.persistence.ServerSettingsBean;
import com.jdimension.jlawyer.security.jwt.JwtService;
import com.jdimension.jlawyer.server.services.settings.ServerSettingsKeys;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.SystemManagementLocal;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.tools.RestErrorResponses;
import org.jlawyer.io.rest.v8.pojo.RestfulEditorConfigV8;
import org.jlawyer.io.rest.v8.pojo.RestfulOfficeSettingsV8;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * v8 Office-editing endpoint (OpenSpec add-web-client, Decision 6). Holds the connection settings to
 * the external document server (Collabora / OnlyOffice; {@code sysAdminRole}), tells the web client
 * whether the feature is enabled ({@code loginRole}), and builds the per-document editor config —
 * minting the short-lived WOPI access token after an ACL check on the document's case.
 *
 * @author jens
 */
@Stateless
@Path("/v8/office")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Office"})
public class OfficeEndpointV8 implements OfficeEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(OfficeEndpointV8.class.getName());
    private static final String LOOKUP_SYSMAN = "java:global/j-lawyer-server/j-lawyer-server-ejb/SystemManagement!com.jdimension.jlawyer.services.SystemManagementLocal";
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";

    private final JwtService jwt = new JwtService();

    /** Returns the document-server connection settings (the secret is masked; {@code secretSet} indicates one is stored). */
    @Override
    @GET
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"sysAdminRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the Office document-server connection settings", response = RestfulOfficeSettingsV8.class)
    public Response getSettings() {
        try {
            SystemManagementLocal system = system();
            RestfulOfficeSettingsV8 s = new RestfulOfficeSettingsV8();
            s.setProvider(orDefault(readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_PROVIDER), "none"));
            s.setBaseUrl(orEmpty(readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_BASEURL)));
            s.setWopiPublicUrl(orEmpty(readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_WOPIPUBLICURL)));
            String secret = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_SECRET);
            s.setSecret("");
            s.setSecretSet(secret != null && !secret.isEmpty());
            return Response.ok(s).build();
        } catch (Exception ex) {
            log.error("Cannot read office settings", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /** Saves the connection settings. An empty secret leaves the stored one unchanged (write-only). */
    @Override
    @PUT
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"sysAdminRole"})
    @io.swagger.annotations.ApiOperation(value = "Saves the Office document-server connection settings", response = RestfulOfficeSettingsV8.class)
    public Response putSettings(RestfulOfficeSettingsV8 settings) {
        try {
            if (settings == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            SystemManagementLocal system = system();
            system.setSetting(ServerSettingsKeys.SERVERCONF_OFFICE_PROVIDER, orDefault(settings.getProvider(), "none"));
            system.setSetting(ServerSettingsKeys.SERVERCONF_OFFICE_BASEURL, orEmpty(settings.getBaseUrl()));
            system.setSetting(ServerSettingsKeys.SERVERCONF_OFFICE_WOPIPUBLICURL, orEmpty(settings.getWopiPublicUrl()));
            if (settings.getSecret() != null && !settings.getSecret().isEmpty()) {
                system.setSetting(ServerSettingsKeys.SERVERCONF_OFFICE_SECRET, settings.getSecret());
            }
            return getSettings();
        } catch (Exception ex) {
            log.error("Cannot save office settings", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /** Tells the web client whether in-browser Office editing is enabled (a provider + base URL are configured). */
    @Override
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Whether in-browser Office editing is enabled")
    public Response status() {
        try {
            SystemManagementLocal system = system();
            String provider = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_PROVIDER);
            String baseUrl = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_BASEURL);
            boolean enabled = isConfigured(provider, baseUrl);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enabled", enabled);
            m.put("provider", enabled ? provider : "none");
            return Response.ok(m).build();
        } catch (Exception ex) {
            log.error("Cannot read office status", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Builds the editor config for one document: resolves the document server's {@code urlsrc} for the
     * file type, mints a short-lived WOPI access token (after an ACL check via {@code getDocument}) and
     * returns the WOPI callback URL.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Document not found / not accessible
     * @response 415 File type not editable by the document server
     * @response 503 In-browser editing not configured
     */
    @Override
    @GET
    @Path("/editor-config/{documentId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Editor config to embed the Office editor for a document", response = RestfulEditorConfigV8.class)
    public Response editorConfig(@PathParam("documentId") String documentId, @Context SecurityContext securityContext) {
        try {
            SystemManagementLocal system = system();
            String provider = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_PROVIDER);
            String baseUrl = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_BASEURL);
            String wopiPublicUrl = readSetting(system, ServerSettingsKeys.SERVERCONF_OFFICE_WOPIPUBLICURL);
            if (!isConfigured(provider, baseUrl)) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
            }

            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) new InitialContext().lookup(LOOKUP_CASES);
            ArchiveFileDocumentsBean doc;
            try {
                doc = cases.getDocument(documentId); // enforces the per-case ACL for the caller
            } catch (Exception notFoundOrForbidden) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String fileName = doc.getName();
            String urlsrc = resolveUrlsrc(baseUrl, ext(fileName));
            if (urlsrc == null) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
            }

            String user = securityContext.getUserPrincipal().getName();
            long now = Instant.now().getEpochSecond();
            // Mint so the token passes the Elytron token-realm when Collabora sends it as a Bearer:
            // login issuer/audience, NO roles (authorises nothing if replayed), document id in jti.
            String token = jwt.issue(JwtKeyProvider.getPrivateKey(), user, Collections.emptyList(),
                    WopiEndpoint.TOKEN_ISSUER, WopiEndpoint.TOKEN_AUDIENCE, now, WopiEndpoint.WOPI_TTL_SECONDS, documentId);

            String wopiSrc = trimSlash(orEmpty(wopiPublicUrl)) + "/j-lawyer-io/rest/wopi/files/" + documentId;
            RestfulEditorConfigV8 cfg = new RestfulEditorConfigV8();
            cfg.setProvider(provider);
            // Return the complete editor action URL (WOPISrc embedded), handling both the "bare"
            // Collabora urlsrc and OnlyOffice's WOPI-discovery placeholders.
            cfg.setUrlsrc(buildActionUrl(urlsrc, wopiSrc));
            cfg.setWopiSrc(wopiSrc);
            cfg.setAccessToken(token);
            cfg.setAccessTokenTtl(WopiEndpoint.WOPI_TTL_SECONDS);
            cfg.setFileName(fileName);
            return Response.ok(cfg).build();
        } catch (Exception ex) {
            log.error("Cannot build editor config for document " + documentId, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /** Fetches the document server's WOPI discovery and returns the edit {@code urlsrc} for a file extension, or null. */
    private String resolveUrlsrc(String baseUrl, String ext) throws Exception {
        if (ext == null) {
            return null;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        HttpURLConnection con = (HttpURLConnection) new URL(trimSlash(baseUrl) + "/hosting/discovery").openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        try (InputStream in = con.getInputStream()) {
            Document xml = builder.parse(in);
            NodeList actions = xml.getElementsByTagName("action");
            for (int i = 0; i < actions.getLength(); i++) {
                Element a = (Element) actions.item(i);
                if ("edit".equals(a.getAttribute("name")) && ext.equalsIgnoreCase(a.getAttribute("ext"))) {
                    return a.getAttribute("urlsrc");
                }
            }
            return null;
        } finally {
            con.disconnect();
        }
    }

    /**
     * WOPI discovery placeholder, e.g. {@code <wopisrc=WOPI_SOURCE&>} — parameter name and the
     * placeholder value the host must substitute or strip.
     */
    private static final java.util.regex.Pattern PLACEHOLDER = java.util.regex.Pattern.compile("<([^=<>]+)=([^<>&]+)&>");

    /**
     * Builds the final editor action URL from a discovery {@code urlsrc}: OnlyOffice-style urlsrc
     * carries WOPI placeholders (sets {@code WOPI_SOURCE} → WOPISrc + the UI language, strips the
     * rest); a "bare" Collabora-style urlsrc simply gets WOPISrc appended.
     */
    private static String buildActionUrl(String urlsrc, String wopiSrc) {
        String enc;
        try {
            enc = java.net.URLEncoder.encode(wopiSrc, "UTF-8");
        } catch (Exception ex) {
            enc = wopiSrc;
        }
        if (urlsrc.contains("<")) {
            java.util.regex.Matcher m = PLACEHOLDER.matcher(urlsrc);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String param = m.group(1);
                String value = m.group(2);
                String repl;
                if ("WOPI_SOURCE".equals(value)) {
                    repl = param + "=" + enc + "&";
                } else if ("UI_LLCC".equals(value) || "DC_LLCC".equals(value)) {
                    repl = param + "=de-DE&";
                } else {
                    repl = "";
                }
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        String sep = (urlsrc.endsWith("?") || urlsrc.endsWith("&")) ? "" : (urlsrc.contains("?") ? "&" : "?");
        return urlsrc + sep + "WOPISrc=" + enc + "&lang=de-DE";
    }

    private static boolean isConfigured(String provider, String baseUrl) {
        return provider != null && !provider.isEmpty() && !"none".equalsIgnoreCase(provider)
                && baseUrl != null && !baseUrl.isEmpty();
    }

    private static String ext(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0 && dot < fileName.length() - 1) ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    private static String trimSlash(String s) {
        return s == null ? "" : s.replaceAll("/+$", "");
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String orDefault(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static String readSetting(SystemManagementLocal system, String key) {
        ServerSettingsBean b = system.getSetting(key);
        return b == null ? null : b.getSettingValue();
    }

    private SystemManagementLocal system() throws Exception {
        return (SystemManagementLocal) new InitialContext().lookup(LOOKUP_SYSMAN);
    }

}
