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
package org.jlawyer.io.rest.v7;

import javax.ejb.Local;
import javax.ws.rs.core.Response;
import org.jlawyer.io.rest.v7.pojo.RestfulAppendMailRequestV7;
import org.jlawyer.io.rest.v7.pojo.RestfulCreateFolderRequestV7;
import org.jlawyer.io.rest.v7.pojo.RestfulSendMailRequestV7;

@Local
public interface EmailEndpointLocalV7 {

    Response listMailboxes();

    Response listFolders(String mailboxId);

    Response createFolder(String mailboxId, RestfulCreateFolderRequestV7 request);

    Response deleteFolder(String mailboxId, String folderId);

    Response emptyTrash(String mailboxId, String folderId);

    Response listMessages(String mailboxId, String folderId, int top, int offset, String sinceDate, boolean unreadOnly, String search);

    Response getMessage(String mailboxId, String messageRef, boolean includeAttachments);

    Response deleteMessage(String mailboxId, String messageRef);

    Response moveMessage(String mailboxId, String messageRef, String targetFolderId);

    Response markAsRead(String mailboxId, String messageRef, boolean read);

    Response getMessageAsEml(String mailboxId, String messageRef);

    Response getAttachment(String mailboxId, String messageRef, String attachmentId);

    Response sendMail(String mailboxId, RestfulSendMailRequestV7 request);

    Response appendToFolder(String mailboxId, String folderId, RestfulAppendMailRequestV7 request);

    Response testConnection(String mailboxId);

    Response invalidateCaches(String mailboxId);
}
