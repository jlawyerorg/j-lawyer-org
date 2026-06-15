/*
 * Copyright (C) j-lawyer.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v7.pojo;

import com.jdimension.jlawyer.services.MailAttachmentDTO;
import java.io.Serializable;
import java.util.Base64;

public class RestfulMailAttachmentV7 implements Serializable {

    protected String attachmentId;
    protected String name;
    protected String contentType;
    protected long size;
    protected boolean inline;
    protected String contentId;
    protected String contentBase64;

    public static RestfulMailAttachmentV7 fromMailAttachmentDTO(MailAttachmentDTO dto) {
        RestfulMailAttachmentV7 r = new RestfulMailAttachmentV7();
        r.setAttachmentId(dto.getAttachmentId());
        r.setName(dto.getName());
        r.setContentType(dto.getContentType());
        r.setSize(dto.getSize());
        r.setInline(dto.isInline());
        r.setContentId(dto.getContentId());
        if (dto.getContent() != null) {
            r.setContentBase64(Base64.getEncoder().encodeToString(dto.getContent()));
        }
        return r;
    }

    public MailAttachmentDTO toMailAttachmentDTO() {
        MailAttachmentDTO dto = new MailAttachmentDTO();
        dto.setAttachmentId(this.attachmentId);
        dto.setName(this.name);
        dto.setContentType(this.contentType);
        dto.setSize(this.size);
        dto.setInline(this.inline);
        dto.setContentId(this.contentId);
        if (this.contentBase64 != null) {
            dto.setContent(Base64.getDecoder().decode(this.contentBase64));
        }
        return dto;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }
}
