package com.jdimension.jlawyer.client.utils.convert;

import static com.jdimension.jlawyer.client.utils.convert.Helper.DATE_FORMATTER;
import static com.jdimension.jlawyer.client.utils.convert.Helper.HEADER_PARAM_DATE;
import static com.jdimension.jlawyer.client.utils.convert.Helper.HEADER_PARAM_FROM;
import static com.jdimension.jlawyer.client.utils.convert.Helper.HEADER_PARAM_SUBJECT;
import static com.jdimension.jlawyer.client.utils.convert.Helper.HEADER_PARAM_TO;
import static com.jdimension.jlawyer.client.utils.convert.Helper.UNKNOWN;
//import org.simplejavamail.converter.EmailConverter;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import javax.mail.Address;
import static javax.print.DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8;

class MimeMessageParser {

    public static final String FILE_EXTENSION_MSG = ".msg";
    private final MimeMessage mimeMessage;
    private final MimeMessageObject mimeMessageObject;

    private MimeMessageParser(InputStream inputStream) throws Exception {
        this.mimeMessage = new MimeMessage(null, inputStream);
        this.mimeMessageObject = new MimeMessageObject(new ContentType(TEXT_PLAIN_UTF_8.toString()), mimeMessage);
    }

    public static MimeMessageParser instance(InputStream emailInputStream) throws Exception {
        return new MimeMessageParser(emailInputStream);
    }

    public static MimeMessageParser instance(String emailFilePath) throws Exception {
//        if (emailFilePath.toLowerCase().endsWith(FILE_EXTENSION_MSG)) {
//            String emlString = EmailConverter.outlookMsgToEML(new FileInputStream(emailFilePath));
//            return instance(emlString.getBytes(UTF_8));
//        }
        return new MimeMessageParser(new FileInputStream(emailFilePath));
    }

    public static MimeMessageParser instance(byte[] email) throws Exception {
        return new MimeMessageParser(new ByteArrayInputStream(email));
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public MimeMessageObject getMimeMessageObject() {
        return mimeMessageObject;
    }

    public void appendToHtmlBody(String str) {
        this.mimeMessageObject.appendToHtmlBody(str);
    }

    public List<HeaderPart> getHeaderData() throws MessagingException {
        return Arrays.asList(getSender(), getSubject(), getRecipients(), getSentDateInString());
    }

    private HeaderPart getSender() throws MessagingException {
        InternetAddress[] fromAddresses = (InternetAddress[]) mimeMessage.getFrom();
        String sender = nonNull(fromAddresses) ?
                getAddressString(fromAddresses)
                : ((InternetAddress) mimeMessage.getSender()).getAddress();

        return HeaderPart.builder()
                .name(HEADER_PARAM_FROM)
                .data(sender)
                .build();
    }

    private String getAddressString(InternetAddress[] addresses) {
        return Arrays.stream(addresses).map(InternetAddress::getAddress).collect(Collectors.joining(","));
    }

    private HeaderPart getSubject() throws MessagingException {
        return HeaderPart.builder()
                .name(HEADER_PARAM_SUBJECT)
                .subject(mimeMessage.getSubject())
                .build();
    }

    private HeaderPart getRecipients() throws MessagingException {
        Address[] allAdrs=mimeMessage.getAllRecipients();
        InternetAddress[] adrs=new InternetAddress[allAdrs.length];
        for(int i=0;i<allAdrs.length;i++) {
            adrs[i]=(InternetAddress)allAdrs[i];
        }
        
        return HeaderPart.builder()
                .name(HEADER_PARAM_TO)
                .data(getAddressString(adrs))
                //.data(getAddressString((Address[]) mimeMessage.getAllRecipients()))
                .build();
    }

    private HeaderPart getSentDateInString() throws MessagingException {
        return HeaderPart.builder()
                .name(HEADER_PARAM_DATE)
                .data(ofNullable(mimeMessage.getSentDate()).map(DATE_FORMATTER::format).orElse(UNKNOWN))
                .build();
    }

}
