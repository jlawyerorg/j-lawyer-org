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

import com.jdimension.jlawyer.services.bea.rest.BeaAttachment;
import java.util.Base64;

/**
 * A single beA message attachment with its content carried as a Base64 {@code String}
 * ({@code contentBase64}) rather than a raw {@code byte[]}. This follows the REST convention used
 * throughout the API (a raw {@code byte[]} is serialized inconsistently across JSON providers and
 * cannot be decoded reliably in JavaScript) and is returned by the browser-oriented
 * {@code .../attachments/{name}/base64} endpoint. The desktop/EJB clients continue to use the
 * unchanged {@link BeaAttachment}.
 */
public class RestfulBeaAttachmentV8 {

    protected String name = null;
    protected String alias = null;
    protected int type = 0;
    protected long size = 0;
    protected boolean technicalAttachment = false;
    protected String contentBase64 = null;

    public RestfulBeaAttachmentV8() {
    }

    /**
     * Wraps a {@link BeaAttachment}, Base64-encoding its content (if any).
     *
     * @param a the attachment to wrap; may be {@code null}
     * @return the wrapped attachment, or {@code null} if {@code a} is {@code null}
     */
    public static RestfulBeaAttachmentV8 fromBea(BeaAttachment a) {
        if (a == null) {
            return null;
        }
        RestfulBeaAttachmentV8 r = new RestfulBeaAttachmentV8();
        r.setName(a.getName());
        r.setAlias(a.getAlias());
        r.setType(a.getType());
        r.setSize(a.getSize());
        r.setTechnicalAttachment(a.isTechnicalAttachment());
        if (a.getContent() != null) {
            r.setContentBase64(Base64.getEncoder().encodeToString(a.getContent()));
        }
        return r;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isTechnicalAttachment() {
        return technicalAttachment;
    }

    public void setTechnicalAttachment(boolean technicalAttachment) {
        this.technicalAttachment = technicalAttachment;
    }

    /**
     * @return the attachment content, Base64-encoded, or {@code null} if not loaded
     */
    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

}
