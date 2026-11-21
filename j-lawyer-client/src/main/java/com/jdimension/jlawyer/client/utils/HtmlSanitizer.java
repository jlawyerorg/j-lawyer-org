/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jdimension.jlawyer.client.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Prepares HTML from external sources (e-mails, beA messages, HTML documents) for display: removes
 * scripts, embedded frames and objects, forms, redirects and event handlers. Layout (styles,
 * tables, fonts, images) is kept; oversized images are shrunk to the width of the view.
 *
 * @author jens
 */
public final class HtmlSanitizer {

    private static final Logger log = Logger.getLogger(HtmlSanitizer.class.getName());

    // elements that are removed including their content
    private static final String REMOVED_ELEMENTS = "script, iframe, frame, frameset, object, embed, applet, base, portal, "
            + "noembed, animate, set, animatemotion, animatetransform, handler, listener";

    // images declared wider than this (CSS pixels) are shrunk to the width of the view
    static final double FIT_IMAGE_MIN_WIDTH_PX = 800d;

    private HtmlSanitizer() {
    }

    /**
     * @param html HTML from an external source
     * @return the HTML without active content
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        try {
            Document doc = Jsoup.parse(html);
            doc.select(REMOVED_ELEMENTS).remove();
            for (Element meta : doc.select("meta[http-equiv]")) {
                if (!"content-type".equalsIgnoreCase(meta.attr("http-equiv").trim())) {
                    meta.remove();
                }
            }
            for (Element link : doc.select("link")) {
                if (!"stylesheet".equalsIgnoreCase(link.attr("rel").trim())) {
                    link.remove();
                }
            }
            doc.select("form").unwrap();

            for (Element e : doc.getAllElements()) {
                List<String> removeAttributes = new ArrayList<>();
                for (Attribute a : e.attributes()) {
                    String name = a.getKey().toLowerCase(Locale.ROOT);
                    if (name.startsWith("on") || "srcdoc".equals(name) || "formaction".equals(name)
                            || isActiveUrl(a.getValue())) {
                        removeAttributes.add(a.getKey());
                    }
                }
                for (String name : removeAttributes) {
                    e.removeAttr(name);
                }
            }
            for (Element img : doc.select("img")) {
                fitImage(img);
            }
            doc.outputSettings().prettyPrint(false);
            return doc.outerHtml();
        } catch (Throwable t) {
            log.error("Unable to sanitize HTML - showing it as text", t);
            return "<html><body><pre>" + escape(html) + "</pre></body></html>";
        }
    }

    /**
     * Lets an image shrink to the width of the view instead of being shown 1:1 - mail clients
     * such as Outlook embed photos with their full pixel size (e.g. width="4000" plus
     * style="width:41.6666in"), of which the view would only show the upper left corner. Only images
     * without any size and images declared wider than {@link #FIT_IMAGE_MIN_WIDTH_PX} are touched:
     * "height:auto" scales by the image's own proportions, which would distort images whose declared
     * size deliberately differs from them (spacers, e.g. a 1x1 GIF at width="600" height="1") - the
     * WebView does not support "aspect-ratio" to keep the declared proportions instead.
     *
     * @param img an img element
     */
    static void fitImage(Element img) {
        String style = img.attr("style");
        double width = cssLength(lastDeclaration(style, "width"));
        if (Double.isNaN(width)) {
            width = cssLength(img.attr("width"));
        }
        boolean sizeDeclared = img.hasAttr("width") || img.hasAttr("height")
                || lastDeclaration(style, "width") != null || lastDeclaration(style, "height") != null;
        if (!sizeDeclared || (!Double.isNaN(width) && width > FIT_IMAGE_MIN_WIDTH_PX)) {
            String s = style.trim();
            if (!s.isEmpty() && !s.endsWith(";")) {
                s = s + ";";
            }
            img.attr("style", s + "max-width:100%;height:auto");
        }
    }

    /**
     * @param style an inline style
     * @param property a CSS property name
     * @return the value of the last declaration of the property, null if there is none
     */
    private static String lastDeclaration(String style, String property) {
        if (style == null) {
            return null;
        }
        String value = null;
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon > 0 && property.equalsIgnoreCase(declaration.substring(0, colon).trim())) {
                value = declaration.substring(colon + 1).trim();
            }
        }
        return value;
    }

    /**
     * @param value a CSS length or HTML width attribute, e.g. "4000", "600px", "41.6666in"
     * @return the length in CSS pixels, NaN if the value is missing, relative (%, em, ...) or invalid
     */
    static double cssLength(String value) {
        if (value == null) {
            return Double.NaN;
        }
        String v = value.toLowerCase(Locale.ROOT).replace("!important", "").trim();
        int unitStart = 0;
        while (unitStart < v.length() && (Character.isDigit(v.charAt(unitStart)) || v.charAt(unitStart) == '.')) {
            unitStart++;
        }
        if (unitStart == 0) {
            return Double.NaN;
        }
        double number;
        try {
            number = Double.parseDouble(v.substring(0, unitStart));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
        switch (v.substring(unitStart).trim()) {
            case "":
            case "px":
                return number;
            case "in":
                return number * 96d;
            case "cm":
                return number * 96d / 2.54d;
            case "mm":
                return number * 96d / 25.4d;
            case "pt":
                return number * 96d / 72d;
            case "pc":
                return number * 16d;
            default:
                return Double.NaN;
        }
    }

    /**
     * @param value an attribute value
     * @return true if the value is a URL that runs code or loads a document when used (javascript:,
     * vbscript:, data: other than raster images)
     */
    static boolean isActiveUrl(String value) {
        if (value == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            // browsers ignore whitespace and control characters within the scheme
            if (c > 0x20 && c != 0x7f) {
                sb.append(Character.toLowerCase(c));
            }
        }
        String v = sb.toString();
        if (v.startsWith("javascript:") || v.startsWith("vbscript:") || v.startsWith("livescript:")) {
            return true;
        }
        return v.startsWith("data:") && !(v.startsWith("data:image/") && !v.startsWith("data:image/svg"));
    }

    /**
     * @param url a link target from an external source
     * @return true if the link may be passed to the system browser (http and https only)
     */
    public static boolean isWebLink(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
