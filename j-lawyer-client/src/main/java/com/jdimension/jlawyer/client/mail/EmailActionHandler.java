/*
 * Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jdimension.jlawyer.client.mail;

import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.client.utils.FileUtils;
import com.jdimension.jlawyer.client.utils.FrameUtils;
import com.jdimension.jlawyer.client.utils.StringUtils;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.CaseFolder;
import com.jdimension.jlawyer.persistence.MailboxSetup;
import com.jdimension.jlawyer.server.utils.ContentTypes;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeUtility;
import org.apache.log4j.Logger;
import org.simplejavamail.outlookmessageparser.model.OutlookFileAttachment;
import org.simplejavamail.outlookmessageparser.model.OutlookMessage;

/**
 * Shared implementation of the Reply / Reply-All / Forward / Edit-Draft
 * actions available on both the standalone email viewer window and the
 * inline email preview panels in the case documents list.
 *
 * Every action builds a preconfigured {@link SendEmailFrame}, wires it up
 * with the case context and party candidates, shows and returns it. The
 * caller is responsible for any additional lifecycle handling (e.g.
 * disposing an owning frame).
 */
public class EmailActionHandler {

    private static final Logger log = Logger.getLogger(EmailActionHandler.class.getName());

    private EmailActionHandler() {
    }

    public static SendEmailFrame reply(MessageContainer emlMsg, OutlookMessage outlookMsg, MailContentUI content, ArchiveFileBean contextArchiveFile, CaseFolder contextFolder) {
        SendEmailFrame dlg;
        if (emlMsg != null) {
            if (emlMsg.isServerBased()) {
                dlg = EmailUtils.reply(emlMsg.getMessageDTO(), emlMsg.getMailboxId(), null, content.getBody(), content.getContentType());
            } else {
                Message m = emlMsg.getMessage();
                dlg = EmailUtils.reply(m, content.getBody(), content.getContentType());
            }
        } else {
            dlg = EmailUtils.reply(outlookMsg, content.getBody(), content.getContentType());
        }
        dlg.setArchiveFile(contextArchiveFile, contextFolder);
        setPartiesToSendDialog(dlg, contextArchiveFile, false);
        showFrame(dlg);
        return dlg;
    }

    public static SendEmailFrame replyAll(MessageContainer emlMsg, OutlookMessage outlookMsg, MailContentUI content, ArchiveFileBean contextArchiveFile, CaseFolder contextFolder) {
        SendEmailFrame dlg = new SendEmailFrame(true);
        dlg.setArchiveFile(contextArchiveFile, contextFolder);
        setPartiesToSendDialog(dlg, contextArchiveFile, false);

        if (emlMsg != null) {
            try {
                if (emlMsg.isServerBased()) {
                    com.jdimension.jlawyer.services.MailMessageDTO dto = emlMsg.getMessageDTO();
                    for (MailboxSetup mbx : UserSettings.getInstance().getMailboxes(UserSettings.getInstance().getCurrentUser().getPrincipalId())) {
                        if (mbx.getId().equals(emlMsg.getMailboxId())) {
                            dlg.setFrom(mbx);
                            break;
                        }
                    }
                    StringBuilder toSb = new StringBuilder();
                    if (dto.getFrom() != null) {
                        toSb.append(dto.getFrom());
                    }
                    if (dto.getTo() != null) {
                        for (String t : dto.getTo()) {
                            if (toSb.length() > 0) {
                                toSb.append(", ");
                            }
                            toSb.append(t);
                        }
                    }
                    dlg.setTo(toSb.toString());
                    if (dto.getCc() != null) {
                        StringBuilder ccSb = new StringBuilder();
                        for (String c : dto.getCc()) {
                            if (ccSb.length() > 0) {
                                ccSb.append(", ");
                            }
                            ccSb.append(c);
                        }
                        dlg.setCC(ccSb.toString());
                    }
                    String subject = dto.getSubject() != null ? dto.getSubject() : "";
                    if (!subject.startsWith("Re: ")) {
                        subject = "Re: " + subject;
                    }
                    dlg.setSubject(subject);
                    String decodedTo = dto.getFrom() != null ? dto.getFrom() : "";
                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith("text/html")) {
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), "text/plain", decodedTo, dto.getDate()), "text/plain");
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), "text/html", decodedTo, dto.getDate()), "text/html");
                    } else {
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), "text/plain", decodedTo, dto.getDate()), "text/plain");
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), "text/html", decodedTo, dto.getDate()), "text/html");
                    }
                    if (dto.getMessageId() != null) {
                        String refs = dto.getReferences() != null ? dto.getReferences() + " " + dto.getMessageId() : dto.getMessageId();
                        dlg.setThreadingHeaders(dto.getMessageId(), refs);
                    }
                } else {
                    Message origM = emlMsg.getMessage();
                    MailboxSetup ms = EmailUtils.getMailboxSetup(origM);
                    if (ms != null) {
                        dlg.setFrom(ms);
                    }

                    Message m = origM.reply(true);

                    try {
                        Address[] to = m.getRecipients(Message.RecipientType.TO);
                        if (ms != null) {
                            to = EmailUtils.filterOut(to, ms.getEmailAddress());
                        }
                        String toString = EmailUtils.getAddressesAsList(to);

                        Address[] cc = m.getRecipients(Message.RecipientType.CC);
                        if (ms != null) {
                            cc = EmailUtils.filterOut(cc, ms.getEmailAddress());
                        }
                        String ccString = EmailUtils.getAddressesAsList(cc);

                        Address[] bcc = m.getRecipients(Message.RecipientType.BCC);
                        if (ms != null) {
                            bcc = EmailUtils.filterOut(bcc, ms.getEmailAddress());
                        }
                        String bccString = EmailUtils.getAddressesAsList(bcc);

                        dlg.setTo(toString);
                        dlg.setCC(ccString);
                        dlg.setBCC(bccString);

                    } catch (Throwable t) {
                        log.error(t);
                        dlg.setTo(m.getRecipients(Message.RecipientType.TO)[0].toString());
                    }

                    String subject = m.getSubject();
                    if (subject == null) {
                        subject = "";
                    }
                    if (!subject.startsWith("Re: ")) {
                        subject = "Re: " + subject;
                    }
                    dlg.setSubject(subject);

                    String decodedTo = origM.getFrom()[0].toString();
                    try {
                        decodedTo = MimeUtility.decodeText(origM.getFrom()[0].toString());
                    } catch (Throwable t) {
                        log.error(t);
                    }
                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), ContentTypes.TEXT_PLAIN, decodedTo, origM.getSentDate()), ContentTypes.TEXT_PLAIN);
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_HTML, decodedTo, origM.getSentDate()), ContentTypes.TEXT_HTML);
                    } else {
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_PLAIN, decodedTo, origM.getSentDate()), ContentTypes.TEXT_PLAIN);
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), ContentTypes.TEXT_HTML, decodedTo, origM.getSentDate()), ContentTypes.TEXT_HTML);
                    }
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        } else {
            try {
                MailboxSetup ms = EmailUtils.getMailboxSetup(outlookMsg);
                if (ms != null) {
                    dlg.setFrom(ms);
                }

                String subject = outlookMsg.getSubject();
                if (subject == null) {
                    subject = "";
                }
                if (!subject.startsWith("Re: ")) {
                    subject = "Re: " + subject;
                }
                dlg.setSubject(subject);

                String decodedTo = outlookMsg.getFromEmail();
                String contentType = content.getContentType();
                dlg.setContentType(contentType);
                if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                    dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), ContentTypes.TEXT_PLAIN, decodedTo, outlookMsg.getDate()), ContentTypes.TEXT_PLAIN);
                    dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_HTML, decodedTo, outlookMsg.getDate()), ContentTypes.TEXT_HTML);
                } else {
                    dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_PLAIN, decodedTo, outlookMsg.getDate()), ContentTypes.TEXT_PLAIN);
                    dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), ContentTypes.TEXT_HTML, decodedTo, outlookMsg.getDate()), ContentTypes.TEXT_HTML);
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        }

        showFrame(dlg);
        return dlg;
    }

    public static SendEmailFrame forward(MessageContainer emlMsg, OutlookMessage outlookMsg, MailContentUI content, ArchiveFileBean contextArchiveFile, CaseFolder contextFolder) {
        SendEmailFrame dlg = new SendEmailFrame(true);
        dlg.setArchiveFile(contextArchiveFile, contextFolder);
        setPartiesToSendDialog(dlg, contextArchiveFile, false);

        if (emlMsg != null) {
            try {
                if (emlMsg.isServerBased()) {
                    com.jdimension.jlawyer.services.MailMessageDTO dto = emlMsg.getMessageDTO();
                    for (MailboxSetup mbx : UserSettings.getInstance().getMailboxes(UserSettings.getInstance().getCurrentUser().getPrincipalId())) {
                        if (mbx.getId().equals(emlMsg.getMailboxId())) {
                            dlg.setFrom(mbx);
                            break;
                        }
                    }
                    String subject = dto.getSubject() != null ? dto.getSubject() : "";
                    if (!subject.startsWith("Fw: ")) {
                        subject = "Fw: " + subject;
                    }
                    dlg.setSubject(subject);
                    String decodedFrom = dto.getFrom() != null ? dto.getFrom() : "";
                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith("text/html")) {
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), "text/plain", decodedFrom, dto.getDate()), "text/plain");
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), "text/html", decodedFrom, dto.getDate()), "text/html");
                    } else {
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), "text/plain", decodedFrom, dto.getDate()), "text/plain");
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), "text/html", decodedFrom, dto.getDate()), "text/html");
                    }
                    try {
                        ClientSettings settings = ClientSettings.getInstance();
                        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
                        List<com.jdimension.jlawyer.services.MailAttachmentDTO> atts = locator.lookupEmailServiceRemote().getAttachments(emlMsg.getMailboxId(), emlMsg.getMessageRef());
                        if (atts != null) {
                            for (com.jdimension.jlawyer.services.MailAttachmentDTO att : atts) {
                                if (!att.isInline() && att.getContent() != null) {
                                    String attachmentUrl = FileUtils.createTempFile(att.getName(), att.getContent());
                                    new File(attachmentUrl).deleteOnExit();
                                    dlg.addAttachment(attachmentUrl, "");
                                }
                            }
                        }
                    } catch (Throwable t) {
                        log.error("Error forwarding attachments", t);
                    }
                    if (dto.getMessageId() != null) {
                        dlg.setThreadingHeaders(dto.getMessageId(), null);
                    }
                } else {
                    Message m = emlMsg.getMessage();
                    MailboxSetup ms = EmailUtils.getMailboxSetup(m);
                    if (ms != null) {
                        dlg.setFrom(ms);
                    }
                    Address from = m.getFrom()[0];

                    String subject = m.getSubject();
                    if (subject == null) {
                        subject = "";
                    }
                    if (!subject.startsWith("Fw: ")) {
                        subject = "Fw: " + subject;
                    }
                    dlg.setSubject(subject);

                    String decodedFrom = from.toString();
                    try {
                        decodedFrom = MimeUtility.decodeText(from.toString());
                    } catch (Throwable t) {
                        log.error(t);
                    }
                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), ContentTypes.TEXT_PLAIN, decodedFrom, m.getSentDate()), ContentTypes.TEXT_PLAIN);
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_HTML, decodedFrom, m.getSentDate()), ContentTypes.TEXT_HTML);
                    } else {
                        dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_PLAIN, decodedFrom, m.getSentDate()), ContentTypes.TEXT_PLAIN);
                        dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), ContentTypes.TEXT_HTML, decodedFrom, m.getSentDate()), ContentTypes.TEXT_HTML);
                    }

                    try {
                        if (m.getFolder() != null) {
                            if (!m.getFolder().isOpen()) {
                                m.getFolder().open(Folder.READ_WRITE);
                            }
                        }
                        ArrayList<String> attachmentNames = EmailUtils.getAttachmentNames(m.getContent());
                        for (String attName : attachmentNames) {
                            byte[] data = EmailUtils.getAttachmentBytes(attName, emlMsg);
                            if (data != null) {
                                String attachmentUrl = FileUtils.createTempFile(attName, data);
                                new File(attachmentUrl).deleteOnExit();
                                dlg.addAttachment(attachmentUrl, "");
                            }
                        }

                        if (m.getFolder() != null && !EmailUtils.isInbox(m.getFolder())) {
                            if (m.getFolder().isOpen()) {
                                EmailUtils.closeIfIMAP(m.getFolder());
                            }
                        }

                    } catch (Throwable t) {
                        log.error("Error forwarding attachments", t);
                    }
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        } else {
            try {
                MailboxSetup ms = EmailUtils.getMailboxSetup(outlookMsg);
                if (ms != null) {
                    dlg.setFrom(ms);
                }
                String from = outlookMsg.getFromEmail();

                String subject = outlookMsg.getSubject();
                if (subject == null) {
                    subject = "";
                }
                if (!subject.startsWith("Fw: ")) {
                    subject = "Fw: " + subject;
                }
                dlg.setSubject(subject);

                String contentType = content.getContentType();
                dlg.setContentType(contentType);
                if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                    dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.html2Text(content.getBody()), ContentTypes.TEXT_PLAIN, from, outlookMsg.getDate()), ContentTypes.TEXT_PLAIN);
                    dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_HTML, from, outlookMsg.getDate()), ContentTypes.TEXT_HTML);
                } else {
                    dlg.setBody("", EmailUtils.getQuotedBody(content.getBody(), ContentTypes.TEXT_PLAIN, from, outlookMsg.getDate()), ContentTypes.TEXT_PLAIN);
                    dlg.setBody("", EmailUtils.getQuotedBody(EmailUtils.text2Html(content.getBody()), ContentTypes.TEXT_HTML, from, outlookMsg.getDate()), ContentTypes.TEXT_HTML);
                }

                try {
                    List<OutlookFileAttachment> attachments = outlookMsg.fetchTrueAttachments();
                    for (OutlookFileAttachment ofa : attachments) {
                        byte[] data = ofa.getData();
                        if (data != null) {
                            String fileName = ofa.getFilename();
                            if (StringUtils.isEmpty(fileName)) {
                                fileName = ofa.getLongFilename();
                            }
                            String attachmentUrl = FileUtils.createTempFile(fileName, data);
                            new File(attachmentUrl).deleteOnExit();
                            dlg.addAttachment(attachmentUrl, "");
                        }
                    }

                } catch (Throwable t) {
                    log.error("Error forwarding attachments", t);
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        }

        showFrame(dlg);
        return dlg;
    }

    public static SendEmailFrame editDraft(MessageContainer emlMsg, OutlookMessage outlookMsg, MailContentUI content, ArchiveFileBean contextArchiveFile, CaseFolder contextFolder) {
        SendEmailFrame dlg = new SendEmailFrame(true);
        dlg.setArchiveFile(contextArchiveFile, contextFolder);
        setPartiesToSendDialog(dlg, contextArchiveFile, false);

        if (emlMsg != null) {
            try {
                if (emlMsg.isServerBased()) {
                    com.jdimension.jlawyer.services.MailMessageDTO dto = emlMsg.getMessageDTO();
                    for (MailboxSetup mbx : UserSettings.getInstance().getMailboxes(UserSettings.getInstance().getCurrentUser().getPrincipalId())) {
                        if (mbx.getId().equals(emlMsg.getMailboxId())) {
                            dlg.setFrom(mbx);
                            break;
                        }
                    }
                    dlg.setSubject(dto.getSubject() != null ? dto.getSubject() : "");
                    if (dto.getTo() != null) {
                        dlg.setTo(String.join(", ", dto.getTo()));
                    }
                    if (dto.getCc() != null) {
                        dlg.setCC(String.join(", ", dto.getCc()));
                    }

                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                        dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                    } else {
                        dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                    }
                    dlg.setBody("", content.getBody(), ContentTypes.TEXT_HTML, false);

                    try {
                        ClientSettings settings = ClientSettings.getInstance();
                        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
                        List<com.jdimension.jlawyer.services.MailAttachmentDTO> atts = locator.lookupEmailServiceRemote().getAttachments(emlMsg.getMailboxId(), emlMsg.getMessageRef());
                        if (atts != null) {
                            for (com.jdimension.jlawyer.services.MailAttachmentDTO att : atts) {
                                if (!att.isInline() && att.getContent() != null) {
                                    String attachmentUrl = FileUtils.createTempFile(att.getName(), att.getContent());
                                    new File(attachmentUrl).deleteOnExit();
                                    dlg.addAttachment(attachmentUrl, "");
                                }
                            }
                        }
                    } catch (Throwable t) {
                        log.error("Error attaching draft attachments", t);
                    }
                } else {
                    Message m = emlMsg.getMessage();
                    MailboxSetup ms = EmailUtils.getMailboxSetup(m);
                    if (ms != null) {
                        dlg.setFrom(ms);
                    }

                    String subject = m.getSubject();
                    if (subject == null) {
                        subject = "";
                    }
                    dlg.setSubject(subject);

                    try {
                        Address[] to = m.getRecipients(Message.RecipientType.TO);
                        String toString = EmailUtils.getAddressesAsList(to);

                        Address[] cc = m.getRecipients(Message.RecipientType.CC);
                        String ccString = EmailUtils.getAddressesAsList(cc);

                        Address[] bcc = m.getRecipients(Message.RecipientType.BCC);
                        String bccString = EmailUtils.getAddressesAsList(bcc);

                        dlg.setTo(toString);
                        dlg.setCC(ccString);
                        dlg.setBCC(bccString);

                    } catch (Throwable t) {
                        log.error(t);
                        dlg.setTo(m.getRecipients(Message.RecipientType.TO)[0].toString());
                    }

                    String contentType = content.getContentType();
                    dlg.setContentType(contentType);
                    if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                        dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                    } else {
                        dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                    }
                    dlg.setBody("", content.getBody(), ContentTypes.TEXT_HTML, false);

                    try {
                        if (m.getFolder() != null) {
                            if (!m.getFolder().isOpen()) {
                                m.getFolder().open(Folder.READ_WRITE);
                            }
                        }
                        ArrayList<String> attachmentNames = EmailUtils.getAttachmentNames(m.getContent());
                        for (String attName : attachmentNames) {
                            byte[] data = EmailUtils.getAttachmentBytes(attName, emlMsg);
                            if (data != null) {
                                String attachmentUrl = FileUtils.createTempFile(attName, data);
                                new File(attachmentUrl).deleteOnExit();
                                dlg.addAttachment(attachmentUrl, "");
                            }
                        }

                        if (m.getFolder() != null && !EmailUtils.isInbox(m.getFolder())) {
                            if (m.getFolder().isOpen()) {
                                EmailUtils.closeIfIMAP(m.getFolder());
                            }
                        }

                    } catch (Throwable t) {
                        log.error("Error forwarding attachments", t);
                    }
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        } else {
            try {
                MailboxSetup ms = EmailUtils.getMailboxSetup(outlookMsg);
                if (ms != null) {
                    dlg.setFrom(ms);
                }

                try {
                    String toString = EmailUtils.getAddressesAsList(outlookMsg.getToRecipients());
                    String ccString = EmailUtils.getAddressesAsList(outlookMsg.getCcRecipients());
                    String bccString = EmailUtils.getAddressesAsList(outlookMsg.getBccRecipients());

                    dlg.setTo(toString);
                    dlg.setCC(ccString);
                    dlg.setBCC(bccString);

                } catch (Throwable t) {
                    log.error(t);
                }

                String subject = outlookMsg.getSubject();
                if (subject == null) {
                    subject = "";
                }
                dlg.setSubject(subject);

                String contentType = content.getContentType();
                dlg.setContentType(contentType);
                if (contentType.toLowerCase().startsWith(ContentTypes.TEXT_HTML)) {
                    dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                } else {
                    dlg.setBody("", content.getBody(), ContentTypes.TEXT_PLAIN, false);
                }
                dlg.setBody("", content.getBody(), ContentTypes.TEXT_HTML, false);

                try {
                    List<OutlookFileAttachment> attachments = outlookMsg.fetchTrueAttachments();
                    for (OutlookFileAttachment ofa : attachments) {
                        byte[] data = ofa.getData();
                        if (data != null) {
                            String fileName = ofa.getFilename();
                            if (StringUtils.isEmpty(fileName)) {
                                fileName = ofa.getLongFilename();
                            }
                            String attachmentUrl = FileUtils.createTempFile(fileName, data);
                            new File(attachmentUrl).deleteOnExit();
                            dlg.addAttachment(attachmentUrl, "");
                        }
                    }

                } catch (Throwable t) {
                    log.error("Error forwarding attachments", t);
                }

            } catch (Exception ex) {
                log.error(ex);
            }
        }

        showFrame(dlg);
        return dlg;
    }

    private static void showFrame(SendEmailFrame dlg) {
        FrameUtils.centerFrame(dlg, null);
        EditorsRegistry.getInstance().registerFrame(dlg);
        dlg.setVisible(true);
    }

    private static void setPartiesToSendDialog(SendEmailFrame dlg, ArchiveFileBean contextArchiveFile, boolean evaluateTemplates) {
        if (contextArchiveFile != null) {
            try {
                ClientSettings settings = ClientSettings.getInstance();
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
                ArchiveFileServiceRemote afs = locator.lookupArchiveFileServiceRemote();
                List<ArchiveFileAddressesBean> list = afs.getInvolvementDetailsForCase(contextArchiveFile.getId(), false);
                for (ArchiveFileAddressesBean aab : list) {
                    dlg.addParty(aab, evaluateTemplates);
                }
            } catch (Throwable t) {
                log.error("Unable to add recipient candidates", t);
            }
        }
    }
}
