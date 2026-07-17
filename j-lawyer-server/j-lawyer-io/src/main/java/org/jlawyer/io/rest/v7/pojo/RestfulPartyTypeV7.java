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

import com.jdimension.jlawyer.persistence.PartyTypeBean;

/**
 * A party type (Beteiligtentyp) — the role a party can take in a case, e.g. client or opponent.
 * Carries the display name, the document placeholder token, a color, and the sort sequence.
 */
public class RestfulPartyTypeV7 {

    private String id = null;
    private String name = null;
    private String placeHolder = null;
    private int color = 0;
    private int sequenceNumber = 1;

    public RestfulPartyTypeV7() {
    }

    /** Maps a persistent party type to its REST representation. */
    public static RestfulPartyTypeV7 fromEntity(PartyTypeBean p) {
        RestfulPartyTypeV7 pt = new RestfulPartyTypeV7();
        pt.setId(p.getId());
        pt.setName(p.getName());
        pt.setPlaceHolder(p.getPlaceHolder());
        pt.setColor(p.getColor());
        pt.setSequenceNumber(p.getSequenceNumber());
        return pt;
    }

    /** Maps this REST representation to a persistent party type. */
    public PartyTypeBean toEntity() {
        PartyTypeBean p = new PartyTypeBean();
        p.setId(this.id);
        p.setName(this.name);
        p.setPlaceHolder(this.placeHolder);
        p.setColor(this.color);
        p.setSequenceNumber(this.sequenceNumber);
        return p;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(String placeHolder) {
        this.placeHolder = placeHolder;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
