package com.jdimension.jlawyer.client.utils.convert;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;

public class Helper {

    public static final String HTML_WRAPPER_TEMPLATE = "<!DOCTYPE html><html><head><style>body{font-size: 0.5cm;}</style><meta charset=\"%s\"><title>title</title></head><body>%s</body></html>";
    public static final Pattern HTML_META_CHARSET_REGEX = Pattern.compile("(<meta(?!\\s*(?:name|value)\\s*=)[^>]*?charset\\s*=[\\s\"']*)([^\\s\"'/>]*)", Pattern.DOTALL);
    public static final Pattern IMG_CID_REGEX = Pattern.compile("cid:(.*?)\"", Pattern.DOTALL);
    public static final Pattern IMG_CID_PLAIN_REGEX = Pattern.compile("\\[cid:(.*?)\\]", Pattern.DOTALL);
    public static final DateFormat DATE_FORMATTER = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.GERMAN);

    public static final String CHARSET = "charset";
    public static final String MULTIPART_TYPE = "multipart/*";
    public static final String IMAGE_TYPE = "image/*";
    public static final String CONTENT_ID = "Content-Id";
    public static final String HEADER_PARAM_FROM = "Von";
    public static final String HEADER_PARAM_SUBJECT = "Betreff";
    public static final String HEADER_PARAM_TO = "An";
    public static final String HEADER_PARAM_DATE = "Datum";
    public static final String UNKNOWN = "unknown_";

    public static final String TEMP_DIR = "temp_eml_converter_" + now();
    public static final String DEFAULT_HTML_NAME = String.format("html_mail_%s.html", now().toString());
    public static final String DEFAULT_PDF_NAME = String.format("pdf_mail_%s.pdf", now().toString());

    public static final String HEADER_TEMPLATE_CONTAINER = "/com/jdimension/jlawyer/client/utils/convert/header_template_container.html";
    public static final String EMAIL_HEADER_ID = "header_fields";
    private static final String HEADER_TEMPLATE = "/com/jdimension/jlawyer/client/utils/convert/header_template.html";
    public static String templateHeaderBody;

    static {
        //try {
            //templateHeaderBody = readTemplate(HEADER_TEMPLATE);
            templateHeaderBody="<tr>\n" +
"    <td class=\"header-name\" style=\"padding-right: 5px; color: #9E9E9E; text-align: right; vertical-align: top;\">%s</td>\n" +
"    <td class=\"header-value\">%s</td>\n" +
"</tr>";
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            templateHeaderBody = "";
//        }
    }

    /**
     * @param templateName Name of the template file
     * @return text in UTF-8
     * @throws IOException Template file is not found
     */
    public static String readTemplate(String templateName) throws IOException {
        URL headerResource = ParserUtil.class.getClassLoader().getResource(templateName);
        if (isNull(headerResource)) {
            throw new FileNotFoundException("Header template is not found");
        }
        return IOUtils.toString(headerResource, UTF_8);
    }

    /**
     * @param htmlString Html string
     * @param regex      which elements want to find
     * @param callback   replacement logic
     * @return modified String
     * @throws Exception
     */
    public static String replace(String htmlString, Pattern regex, Replacer callback) throws Exception {
        StringBuffer resultString = new StringBuffer();
        Matcher regexMatcher = regex.matcher(htmlString);
        while (regexMatcher.find()) {
            regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(callback.replace(regexMatcher)));
        }
        regexMatcher.appendTail(resultString);
        return resultString.toString();
    }

    public static void writeToFile(byte[] bytes, final File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }

    public static void writeToFile(final InputStream inputStream, final File file) throws IOException {
        writeToFile(IOUtils.toByteArray(inputStream), file);
    }

}
