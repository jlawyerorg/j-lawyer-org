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

import com.jdimension.jlawyer.persistence.MailboxSetup;
import java.io.Serializable;

public class RestfulMailboxV7 implements Serializable {

    protected String id;
    protected String displayName;
    protected String emailAddress;
    protected String type;

    public static RestfulMailboxV7 fromMailboxSetup(MailboxSetup ms) {
        RestfulMailboxV7 r = new RestfulMailboxV7();
        r.setId(ms.getId());
        r.setDisplayName(ms.getDisplayName() != null ? ms.getDisplayName() : ms.getEmailAddress());
        r.setEmailAddress(ms.getEmailAddress());
        r.setType(ms.isMsExchange() ? "exchange" : "imap");
        return r;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
