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
package org.jlawyer.io.rest.v7.pojo;

/**
 * A calendar entry template (Ereignisvorlage): reusable name/description for an event, optionally with
 * a related follow-up event created a number of days later.
 */
public class RestfulCalendarEntryTemplateV7 {

    private String id = null;
    private String name = "";
    private String description = "";
    private boolean related = false;
    private String relatedName = "";
    private String relatedDescription = "";
    private int relatedOffsetDays = 0;

    public RestfulCalendarEntryTemplateV7() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRelated() { return related; }
    public void setRelated(boolean related) { this.related = related; }
    public String getRelatedName() { return relatedName; }
    public void setRelatedName(String relatedName) { this.relatedName = relatedName; }
    public String getRelatedDescription() { return relatedDescription; }
    public void setRelatedDescription(String relatedDescription) { this.relatedDescription = relatedDescription; }
    public int getRelatedOffsetDays() { return relatedOffsetDays; }
    public void setRelatedOffsetDays(int relatedOffsetDays) { this.relatedOffsetDays = relatedOffsetDays; }

}
