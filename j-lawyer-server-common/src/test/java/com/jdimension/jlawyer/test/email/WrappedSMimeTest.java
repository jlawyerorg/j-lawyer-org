package com.jdimension.jlawyer.test.email;

import com.jdimension.jlawyer.email.AttachmentInfo;
import com.jdimension.jlawyer.email.CommonMailUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.InternetHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Some sending gateways wrap an entire signed message as a single body part
 * flagged with Content-Disposition: attachment and a file name of smime.p7m,
 * while its Content-Type stays multipart/signed. The walkers used to treat such
 * a part as a leaf attachment, hiding the real attachments inside it.
 */
public class WrappedSMimeTest {

    private MimeMessage load(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("fixture not found: " + resource, is);
            return new MimeMessage(Session.getInstance(new Properties()), is);
        }
    }

    @Test
    public void attachmentsInsideWrappedPartAreListed() throws Exception {
        MimeMessage msg = load("wrapped-smime.eml");

        ArrayList<AttachmentInfo> infos = CommonMailUtils.getAttachmentInfo(msg.getContent());
        ArrayList<String> names = new ArrayList<>();
        for (AttachmentInfo i : infos) {
            names.add(i.getFileName());
        }

        assertTrue("expected the wrapped PDF to be listed, got " + names,
                names.contains("ATTACH-001.PDF"));
        assertTrue("the smime.p7m container must not be listed as an attachment, got " + names,
                !names.contains("smime.p7m"));
    }

    @Test
    public void attachmentBytesInsideWrappedPartAreRetrievable() throws Exception {
        MimeMessage msg = load("wrapped-smime.eml");

        byte[] data = CommonMailUtils.getAttachmentBytes("ATTACH-001.PDF", msg);

        assertNotNull("wrapped attachment could not be retrieved", data);
        assertTrue("retrieved content is not the expected PDF",
                new String(data).startsWith("%PDF-"));
    }

    @Test
    public void plainAttachmentIsUnaffected() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        MimeMultipart mp = new MimeMultipart();

        MimeBodyPart body = new MimeBodyPart();
        body.setText("hello");
        mp.addBodyPart(body);

        MimeBodyPart att = new MimeBodyPart();
        att.setText("content of the attachment");
        att.setFileName("report.txt");
        att.setDisposition(javax.mail.Part.ATTACHMENT);
        mp.addBodyPart(att);

        msg.setContent(mp);
        msg.saveChanges();

        ArrayList<AttachmentInfo> infos = CommonMailUtils.getAttachmentInfo(msg.getContent());
        assertEquals(1, infos.size());
        assertEquals("report.txt", infos.get(0).getFileName());

        byte[] data = CommonMailUtils.getAttachmentBytes("report.txt", msg);
        assertNotNull(data);
        assertEquals("content of the attachment", new String(data));
    }

    @Test
    public void forwardedMessageWithoutFileNameIsUnaffected() throws Exception {
        // an attachment without a file name but of type message/rfc822 gets a
        // synthetic name - that path must keep working
        MimeMessage inner = new MimeMessage(Session.getInstance(new Properties()));
        inner.setSubject("forwarded");
        inner.setText("forwarded body");
        inner.saveChanges();
        ByteArrayOutputStream innerOut = new ByteArrayOutputStream();
        inner.writeTo(innerOut);

        InternetHeaders ih = new InternetHeaders();
        ih.addHeader("Content-Type", "message/rfc822");
        ih.addHeader("Content-Disposition", "attachment");
        MimeBodyPart att = new MimeBodyPart(ih, innerOut.toByteArray());

        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart body = new MimeBodyPart();
        body.setText("see attached");
        mp.addBodyPart(body);
        mp.addBodyPart(att);

        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        msg.setContent(mp);
        msg.saveChanges();

        ArrayList<AttachmentInfo> infos = CommonMailUtils.getAttachmentInfo(msg.getContent());
        assertEquals(1, infos.size());
        assertTrue("expected a synthesized .eml name, got " + infos.get(0).getFileName(),
                infos.get(0).getFileName().startsWith("Nachricht_")
                && infos.get(0).getFileName().endsWith(".eml"));
    }
}
