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
import com.jdimension.jlawyer.services.MailMessageDTO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RestfulMailMessageV7 implements Serializable {

    protected String messageRef;
    protected String messageId;
    protected String subject;
    protected String from;
    protected String[] to;
    protected String[] cc;
    protected Date date;
    protected boolean read;
    protected boolean hasAttachments;
    protected String body;
    protected String bodyContentType;
    protected String inReplyTo;
    protected String references;
    protected boolean readReceiptRequested;
    protected List<RestfulMailAttachmentV7> attachments;

    public static RestfulMailMessageV7 fromMailMessageDTO(MailMessageDTO dto) {
        RestfulMailMessageV7 r = new RestfulMailMessageV7();
        r.setMessageRef(dto.getMessageRef());
        r.setMessageId(dto.getMessageId());
        r.setSubject(dto.getSubject());
        r.setFrom(dto.getFrom());
        r.setTo(dto.getTo());
        r.setCc(dto.getCc());
        r.setDate(dto.getDate());
        r.setRead(dto.isRead());
        r.setHasAttachments(dto.isHasAttachments());
        r.setBody(dto.getBody());
        r.setBodyContentType(dto.getBodyContentType());
        r.setInReplyTo(dto.getInReplyTo());
        r.setReferences(dto.getReferences());
        r.setReadReceiptRequested(dto.isReadReceiptRequested());
        if (dto.getAttachments() != null) {
            ArrayList<RestfulMailAttachmentV7> attList = new ArrayList<>();
            for (MailAttachmentDTO att : dto.getAttachments()) {
                attList.add(RestfulMailAttachmentV7.fromMailAttachmentDTO(att));
            }
            r.setAttachments(attList);
        }
        return r;
    }

    public String getMessageRef() {
        return messageRef;
    }

    public void setMessageRef(String messageRef) {
        this.messageRef = messageRef;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String[] getTo() {
        return to;
    }

    public void setTo(String[] to) {
        this.to = to;
    }

    public String[] getCc() {
        return cc;
    }

    public void setCc(String[] cc) {
        this.cc = cc;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyContentType() {
        return bodyContentType;
    }

    public void setBodyContentType(String bodyContentType) {
        this.bodyContentType = bodyContentType;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public boolean isReadReceiptRequested() {
        return readReceiptRequested;
    }

    public void setReadReceiptRequested(boolean readReceiptRequested) {
        this.readReceiptRequested = readReceiptRequested;
    }

    public List<RestfulMailAttachmentV7> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<RestfulMailAttachmentV7> attachments) {
        this.attachments = attachments;
    }
}
