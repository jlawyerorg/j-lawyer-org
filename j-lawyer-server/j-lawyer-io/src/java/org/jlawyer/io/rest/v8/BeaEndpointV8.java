package org.jlawyer.io.rest.v8;

import com.jdimension.jlawyer.services.BeaServiceLocal;
import com.jdimension.jlawyer.services.bea.rest.*;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebConfirmationV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebRejectionV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebRenderRequestV8;

/**
 * REST API for beA (besonderes elektronisches Anwaltspostfach) operations.
 * Delegates all calls to BeaServiceLocal.
 */
@Stateless
@Path("/v8/bea")
@Consumes({"application/json"})
@Produces({"application/json"})
public class BeaEndpointV8 implements BeaEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(BeaEndpointV8.class.getName());
    private static final String BEA_SERVICE_JNDI = "java:global/j-lawyer-server/j-lawyer-server-ejb/BeaService!com.jdimension.jlawyer.services.BeaServiceLocal";

    // ========== Auth & Session ==========

    /**
     * Authenticates the current user with beA via beAstie.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/login")
    @RolesAllowed({"loginRole"})
    public Response login() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaLoginResult result = bea.login();
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("beA login failed", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Invalidates the current user's beA session.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/logout")
    @RolesAllowed({"loginRole"})
    public Response logout() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            bea.logout();
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("beA logout failed", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the beAstie version string.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/version")
    @RolesAllowed({"loginRole"})
    public Response getBeaWrapperVersion() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            String version = bea.getBeaWrapperVersion();
            return Response.ok(version).build();
        } catch (Exception ex) {
            log.error("can not get beA wrapper version", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves certificate attributes from a beA certificate.
     *
     * @param request certificate and password
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/certificate-info")
    @RolesAllowed({"loginRole"})
    public Response getCertificateInformation(BeaCertificateInfoRequest request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            Map<String, String> info = bea.getCertificateInformation(request.getCertificate(), request.getPassword());
            return Response.ok(info).build();
        } catch (Exception ex) {
            log.error("can not get certificate information", ex);
            return Response.serverError().build();
        }
    }

    // ========== Postboxes ==========

    /**
     * Lists all postboxes available to the authenticated user.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes")
    @RolesAllowed({"loginRole"})
    public Response getPostboxes() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaPostbox> postboxes = bea.getPostboxes();
            return Response.ok(postboxes).build();
        } catch (Exception ex) {
            log.error("can not get postboxes", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves details for a specific postbox.
     *
     * @param safeId the Safe-ID of the postbox
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}")
    @RolesAllowed({"loginRole"})
    public Response getPostbox(@PathParam("safeId") String safeId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaPostbox postbox = bea.getPostbox(safeId);
            return Response.ok(postbox).build();
        } catch (Exception ex) {
            log.error("can not get postbox " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Checks whether the outbox of a postbox is empty.
     *
     * @param safeId the Safe-ID of the postbox
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/outbox/empty")
    @RolesAllowed({"loginRole"})
    public Response isOutboxEmpty(@PathParam("safeId") String safeId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean empty = bea.isOutboxEmpty(safeId);
            return Response.ok(empty).build();
        } catch (Exception ex) {
            log.error("can not check outbox for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Checks whether a given Safe-ID or user name represents an EGVP postbox.
     *
     * @param safeId the Safe-ID to check
     * @param userName optional user name to check
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/egvp")
    @RolesAllowed({"loginRole"})
    public Response isEgvpPostBox(@PathParam("safeId") String safeId, @QueryParam("userName") String userName) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean egvp = bea.isEgvpPostBox(safeId, userName);
            return Response.ok(egvp).build();
        } catch (Exception ex) {
            log.error("can not check EGVP status for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    // ========== Folders ==========

    /**
     * Lists all folders in a postbox.
     *
     * @param safeId the Safe-ID of the postbox
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/folders")
    @RolesAllowed({"loginRole"})
    public Response getFolders(@PathParam("safeId") String safeId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaFolder> folders = bea.getFolders(safeId);
            return Response.ok(folders).build();
        } catch (Exception ex) {
            log.error("can not get folders for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves details for a specific folder.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/folders/{folderId}")
    @RolesAllowed({"loginRole"})
    public Response getFolder(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaFolder folder = bea.getFolder(safeId, folderId);
            return Response.ok(folder).build();
        } catch (Exception ex) {
            log.error("can not get folder " + folderId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Creates a new folder in a postbox.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folder folder data with name and optional parentFolderId
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/folders")
    @RolesAllowed({"loginRole"})
    public Response createFolder(@PathParam("safeId") String safeId, BeaFolder folder) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            Long parentId = folder.getParentId() != 0 ? folder.getParentId() : null;
            BeaFolder created = bea.createFolder(safeId, folder.getName(), parentId);
            return Response.ok(created).build();
        } catch (Exception ex) {
            log.error("can not create folder for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes a folder from a postbox.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID to delete
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/folders/{folderId}")
    @RolesAllowed({"loginRole"})
    public Response deleteFolder(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            bea.deleteFolder(safeId, folderId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete folder " + folderId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    // ========== Messages ==========

    /**
     * Lists all messages in a folder.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/folders/{folderId}/messages")
    @RolesAllowed({"loginRole"})
    public Response getMessages(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaMessageHeader> messages = bea.getMessages(safeId, folderId);
            return Response.ok(messages).build();
        } catch (Exception ex) {
            log.error("can not get messages for folder " + folderId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Searches messages in a folder with filter criteria.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID
     * @param filter the search/filter criteria
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/folders/{folderId}/messages/search")
    @RolesAllowed({"loginRole"})
    public Response searchMessages(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId, BeaMessageFilter filter) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaMessageHeader> messages = bea.searchMessages(safeId, folderId, filter);
            return Response.ok(messages).build();
        } catch (Exception ex) {
            log.error("can not search messages for folder " + folderId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Lists only the message IDs in a folder.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/folders/{folderId}/messageids")
    @RolesAllowed({"loginRole"})
    public Response getMessageIds(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<String> ids = bea.getMessageIds(safeId, folderId);
            return Response.ok(ids).build();
        } catch (Exception ex) {
            log.error("can not get message IDs for folder " + folderId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Searches for message IDs in a folder with filter criteria.
     *
     * @param safeId the Safe-ID of the postbox
     * @param folderId the folder ID
     * @param filter the search/filter criteria
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/folders/{folderId}/messageids/search")
    @RolesAllowed({"loginRole"})
    public Response searchMessageIds(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId, BeaMessageFilter filter) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<String> ids = bea.searchMessageIds(safeId, folderId, filter);
            return Response.ok(ids).build();
        } catch (Exception ex) {
            log.error("can not search message IDs for folder " + folderId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves a full message including body and attachments.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}")
    @RolesAllowed({"loginRole"})
    public Response getMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessage message = bea.getMessage(safeId, messageId);
            return Response.ok(message).build();
        } catch (Exception ex) {
            log.error("can not get message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves a message header without attachments.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/header")
    @RolesAllowed({"loginRole"})
    public Response getMessageHeader(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessageHeader header = bea.getMessageHeader(safeId, messageId);
            return Response.ok(header).build();
        } catch (Exception ex) {
            log.error("can not get message header " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Sends a beA message.
     *
     * @param safeId the Safe-ID of the sender's postbox
     * @param request the send message request
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/messages")
    @RolesAllowed({"loginRole"})
    public Response sendMessage(@PathParam("safeId") String safeId, BeaSendMessageRequest request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessage sent = bea.sendMessage(safeId, request);
            return Response.ok(sent).build();
        } catch (Exception ex) {
            log.error("can not send message for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Saves a message as draft.
     *
     * @param safeId the Safe-ID of the postbox
     * @param request the draft message request
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/messages/draft")
    @RolesAllowed({"loginRole"})
    public Response saveDraft(@PathParam("safeId") String safeId, BeaSaveDraftRequest request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            String draftId = bea.saveDraft(safeId, request);
            return Response.ok(draftId).build();
        } catch (Exception ex) {
            log.error("can not save draft for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes a message (moves to trash).
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID to delete
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}")
    @RolesAllowed({"loginRole"})
    public Response deleteMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            bea.deleteMessage(safeId, messageId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Restores a message from trash.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID to restore
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/restore")
    @RolesAllowed({"loginRole"})
    public Response restoreMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean restored = bea.restoreMessage(safeId, messageId);
            return Response.ok(restored).build();
        } catch (Exception ex) {
            log.error("can not restore message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Moves a message to a different folder.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID to move
     * @param request the move request with targetFolderId
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/messages/{messageId}/move")
    @RolesAllowed({"loginRole"})
    public Response moveMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, BeaMoveMessageRequest request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean moved = bea.moveMessage(safeId, messageId, request.getTargetFolderId());
            return Response.ok(moved).build();
        } catch (Exception ex) {
            log.error("can not move message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Marks a message as read.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID to mark as read
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/read")
    @RolesAllowed({"loginRole"})
    public Response markMessageRead(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean marked = bea.markMessageRead(safeId, messageId);
            return Response.ok(marked).build();
        } catch (Exception ex) {
            log.error("can not mark message " + messageId + " as read for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Checks whether a message has been read by a specific identity.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @param targetSafeId the Safe-ID of the identity to check
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/read/{targetSafeId}")
    @RolesAllowed({"loginRole"})
    public Response isMessageReadByIdentity(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @PathParam("targetSafeId") String targetSafeId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean read = bea.isMessageReadByIdentity(safeId, messageId, targetSafeId);
            return Response.ok(read).build();
        } catch (Exception ex) {
            log.error("can not check read status for message " + messageId + " by " + targetSafeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves the journal entries for a message.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/journal")
    @RolesAllowed({"loginRole"})
    public Response getMessageJournal(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaMessageJournalEntry> journal = bea.getMessageJournal(safeId, messageId);
            return Response.ok(journal).build();
        } catch (Exception ex) {
            log.error("can not get journal for message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves the process cards (Laufzettel) for a message.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/processcards")
    @RolesAllowed({"loginRole"})
    public Response getProcessCards(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaProcessCard> cards = bea.getProcessCards(safeId, messageId);
            return Response.ok(cards).build();
        } catch (Exception ex) {
            log.error("can not get process cards for message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Verifies the signature of a message.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/verify")
    @RolesAllowed({"loginRole"})
    public Response verifyMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaVerificationResult result = bea.verifyMessage(safeId, messageId);
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not verify message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    // ========== Identity ==========

    /**
     * Looks up a beA identity by Safe-ID, optionally filtered by ZIP code.
     *
     * @param safeId the Safe-ID to look up
     * @param zipCode optional ZIP code filter
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/identities/{safeId}")
    @RolesAllowed({"loginRole"})
    public Response getIdentity(@PathParam("safeId") String safeId, @QueryParam("zipCode") String zipCode) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaIdentity identity;
            if (zipCode != null && !zipCode.isEmpty()) {
                identity = bea.getIdentity(safeId, zipCode);
            } else {
                identity = bea.getIdentity(safeId);
            }
            return Response.ok(identity).build();
        } catch (Exception ex) {
            log.error("can not get identity for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Searches for beA identities by multiple criteria.
     *
     * @param request the search criteria
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/identities/search")
    @RolesAllowed({"loginRole"})
    public Response searchIdentity(BeaIdentitySearchRequest request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaIdentity> identities = bea.searchIdentity(request);
            return Response.ok(identities).build();
        } catch (Exception ex) {
            log.error("can not search identities", ex);
            return Response.serverError().build();
        }
    }

    // ========== Reference Data ==========

    /**
     * Lists all legal authorities.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/legal-authorities")
    @RolesAllowed({"loginRole"})
    public Response getLegalAuthorities() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaListItem> authorities = bea.getLegalAuthorities();
            return Response.ok(authorities).build();
        } catch (Exception ex) {
            log.error("can not get legal authorities", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the default legal authority.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/legal-authorities/default")
    @RolesAllowed({"loginRole"})
    public Response getDefaultLegalAuthority() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaListItem authority = bea.getDefaultLegalAuthority();
            return Response.ok(authority).build();
        } catch (Exception ex) {
            log.error("can not get default legal authority", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Lists all message priorities.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/message-priorities")
    @RolesAllowed({"loginRole"})
    public Response getMessagePriorities() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaListItem> priorities = bea.getMessagePriorities();
            return Response.ok(priorities).build();
        } catch (Exception ex) {
            log.error("can not get message priorities", ex);
            return Response.serverError().build();
        }
    }

    // ========== eEB ==========

    /**
     * Checks whether the given XML is an eEB request.
     *
     * @param xml the XML content to check
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/eeb/check-request")
    @RolesAllowed({"loginRole"})
    public Response isEebRequest(String xml) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean result = bea.isEebRequest(xml);
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not check eEB request", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Checks whether the given XML is an eEB response.
     *
     * @param xml the XML content to check
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/eeb/check-response")
    @RolesAllowed({"loginRole"})
    public Response isEebResponse(String xml) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean result = bea.isEebResponse(xml);
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not check eEB response", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Extracts attributes from an eEB request XML.
     *
     * @param xml the eEB request XML
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/eeb/request-attributes")
    @RolesAllowed({"loginRole"})
    public Response getEebRequestAttributes(String xml) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaEebRequestAttributes attributes = bea.getEebRequestAttributes(xml);
            return Response.ok(attributes).build();
        } catch (Exception ex) {
            log.error("can not get eEB request attributes", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Extracts attributes from an eEB response XML.
     *
     * @param xml the eEB response XML
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/eeb/response-attributes")
    @RolesAllowed({"loginRole"})
    public Response getEebResponseAttributes(String xml) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaEebResponseAttributes attributes = bea.getEebResponseAttributes(xml);
            return Response.ok(attributes).build();
        } catch (Exception ex) {
            log.error("can not get eEB response attributes", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Renders an eEB as HTML for display.
     *
     * @param request the render request with xmlRequest and xmlResponse
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/eeb/render-html")
    @RolesAllowed({"loginRole"})
    public Response renderEebHtml(RestfulBeaEebRenderRequestV8 request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            String html = bea.renderEebHtml(request.getXmlRequest(), request.getXmlResponse());
            return Response.ok(html).build();
        } catch (Exception ex) {
            log.error("can not render eEB HTML", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves the list of eEB rejection reasons.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/eeb/rejection-reasons")
    @RolesAllowed({"loginRole"})
    public Response getEebRejectionReasons() {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            List<BeaListItem> reasons = bea.getEebRejectionReasons();
            return Response.ok(reasons).build();
        } catch (Exception ex) {
            log.error("can not get eEB rejection reasons", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Sends an eEB confirmation.
     *
     * @param safeId the Safe-ID of the sender's postbox
     * @param messageId the original message ID
     * @param request the confirmation details
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/messages/{messageId}/eeb/confirm")
    @RolesAllowed({"loginRole"})
    public Response sendEebConfirmation(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, RestfulBeaEebConfirmationV8 request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessage result = bea.sendEebConfirmation(safeId, messageId, request.getSenderSafeId(), request.getRecipientSafeId(), request.getAbgabeDate());
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not send eEB confirmation for message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Sends an eEB rejection.
     *
     * @param safeId the Safe-ID of the sender's postbox
     * @param messageId the original message ID
     * @param request the rejection details
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/postboxes/{safeId}/messages/{messageId}/eeb/reject")
    @RolesAllowed({"loginRole"})
    public Response sendEebRejection(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, RestfulBeaEebRejectionV8 request) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessage result = bea.sendEebRejection(safeId, messageId, request.getSenderSafeId(), request.getRecipientSafeId(), request.getCode(), request.getComment());
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not send eEB rejection for message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

}
