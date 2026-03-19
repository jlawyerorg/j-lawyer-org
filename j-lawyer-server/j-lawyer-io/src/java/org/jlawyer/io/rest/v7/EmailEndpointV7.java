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

import com.jdimension.jlawyer.persistence.MailboxSetup;
import com.jdimension.jlawyer.services.EmailServiceLocal;
import com.jdimension.jlawyer.services.MailAttachmentDTO;
import com.jdimension.jlawyer.services.MailFolderDTO;
import com.jdimension.jlawyer.services.MailMessageDTO;
import com.jdimension.jlawyer.services.SecurityServiceLocal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v7.pojo.RestfulAppendMailRequestV7;
import org.jlawyer.io.rest.v7.pojo.RestfulCreateFolderRequestV7;
import org.jlawyer.io.rest.v7.pojo.RestfulMailAttachmentV7;
import org.jlawyer.io.rest.v7.pojo.RestfulMailFolderV7;
import org.jlawyer.io.rest.v7.pojo.RestfulMailMessageV7;
import org.jlawyer.io.rest.v7.pojo.RestfulMailboxV7;
import org.jlawyer.io.rest.v7.pojo.RestfulSendMailRequestV7;

@Stateless
@Path("/v7/email")
@Consumes({"application/json"})
@Produces({"application/json"})
public class EmailEndpointV7 implements EmailEndpointLocalV7 {

    private static final Logger log = Logger.getLogger(EmailEndpointV7.class.getName());

    private static final String LOOKUP_EMAIL = "java:global/j-lawyer-server/j-lawyer-server-ejb/EmailService!com.jdimension.jlawyer.services.EmailServiceLocal";
    private static final String LOOKUP_SECURITY = "java:global/j-lawyer-server/j-lawyer-server-ejb/SecurityService!com.jdimension.jlawyer.services.SecurityServiceLocal";

    @Context
    private SecurityContext securityContext;

    private boolean hasMailboxAccess(InitialContext ic, String mailboxId) throws Exception {
        SecurityServiceLocal secService = (SecurityServiceLocal) ic.lookup(LOOKUP_SECURITY);
        String principal = securityContext.getUserPrincipal().getName();
        List<MailboxSetup> mailboxes = secService.getMailboxesForUser(principal);
        if (mailboxes != null) {
            for (MailboxSetup ms : mailboxes) {
                if (ms.getId().equals(mailboxId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns all mailboxes accessible to the authenticated user.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes")
    @RolesAllowed({"loginRole"})
    public Response listMailboxes() {
        try {
            InitialContext ic = new InitialContext();
            SecurityServiceLocal secService = (SecurityServiceLocal) ic.lookup(LOOKUP_SECURITY);

            String principal = securityContext.getUserPrincipal().getName();
            List<MailboxSetup> mailboxes = secService.getMailboxesForUser(principal);
            ArrayList<RestfulMailboxV7> result = new ArrayList<>();
            if (mailboxes != null) {
                for (MailboxSetup ms : mailboxes) {
                    result.add(RestfulMailboxV7.fromMailboxSetup(ms));
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list mailboxes", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns all folders for the given mailbox.
     *
     * @param mailboxId mailbox ID
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders")
    @RolesAllowed({"loginRole"})
    public Response listFolders(@PathParam("mailboxId") String mailboxId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            List<MailFolderDTO> folders = emailService.listFolders(mailboxId);
            ArrayList<RestfulMailFolderV7> result = new ArrayList<>();
            if (folders != null) {
                for (MailFolderDTO f : folders) {
                    result.add(RestfulMailFolderV7.fromMailFolderDTO(f));
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list folders for mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Creates a new folder in the given mailbox.
     *
     * @param mailboxId mailbox ID
     * @param request folder creation request with parentFolderId and folderName
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders")
    @RolesAllowed({"loginRole"})
    public Response createFolder(@PathParam("mailboxId") String mailboxId, RestfulCreateFolderRequestV7 request) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            MailFolderDTO created = emailService.createFolder(mailboxId, request.getParentFolderId(), request.getFolderName());
            return Response.ok(RestfulMailFolderV7.fromMailFolderDTO(created)).build();
        } catch (Exception ex) {
            log.error("can not create folder in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes a folder from the given mailbox.
     *
     * @param mailboxId mailbox ID
     * @param folderId folder ID to delete
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders/{folderId}")
    @RolesAllowed({"loginRole"})
    public Response deleteFolder(@PathParam("mailboxId") String mailboxId, @PathParam("folderId") String folderId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.deleteFolder(mailboxId, folderId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete folder " + folderId + " in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Empties the trash folder by permanently deleting all messages in it.
     *
     * @param mailboxId mailbox ID
     * @param folderId trash folder ID
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders/{folderId}/trash")
    @RolesAllowed({"loginRole"})
    public Response emptyTrash(@PathParam("mailboxId") String mailboxId, @PathParam("folderId") String folderId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.emptyTrash(mailboxId, folderId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not empty trash in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Lists messages in a folder with pagination and optional filtering.
     *
     * @param mailboxId mailbox ID
     * @param folderId folder ID
     * @param top maximum number of messages to return (default 50)
     * @param offset number of messages to skip (default 0)
     * @param sinceDate only return messages after this date (ISO 8601 format, e.g. 2024-01-15)
     * @param unreadOnly if true, only return unread messages
     * @param search search term to filter by subject, from, to, body
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders/{folderId}/messages")
    @RolesAllowed({"loginRole"})
    public Response listMessages(@PathParam("mailboxId") String mailboxId,
            @PathParam("folderId") String folderId,
            @QueryParam("top") @DefaultValue("50") int top,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("sinceDate") @DefaultValue("") String sinceDate,
            @QueryParam("unreadOnly") @DefaultValue("false") boolean unreadOnly,
            @QueryParam("search") @DefaultValue("") String search) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);

            Date since = null;
            if (sinceDate != null && !sinceDate.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                since = sdf.parse(sinceDate);
            }

            List<MailMessageDTO> messages;
            if (search != null && !search.isEmpty()) {
                messages = emailService.listMessages(mailboxId, folderId, top, offset, since, unreadOnly, search);
            } else if (since != null || unreadOnly) {
                messages = emailService.listMessages(mailboxId, folderId, top, offset, since, unreadOnly);
            } else {
                messages = emailService.listMessages(mailboxId, folderId, top, offset);
            }

            ArrayList<RestfulMailMessageV7> result = new ArrayList<>();
            if (messages != null) {
                for (MailMessageDTO m : messages) {
                    result.add(RestfulMailMessageV7.fromMailMessageDTO(m));
                }
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list messages in folder " + folderId + " of mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves a full message by its opaque reference.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @param includeAttachments if true, include attachment metadata in response
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}")
    @RolesAllowed({"loginRole"})
    public Response getMessage(@PathParam("mailboxId") String mailboxId,
            @PathParam("messageRef") String messageRef,
            @QueryParam("includeAttachments") @DefaultValue("false") boolean includeAttachments) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            MailMessageDTO msg = emailService.getMessage(mailboxId, messageRef, includeAttachments);
            return Response.ok(RestfulMailMessageV7.fromMailMessageDTO(msg)).build();
        } catch (Exception ex) {
            log.error("can not get message " + messageRef + " from mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes a message.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}")
    @RolesAllowed({"loginRole"})
    public Response deleteMessage(@PathParam("mailboxId") String mailboxId, @PathParam("messageRef") String messageRef) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.deleteMessage(mailboxId, messageRef);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete message " + messageRef + " from mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Moves a message to another folder.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @param targetFolderId target folder ID (as query parameter)
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}/move")
    @RolesAllowed({"loginRole"})
    public Response moveMessage(@PathParam("mailboxId") String mailboxId,
            @PathParam("messageRef") String messageRef,
            @QueryParam("targetFolderId") String targetFolderId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.moveMessage(mailboxId, messageRef, targetFolderId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not move message " + messageRef + " in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Marks a message as read or unread.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @param read true to mark as read, false to mark as unread
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}/read")
    @RolesAllowed({"loginRole"})
    public Response markAsRead(@PathParam("mailboxId") String mailboxId,
            @PathParam("messageRef") String messageRef,
            @QueryParam("read") @DefaultValue("true") boolean read) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.markAsRead(mailboxId, messageRef, read);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not mark message " + messageRef + " as read=" + read + " in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Downloads a message as RFC 822 EML file.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces("message/rfc822")
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}/eml")
    @RolesAllowed({"loginRole"})
    public Response getMessageAsEml(@PathParam("mailboxId") String mailboxId, @PathParam("messageRef") String messageRef) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            byte[] eml = emailService.getMessageAsEml(mailboxId, messageRef);
            return Response.ok(eml, "message/rfc822")
                    .header("Content-Disposition", "attachment; filename=\"message.eml\"")
                    .build();
        } catch (Exception ex) {
            log.error("can not get EML for message " + messageRef + " from mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Downloads an attachment. Returns JSON with Base64-encoded content by default,
     * or raw binary if Accept header is application/octet-stream.
     *
     * @param mailboxId mailbox ID
     * @param messageRef opaque message reference
     * @param attachmentId attachment ID
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8", MediaType.APPLICATION_OCTET_STREAM})
    @Path("/mailboxes/{mailboxId}/messages/{messageRef}/attachments/{attachmentId}")
    @RolesAllowed({"loginRole"})
    public Response getAttachment(@PathParam("mailboxId") String mailboxId,
            @PathParam("messageRef") String messageRef,
            @PathParam("attachmentId") String attachmentId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            MailAttachmentDTO att = emailService.getAttachmentContent(mailboxId, messageRef, attachmentId);

            RestfulMailAttachmentV7 result = RestfulMailAttachmentV7.fromMailAttachmentDTO(att);
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not get attachment " + attachmentId + " for message " + messageRef + " in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Sends an email through the mailbox's configured backend.
     *
     * @param mailboxId mailbox ID
     * @param request send mail request with recipients, subject, body, attachments
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/send")
    @RolesAllowed({"loginRole"})
    public Response sendMail(@PathParam("mailboxId") String mailboxId, RestfulSendMailRequestV7 request) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);

            List<MailAttachmentDTO> attachments = new ArrayList<>();
            if (request.getAttachments() != null) {
                for (RestfulMailAttachmentV7 ra : request.getAttachments()) {
                    attachments.add(ra.toMailAttachmentDTO());
                }
            }

            emailService.sendMail(mailboxId,
                    request.getTo(),
                    request.getCc(),
                    request.getBcc(),
                    request.getSubject(),
                    request.getBody(),
                    request.getContentType(),
                    attachments,
                    request.getPriority(),
                    request.isReadReceipt(),
                    request.getInReplyTo(),
                    request.getReferences());

            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not send mail via mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Appends a message to a specific folder (e.g. Sent, Drafts).
     *
     * @param mailboxId mailbox ID
     * @param folderId target folder ID
     * @param request message data to append
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/folders/{folderId}/append")
    @RolesAllowed({"loginRole"})
    public Response appendToFolder(@PathParam("mailboxId") String mailboxId,
            @PathParam("folderId") String folderId,
            RestfulAppendMailRequestV7 request) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);

            List<MailAttachmentDTO> attachments = new ArrayList<>();
            if (request.getAttachments() != null) {
                for (RestfulMailAttachmentV7 ra : request.getAttachments()) {
                    attachments.add(ra.toMailAttachmentDTO());
                }
            }

            emailService.appendToFolder(mailboxId,
                    folderId,
                    request.getTo(),
                    request.getCc(),
                    request.getBcc(),
                    request.getSubject(),
                    request.getBody(),
                    request.getContentType(),
                    attachments,
                    request.isMarkAsRead());

            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not append message to folder " + folderId + " in mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Tests the connection to a mailbox.
     *
     * @param mailboxId mailbox ID
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/test")
    @RolesAllowed({"loginRole"})
    public Response testConnection(@PathParam("mailboxId") String mailboxId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            String error = emailService.testConnection(mailboxId);
            if (error == null) {
                return Response.ok("{\"success\":true}").build();
            } else {
                return Response.ok("{\"success\":false,\"error\":\"" + error.replace("\"", "\\\"") + "\"}").build();
            }
        } catch (Exception ex) {
            log.error("can not test connection for mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Invalidates all server-side caches for the given mailbox.
     *
     * @param mailboxId mailbox ID
     * @response 401 User not authorized
     * @response 403 User not authenticated / no mailbox access
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/mailboxes/{mailboxId}/cache")
    @RolesAllowed({"loginRole"})
    public Response invalidateCaches(@PathParam("mailboxId") String mailboxId) {
        try {
            InitialContext ic = new InitialContext();
            if (!hasMailboxAccess(ic, mailboxId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            EmailServiceLocal emailService = (EmailServiceLocal) ic.lookup(LOOKUP_EMAIL);
            emailService.invalidateCaches(mailboxId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not invalidate caches for mailbox " + mailboxId, ex);
            return Response.serverError().build();
        }
    }
}
