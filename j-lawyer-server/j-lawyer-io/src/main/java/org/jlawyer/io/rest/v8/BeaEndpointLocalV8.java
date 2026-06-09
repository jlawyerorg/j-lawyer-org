package org.jlawyer.io.rest.v8;

import com.jdimension.jlawyer.services.bea.rest.*;
import javax.ejb.Local;
import javax.ws.rs.core.Response;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebConfirmationV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebRejectionV8;
import org.jlawyer.io.rest.v8.pojo.RestfulBeaEebRenderRequestV8;

/**
 *
 * @author jens
 */
@Local
public interface BeaEndpointLocalV8 {

    // Auth & Session
    Response login();
    Response logout();
    Response getBeaWrapperVersion();
    Response getCertificateInformation(BeaCertificateInfoRequest request);

    // Postboxes
    Response getPostboxes();
    Response getPostbox(String safeId);
    Response isOutboxEmpty(String safeId);
    Response isEgvpPostBox(String safeId, String userName);

    // Folders
    Response getFolders(String safeId);
    Response getFolder(String safeId, long folderId);
    Response createFolder(String safeId, BeaFolder folder);
    Response deleteFolder(String safeId, long folderId);

    // Messages
    Response getMessages(String safeId, long folderId);
    Response searchMessages(String safeId, long folderId, BeaMessageFilter filter);
    Response getMessageIds(String safeId, long folderId);
    Response searchMessageIds(String safeId, long folderId, BeaMessageFilter filter);
    Response getMessage(String safeId, String messageId, Boolean includeAttachments);
    Response getAttachmentContent(String safeId, String messageId, String attachmentName);
    Response getMessageHeader(String safeId, String messageId);
    Response sendMessage(String safeId, BeaSendMessageRequest request);
    Response saveDraft(String safeId, BeaSaveDraftRequest request);
    Response deleteMessage(String safeId, String messageId);
    Response restoreMessage(String safeId, String messageId);
    Response moveMessage(String safeId, String messageId, BeaMoveMessageRequest request);
    Response markMessageRead(String safeId, String messageId);
    Response isMessageReadByIdentity(String safeId, String messageId, String targetSafeId);
    Response getMessageJournal(String safeId, String messageId);
    Response getProcessCards(String safeId, String messageId);
    Response verifyMessage(String safeId, String messageId);

    // Identity
    Response getIdentity(String safeId, String zipCode);
    Response searchIdentity(BeaIdentitySearchRequest request);

    // Reference Data
    Response getLegalAuthorities();
    Response getDefaultLegalAuthority();
    Response getMessagePriorities();

    // eEB
    Response isEebRequest(String xml);
    Response isEebResponse(String xml);
    Response getEebRequestAttributes(String xml);
    Response getEebResponseAttributes(String xml);
    Response renderEebHtml(RestfulBeaEebRenderRequestV8 request);
    Response getEebRejectionReasons();
    Response sendEebConfirmation(String safeId, String messageId, RestfulBeaEebConfirmationV8 request);
    Response sendEebRejection(String safeId, String messageId, RestfulBeaEebRejectionV8 request);

}
