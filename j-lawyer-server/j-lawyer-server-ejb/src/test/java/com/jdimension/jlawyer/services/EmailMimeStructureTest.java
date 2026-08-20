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
package com.jdimension.jlawyer.services;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.junit.Assert;
import org.junit.Test;

public class EmailMimeStructureTest {

    @Test
    public void createsRelatedMultipartForHtmlWithInlineImage() throws Exception {
        MailAttachmentDTO inlineImage = createAttachment(
                "logo.jpg", "image/jpeg", true, "signature-logo@jlawyer");

        MimeMultipart multipart = saveMultipart(EmailService.buildMimeMultipart(
                "<p>Text</p><img src=\"cid:signature-logo@jlawyer\">",
                "text/html",
                Collections.singletonList(inlineImage)));

        Assert.assertTrue(multipart.getContentType().startsWith("multipart/related"));
        Assert.assertTrue(multipart.getContentType().contains("type=\"text/html\""));
        Assert.assertEquals(2, multipart.getCount());
        Assert.assertTrue(multipart.getBodyPart(0).getContentType().startsWith("text/html"));

        MimeBodyPart imagePart = (MimeBodyPart) multipart.getBodyPart(1);
        Assert.assertEquals(Part.INLINE, imagePart.getDisposition());
        Assert.assertEquals("<signature-logo@jlawyer>", imagePart.getContentID());
        Assert.assertEquals("logo.jpg", imagePart.getFileName());
    }

    @Test
    public void nestsRelatedMultipartInsideMixedForRegularAttachments() throws Exception {
        MailAttachmentDTO inlineImage = createAttachment(
                "logo.jpg", "image/jpeg", true, "signature-logo@jlawyer");
        MailAttachmentDTO regularAttachment = createAttachment(
                "agreement.pdf", "application/pdf", false, null);

        MimeMultipart multipart = saveMultipart(EmailService.buildMimeMultipart(
                "<p>Text</p><img src=\"cid:signature-logo@jlawyer\">",
                "text/html",
                Arrays.asList(inlineImage, regularAttachment)));

        Assert.assertTrue(multipart.getContentType().startsWith("multipart/mixed"));
        Assert.assertEquals(2, multipart.getCount());
        Assert.assertTrue(multipart.getBodyPart(0).getContent() instanceof MimeMultipart);

        MimeMultipart related = (MimeMultipart) multipart.getBodyPart(0).getContent();
        Assert.assertTrue(related.getContentType().startsWith("multipart/related"));
        Assert.assertTrue(related.getContentType().contains("type=\"text/html\""));
        Assert.assertEquals(2, related.getCount());

        MimeBodyPart attachmentPart = (MimeBodyPart) multipart.getBodyPart(1);
        Assert.assertEquals(Part.ATTACHMENT, attachmentPart.getDisposition());
        Assert.assertEquals("agreement.pdf", attachmentPart.getFileName());
    }

    private MimeMultipart saveMultipart(MimeMultipart multipart) throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setContent(multipart);
        message.saveChanges();
        return (MimeMultipart) message.getContent();
    }

    private MailAttachmentDTO createAttachment(String name, String contentType,
            boolean inline, String contentId) {
        MailAttachmentDTO attachment = new MailAttachmentDTO();
        attachment.setName(name);
        attachment.setContentType(contentType);
        attachment.setInline(inline);
        attachment.setContentId(contentId);
        attachment.setContent("test-content".getBytes(StandardCharsets.UTF_8));
        return attachment;
    }
}
