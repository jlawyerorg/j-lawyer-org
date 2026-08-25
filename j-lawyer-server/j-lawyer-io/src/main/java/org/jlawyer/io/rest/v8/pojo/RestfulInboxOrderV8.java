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

import java.util.ArrayList;
import java.util.List;

/**
 * The order in which the current user wants their mailboxes and beA postboxes to be presented.
 * Shared with the desktop client, which stores the very same per-user settings keys - so a user
 * arranging their mailboxes in the web UI sees that order in the desktop client as well.
 *
 * Both lists are hints: ids that no longer resolve to a mailbox / postbox are ignored, and
 * anything not covered by the list is appended at the end. An empty list means "no order stored
 * yet", in which case the server side default order applies.
 *
 * @author jens
 */
public class RestfulInboxOrderV8 {

    private List<String> mailboxOrder = new ArrayList<>();
    private List<String> beaPostboxOrder = new ArrayList<>();

    public RestfulInboxOrderV8() {
    }

    public RestfulInboxOrderV8(List<String> mailboxOrder, List<String> beaPostboxOrder) {
        this.mailboxOrder = mailboxOrder == null ? new ArrayList<>() : mailboxOrder;
        this.beaPostboxOrder = beaPostboxOrder == null ? new ArrayList<>() : beaPostboxOrder;
    }

    /**
     * @return ids of the mailboxes (MailboxSetup ids), in the order chosen by the user
     */
    public List<String> getMailboxOrder() {
        return mailboxOrder;
    }

    public void setMailboxOrder(List<String> mailboxOrder) {
        this.mailboxOrder = mailboxOrder;
    }

    /**
     * @return safe ids of the beA postboxes, in the order chosen by the user
     */
    public List<String> getBeaPostboxOrder() {
        return beaPostboxOrder;
    }

    public void setBeaPostboxOrder(List<String> beaPostboxOrder) {
        this.beaPostboxOrder = beaPostboxOrder;
    }

}
