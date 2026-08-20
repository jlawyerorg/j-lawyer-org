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

import com.jdimension.jlawyer.persistence.AssistantReplacement;

/**
 * An automatic text replacement applied to transcription / dictation output (RestfulAssistantReplacementV8)
 * — the web equivalent of the desktop "automatische Ersetzungen" dialog. Global.
 */
public class RestfulAssistantReplacementV8 {

    private String id;
    private String searchString;
    private String replaceWith;
    private boolean caseInsensitive;

    public RestfulAssistantReplacementV8() {
    }

    public static RestfulAssistantReplacementV8 fromEntity(AssistantReplacement r) {
        RestfulAssistantReplacementV8 dto = new RestfulAssistantReplacementV8();
        dto.id = r.getId();
        dto.searchString = r.getSearchString();
        dto.replaceWith = r.getReplaceWith();
        dto.caseInsensitive = r.isCaseInsensitive();
        return dto;
    }

    public void applyTo(AssistantReplacement r) {
        r.setSearchString(this.searchString);
        r.setReplaceWith(this.replaceWith);
        r.setCaseInsensitive(this.caseInsensitive);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getReplaceWith() {
        return replaceWith;
    }

    public void setReplaceWith(String replaceWith) {
        this.replaceWith = replaceWith;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

}
