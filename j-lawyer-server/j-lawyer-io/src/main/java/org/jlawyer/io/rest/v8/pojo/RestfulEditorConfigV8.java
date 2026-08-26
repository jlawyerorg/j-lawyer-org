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
package org.jlawyer.io.rest.v8.pojo;

/**
 * Everything the web client needs to embed the Office editor for one document (OpenSpec
 * add-web-client, Decision 6): the {@code provider}, the editor {@code urlsrc} (from the document
 * server's WOPI discovery, for the document's file type + edit action), the {@code wopiSrc} (the
 * j-lawyer WOPI file URL the document server calls back to) and a short-lived {@code accessToken}
 * (bound to this document + user + permission) with its {@code accessTokenTtl} in seconds.
 *
 * @author jens
 */
public class RestfulEditorConfigV8 {

    private String provider;
    private String urlsrc;
    private String wopiSrc;
    private String accessToken;
    private long accessTokenTtl;
    private String fileName;

    public RestfulEditorConfigV8() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUrlsrc() {
        return urlsrc;
    }

    public void setUrlsrc(String urlsrc) {
        this.urlsrc = urlsrc;
    }

    public String getWopiSrc() {
        return wopiSrc;
    }

    public void setWopiSrc(String wopiSrc) {
        this.wopiSrc = wopiSrc;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(long accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
