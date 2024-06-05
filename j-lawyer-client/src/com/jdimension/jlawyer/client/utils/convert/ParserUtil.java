package com.jdimension.jlawyer.client.utils.convert;

import static com.jdimension.jlawyer.client.utils.convert.Helper.DEFAULT_HTML_NAME;
import static com.jdimension.jlawyer.client.utils.convert.Helper.DEFAULT_PDF_NAME;
import static com.jdimension.jlawyer.client.utils.convert.Helper.EMAIL_HEADER_ID;
import static com.jdimension.jlawyer.client.utils.convert.Helper.HEADER_TEMPLATE_CONTAINER;
import static com.jdimension.jlawyer.client.utils.convert.Helper.TEMP_DIR;
import static com.jdimension.jlawyer.client.utils.convert.Helper.UNKNOWN;
import static com.jdimension.jlawyer.client.utils.convert.Helper.readTemplate;
import static com.jdimension.jlawyer.client.utils.convert.Helper.writeToFile;
import com.lowagie.text.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.simplejavamail.api.email.AttachmentResource;
//import org.simplejavamail.converter.EmailConverter;

import javax.mail.MessagingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Convert eml to HTML and PDF
 * <p>
 * If you want you can download the email attachments to and add header to the
 * generated pdf file (for example: sender, recipients, subject)
 *
 * @author nickdale
 * @version 1.0.2
 */
public class ParserUtil {

    private final MimeMessageParser messageParser;
    private final boolean downloadAttachments;
    private final StringBuilder bodyBuilder;
    private final ConvertedFile convertedFile;
    private final boolean addEmailHeaders;

    public ParserUtil(byte[] email) throws Exception {
        this(email, Boolean.FALSE, Boolean.FALSE);
    }

    public ParserUtil(byte[] email, boolean addEmailHeadersToPdf) throws Exception {
        this(email, Boolean.FALSE, addEmailHeadersToPdf);
    }

    public ParserUtil(byte[] email, boolean downloadAttachments, boolean addEmailHeadersToPdf) throws Exception {
        this(MimeMessageParser.instance(email), downloadAttachments, addEmailHeadersToPdf);
    }

    public ParserUtil(String emailFilePath) throws Exception {
        this(emailFilePath, Boolean.FALSE, Boolean.FALSE);
    }

    public ParserUtil(String emailFilePath, boolean addEmailHeadersToPdf) throws Exception {
        this(emailFilePath, Boolean.FALSE, addEmailHeadersToPdf);
    }

    public ParserUtil(String emailFilePath, boolean downloadAttachments, boolean addEmailHeadersToPdf) throws Exception {
        this(MimeMessageParser.instance(emailFilePath), downloadAttachments, addEmailHeadersToPdf);
    }

    public ParserUtil(InputStream emailInputStream) throws Exception {
        this(emailInputStream, Boolean.FALSE, Boolean.FALSE);
    }

    public ParserUtil(InputStream emailInputStream, boolean addEmailHeadersToPdf) throws Exception {
        this(emailInputStream, Boolean.FALSE, addEmailHeadersToPdf);
    }

    public ParserUtil(InputStream emailInputStream, boolean downloadAttachments, boolean addEmailHeadersToPdf) throws Exception {
        this(MimeMessageParser.instance(emailInputStream), downloadAttachments, addEmailHeadersToPdf);
    }

    private ParserUtil(MimeMessageParser messageParser, boolean downloadAttachments, boolean addEmailHeadersToPdf) {
        this.messageParser = messageParser;
        this.downloadAttachments = downloadAttachments;
        this.addEmailHeaders = addEmailHeadersToPdf;
        this.bodyBuilder = new StringBuilder();
        this.convertedFile = new ConvertedFile();
    }

    /**
     * This method is using the default directory and file names.
     *
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws DocumentException
     */
//    public ConvertedFile createFile() throws IOException, MessagingException, DocumentException {
//        return createFile(null, null, null);
//    }
    /**
     * @param tempDir your directory
     * @param htmlName generated html file name
     * @param pdfName generated pdf file name
     * @return instance of ConvertedFile
     * @throws IOException
     * @throws MessagingException
     * @throws MimeTypeException
     * @throws DocumentException
     * @see ConvertedFile
     */
//    public ConvertedFile createFile(Path tempDir, String htmlName, String pdfName) throws IOException, MessagingException, DocumentException {
//        if (isNull(tempDir)) {
//            tempDir = Files.createTempDirectory(TEMP_DIR);
//        }
//        if (isBlank(htmlName)) {
//            htmlName = DEFAULT_HTML_NAME;
//        }
//        if (isBlank(pdfName)) {
//            pdfName = DEFAULT_PDF_NAME;
//        }
//        File emailFile = Files.createFile(tempDir.resolve(htmlName)).toFile();
//        String htmlBody = messageParser.getMimeMessageObject().getHtmlBody();
//
//        writeToFile(htmlBody.getBytes(ofNullable(messageParser.getMimeMessageObject().getCharset()).orElse(UTF_8)), emailFile);
//        convertedFile.setEmailInHtml(emailFile);
//
//        File pdfFile = Files.createFile(tempDir.resolve(pdfName)).toFile();
//        convertToPdf(htmlBody, pdfFile);
//        convertedFile.setPdf(pdfFile);
//        if (downloadAttachments) {
//            attachments(messageParser, tempDir);
//        }
//        return convertedFile;
//    }
    public String convertToFullHtml(List<String> attachments, List<String> containedAttachments) throws Exception {
        String htmlBody = messageParser.getMimeMessageObject().getHtmlBody();
        final Document document = Jsoup.parse(htmlBody);
        //document.outputSettings() .syntax(Document.OutputSettings.Syntax.xml);
        if (addEmailHeaders) {
            messageParser.getHeaderData().forEach(this::append);
            document.body().prepend("<table id=\"header_fields\"\n"
                    + "       style=\"background: white; margin: auto; border: 1px solid #DDD; border-radius: 3px; padding: 8px; width: 90%; box-sizing: border-box;\">\n"
                    + "</table>\n"
                    + "");
            document.getElementById(EMAIL_HEADER_ID).append(bodyBuilder.toString());
        }
        
        if(attachments!=null && !attachments.isEmpty()) {
            document.body().append("<p>&nbsp;</p><b>Anlagen:</b><br/><table id=\"header_fields\"\n"
                    + "       style=\"background: white; margin: auto; border: 1px solid #DDD; border-radius: 3px; padding: 8px; width: 90%; box-sizing: border-box;\">\n"
                    + "</table>\n"
                    + "");
            for(String att: attachments) {
                document.body().append("<tr><td>");
                document.body().append(att);
                document.body().append("</td</tr>\n");
            }
        }
        document.body().append("</table>\n");
        
        if(containedAttachments!=null) {
            
        }
        
        
        //renderer.setDocumentFromString(document.html());
        return document.html();
    }

//    private void convertToPdf(final String htmlBody, final File pdfFile) throws IOException, DocumentException, MessagingException {
//        ITextRenderer renderer = new ITextRenderer(20f * 4.5f / 3f, 20);
//        OutputStream outputStream = new FileOutputStream(pdfFile);
//
//        final Document document = Jsoup.parse(htmlBody);
//        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
//        if (addEmailHeaders) {
//            messageParser.getHeaderData().forEach(this::append);
//            document.body().prepend(readTemplate(HEADER_TEMPLATE_CONTAINER));
//            document.getElementById(EMAIL_HEADER_ID).append(bodyBuilder.toString());
//        }
//        renderer.setDocumentFromString(document.html());
//        renderer.layout();
//        renderer.createPDF(outputStream);
//    }
    private void append(final HeaderPart headerPart) {
        if (isNotBlank(headerPart.getData())) {
            bodyBuilder.append(String.format(headerPart.getTemplate(), headerPart.getName(), headerPart.getData()));
        }
    }

//    private void attachments(final MimeMessageParser messageParser, final Path dir) throws MimeTypeException, IOException {
//        List<AttachmentResource> attachments = EmailConverter.mimeMessageToEmail(messageParser.getMimeMessage()).getAttachments();
//        if (isEmpty(attachments)) return;
//        for (AttachmentResource resource : attachments) {
//            String attachmentFilename = resource.getDataSource().getName();
//            if (isBlank(attachmentFilename)) {
//                attachmentFilename = UNKNOWN + MimeTypes.getDefaultMimeTypes().forName(resource.getDataSource().getContentType()).getExtension();
//            }
//            File file = Files.createFile(dir.resolve(attachmentFilename)).toFile();
//            writeToFile(resource.getDataSource().getInputStream(), file);
//            this.convertedFile.addAttachment(file);
//        }
//    }
}
