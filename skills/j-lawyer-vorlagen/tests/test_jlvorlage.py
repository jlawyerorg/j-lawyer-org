"""Tests for jlvorlage.py - run from the repository root:

    python3 -m unittest discover -s skills/j-lawyer-vorlagen/tests

Uses the server's test templates in j-lawyer-server-ejb/src/test/resources/data where available.
"""
import contextlib
import io
import json
import os
import sys
import tempfile
import unittest
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "scripts"))
import jlvorlage  # noqa: E402

DATA = os.path.abspath(os.path.join(HERE, "..", "..", "..",
                                    "j-lawyer-server/j-lawyer-server-ejb/src/test/resources/data"))


def read_bytes(path):
    with open(path, "rb") as f:
        return f.read()


def fixture(name):
    path = os.path.join(DATA, name)
    if not os.path.exists(path):
        raise unittest.SkipTest(f"fixture missing: {path}")
    return path


def run(*argv):
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
        code = jlvorlage.main(list(argv))
    return code, out.getvalue()


def validate(path, *extra):
    cat = jlvorlage.Catalog(jlvorlage.load_catalog(), ["MANDANT", "GEGNER", "DRITTE"], list(extra))
    rep, names = jlvorlage.validate_pkg(jlvorlage.Package(path), cat)
    return rep, names


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.tmp.cleanup()

    def path(self, name):
        return os.path.join(self.tmp.name, name)

    def build(self, spec, ext):
        src = self.path("spec.md")
        with open(src, "w", encoding="utf-8") as f:
            f.write(spec)
        out = self.path("out." + ext)
        code, _ = run("build", src, out)
        self.assertEqual(0, code)
        return out


class InspectTest(Base):
    def test_finds_placeholders_docx(self):
        code, out = run("inspect", fixture("template.docx"))
        self.assertEqual(0, code)
        self.assertIn("{{MANDANT_NAME}}  ×11", out)
        self.assertIn("über 2 Runs verteilt", out)

    def test_finds_placeholders_in_odt_header_and_frames(self):
        code, out = run("inspect", fixture("j-lawyer-allgemeiner-Brief-Mandant-run.odt"))
        self.assertIn("Kopfzeile, Textbox/Rahmen] {{PROFIL_FIRMA}}", out)
        self.assertIn("Tabelle Z2/S2] {{AKTE_ZEICHEN}}", out)

    def test_rejects_other_formats(self):
        self.assertEqual(2, run("inspect", fixture("template.ods"))[0])


class ReplaceTest(Base):
    def test_replace_across_runs_and_textboxes(self):
        rules = self.path("map.json")
        with open(rules, "w", encoding="utf-8") as f:
            json.dump([{"find": "Hans Meiser", "replace": "{{MANDANT_VORNAME}}{{MANDANT_NAME}}"},
                       {"find": "anwalt@kanzlei.com", "replace": "{{PROFIL_EMAIL}}"},
                       {"find": r"\d{2}\.\d{2}\.\d{4}", "replace": "{{KURZDATUM}}", "regex": True}], f)
        out = self.path("out.docx")
        code, log = run("replace", fixture("textfield-run.docx"), out, "--map", rules)
        self.assertEqual(0, code)
        texts = [p.text for p in jlvorlage.Package(out).paragraphs()]
        self.assertIn("{{MANDANT_VORNAME}}{{MANDANT_NAME}} ", texts)
        self.assertTrue(any("{{KURZDATUM}}" in t for t in texts))
        xml = zipfile.ZipFile(out).read("word/document.xml").decode("utf-8")
        self.assertNotIn("anwalt@kanzlei.com", xml)  # text box incl. VML fallback
        self.assertEqual(2, xml.count("{{PROFIL_EMAIL}}"))

    def test_occurrence_and_untouched_input(self):
        src = fixture("template.docx")
        before = read_bytes(src)
        out = self.path("out.docx")
        run("replace", src, out, "--ersetze", "{{MANDANT_NAME}}", "{{GEGNER_NAME}}")
        self.assertEqual(before, read_bytes(src))
        _, names = validate(out)
        self.assertIn("GEGNER_NAME", names)
        self.assertNotIn("MANDANT_NAME", names)

    def test_refuses_to_overwrite_input(self):
        src = self.path("in.docx")
        with open(src, "wb") as f:
            f.write(read_bytes(fixture("template.docx")))
        self.assertEqual(2, run("replace", src, src, "--ersetze", "a", "b")[0])

    def test_replace_in_odt_spans(self):
        out = self.path("out.odt")
        run("replace", fixture("template-scripts-run.odt"), out,
            "--ersetze", "{{MANDANT_VORNAME}}{{MANDANT_NAME}}", "{{GEGNER_VORNAME}}{{GEGNER_NAME}}")
        _, names = validate(out)
        self.assertIn("GEGNER_VORNAME", names)


class MultiLineTest(Base):
    def test_multiline_replacement_gets_one_run_per_line(self):
        out = self.path("out.docx")
        run("replace", fixture("template.docx"), out, "--ersetze", "{{MANDANT_ANREDE}}",
            "{{MANDANT_VORNAME}}\n{{MANDANT_NAME}}\t{{MANDANT_ORT}}")
        rep, _ = validate(out)
        self.assertFalse([w for w in rep.warnings if "mehreren Textelementen" in w])
        p = [p for p in jlvorlage.Package(out).paragraphs() if "{{MANDANT_ORT}}" in p.text][0]
        self.assertEqual("{{MANDANT_VORNAME}}\n{{MANDANT_NAME}}\t{{MANDANT_ORT}}", p.text)
        for r in {id(a.container): a.container for a in p.atoms}.values():
            self.assertEqual(1, len([c for c in jlvorlage.element_children(r) if c.localName != "rPr"]))


class FieldTest(Base):
    def test_replacing_field_result_removes_field(self):
        src = self.build("Ort, Datum\n", "docx")
        with zipfile.ZipFile(src) as z:
            files = {n: z.read(n) for n in z.namelist()}
        field = ('<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText> CREATEDATE </w:instrText></w:r>'
                 '<w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>Datum</w:t></w:r>'
                 '<w:r><w:fldChar w:fldCharType="end"/></w:r>')
        xml = files["word/document.xml"].decode("utf-8")
        files["word/document.xml"] = xml.replace('<w:t xml:space="preserve">Ort, Datum</w:t></w:r>',
                                                 '<w:t xml:space="preserve">Ort, </w:t></w:r>' + field).encode("utf-8")
        with_field = self.path("field.docx")
        with zipfile.ZipFile(with_field, "w") as z:
            for n, d in files.items():
                z.writestr(n, d)
        rep, _ = validate(with_field)
        out = self.path("out.docx")
        run("replace", with_field, out, "--ersetze", "Ort, Datum", "{{PROFIL_ORT}}, {{KURZDATUM}}")
        xml = zipfile.ZipFile(out).read("word/document.xml").decode("utf-8")
        self.assertNotIn("fldChar", xml)
        self.assertNotIn("CREATEDATE", xml)
        self.assertIn("{{PROFIL_ORT}}, {{KURZDATUM}}", xml)


class NormalizeTest(Base):
    def test_merges_split_placeholders(self):
        for name in ("template.docx", "template-scripts-run.odt"):
            out = self.path("n" + os.path.splitext(name)[1])
            run("normalize", fixture(name), out)
            for p in jlvorlage.Package(out).paragraphs():
                for m in jlvorlage.TOKEN_RE.finditer(p.text):
                    self.assertEqual(1, p.containers_in(m.start(), m.end()), f"{name}: {m.group()}")


class ValidateTest(Base):
    def messages(self, rep):
        return "\n".join(rep.errors + rep.warnings)

    def test_alias_and_spacing(self):
        rep, _ = validate(fixture("template.docx"))
        msg = self.messages(rep)
        self.assertIn("{{MANDANT_ANREDE}} ist ein veralteter Alias", msg)
        self.assertIn("Leerzeichen zwischen {{MANDANT_VORNAME}} und {{MANDANT_NAME}}", msg)
        self.assertFalse(rep.errors)

    def test_table_placeholder_ok(self):
        rep, _ = validate(fixture("template-table.docx"))
        self.assertFalse(rep.errors)

    def test_script_split_over_paragraphs(self):
        rep, _ = validate(fixture("template-scripts-run.odt"))
        self.assertTrue(any("nicht im selben Absatz" in e for e in rep.errors))

    def test_errors(self):
        spec = ("{{MANDANT_NAMEN}} {{CURSOR}} {{AUTOR_AN}} {{VERSICHERUNG_NAME}} {{KSCHUTZ_GRUND}}\n\n"
                '[[SCRIPT:WENNGLEICH(MANDANT_ORT, "Hamburg");]]\n\n'
                '[[SCRIPT:"Hallo MANDANT_NAME";]]\n\n'
                "[[SCRIPT:FOO(MANDANT_NAME);]]\n\n"
                "[[SCRIPT:WENNLEER(MANDANT_XYZ, \"a\");]]\n\n"
                "{{BELP_NR}}\n\n"
                "| {{TABELLE_1}} | x |\n")
        for ext in ("docx", "odt"):
            rep, _ = validate(self.build(spec, ext), "KSCHUTZ")
            msg = self.messages(rep)
            self.assertIn("{{MANDANT_NAMEN}} ist kein bekannter Platzhalter", msg)
            self.assertIn("{{CURSOR}}", msg)
            self.assertIn("{{AUTOR_AN}}", msg)
            self.assertIn("Beteiligtentyp-Präfix 'VERSICHERUNG'", msg)
            self.assertNotIn("{{KSCHUTZ_GRUND}} ist kein", msg)
            self.assertIn("WENNGLEICH erwartet 3 oder 4 Parameter, hat 2", msg)
            self.assertIn("innerhalb eines Textes", msg)
            self.assertIn("unbekannte Funktion FOO", msg)
            self.assertIn("unbekannter Platzhalter MANDANT_XYZ", msg)
            self.assertIn("{{BELP_NR}} gehört in eine Tabellenzeile", msg)
            self.assertIn("1×1-Tabelle (ist 1×2)", msg)

    def test_valid_scripts(self):
        spec = ('[[SCRIPT:MWDJU(MANDANT_GESCHLECHT, "Herr " + MANDANT_NAME, "Frau " + MANDANT_NAME, "d", "j", "u");]]\n\n'
                '[[SCRIPT:(MANDANT_NAME.equals("Müller") && MANDANT_ORT.equals("X")) ? "a" : "b";]]\n\n'
                '[[SCRIPT:WENNGLEICH(MANDANT_ORT,"ja",""MANDANT_NOTIZ"");]]\n\n'
                '[[SCRIPT:FRISTBANKTAG(KURZDATUM, "14", "EEEE, dd. MMMM yyyy");]]\n\n'
                '[[SCRIPT:WENNFALLDATEN("KSCHUTZ", "x");]]\n')
        rep, _ = validate(self.build(spec, "docx"))
        self.assertEqual([], rep.errors)

    def test_flexible_invoice_table(self):
        spec = "| {{BELP_NR}} | {{BELP_NAME}} | {{BELP_NETTO}}{{BEL_WHRG}} |\n\n| {{BEL_TABELLE}} | | | | | |\n"
        rep, _ = validate(self.build(spec, "docx"))
        self.assertEqual([], rep.errors)


class BuildTest(Base):
    def test_build_both_formats(self):
        spec = ("---\nfusszeile: {{PROFIL_FIRMA}}\n---\n# Titel\n\n**{{MANDANT_NAME}}** und *kursiv*\\\nZeile 2\n\n"
                "\\seitenumbruch\n| a | b |\n|---|---|\n| {{AKTE_ZEICHEN}} | x |\n")
        for ext in ("docx", "odt"):
            out = self.build(spec, ext)
            texts = [p.text for p in jlvorlage.Package(out).paragraphs()]
            self.assertIn("{{MANDANT_NAME}} und kursiv\nZeile 2", texts)
            self.assertIn("{{PROFIL_FIRMA}}", texts)
            self.assertIn("{{AKTE_ZEICHEN}}", texts)
            with zipfile.ZipFile(out) as z:
                self.assertIsNone(z.testzip())
                if ext == "odt":
                    self.assertEqual("mimetype", z.infolist()[0].filename)


class PlaceholdersTest(Base):
    def test_search(self):
        code, out = run("placeholders", "--suche", "vorname", "--partei", "GEGNER")
        self.assertIn("{{GEGNER_VORNAME}}", out)
        self.assertIn("{{USER_VORNAME}}", out)
        self.assertNotIn("{{###", out)


if __name__ == "__main__":
    unittest.main()
