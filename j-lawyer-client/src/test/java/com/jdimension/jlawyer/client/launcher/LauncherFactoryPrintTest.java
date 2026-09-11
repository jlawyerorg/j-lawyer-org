package com.jdimension.jlawyer.client.launcher;

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LauncherFactoryPrintTest {

    @Test
    public void testDefaultPrinterCommandKeepsSeparateDocumentArguments() {
        List<String> command = LauncherFactory.buildLibreOfficePrintCommand(
                "soffice", Arrays.asList("C:\\Test Akte\\Dokument ä.odt"), null, false);

        assertEquals(Arrays.asList(
                "soffice", "-p", "-nologo", "C:\\Test Akte\\Dokument ä.odt"), command);
    }

    @Test
    public void testNamedPrinterCommandKeepsTargetAndDocumentsAsSeparateArguments() {
        List<String> command = LauncherFactory.buildLibreOfficePrintCommand(
                "libreoffice", Arrays.asList("/tmp/Test Dokument.odt", "/tmp/zweites.ods"),
                "Fax Drucker Süd", false);

        assertEquals(Arrays.asList(
                "libreoffice", "--pt", "Fax Drucker Süd", "-nologo",
                "/tmp/Test Dokument.odt", "/tmp/zweites.ods"), command);
    }

    @Test
    public void testMacCommandUsesLongNoLogoOption() {
        List<String> command = LauncherFactory.buildLibreOfficePrintCommand(
                "/Applications/LibreOffice.app/Contents/MacOS/soffice",
                Arrays.asList("/tmp/document.odt"), "Office", true);

        assertEquals("--nologo", command.get(3));
    }
}
