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
package com.jdimension.jlawyer.documents;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the evaluation of [[SCRIPT:...]] expressions in templates: template functions, Java code in
 * scripts, placeholder values that try to break out of their string, and the allow list.
 *
 * @author jens
 */
public class SmartTemplateEvaluatorTest {

    private static HashMap<String, Object> values() {
        HashMap<String, Object> v = new HashMap<>();
        v.put("{{MANDANT_NAME}}", "Müller");
        v.put("{{MANDANT_VORNAME}}", "Anna");
        v.put("{{MANDANT_ORT}}", "Hamburg");
        v.put("{{MANDANT_ORTSTEIL}}", "Altona");
        v.put("{{MANDANT_GESCHLECHT}}", "weiblich");
        v.put("{{MANDANT_STA}}", "deutsch");
        v.put("{{MANDANT_FIRMA}}", "");
        v.put("{{KSCHUTZ_GRUNDK}}", "");
        v.put("{{AKTE_GEGENSTANDSWERT}}", "60.000,00");
        v.put("{{KURZDATUM}}", "01.01.2022");
        return v;
    }

    private static String eval(String script) {
        return SmartTemplateEvaluator.evaluate("", script, values(), new ArrayList<>(), SmartTemplateEvaluator.MODE_ENFORCE);
    }

    private static String evalLog(String script) {
        return SmartTemplateEvaluator.evaluate("", script, values(), new ArrayList<>(), SmartTemplateEvaluator.MODE_LOG);
    }

    private static void assertRejected(String script) {
        String result = eval(script);
        Assert.assertTrue("should be rejected: " + script + " -> " + result, result != null && result.startsWith("SCRIPTFEHLER"));
    }

    // ---- template functions, as used in existing templates

    @Test
    public void templateFunctions() {
        Assert.assertEquals("Frau Anna Müller", eval("[[SCRIPT:MWDJU(MANDANT_GESCHLECHT,\"Herrn \" + MANDANT_VORNAME + \" \" + MANDANT_NAME,\"Frau \" + MANDANT_VORNAME + \" \" + MANDANT_NAME,MANDANT_VORNAME + \" \" + MANDANT_NAME, MANDANT_FIRMA, MANDANT_FIRMA + \" - \" + MANDANT_VORNAME + \" \" + MANDANT_NAME);]]"));
        Assert.assertEquals("11.01.2022", eval("[[SCRIPT:FRIST(\"01.01.2022\",\"10\");]]"));
        Assert.assertEquals("15.01.2022", eval("[[SCRIPT:FRIST(KURZDATUM,\"14\");]]"));
        Assert.assertEquals("17.01.2022", eval("[[SCRIPT:FRISTBANKTAG(KURZDATUM,\"14\");]]"));
        Assert.assertEquals("Weiblich", eval("[[SCRIPT:GROSS(MANDANT_GESCHLECHT);]]"));
        Assert.assertEquals("Moin moin Anna Müller", eval("[[SCRIPT:WENNGLEICH(MANDANT_ORT, \"Hamburg\", \"Moin moin \" +  MANDANT_VORNAME + \" \" + MANDANT_NAME);]]"));
        Assert.assertEquals("Die Person hat die deutsche Staatsangehörigkeit.", eval("[[SCRIPT:WENNENTHAELT(MANDANT_STA, \"deutsch\", \"Die Person hat die deutsche Staatsangehörigkeit.\", \"nein\");]]"));
        Assert.assertEquals("Ein Grund für die Kündigung wurde nicht mitgeteilt.", eval("[[SCRIPT:WENNLEER(KSCHUTZ_GRUNDK, \"Ein Grund für die Kündigung wurde nicht mitgeteilt.\", \"Begründung liegt vor.\");]]"));
        Assert.assertEquals("Das wird ein tolles Mandat!", eval("[[SCRIPT:WENNGROESSER(AKTE_GEGENSTANDSWERT, \"50000\", \"Das wird ein tolles Mandat!\", \"nein\");]]"));
        Assert.assertEquals("jemand anderes", eval("[[SCRIPT:(MANDANT_NAME.equals(\"Müller\") && MANDANT_ORT.equals(\"Dingsstadt\")) ? \"Frau Müller aus Dingsstadt\" : \"jemand anderes\";]]"));
        Assert.assertEquals("Bei der Beklagten ist ein Betriebsrat gebildet.", eval("[[SCRIPT:MWDJU(MANDANT_GESCHLECHT,\"Beim Beklagten \",\"Bei der Beklagten \",\"Bei der beklagten Partei \", \"Bei den Beklagten \", \"Bei der beklagten Partei \") + \"ist ein Betriebsrat gebildet.\";]]"));
        Assert.assertFalse(eval("[[SCRIPT:DATUMZEIT(\"EEE, dd.MM.yyyy\");]]").startsWith("SCRIPTFEHLER"));
        // without trailing semicolon and with typographic quotes
        Assert.assertEquals("ja", eval("[[SCRIPT:WENNGLEICH(MANDANT_ORT, „Hamburg“, „ja“, „nein“)]]"));
    }

    @Test
    public void longerPlaceholderNamesWin() {
        Assert.assertEquals("Altona", eval("[[SCRIPT:MANDANT_ORTSTEIL;]]"));
    }

    // ---- Java code in scripts

    @Test
    public void javaCodeOnValueClasses() {
        Assert.assertEquals("2022", eval("[[SCRIPT:new java.text.SimpleDateFormat(\"yyyy\").format(new java.text.SimpleDateFormat(\"dd.MM.yyyy\").parse(KURZDATUM));]]"));
        Assert.assertEquals("43", eval("[[SCRIPT:String.valueOf(Integer.parseInt(\"42\") + 1);]]"));
        Assert.assertEquals("3.50", eval("[[SCRIPT:new java.math.BigDecimal(\"1.25\").add(new java.math.BigDecimal(\"2.25\")).toString();]]"));
        Assert.assertEquals("MÜLLER", eval("[[SCRIPT:MANDANT_NAME.toUpperCase();]]"));
        Assert.assertEquals("2022", eval("[[SCRIPT:String.valueOf(java.time.LocalDate.of(2022, 1, 1).getYear());]]"));
        Assert.assertEquals("12,50", eval("[[SCRIPT:new java.text.DecimalFormat(\"0.00\", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.GERMANY)).format(12.5d);]]"));
        Assert.assertEquals("2", eval("[[SCRIPT:def c = java.util.Calendar.getInstance(); c.set(2022, java.util.Calendar.FEBRUARY, 1); String.valueOf(c.get(java.util.Calendar.MONTH) + 1);]]"));
    }

    // ---- placeholder values must stay data

    @Test
    public void valuesCannotBreakOutOfTheirString() {
        HashMap<String, Object> v = new HashMap<>();
        v.put("{{MANDANT_NAME}}", "x' + 'y\" + \"z ${7*7} \\ \n Zeile");
        String result = SmartTemplateEvaluator.evaluate("", "[[SCRIPT:MANDANT_NAME;]]", v, new ArrayList<>(), SmartTemplateEvaluator.MODE_ENFORCE);
        Assert.assertEquals("x' + 'y\" + \"z ${7*7} \\ \n Zeile", result);
    }

    @Test
    public void typographicQuotesInValuesAreKept() {
        HashMap<String, Object> v = new HashMap<>();
        v.put("{{MANDANT_NAME}}", "„Alpha“ GmbH");
        Assert.assertEquals("„Alpha“ GmbH", SmartTemplateEvaluator.evaluate("", "[[SCRIPT:MANDANT_NAME;]]", v, new ArrayList<>(), SmartTemplateEvaluator.MODE_ENFORCE));
    }

    @Test
    public void literalEscaping() {
        Assert.assertEquals("'a\\'b\\\\c\\nd'", SmartTemplateEvaluator.toLiteral("a'b\\c\nd"));
    }

    // ---- allow list

    @Test
    public void processesAndSystemAccessAreRejected() {
        assertRejected("[[SCRIPT:\"id\".execute().text;]]");
        assertRejected("[[SCRIPT:Runtime.getRuntime().exec(\"id\");]]");
        assertRejected("[[SCRIPT:new ProcessBuilder(\"id\").start();]]");
        assertRejected("[[SCRIPT:System.getProperty(\"user.home\");]]");
        assertRejected("[[SCRIPT:System.exit(0);]]");
    }

    @Test
    public void fileAndNetworkAccessIsRejected() {
        assertRejected("[[SCRIPT:new File(\"/etc/passwd\").text;]]");
        assertRejected("[[SCRIPT:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(\"/etc/passwd\")).toString();]]");
        assertRejected("[[SCRIPT:new URL(\"http://example.org\").text;]]");
    }

    @Test
    public void reflectionAndMetaProgrammingAreRejected() {
        assertRejected("[[SCRIPT:\"a\".getClass().forName(\"java.lang.Runtime\");]]");
        assertRejected("[[SCRIPT:\"a\".class.name;]]");
        assertRejected("[[SCRIPT:Class.forName(\"java.lang.Runtime\");]]");
        assertRejected("[[SCRIPT:\"a\".metaClass;]]");
        assertRejected("[[SCRIPT:this.binding;]]");
        assertRejected("[[SCRIPT:evaluate(\"1\");]]");
        assertRejected("[[SCRIPT:\"a\".&toString;]]");
        assertRejected("[[SCRIPT:getClass();]]");
    }

    @Test
    public void structureIsRestricted() {
        assertRejected("[[SCRIPT:@groovy.transform.ASTTest(value={ assert true }) def x = 1; x;]]");
        assertRejected("[[SCRIPT:import java.io.File; 1;]]");
        assertRejected("[[SCRIPT:class X { }; 1;]]");
        assertRejected("[[SCRIPT:def f() { 1 }; f();]]");
    }

    @Test
    public void unknownPlaceholdersAreAnError() {
        assertRejected("[[SCRIPT:UNBEKANNTER_PLATZHALTER;]]");
    }

    @Test
    public void logModeStillRunsScriptsOutsideOfTheAllowList() {
        Assert.assertEquals(System.getProperty("java.version"), evalLog("[[SCRIPT:System.getProperty(\"java.version\");]]"));
        Assert.assertTrue(eval("[[SCRIPT:System.getProperty(\"java.version\");]]").startsWith("SCRIPTFEHLER"));
    }
}
