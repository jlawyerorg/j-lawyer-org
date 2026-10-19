#!/usr/bin/env python3
"""jlvorlage - Werkzeug für j-lawyer.org-Dokumentvorlagen (DOCX/ODT).

Nur Python-Standardbibliothek, damit es auch in der Code-Ausführung von claude.ai läuft.

Unterbefehle:
  inspect       Text je Absatz mit Fundstelle, gefundene Platzhalter und Skripte
  replace       Text (auch über Run-/Span-Grenzen) ersetzen, z.B. konkrete Werte durch Platzhalter
  normalize     jeden Platzhalter / jedes Skript in genau einen Run legen
  validate      Vorlage gegen die Regeln der j-lawyer-Vorlagen-Engine prüfen
  build         neue Vorlage (DOCX/ODT) aus einer einfachen Markdown-Beschreibung erzeugen
  placeholders  Platzhalterkatalog durchsuchen

Die Eingabedatei wird nie verändert; Ergebnisse werden immer in eine neue Datei geschrieben.
"""
import argparse
import json
import os
import re
import sys
import zipfile
from xml.dom import minidom

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
MC = "http://schemas.openxmlformats.org/markup-compatibility/2006"
XML_NS = "http://www.w3.org/XML/1998/namespace"
OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
DRAW = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
STYLE = "urn:oasis:names:tc:opendocument:xmlns:style:1.0"

CATALOG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "catalog.json")

# a placeholder or a script; a script ends at the first "]" (same as the server's regex)
TOKEN_RE = re.compile(r"\{\{[^{}]*\}\}|\[\[SCRIPT:[^\]]*\]\]")
PLACEHOLDER_RE = re.compile(r"\{\{([^{}]*)\}\}")
SCRIPT_RE = re.compile(r"\[\[SCRIPT:([^\]]*)\]\]")
NAME_RE = re.compile(r"^[A-Z0-9_ÄÖÜ]+$")
# characters after which the server does not add a space behind a placeholder value
NO_SPACE_AFTER = set(".,;:!?'\" /)]-")


class UserError(Exception):
    pass


def load_catalog():
    with open(CATALOG_PATH, encoding="utf-8") as f:
        return json.load(f)


# --------------------------------------------------------------------------------------------
# document model
# --------------------------------------------------------------------------------------------

def ancestors(node):
    node = node.parentNode
    while node is not None and node.nodeType == node.ELEMENT_NODE:
        yield node
        node = node.parentNode


def is_el(node, ns, local):
    return node.nodeType == node.ELEMENT_NODE and node.namespaceURI == ns and node.localName == local


def element_children(node, ns=None, local=None):
    return [c for c in node.childNodes if c.nodeType == c.ELEMENT_NODE
            and (ns is None or c.namespaceURI == ns) and (local is None or c.localName == local)]


class Atom:
    """A piece of paragraph text: an editable text node or a fixed element (tab, line break, ...)."""

    def __init__(self, kind, text, node, container, context):
        self.kind = kind            # "text" or "fixed"
        self.text = text
        self.node = node            # DOCX: w:t element / fixed element; ODT: Text node / fixed element
        self.container = container  # DOCX: w:r, ODT: parent element of the text node
        self.context = context      # set of wrapper element names between paragraph and atom


class Paragraph:
    def __init__(self, pkg, part, element):
        self.pkg = pkg
        self.part = part
        self.element = element
        self.id = None
        self.rebuild()

    def rebuild(self):
        self.atoms = []
        if self.pkg.kind == "docx":
            self._collect_docx(self.element, frozenset())
        else:
            self._collect_odt(self.element, frozenset())
        self.text = "".join(a.text for a in self.atoms)

    def _collect_docx(self, node, ctx):
        for c in node.childNodes:
            if c.nodeType != c.ELEMENT_NODE:
                continue
            if c.namespaceURI == W:
                name = c.localName
                if name in ("pPr", "rPr", "del", "moveFrom", "txbxContent", "instrText", "delText", "fldChar"):
                    continue
                if name == "r":
                    self._collect_docx_run(c, ctx)
                    continue
                if name in ("hyperlink", "sdt", "ins", "moveTo", "fldSimple", "smartTag", "customXml"):
                    self._collect_docx(c, ctx | {name})
                    continue
            if c.namespaceURI == MC and c.localName == "AlternateContent":
                continue  # drawings / text boxes: their paragraphs are handled on their own
            self._collect_docx(c, ctx)

    def _collect_docx_run(self, run, ctx):
        for c in run.childNodes:
            if c.nodeType != c.ELEMENT_NODE or c.namespaceURI != W:
                continue
            if c.localName == "t":
                txt = "".join(t.data for t in c.childNodes if t.nodeType == t.TEXT_NODE)
                if txt:
                    self.atoms.append(Atom("text", txt, c, run, ctx))
            elif c.localName == "tab":
                self.atoms.append(Atom("fixed", "\t", c, run, ctx))
            elif c.localName in ("br", "cr"):
                self.atoms.append(Atom("fixed", "\n", c, run, ctx))
            elif c.localName == "noBreakHyphen":
                self.atoms.append(Atom("fixed", "-", c, run, ctx))

    def _collect_odt(self, node, ctx):
        for c in node.childNodes:
            if c.nodeType == c.TEXT_NODE:
                if c.data:
                    self.atoms.append(Atom("text", c.data, c, node, ctx))
                continue
            if c.nodeType != c.ELEMENT_NODE:
                continue
            if c.namespaceURI == TEXT:
                name = c.localName
                if name == "s":
                    n = int(c.getAttributeNS(TEXT, "c") or "1")
                    self.atoms.append(Atom("fixed", " " * n, c, node, ctx))
                    continue
                if name == "tab":
                    self.atoms.append(Atom("fixed", "\t", c, node, ctx))
                    continue
                if name == "line-break":
                    self.atoms.append(Atom("fixed", "\n", c, node, ctx))
                    continue
                if name in ("note", "tracked-changes", "deletion"):
                    continue
                if name in ("a", "meta", "ruby"):
                    self._collect_odt(c, ctx | {name})
                    continue
            if c.namespaceURI == DRAW or (c.namespaceURI == OFFICE and c.localName == "annotation"):
                continue  # frames / text boxes / comments: own paragraphs
            self._collect_odt(c, ctx)

    # --- location -----------------------------------------------------------------------------

    def location(self):
        loc = {"part": self.part, "area": self.pkg.part_area(self.part), "textbox": False,
               "fallback": False, "table_depth": 0, "cell": None}
        for a in ancestors(self.element):
            if self.pkg.kind == "docx":
                if is_el(a, W, "txbxContent"):
                    loc["textbox"] = True
                elif is_el(a, MC, "Fallback"):
                    loc["fallback"] = True
                elif is_el(a, W, "tbl"):
                    loc["table_depth"] += 1
                elif is_el(a, W, "tc") and loc["cell"] is None:
                    loc["cell"] = a
            else:
                if a.namespaceURI == DRAW and a.localName in ("frame", "text-box"):
                    loc["textbox"] = True
                elif is_el(a, TABLE, "table"):
                    loc["table_depth"] += 1
                elif is_el(a, TABLE, "table-cell") and loc["cell"] is None:
                    loc["cell"] = a
                elif a.namespaceURI == STYLE and a.localName.startswith(("header", "footer")):
                    loc["area"] = "Kopfzeile" if a.localName.startswith("header") else "Fußzeile"
        return loc

    # --- editing ------------------------------------------------------------------------------

    def atom_spans(self):
        pos = 0
        for a in self.atoms:
            yield a, pos, pos + len(a.text)
            pos += len(a.text)

    def atoms_in(self, s, e):
        return [a for a, a0, a1 in self.atom_spans() if a0 < e and a1 > s]

    def containers_in(self, s, e):
        """Number of distinct runs (DOCX) / text containers (ODT) holding the text [s, e)."""
        return len({id(a.container) for a in self.atoms_in(s, e)})

    def replace_range(self, s, e, new):
        """Replaces paragraph text [s, e) with new; the new text goes into the first text atom."""
        hit = [(a, a0, a1) for a, a0, a1 in self.atom_spans() if a0 < e and a1 > s]
        if not hit:
            return False
        first = next(((a, a0, a1) for a, a0, a1 in hit if a.kind == "text"), None)
        if first is None:
            return False
        a, a0, a1 = first
        prefix = a.text[:max(0, s - a0)] if a0 <= s else ""
        suffix = a.text[e - a0:] if a1 > e else ""
        # text of hit atoms before the first text atom (fixed atoms at the match start) is removed
        for b, b0, b1 in hit:
            if b is a:
                break
            self._remove_atom(b)
        self._set_text(a, prefix + new + suffix)
        after = False
        for b, b0, b1 in hit:
            if b is a:
                after = True
                continue
            if not after:
                continue
            if b.kind == "text" and b1 > e:
                self._set_text(b, b.text[e - b0:])
            else:
                self._remove_atom(b)
        self.rebuild()
        return True

    def _set_text(self, atom, value):
        if self.pkg.kind == "docx":
            self._set_docx_text(atom, value)
        else:
            self._set_odt_text(atom, value)

    def _set_docx_text(self, atom, value):
        doc = atom.node.ownerDocument
        prefix = atom.node.prefix or "w"
        if value == "":
            self._remove_atom(atom)
            return
        nodes = []
        for part in re.split(r"(\t|\n)", value):
            if part == "\t":
                nodes.append(doc.createElementNS(W, prefix + ":tab"))
            elif part == "\n":
                nodes.append(doc.createElementNS(W, prefix + ":br"))
            elif part:
                t = doc.createElementNS(W, prefix + ":t")
                t.setAttributeNS(XML_NS, "xml:space", "preserve")
                t.appendChild(doc.createTextNode(part))
                nodes.append(t)
        parent = atom.node.parentNode
        for n in nodes:
            parent.insertBefore(n, atom.node)
        parent.removeChild(atom.node)

    def _set_odt_text(self, atom, value):
        doc = atom.node.ownerDocument
        if value == "":
            self._remove_atom(atom)
            return
        prefix = self.pkg.text_prefix
        nodes = []
        for part in re.split(r"(\t|\n| {2,})", value):
            if part == "\t":
                nodes.append(doc.createElementNS(TEXT, prefix + ":tab"))
            elif part == "\n":
                nodes.append(doc.createElementNS(TEXT, prefix + ":line-break"))
            elif part.startswith("  "):
                nodes.append(doc.createTextNode(" "))
                s = doc.createElementNS(TEXT, prefix + ":s")
                if len(part) > 2:
                    s.setAttributeNS(TEXT, prefix + ":c", str(len(part) - 1))
                nodes.append(s)
            elif part:
                nodes.append(doc.createTextNode(part))
        parent = atom.node.parentNode
        for n in nodes:
            parent.insertBefore(n, atom.node)
        parent.removeChild(atom.node)

    def _remove_atom(self, atom):
        node = atom.node
        parent = node.parentNode
        if parent is None:
            return
        parent.removeChild(node)
        if self.pkg.kind == "docx" and is_el(parent, W, "r"):
            rest = [c for c in element_children(parent) if not is_el(c, W, "rPr")]
            if not rest and parent.parentNode is not None:
                parent.parentNode.removeChild(parent)


class Package:
    def __init__(self, path):
        self.path = path
        ext = os.path.splitext(path)[1].lower()
        if ext in (".docx", ".dotx", ".docm"):
            self.kind = "docx"
        elif ext in (".odt", ".ott"):
            self.kind = "odt"
        else:
            raise UserError(f"Nicht unterstütztes Format '{ext}' - nur .docx und .odt")
        try:
            with zipfile.ZipFile(path) as z:
                self.infos = z.infolist()
                self.data = {i.filename: z.read(i.filename) for i in self.infos}
        except (zipfile.BadZipFile, FileNotFoundError) as e:
            raise UserError(f"Datei kann nicht gelesen werden: {e}")
        self.doms = {}
        self.text_prefix = "text"

    def text_parts(self):
        names = [i.filename for i in self.infos]
        if self.kind == "docx":
            def num(n):
                m = re.search(r"(\d+)", n)
                return int(m.group(1)) if m else 0
            hdr = sorted((n for n in names if re.match(r"word/header\d*\.xml$", n)), key=num)
            ftr = sorted((n for n in names if re.match(r"word/footer\d*\.xml$", n)), key=num)
            return ["word/document.xml"] + hdr + ftr
        return [n for n in ("content.xml", "styles.xml") if n in names]

    def part_area(self, part):
        if self.kind == "docx":
            if "header" in part:
                return "Kopfzeile"
            if "footer" in part:
                return "Fußzeile"
        return "Text"

    def dom(self, part):
        if part not in self.doms:
            self.doms[part] = minidom.parseString(self.data[part])
            if self.kind == "odt":
                root = self.doms[part].documentElement
                for i in range(root.attributes.length):
                    attr = root.attributes.item(i)
                    if attr.value == TEXT and attr.name.startswith("xmlns:"):
                        self.text_prefix = attr.name[6:]
        return self.doms[part]

    def paragraphs(self):
        result = []
        for part in self.text_parts():
            doc = self.dom(part)
            if self.kind == "docx":
                elements = doc.getElementsByTagNameNS(W, "p")
            else:
                elements = [e for e in doc.getElementsByTagNameNS(TEXT, "*") if e.localName in ("p", "h")]
                if part == "styles.xml":
                    elements = [e for e in elements
                                if any(a.namespaceURI == STYLE and a.localName == "master-page" for a in ancestors(e))]
            for el in elements:
                result.append(Paragraph(self, part, el))
        for i, p in enumerate(result, 1):
            p.id = f"P{i}"
        return result

    def save(self, out_path):
        if os.path.abspath(out_path) == os.path.abspath(self.path):
            raise UserError("Ausgabedatei darf nicht die Eingabedatei sein")
        tmp = out_path + ".tmp"
        with zipfile.ZipFile(tmp, "w") as z:
            for info in self.infos:
                data = self.data[info.filename]
                if info.filename in self.doms:
                    data = serialize(self.doms[info.filename], data)
                z.writestr(info, data)
        os.replace(tmp, out_path)


def serialize(doc, original):
    m = re.match(rb"\s*(<\?xml[^>]*\?>)", original)
    decl = m.group(1).decode("utf-8") if m else '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    return (decl + "\n" + doc.documentElement.toxml()).encode("utf-8")


# --------------------------------------------------------------------------------------------
# tables
# --------------------------------------------------------------------------------------------

def table_info(pkg, cell):
    """Returns (rows, columns_in_first_row, row_index, col_index, cell_text) for a table cell."""
    if pkg.kind == "docx":
        row = cell.parentNode
        table = row.parentNode
        rows = element_children(table, W, "tr")
        cells = element_children(row, W, "tc")
        first_cols = len(element_children(rows[0], W, "tc")) if rows else 0
        texts = [t.data for t_el in cell.getElementsByTagNameNS(W, "t") for t in t_el.childNodes
                 if t.nodeType == t.TEXT_NODE]
    else:
        row = cell.parentNode
        table = row
        while table is not None and not is_el(table, TABLE, "table"):
            table = table.parentNode
        rows = [r for r in table.getElementsByTagNameNS(TABLE, "table-row")
                if next(a for a in ancestors(r) if is_el(a, TABLE, "table")) is table]
        cells = [c for c in element_children(row, TABLE) if c.localName in ("table-cell", "covered-table-cell")]
        first_cols = sum(int(c.getAttributeNS(TABLE, "number-columns-repeated") or "1")
                         for c in element_children(rows[0], TABLE) if c.localName in ("table-cell", "covered-table-cell")) if rows else 0
        texts = []

        def walk(n):
            for c in n.childNodes:
                if c.nodeType == c.TEXT_NODE:
                    texts.append(c.data)
                elif c.nodeType == c.ELEMENT_NODE:
                    walk(c)
        walk(cell)
    return len(rows), first_cols, rows.index(row) + 1, cells.index(cell) + 1, "".join(texts).strip()


def describe(pkg, p, loc=None):
    loc = loc or p.location()
    parts = []
    if loc["area"] != "Text":
        parts.append(f"{loc['area']} ({loc['part']})" if pkg.kind == "docx" else loc["area"])
    if loc["cell"] is not None:
        _, _, r, c, _ = table_info(pkg, loc["cell"])
        parts.append(f"Tabelle Z{r}/S{c}" + (" (verschachtelt)" if loc["table_depth"] > 1 else ""))
    if loc["textbox"]:
        parts.append("Textbox/Rahmen")
    return ", ".join(parts)


# --------------------------------------------------------------------------------------------
# inspect
# --------------------------------------------------------------------------------------------

def cmd_inspect(args):
    pkg = Package(args.datei)
    paras = pkg.paragraphs()
    print(f"Datei: {os.path.basename(args.datei)} ({pkg.kind.upper()}), {len(paras)} Absätze")
    print("Legende: [Absatz-ID | Fundstelle] Text   (⟶ = Tab, ↵ = Zeilenumbruch)")
    print()
    found = {}
    scripts = []
    for p in paras:
        loc = p.location()
        if loc["fallback"]:
            continue  # duplicate of a text box (legacy VML rendering)
        if not p.text.strip() and not args.leere:
            continue
        where = describe(pkg, p, loc)
        shown = p.text.replace("\t", "⟶").replace("\n", "↵")
        print(f"[{p.id}{' | ' + where if where else ''}] {shown}")
        for m in TOKEN_RE.finditer(p.text):
            n_atoms = p.containers_in(m.start(), m.end())
            if m.group().startswith("[[SCRIPT:"):
                scripts.append((p.id, m.group()))
            else:
                found.setdefault(m.group(), []).append(p.id + (f" (über {n_atoms} Runs verteilt)" if n_atoms > 1 else ""))
    print()
    print(f"--- Platzhalter ({len(found)} verschiedene) ---")
    for k in sorted(found):
        print(f"{k}  ×{len(found[k])}  ({', '.join(found[k])})")
    print()
    print(f"--- Skripte ({len(scripts)}) ---")
    for pid, s in scripts:
        print(f"{pid}: {s}")
    return 0


# --------------------------------------------------------------------------------------------
# replace
# --------------------------------------------------------------------------------------------

def load_rules(args):
    if args.map:
        with open(args.map, encoding="utf-8") as f:
            rules = json.load(f)
    else:
        rules = []
    for pair in args.ersetze or []:
        rules.append({"find": pair[0], "replace": pair[1]})
    if not rules:
        raise UserError("Keine Ersetzungen angegeben (--map datei.json oder --ersetze SUCHE ERSATZ)")
    for r in rules:
        if "find" not in r or "replace" not in r:
            raise UserError(f"Regel ohne 'find'/'replace': {r}")
    return rules


def area_matches(rule, loc):
    wanted = rule.get("bereich", "alle")
    if wanted == "alle":
        return True
    return {"text": "Text", "kopf": "Kopfzeile", "fuss": "Fußzeile"}.get(wanted) == loc["area"]


def apply_rule(pkg, paras, rule):
    pattern = re.compile(rule["find"] if rule.get("regex") else re.escape(rule["find"]),
                         0 if rule.get("case", True) else re.IGNORECASE)
    occurrences = set(rule.get("vorkommen") or [])
    count = 0
    replaced = 0
    for p in paras:
        loc = p.location()
        if not area_matches(rule, loc):
            continue
        pos = 0
        while pos <= len(p.text):
            m = pattern.search(p.text, pos)
            if not m:
                break
            if m.end() == m.start():
                pos = m.end() + 1
                continue
            if not loc["fallback"]:
                count += 1
                if occurrences and count not in occurrences:
                    pos = m.end()
                    continue
            elif occurrences:
                pos = m.end()
                continue
            new = m.expand(rule["replace"]) if rule.get("regex") else rule["replace"]
            if p.replace_range(m.start(), m.end(), new):
                if not loc["fallback"]:
                    replaced += 1
                pos = m.start() + len(new)
            else:
                pos = m.end()
    return count, replaced


def cmd_replace(args):
    pkg = Package(args.eingabe)
    rules = load_rules(args)
    paras = pkg.paragraphs()
    missing = 0
    for r in rules:
        count, replaced = apply_rule(pkg, paras, r)
        label = r["find"] if len(r["find"]) <= 60 else r["find"][:57] + "..."
        status = "OK " if replaced else "-- "
        print(f"{status} {replaced}× ersetzt ({count} gefunden): {label!r} -> {r['replace']!r}")
        if not replaced:
            missing += 1
    pkg.save(args.ausgabe)
    print(f"\nGespeichert: {args.ausgabe}")
    if missing:
        print(f"Hinweis: {missing} Regel(n) ohne Treffer.")
    return 0


# --------------------------------------------------------------------------------------------
# normalize
# --------------------------------------------------------------------------------------------

def unwrap(el):
    parent = el.parentNode
    while el.firstChild is not None:
        parent.insertBefore(el.firstChild, el)
    parent.removeChild(el)


def normalize_pkg(pkg, accept_changes=False, unwrap_controls=False):
    stats = {"zusammengeführt": 0, "proofErr entfernt": 0, "Änderungen angenommen": 0, "Steuerelemente aufgelöst": 0}
    if pkg.kind == "docx":
        for part in pkg.text_parts():
            doc = pkg.dom(part)
            for name in ("proofErr", "lastRenderedPageBreak"):
                for el in list(doc.getElementsByTagNameNS(W, name)):
                    el.parentNode.removeChild(el)
                    stats["proofErr entfernt"] += name == "proofErr"
            if accept_changes:
                for name in ("del", "moveFrom"):
                    for el in list(doc.getElementsByTagNameNS(W, name)):
                        if el.parentNode is not None and not is_el(el.parentNode, W, "rPr"):
                            el.parentNode.removeChild(el)
                            stats["Änderungen angenommen"] += 1
                for name in ("ins", "moveTo"):
                    for el in list(doc.getElementsByTagNameNS(W, name)):
                        if el.parentNode is not None and not is_el(el.parentNode, W, "rPr"):
                            unwrap(el)
                            stats["Änderungen angenommen"] += 1
                for name in ("rPrChange", "pPrChange", "sectPrChange", "tblPrChange", "trPrChange", "tcPrChange",
                             "moveFromRangeStart", "moveFromRangeEnd", "moveToRangeStart", "moveToRangeEnd"):
                    for el in list(doc.getElementsByTagNameNS(W, name)):
                        el.parentNode.removeChild(el)
            if unwrap_controls:
                for el in list(doc.getElementsByTagNameNS(W, "sdt")):
                    content = element_children(el, W, "sdtContent")
                    if content:
                        unwrap(content[0])
                        for c in element_children(el, W):
                            if c.localName in ("sdtPr", "sdtEndPr"):
                                el.removeChild(c)
                        unwrap(el)
                        stats["Steuerelemente aufgelöst"] += 1
    for p in pkg.paragraphs():
        pos = 0
        while True:
            m = TOKEN_RE.search(p.text, pos)
            if not m:
                break
            if p.containers_in(m.start(), m.end()) > 1:
                p.replace_range(m.start(), m.end(), m.group())
                stats["zusammengeführt"] += 1
            pos = m.start() + len(m.group())
    return stats


def cmd_normalize(args):
    pkg = Package(args.eingabe)
    stats = normalize_pkg(pkg, args.aenderungen_annehmen, args.steuerelemente_aufloesen)
    pkg.save(args.ausgabe)
    for k, v in stats.items():
        if v:
            print(f"{k}: {v}")
    print(f"Gespeichert: {args.ausgabe}")
    return 0


# --------------------------------------------------------------------------------------------
# validate
# --------------------------------------------------------------------------------------------

class Catalog:
    def __init__(self, data, parties, forms):
        self.data = data
        self.fixed = {p["name"]: p for p in data["fixed"]}
        self.suffixes = {s["suffix"]: s for s in data["party_suffixes"]}
        self.suffix_order = sorted(self.suffixes, key=len, reverse=True)
        self.parties = parties
        self.forms = forms
        self.functions = {f["name"]: f for f in data["functions"]}
        self.tables = data["table_placeholders"]
        self.row_prefixes = tuple(data["row_placeholder_prefixes"])
        self.unsupported = data["documented_unsupported"]

    def classify(self, name):
        """Returns (status, detail); status: ok, alias, form, unknown_party, not_in_documents, unsupported, unknown."""
        if name in self.fixed:
            p = self.fixed[name]
            if not p["in_documents"]:
                return "not_in_documents", p["label"]
            if p["alias_of"]:
                return "alias", p["alias_of"]
            return "ok", p["label"]
        if name in self.unsupported:
            return "unsupported", self.unsupported[name]
        for f in self.forms:
            if name.startswith(f + "_"):
                return "form", f
        for suffix in self.suffix_order:
            if name.endswith("_" + suffix):
                prefix = name[:-len(suffix) - 1]
                s = self.suffixes[suffix]
                status = "alias" if s["alias_of"] else "ok"
                detail = f"{prefix}_{s['alias_of']}" if s["alias_of"] else s["label"]
                if prefix in self.parties:
                    return status, detail
                if prefix in self.fixed or not prefix:
                    break
                return "unknown_party", prefix
        return "unknown", None

    def known_script_name(self, name):
        st, _ = self.classify(name)
        return st in ("ok", "alias", "form")


class Report:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.infos = []

    def error(self, where, msg):
        self.errors.append(f"[{where}] {msg}")

    def warn(self, where, msg):
        self.warnings.append(f"[{where}] {msg}")

    def info(self, where, msg):
        self.infos.append(f"[{where}] {msg}" if where else msg)


def check_script(script, cat, rep, where):
    body = script.replace("„", '"').replace("“", '"').replace("”", '"')
    if "{{" in body or "}}" in body:
        rep.error(where, "Im Skript werden Platzhalter OHNE {{ }} geschrieben, z.B. MANDANT_NAME")
    i = 0
    stack = []  # function frames: [name, args, has_content]
    last_sig = ""
    n = len(body)
    while i < n:
        c = body[i]
        if c == '"':
            if body.startswith('"""', i):
                j = body.find('"""', i + 3)
                if j < 0:
                    rep.error(where, 'Skript: nicht geschlossener """-Text')
                    return
                literal = body[i + 3:j]
                i = j + 3
            elif body.startswith('""', i) and i + 2 < n and re.match(r"[A-Za-z_]", body[i + 2]):
                # ""PLACEHOLDER"" - placeholder with a multi-line value
                m = re.match(r'""([A-Za-z0-9_ÄÖÜ]+)""', body[i:])
                if not m:
                    rep.error(where, 'Skript: ""NAME"" nicht korrekt geschlossen')
                    return
                if not cat.known_script_name(m.group(1)):
                    rep.error(where, f"Skript: unbekannter Platzhalter {m.group(1)}")
                i += m.end()
                if stack:
                    stack[-1][2] = True
                last_sig = "v"
                continue
            else:
                j = i + 1
                while j < n and body[j] != '"':
                    j += 2 if body[j] == "\\" else 1
                if j >= n:
                    rep.error(where, "Skript: nicht geschlossenes Anführungszeichen")
                    return
                literal = body[i + 1:j]
                i = j + 1
            for name in re.findall(r"[A-Z][A-Z0-9]*_[A-Z0-9_]+", literal):
                if cat.known_script_name(name):
                    rep.error(where, f"Skript: der Platzhaltername {name} steht innerhalb eines Textes in "
                                     f"Anführungszeichen - der Server ersetzt ihn dort durch \"Wert\" und das "
                                     f"Skript bricht. Stattdessen verketten: \"…\" + {name} + \"…\"")
            if stack:
                stack[-1][2] = True
            last_sig = "v"
            continue
        if re.match(r"[A-Za-z_ÄÖÜäöüß]", c):
            m = re.match(r"[A-Za-z0-9_ÄÖÜäöüß]+", body[i:])
            ident = m.group()
            i += m.end()
            j = i
            while j < n and body[j] in " \t":
                j += 1
            is_call = j < n and body[j] == "("
            after_dot = last_sig == "."
            if is_call:
                if not after_dot and ident not in cat.functions:
                    rep.error(where, f"Skript: unbekannte Funktion {ident}(…)")
                if stack:
                    stack[-1][2] = True
                stack.append([ident if not after_dot else None, 0, False])
                i = j + 1
                last_sig = "("
                continue
            if not after_dot and NAME_RE.match(ident) and "_" in ident and not cat.known_script_name(ident):
                st, detail = cat.classify(ident)
                if st == "unknown_party":
                    rep.warn(where, f"Skript: {ident} - Beteiligtentyp-Präfix '{detail}' unbekannt (nur gültig, wenn er in der Kanzlei existiert)")
                else:
                    rep.error(where, f"Skript: unbekannter Platzhalter {ident} (führt zu SCRIPTFEHLER)")
            if stack:
                stack[-1][2] = True
            last_sig = "v"
            continue
        if c == "(":
            stack.append([None, 0, False])
            last_sig = "("
        elif c == ")":
            if not stack:
                rep.error(where, "Skript: schließende Klammer ohne öffnende")
                return
            name, commas, content = stack.pop()
            args = commas + 1 if content or commas else 0
            if name and name in cat.functions and args not in cat.functions[name]["arities"]:
                rep.error(where, f"Skript: {name} erwartet {' oder '.join(map(str, cat.functions[name]['arities']))} "
                                 f"Parameter, hat {args}")
            if stack:
                stack[-1][2] = True
            last_sig = "v"
        elif c == ",":
            if stack:
                stack[-1][1] += 1
            last_sig = ","
        elif c == ".":
            last_sig = "."
        elif not c.isspace():
            last_sig = c
        i += 1
    if stack:
        rep.error(where, "Skript: Klammern nicht ausgeglichen")


PII_PATTERNS = [
    ("IBAN", re.compile(r"\b[A-Z]{2}\d{2}(?: ?[A-Z0-9]{4}){3,7}(?: ?[A-Z0-9]{1,3})?\b")),
    ("E-Mail-Adresse", re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+")),
    ("Datum", re.compile(r"\b\d{1,2}\.\s?\d{1,2}\.\s?(?:\d{4}|\d{2})\b")),
    ("Geldbetrag", re.compile(r"\b\d{1,3}(?:\.\d{3})*,\d{2}\s?(?:€|EUR|Euro)")),
    ("Telefonnummer", re.compile(r"(?:\+49|\b0)\d{2,5}[ /-]?\d{3,}[ -]?\d*")),
    ("Aktenzeichen", re.compile(r"\b\d{1,5}\s?/\s?\d{2,4}\b|\b\d{1,3}\s[A-Z][a-zA-Z]{0,3}\s\d{1,5}/\d{2}\b")),
    ("PLZ + Ort", re.compile(r"\b\d{5}\s+[A-ZÄÖÜ][a-zäöüß]+")),
]


def validate_pkg(pkg, cat, check_pii=True):
    rep = Report()
    paras = pkg.paragraphs()
    seen_names = {}
    has_header_footer_text = False
    for p in paras:
        loc = p.location()
        if loc["fallback"]:
            continue
        where = p.id + (f" | {describe(pkg, p, loc)}" if describe(pkg, p, loc) else "")
        text = p.text
        if loc["area"] != "Text" and text.strip():
            has_header_footer_text = True

        # stray / malformed braces
        stripped = TOKEN_RE.sub(lambda m: "\0" * len(m.group()), text)
        for m in re.finditer(r"\{\{|\}\}|\[\[SCRIPT:", stripped):
            if m.group() == "[[SCRIPT:":
                rep.error(where, "Skript wird nicht im selben Absatz mit ]] geschlossen - Zeilen innerhalb eines Skripts "
                                 "nur mit Umschalt+Enter (Zeilenumbruch) trennen, nicht mit Enter")
            else:
                rep.error(where, f"Unvollständiger Platzhalter bei '{text[max(0, m.start() - 15):m.end() + 15]}'")
        for m in re.finditer(r"(?<![{])\{[A-Z_]{3,}\}(?![}])", text):
            rep.error(where, f"{m.group()} hat nur einfache geschweifte Klammern - richtig ist {{{m.group()}}}")

        for m in TOKEN_RE.finditer(text):
            token = m.group()
            atoms = p.atoms_in(m.start(), m.end())
            text_atoms = [a for a in atoms if a.kind == "text"]
            ctx = set().union(*[a.context for a in atoms]) if atoms else set()

            if pkg.kind == "docx":
                if len({id(a.container) for a in text_atoms}) > 1:
                    if loc["textbox"]:
                        rep.error(where, f"{token} ist in einer Textbox über mehrere Runs verteilt - wird nicht ersetzt (normalize ausführen)")
                    else:
                        rep.warn(where, f"{token} ist über {len(text_atoms)} Runs verteilt (normalize empfohlen)")
                if any(len(element_children(a.container, W, "t")) > 1 for a in text_atoms if a.container is not None):
                    rep.warn(where, f"{token} steht in einem Run mit mehreren Textelementen - normalize empfohlen")
                for wrapper, label in (("hyperlink", "einem Hyperlink"), ("sdt", "einem Inhaltssteuerelement"),
                                       ("ins", "einer nachverfolgten Änderung"), ("moveTo", "einer nachverfolgten Änderung"),
                                       ("fldSimple", "einem Feld")):
                    if wrapper in ctx:
                        rep.error(where, f"{token} steht in {label} - wird vom Server nicht gefunden")
            elif "a" in ctx:
                rep.warn(where, f"{token} steht in einem Hyperlink")

            if token.startswith("[[SCRIPT:"):
                check_script(token[9:-2], cat, rep, where)
                rest = text[m.end():]
                if rest.startswith("]"):
                    rep.error(where, "Skript enthält ']' - ein Skript endet beim ersten ']'")
                continue

            name = token[2:-2]
            if name != name.strip() or not NAME_RE.match(name.strip()):
                rep.error(where, f"{token}: Platzhalter nur aus GROSSBUCHSTABEN, Ziffern und _ ohne Leerzeichen")
                continue
            seen_names.setdefault(name, []).append(p.id)
            st, detail = cat.classify(name)
            if st == "alias":
                rep.warn(where, f"{token} ist ein veralteter Alias - besser {{{{{detail}}}}}")
            elif st == "unknown_party":
                rep.warn(where, f"{token}: Beteiligtentyp-Präfix '{detail}' ist kein Standardtyp - nur gültig, wenn er in der Kanzlei so angelegt ist (--parteien)")
            elif st == "not_in_documents":
                rep.error(where, f"{token} ({detail}) wird nur in E-Mail-/beA-Vorlagen ersetzt, nicht in Dokumenten")
            elif st == "unsupported":
                rep.error(where, f"{token}: {detail}")
            elif st == "unknown":
                rep.error(where, f"{token} ist kein bekannter Platzhalter (Falldatenblatt? dann --formulare PRÄFIX)")

            # table placeholders
            if name in cat.tables:
                spec = cat.tables[name]
                if loc["cell"] is None:
                    rep.error(where, f"{token} muss allein in einer Tabellenzelle stehen ({spec['note']})")
                else:
                    rows, cols, _, _, cell_text = table_info(pkg, loc["cell"])
                    if cell_text != token:
                        rep.error(where, f"{token} muss der einzige Inhalt seiner Tabellenzelle sein")
                    if spec["columns"] == [1] and (rows != 1 or cols != 1):
                        rep.error(where, f"{token} braucht eine 1×1-Tabelle (ist {rows}×{cols})")
                    elif cols not in spec["columns"] and not (rows == 1 and cols == 1):
                        rep.error(where, f"{token}: Tabelle hat {cols} Spalten - erlaubt: 1×1 oder {spec['columns'][-1]} Spalten")
                    if pkg.kind == "docx" and loc["table_depth"] > 1:
                        rep.error(where, f"{token} in verschachtelter Tabelle wird in DOCX nicht gefunden")
                if loc["area"] != "Text":
                    rep.error(where, f"{token} in Kopf-/Fußzeile wird nicht ersetzt")
            if name.startswith(cat.row_prefixes):
                if loc["cell"] is None:
                    rep.error(where, f"{token} gehört in eine Tabellenzeile (flexible Belegtabelle)")
                elif pkg.kind == "docx" and (loc["table_depth"] > 1 or loc["area"] != "Text"):
                    rep.error(where, f"{token}: flexible Belegtabelle nur in Tabellen des Haupttextes (nicht verschachtelt, nicht Kopf-/Fußzeile)")

            # automatic spacing
            nxt = text[m.end():m.end() + 1]
            if nxt and nxt not in NO_SPACE_AFTER and not nxt.isspace() and not text.startswith("{{", m.end()) \
                    and not text.startswith("[[", m.end()):
                rep.warn(where, f"Nach {token} folgt direkt '{nxt}' - der Server fügt dazwischen ein Leerzeichen ein")
            after = text[m.end():]
            sp = re.match(r" +(\{\{[^{}]*\}\})", after)
            if sp:
                rep.warn(where, f"Leerzeichen zwischen {token} und {sp.group(1)} - ist ein Wert leer, entsteht ein doppeltes "
                                f"Leerzeichen. Besser ohne Leerzeichen schreiben: {token}{sp.group(1)} (Leerzeichen kommt automatisch)")

        if check_pii and loc["area"] == "Text":
            plain = TOKEN_RE.sub(" ", text)
            for label, rx in PII_PATTERNS:
                for m in rx.finditer(plain):
                    rep.info(where, f"Mögliche konkrete Angabe ({label}): '{m.group().strip()}' - ggf. durch Platzhalter ersetzen")

    if pkg.kind == "docx":
        names = [i.filename for i in pkg.infos]
        for part in ("word/footnotes.xml", "word/endnotes.xml", "word/comments.xml"):
            if part in names and TOKEN_RE.search(re.sub(rb"<[^>]+>", b"", pkg.data[part]).decode("utf-8", "replace")):
                rep.error(part, "Platzhalter in Fuß-/Endnoten oder Kommentaren werden nicht ersetzt")
        doc_xml = pkg.data.get("word/document.xml", b"")
        if b"<w:ins " in doc_xml or b"<w:del " in doc_xml:
            rep.warn("Dokument", "Enthält nachverfolgte Änderungen - annehmen (normalize --aenderungen-annehmen)")
        if has_header_footer_text:
            rep.info("", "Kopf-/Fußzeilen enthalten Text. Hinweis: wird beim Erstellen ein Briefkopf (DOCX) gewählt, "
                         "übernimmt j-lawyer nur den Haupttext der Vorlage.")
    return rep, seen_names


def parse_list(value, default):
    if value is None:
        return list(default)
    return [v.strip().upper() for v in value.split(",") if v.strip()]


def cmd_validate(args):
    data = load_catalog()
    parties = parse_list(args.parteien, data["default_party_prefixes"])
    forms = parse_list(args.formulare, [])
    cat = Catalog(data, parties, forms)
    pkg = Package(args.datei)
    rep, names = validate_pkg(pkg, cat, not args.ohne_datenpruefung)
    print(f"Prüfung: {os.path.basename(args.datei)} ({pkg.kind.upper()}) - Beteiligtentypen: {', '.join(parties)}"
          + (f"; Falldatenblätter: {', '.join(forms)}" if forms else ""))
    print(f"{len(names)} verschiedene Platzhalter verwendet.")
    for title, items in (("FEHLER", rep.errors), ("WARNUNG", rep.warnings), ("HINWEIS", rep.infos)):
        if items:
            print(f"\n{title} ({len(items)}):")
            for it in items:
                print("  - " + it)
    if not rep.errors and not rep.warnings:
        print("\nKeine Fehler oder Warnungen.")
    return 1 if rep.errors else 0


# --------------------------------------------------------------------------------------------
# placeholders
# --------------------------------------------------------------------------------------------

def cmd_placeholders(args):
    data = load_catalog()
    q = (args.suche or "").lower()
    cat_filter = (args.kategorie or "").lower()
    rows = []
    for p in data["fixed"]:
        if not p["in_documents"] or p["alias_of"]:
            continue
        if cat_filter and cat_filter not in p["category"].lower():
            continue
        if q and q not in p["name"].lower() and q not in p["label"].lower():
            continue
        rows.append((p["category"], "{{" + p["name"] + "}}", p["label"]))
    if not cat_filter or cat_filter in "beteiligte":
        prefix = (args.partei or "###").upper()
        for s in data["party_suffixes"]:
            if s["alias_of"]:
                continue
            name = f"{prefix}_{s['suffix']}"
            if q and q not in name.lower() and q not in s["label"].lower():
                continue
            rows.append(("Beteiligte", "{{" + name + "}}", s["label"]))
    if not cat_filter or cat_filter in "skriptfunktionen":
        for f in data["functions"]:
            if q and q not in f["name"].lower():
                continue
            rows.append(("Skriptfunktion", f"{f['name']}({', '.join(f['params'])})",
                         "Parameter: " + "/".join(map(str, f["arities"]))))
    for r in rows:
        print(f"{r[0]:<20} {r[1]:<34} {r[2]}")
    if not rows:
        print("Keine Treffer.")
    elif "###" in "".join(r[1] for r in rows):
        print("\n### = Präfix des Beteiligtentyps (MANDANT, GEGNER, DRITTE oder kanzleieigene Typen); --partei MANDANT setzt ihn ein.")
    return 0


# --------------------------------------------------------------------------------------------
# build
# --------------------------------------------------------------------------------------------

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


INLINE_RE = re.compile(r"(\*\*|__|\*)")
ALIGN_RE = re.compile(r"^\[(links|rechts|zentriert|block)\]\s*")


def inline_runs(text):
    """Splits text into (segment, bold, italic, underline); placeholders/scripts are never split."""
    runs = []
    b = i = u = False
    pos = 0
    for m in TOKEN_RE.finditer(text):
        for seg in INLINE_RE.split(text[pos:m.start()]):
            if seg == "**":
                b = not b
            elif seg == "__":
                u = not u
            elif seg == "*":
                i = not i
            elif seg:
                runs.append((seg, b, i, u))
        runs.append((m.group(), b, i, u))
        pos = m.end()
    for seg in INLINE_RE.split(text[pos:]):
        if seg == "**":
            b = not b
        elif seg == "__":
            u = not u
        elif seg == "*":
            i = not i
        elif seg:
            runs.append((seg, b, i, u))
    return runs


def parse_spec(src):
    """Parses the Markdown subset into (settings, blocks)."""
    settings = {"schrift": "Arial", "groesse": "11", "raender": "2.0,2.0,2.0,2.5", "absatzabstand": "6",
                "kopfzeile": "", "fusszeile": "", "zeilenabstand": "1.0"}
    lines = src.replace("\r\n", "\n").split("\n")
    if lines and lines[0].strip() == "---":
        end = lines.index("---", 1)
        for line in lines[1:end]:
            if ":" in line:
                k, v = line.split(":", 1)
                settings[k.strip().lower()] = v.strip()
        lines = lines[end + 1:]
    blocks = []
    para = []
    widths = None

    def flush():
        if para:
            text = ""
            for l in para:
                text += l[:-1] + "\n" if l.endswith("\\") else l + " "
            text = text.rstrip(" ")
            align = None
            m = ALIGN_RE.match(text)
            if m:
                align = m.group(1)
                text = text[m.end():]
            blocks.append({"type": "p", "text": text, "align": align})
            para.clear()

    i = 0
    while i < len(lines):
        line = lines[i]
        s = line.strip()
        if not s:
            flush()
        elif s.startswith("#"):
            flush()
            level = len(s) - len(s.lstrip("#"))
            blocks.append({"type": "h", "level": min(level, 3), "text": s[level:].strip()})
        elif s in ("\\seitenumbruch", "\\pagebreak"):
            flush()
            blocks.append({"type": "pagebreak"})
        elif s == "\\leer":
            flush()
            blocks.append({"type": "p", "text": "", "align": None})
        elif s.startswith("\\spalten"):
            flush()
            widths = [float(x) for x in s.split(None, 1)[1].split(",")]
        elif s.startswith("|"):
            flush()
            rows = []
            header = False
            while i < len(lines) and lines[i].strip().startswith("|"):
                row = lines[i].strip().strip("|")
                cells = [c.strip() for c in re.split(r"(?<!\\)\|", row)]
                if all(re.match(r"^:?-{2,}:?$", c) for c in cells if c):
                    header = True
                else:
                    rows.append(cells)
                i += 1
            blocks.append({"type": "table", "rows": rows, "header": header, "widths": widths})
            widths = None
            continue
        else:
            para.append(s)
        i += 1
    flush()
    return settings, blocks


def cm_to_twips(cm):
    return int(round(float(cm) / 2.54 * 1440))


def docx_run(seg, b, i, u):
    props = ("<w:b/>" if b else "") + ("<w:i/>" if i else "") + ('<w:u w:val="single"/>' if u else "")
    rpr = f"<w:rPr>{props}</w:rPr>" if props else ""
    out = []
    for part in re.split(r"(\n|\t)", seg):
        if part == "\n":
            out.append(f"<w:r>{rpr}<w:br/></w:r>")
        elif part == "\t":
            out.append(f"<w:r>{rpr}<w:tab/></w:r>")
        elif part:
            out.append(f'<w:r>{rpr}<w:t xml:space="preserve">{esc(part)}</w:t></w:r>')
    return "".join(out)


def docx_para(text, style=None, align=None, bold_all=False):
    jc = {"rechts": "right", "zentriert": "center", "block": "both", "links": "left"}.get(align)
    ppr = ""
    if style or jc:
        ppr = "<w:pPr>" + (f'<w:pStyle w:val="{style}"/>' if style else "") + (f'<w:jc w:val="{jc}"/>' if jc else "") + "</w:pPr>"
    runs = "".join(docx_run(seg, b or bold_all, i, u) for seg, b, i, u in inline_runs(text))
    return f"<w:p>{ppr}{runs}</w:p>"


def build_docx(settings, blocks, out):
    top, right, bottom, left = [cm_to_twips(x) for x in settings["raender"].split(",")]
    width_twips = 11906 - left - right
    body = []
    for blk in blocks:
        if blk["type"] == "p":
            body.append(docx_para(blk["text"], align=blk["align"]))
        elif blk["type"] == "h":
            body.append(docx_para(blk["text"], style=f"Heading{blk['level']}"))
        elif blk["type"] == "pagebreak":
            body.append('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
        elif blk["type"] == "table":
            ncols = max(len(r) for r in blk["rows"])
            widths = blk["widths"] or [100.0 / ncols] * ncols
            total = sum(widths)
            tw = [int(width_twips * w / total) for w in widths]
            grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in tw)
            rows = []
            for ri, row in enumerate(blk["rows"]):
                cells = []
                for ci in range(ncols):
                    txt = row[ci] if ci < len(row) else ""
                    cells.append(f'<w:tc><w:tcPr><w:tcW w:w="{tw[ci]}" w:type="dxa"/></w:tcPr>'
                                 f'{docx_para(txt, bold_all=blk["header"] and ri == 0)}</w:tc>')
                hdr = "<w:trPr><w:tblHeader/></w:trPr>" if blk["header"] and ri == 0 else ""
                rows.append(f"<w:tr>{hdr}{''.join(cells)}</w:tr>")
            body.append(f'<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="{sum(tw)}" w:type="dxa"/>'
                        f'<w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>{grid}</w:tblGrid>{"".join(rows)}</w:tbl>')
            body.append("<w:p/>")
    rels = ['<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>']
    overrides = []
    hf_refs = ""
    hf_parts = {}
    for key, kind, rid in (("kopfzeile", "header", "rId2"), ("fusszeile", "footer", "rId3")):
        if settings.get(key):
            content = "".join(docx_para(line) for line in settings[key].split(" | "))
            tag = "hdr" if kind == "header" else "ftr"
            hf_parts[f"word/{kind}1.xml"] = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
                                             f'<w:{tag} xmlns:w="{W}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">{content}</w:{tag}>')
            rels.append(f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/{kind}" Target="{kind}1.xml"/>')
            overrides.append(f'<Override PartName="/word/{kind}1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.{kind}+xml"/>')
            hf_refs += f'<w:{kind}Reference w:type="default" r:id="{rid}"/>'
    sect = (f'<w:sectPr>{hf_refs}<w:pgSz w:w="11906" w:h="16838"/>'
            f'<w:pgMar w:top="{top}" w:right="{right}" w:bottom="{bottom}" w:left="{left}" w:header="709" w:footer="709" w:gutter="0"/></w:sectPr>')
    document = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
                f'<w:document xmlns:w="{W}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
                f'<w:body>{"".join(body)}{sect}</w:body></w:document>')
    font = esc(settings["schrift"])
    size = int(float(settings["groesse"]) * 2)
    after = int(float(settings["absatzabstand"]) * 20)
    line = int(float(settings["zeilenabstand"]) * 240)

    def heading(level, factor):
        return (f'<w:style w:type="paragraph" w:styleId="Heading{level}"><w:name w:val="heading {level}"/>'
                f'<w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/>'
                f'<w:spacing w:before="240" w:after="120"/><w:outlineLvl w:val="{level - 1}"/></w:pPr>'
                f'<w:rPr><w:b/><w:sz w:val="{int(size * factor)}"/></w:rPr></w:style>')
    styles = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<w:styles xmlns:w="{W}">'
              f'<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="{font}" w:hAnsi="{font}" w:cs="{font}" w:eastAsia="{font}"/>'
              f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/><w:lang w:val="de-DE"/></w:rPr></w:rPrDefault>'
              f'<w:pPrDefault><w:pPr><w:spacing w:after="{after}" w:line="{line}" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>'
              f'<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>'
              + heading(1, 1.4) + heading(2, 1.2) + heading(3, 1.0) +
              f'<w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/><w:pPr><w:spacing w:after="0"/></w:pPr><w:tblPr>'
              f'<w:tblBorders><w:top w:val="single" w:sz="4" w:space="0" w:color="auto"/><w:left w:val="single" w:sz="4" w:space="0" w:color="auto"/>'
              f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="auto"/><w:right w:val="single" w:sz="4" w:space="0" w:color="auto"/>'
              f'<w:insideH w:val="single" w:sz="4" w:space="0" w:color="auto"/><w:insideV w:val="single" w:sz="4" w:space="0" w:color="auto"/></w:tblBorders>'
              f'<w:tblCellMar><w:left w:w="108" w:type="dxa"/><w:right w:w="108" w:type="dxa"/></w:tblCellMar></w:tblPr></w:style></w:styles>')
    content_types = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
                     '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
                     '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
                     '<Default Extension="xml" ContentType="application/xml"/>'
                     '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
                     '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>'
                     + "".join(overrides) + '</Types>')
    root_rels = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
                 '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
                 '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>'
                 '</Relationships>')
    doc_rels = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
                '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' + "".join(rels) + '</Relationships>')
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", root_rels)
        z.writestr("word/document.xml", document)
        z.writestr("word/styles.xml", styles)
        z.writestr("word/_rels/document.xml.rels", doc_rels)
        for name, xml in hf_parts.items():
            z.writestr(name, xml)


ODF_NS = ('xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" '
          'xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" '
          'xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" '
          'xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" '
          'xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" '
          'xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" office:version="1.3"')


def odt_inline(text):
    out = []
    for seg, b, i, u in inline_runs(text):
        parts = []
        for part in re.split(r"(\n|\t| {2,})", seg):
            if part == "\n":
                parts.append("<text:line-break/>")
            elif part == "\t":
                parts.append("<text:tab/>")
            elif part.startswith("  "):
                parts.append(f' <text:s text:c="{len(part) - 1}"/>')
            elif part:
                parts.append(esc(part))
        content = "".join(parts)
        style = "T" + ("b" if b else "") + ("i" if i else "") + ("u" if u else "")
        out.append(f'<text:span text:style-name="{style}">{content}</text:span>' if style != "T" else content)
    return "".join(out)


def build_odt(settings, blocks, out):
    top, right, bottom, left = [float(x) for x in settings["raender"].split(",")]
    width_cm = 21.0 - left - right
    body = []
    auto = []
    for name in ("Tb", "Ti", "Tu", "Tbi", "Tbu", "Tiu", "Tbiu"):
        props = ('fo:font-weight="bold" ' if "b" in name else "") + ('fo:font-style="italic" ' if "i" in name else "") \
            + ('style:text-underline-style="solid" style:text-underline-width="auto" style:text-underline-color="font-color" ' if "u" in name else "")
        auto.append(f'<style:style style:name="{name}" style:family="text"><style:text-properties {props}/></style:style>')
    for name, val in (("Pleft", "start"), ("Pright", "end"), ("Pcenter", "center"), ("Pjustify", "justify")):
        auto.append(f'<style:style style:name="{name}" style:family="paragraph" style:parent-style-name="Standard">'
                    f'<style:paragraph-properties fo:text-align="{val}"/></style:style>')
    auto.append('<style:style style:name="Pbreak" style:family="paragraph" style:parent-style-name="Standard">'
                '<style:paragraph-properties fo:break-before="page"/></style:style>')
    auto.append('<style:style style:name="Cell" style:family="table-cell"><style:table-cell-properties '
                'fo:padding="0.1cm" fo:border="0.5pt solid #000000"/></style:style>')
    tcount = 0
    pending_break = False
    for blk in blocks:
        if blk["type"] == "pagebreak":
            pending_break = True
            continue
        if blk["type"] == "p":
            style = {"links": "Pleft", "rechts": "Pright", "zentriert": "Pcenter", "block": "Pjustify"}.get(blk["align"], "Standard")
            if pending_break:
                style, pending_break = "Pbreak", False
            body.append(f'<text:p text:style-name="{style}">{odt_inline(blk["text"])}</text:p>')
        elif blk["type"] == "h":
            if pending_break:
                body.append('<text:p text:style-name="Pbreak"/>')
                pending_break = False
            body.append(f'<text:h text:style-name="Heading_20_{blk["level"]}" text:outline-level="{blk["level"]}">{odt_inline(blk["text"])}</text:h>')
        elif blk["type"] == "table":
            if pending_break:
                body.append('<text:p text:style-name="Pbreak"/>')
                pending_break = False
            tcount += 1
            ncols = max(len(r) for r in blk["rows"])
            widths = blk["widths"] or [100.0 / ncols] * ncols
            total = sum(widths)
            tname = f"Tabelle{tcount}"
            auto.append(f'<style:style style:name="{tname}" style:family="table"><style:table-properties '
                        f'style:width="{width_cm:.2f}cm" table:align="left"/></style:style>')
            cols = []
            for ci, w in enumerate(widths):
                auto.append(f'<style:style style:name="{tname}.C{ci}" style:family="table-column"><style:table-column-properties '
                            f'style:column-width="{width_cm * w / total:.2f}cm"/></style:style>')
                cols.append(f'<table:table-column table:style-name="{tname}.C{ci}"/>')
            rows = []
            for ri, row in enumerate(blk["rows"]):
                cells = []
                for ci in range(ncols):
                    txt = row[ci] if ci < len(row) else ""
                    if blk["header"] and ri == 0 and txt:
                        txt = f"**{txt}**"
                    cells.append(f'<table:table-cell table:style-name="Cell" office:value-type="string">'
                                 f'<text:p text:style-name="Table_20_Contents">{odt_inline(txt)}</text:p></table:table-cell>')
                row_xml = f"<table:table-row>{''.join(cells)}</table:table-row>"
                rows.append(f"<table:table-header-rows>{row_xml}</table:table-header-rows>" if blk["header"] and ri == 0 else row_xml)
            body.append(f'<table:table table:name="{tname}" table:style-name="{tname}">{"".join(cols)}{"".join(rows)}</table:table>')
            body.append('<text:p text:style-name="Standard"/>')
    content = (f'<?xml version="1.0" encoding="UTF-8"?>\n<office:document-content {ODF_NS}>'
               f'<office:font-face-decls/><office:automatic-styles>{"".join(auto)}</office:automatic-styles>'
               f'<office:body><office:text>{"".join(body)}</office:text></office:body></office:document-content>')
    font = esc(settings["schrift"])
    size = settings["groesse"]
    after = float(settings["absatzabstand"]) * 0.0353
    line = int(float(settings["zeilenabstand"]) * 100)

    def hf(key, tag):
        if not settings.get(key):
            return ""
        paras = "".join(f'<text:p text:style-name="Standard">{odt_inline(l)}</text:p>' for l in settings[key].split(" | "))
        return f"<style:{tag}>{paras}</style:{tag}>"

    styles = (f'<?xml version="1.0" encoding="UTF-8"?>\n<office:document-styles {ODF_NS}>'
              f'<office:font-face-decls><style:font-face style:name="{font}" svg:font-family="{font}"/></office:font-face-decls>'
              f'<office:styles>'
              f'<style:default-style style:family="paragraph"><style:paragraph-properties fo:margin-top="0cm" fo:margin-bottom="{after:.3f}cm" fo:line-height="{line}%"/>'
              f'<style:text-properties style:font-name="{font}" fo:font-size="{size}pt" fo:language="de" fo:country="DE"/></style:default-style>'
              f'<style:style style:name="Standard" style:family="paragraph" style:class="text"/>'
              f'<style:style style:name="Table_20_Contents" style:display-name="Table Contents" style:family="paragraph" style:parent-style-name="Standard">'
              f'<style:paragraph-properties fo:margin-bottom="0cm"/></style:style>'
              + "".join(f'<style:style style:name="Heading_20_{lv}" style:display-name="Heading {lv}" style:family="paragraph" '
                        f'style:parent-style-name="Standard" style:next-style-name="Standard" style:default-outline-level="{lv}" style:class="text">'
                        f'<style:paragraph-properties fo:margin-top="0.42cm" fo:margin-bottom="0.21cm" fo:keep-with-next="always"/>'
                        f'<style:text-properties fo:font-size="{float(size) * f:.1f}pt" fo:font-weight="bold"/></style:style>'
                        for lv, f in ((1, 1.4), (2, 1.2), (3, 1.0))) +
              f'</office:styles><office:automatic-styles><style:page-layout style:name="pm1">'
              f'<style:page-layout-properties fo:page-width="21cm" fo:page-height="29.7cm" style:print-orientation="portrait" '
              f'fo:margin-top="{top}cm" fo:margin-bottom="{bottom}cm" fo:margin-left="{left}cm" fo:margin-right="{right}cm"/>'
              f'<style:header-style><style:header-footer-properties fo:min-height="0cm" fo:margin-bottom="0.5cm"/></style:header-style>'
              f'<style:footer-style><style:header-footer-properties fo:min-height="0cm" fo:margin-top="0.5cm"/></style:footer-style>'
              f'</style:page-layout></office:automatic-styles><office:master-styles>'
              f'<style:master-page style:name="Standard" style:page-layout-name="pm1">{hf("kopfzeile", "header")}{hf("fusszeile", "footer")}</style:master-page>'
              f'</office:master-styles></office:document-styles>')
    manifest = ('<?xml version="1.0" encoding="UTF-8"?>\n'
                '<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">'
                '<manifest:file-entry manifest:full-path="/" manifest:version="1.3" manifest:media-type="application/vnd.oasis.opendocument.text"/>'
                '<manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>'
                '<manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>'
                '</manifest:manifest>')
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(zipfile.ZipInfo("mimetype"), "application/vnd.oasis.opendocument.text", compress_type=zipfile.ZIP_STORED)
        z.writestr("META-INF/manifest.xml", manifest)
        z.writestr("content.xml", content)
        z.writestr("styles.xml", styles)


def cmd_build(args):
    with open(args.beschreibung, encoding="utf-8") as f:
        settings, blocks = parse_spec(f.read())
    ext = os.path.splitext(args.ausgabe)[1].lower()
    if ext == ".docx":
        build_docx(settings, blocks, args.ausgabe)
    elif ext == ".odt":
        build_odt(settings, blocks, args.ausgabe)
    else:
        raise UserError("Ausgabe muss auf .docx oder .odt enden")
    print(f"Erzeugt: {args.ausgabe} ({len(blocks)} Blöcke)")
    return 0


# --------------------------------------------------------------------------------------------

def main(argv=None):
    ap = argparse.ArgumentParser(prog="jlvorlage", description="Werkzeug für j-lawyer.org-Dokumentvorlagen (DOCX/ODT)")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("inspect", help="Text je Absatz mit Fundstelle, Platzhalter und Skripte anzeigen")
    p.add_argument("datei")
    p.add_argument("--leere", action="store_true", help="auch leere Absätze anzeigen")
    p.set_defaults(func=cmd_inspect)

    p = sub.add_parser("replace", help="Text ersetzen (auch über Formatierungsgrenzen hinweg)")
    p.add_argument("eingabe")
    p.add_argument("ausgabe")
    p.add_argument("--map", help='JSON-Liste: [{"find": "...", "replace": "...", "regex": false, "vorkommen": [1], "bereich": "alle|text|kopf|fuss"}]')
    p.add_argument("--ersetze", nargs=2, action="append", metavar=("SUCHE", "ERSATZ"))
    p.set_defaults(func=cmd_replace)

    p = sub.add_parser("normalize", help="jeden Platzhalter/jedes Skript in genau einen Run legen")
    p.add_argument("eingabe")
    p.add_argument("ausgabe")
    p.add_argument("--aenderungen-annehmen", action="store_true", help="nachverfolgte Änderungen annehmen (DOCX)")
    p.add_argument("--steuerelemente-aufloesen", action="store_true", help="Inhaltssteuerelemente (w:sdt) auflösen (DOCX)")
    p.set_defaults(func=cmd_normalize)

    p = sub.add_parser("validate", help="Vorlage prüfen")
    p.add_argument("datei")
    p.add_argument("--parteien", help="Platzhalter-Präfixe der Beteiligtentypen, Standard: MANDANT,GEGNER,DRITTE")
    p.add_argument("--formulare", help="Platzhalter-Präfixe verwendeter Falldatenblätter, z.B. KSCHUTZ,UKBEA")
    p.add_argument("--ohne-datenpruefung", action="store_true", help="keine Suche nach verbliebenen konkreten Angaben")
    p.set_defaults(func=cmd_validate)

    p = sub.add_parser("build", help="neue Vorlage aus Markdown-Beschreibung erzeugen")
    p.add_argument("beschreibung")
    p.add_argument("ausgabe")
    p.set_defaults(func=cmd_build)

    p = sub.add_parser("placeholders", help="Platzhalterkatalog durchsuchen")
    p.add_argument("--suche")
    p.add_argument("--kategorie")
    p.add_argument("--partei", help="Präfix für Beteiligten-Platzhalter, z.B. MANDANT")
    p.set_defaults(func=cmd_placeholders)

    args = ap.parse_args(argv)
    try:
        return args.func(args)
    except UserError as e:
        print(f"Fehler: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
