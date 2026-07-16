package org.jlawyer.io.rest.v8;

import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.AddressServiceLocal;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.BeaServiceLocal;
import com.jdimension.jlawyer.services.Keyword;
import com.jdimension.jlawyer.services.bea.rest.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
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
import org.jlawyer.io.rest.v8.pojo.RestfulBeaCaseSuggestionRequestV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebRenderRequestV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaExportV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaAttachmentV8;
import org.jlawyer.io.rest.v7.pojo.RestfulCaseSuggestionsV7;
import org.jlawyer.io.rest.v7.pojo.RestfulSuggestedCaseV7;
import org.jlawyer.io.rest.v7.pojo.RestfulSuggestedContactV7;

/**
 * REST API for beA (besonderes elektronisches Anwaltspostfach) operations.
 * Delegates all calls to BeaServiceLocal.
 */
@Stateless
@Path("/v8/bea")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags={"beA"})
public class BeaEndpointV8 implements BeaEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(BeaEndpointV8.class.getName());
    private static final String BEA_SERVICE_JNDI = "java:global/j-lawyer-server/j-lawyer-server-ejb/BeaService!com.jdimension.jlawyer.services.BeaServiceLocal";
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";
    private static final String LOOKUP_ADDRESS = "java:global/j-lawyer-server/j-lawyer-server-ejb/AddressService!com.jdimension.jlawyer.services.AddressServiceLocal";
    private static final int MAX_SUGGESTED_CASES = 50;

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
    @io.swagger.annotations.ApiOperation(value="Authenticates the current user with beA via beAstie.", response=com.jdimension.jlawyer.services.bea.rest.BeaLoginResult.class)
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
    @io.swagger.annotations.ApiOperation(value="Invalidates the current user's beA session.")
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
    @io.swagger.annotations.ApiOperation(value="Returns the beAstie version string.", response=String.class)
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
    @io.swagger.annotations.ApiOperation(value="Retrieves certificate attributes from a beA certificate.", response=Object.class)
    public Response getCertificateInformation(@io.swagger.annotations.ApiParam BeaCertificateInfoRequest request) {
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
    @io.swagger.annotations.ApiOperation(value="Lists all postboxes available to the authenticated user.", response=com.jdimension.jlawyer.services.bea.rest.BeaPostbox.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Retrieves details for a specific postbox.", response=com.jdimension.jlawyer.services.bea.rest.BeaPostbox.class)
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
    @io.swagger.annotations.ApiOperation(value="Checks whether the outbox of a postbox is empty.", response=Boolean.class)
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
    @io.swagger.annotations.ApiOperation(value="Checks whether a given Safe-ID or user name represents an EGVP postbox.", response=Boolean.class)
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
    @io.swagger.annotations.ApiOperation(value="Lists all folders in a postbox.", response=com.jdimension.jlawyer.services.bea.rest.BeaFolder.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Retrieves details for a specific folder.", response=com.jdimension.jlawyer.services.bea.rest.BeaFolder.class)
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
    @io.swagger.annotations.ApiOperation(value="Creates a new folder in a postbox.", response=com.jdimension.jlawyer.services.bea.rest.BeaFolder.class)
    public Response createFolder(@PathParam("safeId") String safeId, @io.swagger.annotations.ApiParam BeaFolder folder) {
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
    @io.swagger.annotations.ApiOperation(value="Deletes a folder from a postbox.")
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
    @io.swagger.annotations.ApiOperation(value="Lists all messages in a folder.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessageHeader.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Searches messages in a folder with filter criteria.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessageHeader.class, responseContainer="List")
    public Response searchMessages(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId, @io.swagger.annotations.ApiParam BeaMessageFilter filter) {
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
    @io.swagger.annotations.ApiOperation(value="Lists only the message IDs in a folder.", response=String.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Searches for message IDs in a folder with filter criteria.", response=String.class, responseContainer="List")
    public Response searchMessageIds(@PathParam("safeId") String safeId, @PathParam("folderId") long folderId, @io.swagger.annotations.ApiParam BeaMessageFilter filter) {
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
     * Retrieves a full message, optionally including attachment content.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @param includeAttachments whether to include attachment content (defaults to true)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value="Retrieves a full message, optionally including attachment content.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessage.class)
    public Response getMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @QueryParam("includeAttachments") Boolean includeAttachments) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            boolean include = (includeAttachments == null) ? true : includeAttachments;
            BeaMessage message = bea.getMessage(safeId, messageId, include);
            return Response.ok(message).build();
        } catch (Exception ex) {
            log.error("can not get message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Exports a beA message as a {@code .bea} file: the whole message (including its attachments) is
     * serialized to XML, the same format the desktop client produces, so it can be stored in a case
     * as a single document. The response carries a suggested file name and the Base64-encoded
     * content.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @return the exported message
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 There is no message with the given id
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/export")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Exports a beA message as a .bea file (the whole message serialized to XML).", response = RestfulBeaExportV8.class)
    public Response exportMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaMessage message = bea.getMessage(safeId, messageId, true);
            if (message == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("There is no message with id " + messageId).build();
            }

            JAXBContext ctx = JAXBContext.newInstance(BeaMessage.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            m.marshal(message, out);

            RestfulBeaExportV8 result = new RestfulBeaExportV8();
            result.setFileName(buildBeaFileName(message));
            result.setContentBase64(java.util.Base64.getEncoder().encodeToString(out.toByteArray()));
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not export message " + messageId + " for " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Builds the {@code .bea} file name for a message, mirroring the desktop client
     * (referenceNumber_subject_sender--recipient.bea, with unsafe characters replaced).
     */
    private static String buildBeaFileName(BeaMessage msg) {
        String refNum = msg.getReferenceNumber() != null ? msg.getReferenceNumber() : "";
        String subj = msg.getSubject() != null ? msg.getSubject() : "";
        String sender = msg.getSenderName() != null ? msg.getSenderName() : "";
        String recipient = "";
        if (msg.getRecipients() != null && !msg.getRecipients().isEmpty() && msg.getRecipients().get(0).getName() != null) {
            recipient = msg.getRecipients().get(0).getName();
        }
        String fileName = refNum + "_" + subj + "_" + sender + "--" + recipient + ".bea";
        return fileName.replaceAll("[^a-zA-Z0-9äöüÄÖÜß._\\-]", "_");
    }

    /**
     * Suggests cases for an opened beA message: the two beA reference numbers plus subject and body
     * are matched against known own and referenced file numbers, and the sender name against the
     * address book. All case lookups are ACL-filtered by the service layer. Mirrors the email
     * case-suggestion heuristic, adapted to beA (references instead of an e-mail address).
     *
     * @param request the message's subject, body, reference numbers and sender name
     * @return the suggested cases, matching contacts and extracted phone numbers
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/case-suggestions")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Suggests cases and contacts for an opened beA message.", response = RestfulCaseSuggestionsV7.class)
    public Response caseSuggestions(@io.swagger.annotations.ApiParam RestfulBeaCaseSuggestionRequestV8 request) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            AddressServiceLocal addresses = (AddressServiceLocal) ic.lookup(LOOKUP_ADDRESS);

            String subject = request.getSubject() != null ? request.getSubject() : "";
            String bodyRaw = request.getBody() != null ? request.getBody() : "";
            String refNumber = request.getReferenceNumber() != null ? request.getReferenceNumber() : "";
            String refJustice = request.getReferenceJustice() != null ? request.getReferenceJustice() : "";
            String senderName = request.getSenderName() != null ? request.getSenderName().trim() : "";

            // The beA reference numbers are part of the searchable text, so a file number appearing
            // in either reference field is matched just like one appearing in the subject or body.
            String haystack = (subject + " " + bodyRaw + " " + refNumber + " " + refJustice).toLowerCase();

            RestfulCaseSuggestionsV7 result = new RestfulCaseSuggestionsV7();
            result.setSenderName(senderName);
            result.setSenderEmail("");

            // Phone numbers from the body (best-effort).
            List<String> phoneNumbers = new ArrayList<>();
            try {
                Collection<Keyword> keywords = cases.extractKeywordsFromText(bodyRaw);
                if (keywords != null) {
                    for (Keyword w : keywords) {
                        if (w.getType() == Keyword.TYPE_PHONENR && w.getValue() != null && !phoneNumbers.contains(w.getValue())) {
                            phoneNumbers.add(w.getValue());
                        }
                    }
                }
            } catch (Throwable t) {
                log.warn("could not extract keywords from beA body", t);
            }
            result.setPhoneNumbers(phoneNumbers);

            Map<String, RestfulSuggestedCaseV7> byId = new LinkedHashMap<>();

            // 1) own file numbers appearing in the searchable text
            for (String fn : cases.getAllArchiveFileNumbers(true)) {
                if (fn == null || fn.isEmpty()) {
                    continue;
                }
                if (haystack.contains(fn.toLowerCase())) {
                    addCase(byId, cases.getArchiveFileByFileNumber(fn), "subjectBody");
                }
                if (byId.size() >= MAX_SUGGESTED_CASES) {
                    break;
                }
            }

            // 2) foreign/referenced file numbers appearing in the searchable text
            for (String fn : cases.getAllReferencedFileNumbers(5, true)) {
                if (fn == null || fn.isEmpty()) {
                    continue;
                }
                if (haystack.contains(fn.toLowerCase())) {
                    List<ArchiveFileAddressesBean> refs = cases.getArchiveFileAddressesByReference(fn);
                    if (refs != null) {
                        for (ArchiveFileAddressesBean aab : refs) {
                            addCase(byId, aab.getArchiveFileKey(), "reference");
                        }
                    }
                }
                if (byId.size() >= MAX_SUGGESTED_CASES) {
                    break;
                }
            }

            // 3) sender name -> matching contacts -> their cases
            List<RestfulSuggestedContactV7> contacts = new ArrayList<>();
            if (!senderName.isEmpty()) {
                AddressBean[] hits = addresses.searchSimple(senderName);
                if (hits != null) {
                    Set<String> seenContacts = new HashSet<>();
                    for (AddressBean h : hits) {
                        if (h == null || h.getId() == null || !seenContacts.add(h.getId())) {
                            continue;
                        }
                        RestfulSuggestedContactV7 c = new RestfulSuggestedContactV7();
                        c.setId(h.getId());
                        c.setDisplayName(contactDisplayName(h));
                        c.setEmail(h.getEmail());
                        contacts.add(c);

                        Collection<?> caseCol = cases.getArchiveFileAddressesForAddress(h.getId());
                        if (caseCol != null) {
                            for (Object o : caseCol) {
                                addCase(byId, ((ArchiveFileAddressesBean) o).getArchiveFileKey(), "sender");
                            }
                        }
                        if (byId.size() >= MAX_SUGGESTED_CASES) {
                            break;
                        }
                    }
                }
            }
            result.setContacts(contacts);
            result.setSuggestedCases(new ArrayList<>(byId.values()));

            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not compute beA case suggestions", ex);
            return Response.serverError().build();
        }
    }

    /** Adds a case to the deduplicated suggestion map (first source wins; null/duplicate ignored). */
    private static void addCase(Map<String, RestfulSuggestedCaseV7> byId, ArchiveFileBean a, String source) {
        if (a == null || a.getId() == null || byId.containsKey(a.getId())) {
            return;
        }
        RestfulSuggestedCaseV7 c = new RestfulSuggestedCaseV7();
        c.setId(a.getId());
        c.setFileNumber(a.getFileNumber());
        c.setName(a.getName());
        c.setReason(a.getReason());
        c.setArchived(a.isArchived());
        c.setSource(source);
        byId.put(a.getId(), c);
    }

    /** Builds a readable contact label: "Firstname Name (Company)", or the company/name alone. */
    private static String contactDisplayName(AddressBean h) {
        String company = h.getCompany() != null ? h.getCompany().trim() : "";
        String person = ((h.getFirstName() != null ? h.getFirstName() : "") + " "
                + (h.getName() != null ? h.getName() : "")).trim();
        if (person.isEmpty()) {
            return company;
        }
        return company.isEmpty() ? person : person + " (" + company + ")";
    }

    /**
     * Retrieves the content of a single attachment from a message.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @param attachmentName the name of the attachment
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/attachments/{attachmentName}")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value="Retrieves the content of a single attachment from a message.", response=com.jdimension.jlawyer.services.bea.rest.BeaAttachment.class)
    public Response getAttachmentContent(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @PathParam("attachmentName") String attachmentName) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaAttachment attachment = bea.getAttachmentContent(safeId, messageId, attachmentName);
            return Response.ok(attachment).build();
        } catch (Exception ex) {
            log.error("can not get attachment " + attachmentName + " for message " + messageId + " in " + safeId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Retrieves the content of a single attachment with the content Base64-encoded. This is a
     * variant of {@link #getAttachmentContent(String, String, String)} intended for browser clients:
     * it returns the content as a Base64 {@code String} ({@code contentBase64}) rather than a raw
     * {@code byte[]}, which is serialized inconsistently across JSON providers and cannot be decoded
     * reliably in JavaScript. The response therefore carries only one copy of the content.
     *
     * @param safeId the Safe-ID of the postbox
     * @param messageId the message ID
     * @param attachmentName the name of the attachment
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/postboxes/{safeId}/messages/{messageId}/attachments/{attachmentName}/base64")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value="Retrieves the content of a single attachment, Base64-encoded (for browser clients).", response=RestfulBeaAttachmentV8.class)
    public Response getAttachmentContentBase64(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @PathParam("attachmentName") String attachmentName) {
        try {
            InitialContext ic = new InitialContext();
            BeaServiceLocal bea = (BeaServiceLocal) ic.lookup(BEA_SERVICE_JNDI);
            BeaAttachment attachment = bea.getAttachmentContent(safeId, messageId, attachmentName);
            return Response.ok(RestfulBeaAttachmentV8.fromBea(attachment)).build();
        } catch (Exception ex) {
            log.error("can not get attachment " + attachmentName + " for message " + messageId + " in " + safeId, ex);
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
    @io.swagger.annotations.ApiOperation(value="Retrieves a message header without attachments.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessageHeader.class)
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
    @io.swagger.annotations.ApiOperation(value="Sends a beA message.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessage.class)
    public Response sendMessage(@PathParam("safeId") String safeId, @io.swagger.annotations.ApiParam BeaSendMessageRequest request) {
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
    @io.swagger.annotations.ApiOperation(value="Saves a message as draft.", response=String.class)
    public Response saveDraft(@PathParam("safeId") String safeId, @io.swagger.annotations.ApiParam BeaSaveDraftRequest request) {
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
    @io.swagger.annotations.ApiOperation(value="Deletes a message (moves to trash).")
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
    @io.swagger.annotations.ApiOperation(value="Restores a message from trash.", response=Boolean.class)
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
    @io.swagger.annotations.ApiOperation(value="Moves a message to a different folder.", response=Boolean.class)
    public Response moveMessage(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @io.swagger.annotations.ApiParam BeaMoveMessageRequest request) {
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
    @io.swagger.annotations.ApiOperation(value="Marks a message as read.", response=Boolean.class)
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
    @io.swagger.annotations.ApiOperation(value="Checks whether a message has been read by a specific identity.", response=Boolean.class)
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
    @io.swagger.annotations.ApiOperation(value="Retrieves the journal entries for a message.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessageJournalEntry.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Retrieves the process cards (Laufzettel) for a message.", response=com.jdimension.jlawyer.services.bea.rest.BeaProcessCard.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Verifies the signature of a message.", response=com.jdimension.jlawyer.services.bea.rest.BeaVerificationResult.class)
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
    @io.swagger.annotations.ApiOperation(value="Looks up a beA identity by Safe-ID, optionally filtered by ZIP code.", response=com.jdimension.jlawyer.services.bea.rest.BeaIdentity.class)
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
    @io.swagger.annotations.ApiOperation(value="Searches for beA identities by multiple criteria.", response=com.jdimension.jlawyer.services.bea.rest.BeaIdentity.class, responseContainer="List")
    public Response searchIdentity(@io.swagger.annotations.ApiParam BeaIdentitySearchRequest request) {
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
    @io.swagger.annotations.ApiOperation(value="Lists all legal authorities.", response=com.jdimension.jlawyer.services.bea.rest.BeaListItem.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Returns the default legal authority.", response=com.jdimension.jlawyer.services.bea.rest.BeaListItem.class)
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
    @io.swagger.annotations.ApiOperation(value="Lists all message priorities.", response=com.jdimension.jlawyer.services.bea.rest.BeaListItem.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Checks whether the given XML is an eEB request.", response=Boolean.class)
    public Response isEebRequest(@io.swagger.annotations.ApiParam String xml) {
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
    @io.swagger.annotations.ApiOperation(value="Checks whether the given XML is an eEB response.", response=Boolean.class)
    public Response isEebResponse(@io.swagger.annotations.ApiParam String xml) {
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
    @io.swagger.annotations.ApiOperation(value="Extracts attributes from an eEB request XML.", response=com.jdimension.jlawyer.services.bea.rest.BeaEebRequestAttributes.class)
    public Response getEebRequestAttributes(@io.swagger.annotations.ApiParam String xml) {
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
    @io.swagger.annotations.ApiOperation(value="Extracts attributes from an eEB response XML.", response=com.jdimension.jlawyer.services.bea.rest.BeaEebResponseAttributes.class)
    public Response getEebResponseAttributes(@io.swagger.annotations.ApiParam String xml) {
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
    @io.swagger.annotations.ApiOperation(value="Renders an eEB as HTML for display.", response=String.class)
    public Response renderEebHtml(@io.swagger.annotations.ApiParam RestfulBeaEebRenderRequestV8 request) {
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
    @io.swagger.annotations.ApiOperation(value="Retrieves the list of eEB rejection reasons.", response=com.jdimension.jlawyer.services.bea.rest.BeaListItem.class, responseContainer="List")
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
    @io.swagger.annotations.ApiOperation(value="Sends an eEB confirmation.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessage.class)
    public Response sendEebConfirmation(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @io.swagger.annotations.ApiParam RestfulBeaEebConfirmationV8 request) {
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
    @io.swagger.annotations.ApiOperation(value="Sends an eEB rejection.", response=com.jdimension.jlawyer.services.bea.rest.BeaMessage.class)
    public Response sendEebRejection(@PathParam("safeId") String safeId, @PathParam("messageId") String messageId, @io.swagger.annotations.ApiParam RestfulBeaEebRejectionV8 request) {
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
