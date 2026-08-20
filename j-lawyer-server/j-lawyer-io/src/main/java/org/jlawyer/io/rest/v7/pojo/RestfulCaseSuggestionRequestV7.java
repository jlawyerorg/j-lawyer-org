/*
 * Copyright (C) j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License, version 3.
 * See the LICENSE file distributed with this project.
 */
package org.jlawyer.io.rest.v7.pojo;

import java.io.Serializable;

/**
 * Request body for the case-suggestions endpoint: the opened email's {@code subject}, {@code body}
 * and {@code from} header. The server derives suggested cases, matching contacts and phone numbers.
 */
public class RestfulCaseSuggestionRequestV7 implements Serializable {

    private String subject;
    private String body;
    private String from;

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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
