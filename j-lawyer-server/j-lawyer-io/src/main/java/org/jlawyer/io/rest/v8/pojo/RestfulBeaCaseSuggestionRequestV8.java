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

import java.io.Serializable;

/**
 * Request for case suggestions for an opened beA message. The server matches the two beA reference
 * numbers plus the subject and body against known file numbers, and the sender name against the
 * address book.
 */
public class RestfulBeaCaseSuggestionRequestV8 implements Serializable {

    private String subject;
    private String body;
    private String referenceNumber;
    private String referenceJustice;
    private String senderName;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @return the sender's own reference number ("Aktenzeichen des Absenders")
     */
    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /**
     * @return the court's reference number ("Aktenzeichen der Justiz")
     */
    public String getReferenceJustice() {
        return referenceJustice;
    }

    public void setReferenceJustice(String referenceJustice) {
        this.referenceJustice = referenceJustice;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

}
