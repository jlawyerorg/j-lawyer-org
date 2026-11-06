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

import org.junit.Assert;
import org.junit.Test;

public class HtmlSanitizerTest {

    private static void assertNotContains(String html, String... parts) {
        String lower = html.toLowerCase();
        for (String p : parts) {
            Assert.assertFalse("should not contain " + p + ": " + html, lower.contains(p.toLowerCase()));
        }
    }

    @Test
    public void removesActiveContent() {
        String out = HtmlSanitizer.sanitize("<html><head><meta http-equiv=\"refresh\" content=\"0;url=https://x\"><base href=\"https://x/\">"
                + "<script>alert(1)</script></head><body onload=\"alert(2)\">"
                + "<img src=\"x\" onerror=\"alert(3)\"><a href=\"javascript:alert(4)\">a</a><a href=\" jav&#x09;ascript:alert(5)\">b</a>"
                + "<iframe src=\"https://x\"></iframe><object data=\"x\"></object><embed src=\"x\">"
                + "<form action=\"https://x\"><input name=\"q\"><button formaction=\"javascript:alert(6)\">go</button></form>"
                + "<svg><script>alert(7)</script><a xlink:href=\"javascript:alert(8)\"><text>t</text></a><animate attributeName=\"href\" values=\"javascript:alert(9)\"/></svg>"
                + "<a href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgxMCk8L3NjcmlwdD4=\">c</a>"
                + "</body></html>");
        assertNotContains(out, "<script", "alert(", "onload", "onerror", "<iframe", "<object", "<embed", "<form", "<base", "refresh", "formaction", "<animate", "data:text");
    }

    @Test
    public void keepsLayoutLinksAndImages() {
        String out = HtmlSanitizer.sanitize("<html><head><style>p{color:red}</style><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head>"
                + "<body><table style=\"width:100%\"><tr><td class=\"c\"><font face=\"Arial\">Hallo</font></td></tr></table>"
                + "<a href=\"https://www.j-lawyer.org\">Link</a><a href=\"mailto:a@b.de\">Mail</a>"
                + "<img src=\"cid:logo123\"><img src=\"data:image/png;base64,iVBORw0KGgo=\"></body></html>");
        Assert.assertTrue(out.contains("p{color:red}"));
        Assert.assertTrue(out.contains("style=\"width:100%\""));
        Assert.assertTrue(out.contains("<font face=\"Arial\">Hallo</font>"));
        Assert.assertTrue(out.contains("href=\"https://www.j-lawyer.org\""));
        Assert.assertTrue(out.contains("href=\"mailto:a@b.de\""));
        Assert.assertTrue(out.contains("src=\"cid:logo123\""));
        Assert.assertTrue(out.contains("src=\"data:image/png;base64,iVBORw0KGgo=\""));
        Assert.assertTrue(out.toLowerCase().contains("content-type"));
    }

    @Test
    public void svgDataImagesAreRemoved() {
        String out = HtmlSanitizer.sanitize("<img src=\"data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=\">");
        assertNotContains(out, "data:image/svg");
    }

    @Test
    public void webLinks() {
        Assert.assertTrue(HtmlSanitizer.isWebLink("https://www.j-lawyer.org"));
        Assert.assertTrue(HtmlSanitizer.isWebLink(" HTTP://example.com/a"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("file:///C:/Windows/System32/calc.exe"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("file://server/share/x.exe"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("smb://server/share"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("search-ms:query=x"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("ms-msdt:/id PCWDiagnostic"));
        Assert.assertFalse(HtmlSanitizer.isWebLink("javascript:alert(1)"));
        Assert.assertFalse(HtmlSanitizer.isWebLink(null));
    }

    @Test
    public void emptyInput() {
        Assert.assertNull(HtmlSanitizer.sanitize(null));
        Assert.assertEquals("", HtmlSanitizer.sanitize(""));
    }
}
