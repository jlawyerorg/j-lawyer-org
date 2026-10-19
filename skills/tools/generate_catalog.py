#!/usr/bin/env python3
"""Generates the placeholder catalog of the j-lawyer-vorlagen skill from the server sources.

Reads PlaceHolders.java (constants + @PlaceHolderInfo annotations, getAllPlaceHolders and
ALLTABLEPLACEHOLDERS) and smarttemplate.groovy (script functions) and writes
- skills/j-lawyer-vorlagen/scripts/catalog.json
- the generated section of skills/j-lawyer-vorlagen/references/platzhalter.md

Run from anywhere: python3 skills/tools/generate_catalog.py
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PLACEHOLDERS_JAVA = os.path.join(ROOT, "j-lawyer-server-common/src/main/java/com/jdimension/jlawyer/documents/PlaceHolders.java")
CATEGORY_JAVA = os.path.join(ROOT, "j-lawyer-server-common/src/main/java/com/jdimension/jlawyer/documents/PlaceHolderCategory.java")
SMART_GROOVY = os.path.join(ROOT, "j-lawyer-server/j-lawyer-server-ejb/src/main/resources/templates/smart/smarttemplate.groovy")
SKILL = os.path.join(ROOT, "skills/j-lawyer-vorlagen")
CATALOG_JSON = os.path.join(SKILL, "scripts/catalog.json")
PLATZHALTER_MD = os.path.join(SKILL, "references/platzhalter.md")

# default party types, see j-lawyer-server/setup/create_database.sql (table party_types)
DEFAULT_PARTY_PREFIXES = ["MANDANT", "GEGNER", "DRITTE"]

# structural requirements of table placeholders - not derivable from PlaceHolders.java, see
# MicrosoftOfficeAccess / LibreOfficeAccess and the column headers built in InvoiceDialog
TABLE_PLACEHOLDERS = {
    "TABELLE_1": {"columns": [1], "note": "Ergebnistabelle aus Berechnungs-Plugins (z.B. RVG)"},
    "BEL_GIROCODE": {"columns": [1], "note": "GiroCode-Grafik des Belegs"},
    "BEL_TABELLE": {"columns": [1, 6], "note": "Positionstabelle, 1x1 oder exakt 6 Spalten"},
    "ZE_TABELLE": {"columns": [1, 6], "note": "Leistungsnachweis, 1x1 oder exakt 6 Spalten"},
    "ZE_SUMMEN": {"columns": [1, 3], "note": "Zeitsummen pro Person, 1x1 oder exakt 3 Spalten"},
}
ROW_PLACEHOLDER_PREFIXES = ["BELP_", "BEL_UST_"]

# named in the user manual, but not resolved by the server
DOCUMENTED_UNSUPPORTED = {
    "AUTOR_AN": "im Handbuch genannt, wird vom Server aber nicht ersetzt",
    "AUTOR_KRZ": "im Handbuch genannt, wird vom Server aber nicht ersetzt",
    "AUTOR_EMAIL": "im Handbuch genannt, wird vom Server aber nicht ersetzt",
}

BEGIN_MARK = "<!-- BEGIN GENERATED: generate_catalog.py -->"
END_MARK = "<!-- END GENERATED -->"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def parse_categories(src):
    return dict(re.findall(r'^\s*([A-Z_]+)\("([^"]+)"\)', src, re.M))


def parse_placeholders(src, categories):
    pattern = re.compile(
        r'@PlaceHolderInfo\(\s*category\s*=\s*PlaceHolderCategory\.(\w+)\s*,\s*label\s*=\s*"([^"]*)"'
        r'(?:\s*,\s*aliasOf\s*=\s*(\w+))?\s*\)\s*public static final String (\w+)\s*=\s*"\{\{([^}]+)\}\}"')
    consts = {}
    for cat, label, alias_const, const, key in pattern.findall(src):
        consts[const] = {"key": key, "category": categories.get(cat, cat), "label": label,
                         "alias_of_const": alias_const or None}
    return consts


def constants_used_in(src, start_marker, end_marker):
    start = src.index(start_marker)
    end = src.index(end_marker, start)
    return set(re.findall(r'\b([A-Z_][A-Z0-9_]*)\b', src[start:end]))


def parse_functions(src):
    funcs = {}
    for name, params in re.findall(r'^\s*String ([A-ZÄÖÜ]+)\(([^)]*)\)', src, re.M):
        names = [p.strip().split()[-1] for p in params.split(",") if p.strip()]
        f = funcs.setdefault(name, {"name": name, "arities": [], "params": []})
        f["arities"].append(len(names))
        if len(names) > len(f["params"]):
            f["params"] = names
    for f in funcs.values():
        f["arities"].sort()
    return sorted(funcs.values(), key=lambda f: f["name"])


def build_catalog():
    src = read(PLACEHOLDERS_JAVA)
    consts = parse_placeholders(src, parse_categories(read(CATEGORY_JAVA)))
    in_documents = constants_used_in(src, "getAllPlaceHolders(", "return ALLPLACEHOLDERS;")
    in_documents |= constants_used_in(src, "ALLTABLEPLACEHOLDERS = new", "public static String insertAt")

    fixed, suffixes = [], []
    for const, c in consts.items():
        alias_of = consts[c["alias_of_const"]]["key"] if c["alias_of_const"] else None
        if c["key"].startswith("###_"):
            suffixes.append({"suffix": c["key"][4:], "label": c["label"],
                             "alias_of": alias_of[4:] if alias_of else None,
                             "in_documents": const in in_documents})
        else:
            fixed.append({"name": c["key"], "category": c["category"], "label": c["label"],
                          "alias_of": alias_of, "in_documents": const in in_documents})

    return {
        "_comment": "GENERIERT von skills/tools/generate_catalog.py - nicht von Hand bearbeiten",
        "fixed": fixed,
        "party_suffixes": suffixes,
        "default_party_prefixes": DEFAULT_PARTY_PREFIXES,
        "table_placeholders": TABLE_PLACEHOLDERS,
        "row_placeholder_prefixes": ROW_PLACEHOLDER_PREFIXES,
        "documented_unsupported": DOCUMENTED_UNSUPPORTED,
        "functions": parse_functions(read(SMART_GROOVY)),
    }


def markdown(catalog):
    out = [BEGIN_MARK, ""]
    by_cat = {}
    for p in catalog["fixed"]:
        if p["in_documents"]:
            by_cat.setdefault(p["category"], []).append(p)
    for cat, items in by_cat.items():
        out += [f"### {cat}", "", "| Platzhalter | Bedeutung |", "|---|---|"]
        for p in items:
            label = p["label"] + (f" – Alias von `{{{{{p['alias_of']}}}}}`, nicht verwenden" if p["alias_of"] else "")
            out.append(f"| `{{{{{p['name']}}}}}` | {label} |")
        out.append("")

    out += ["### Beteiligte (Präfix `###` = Platzhalter des Beteiligtentyps, z.B. MANDANT, GEGNER, DRITTE)", "",
            "| Platzhalter | Bedeutung |", "|---|---|"]
    for s in catalog["party_suffixes"]:
        label = s["label"] + (f" – Alias von `_{s['alias_of']}`, nicht verwenden" if s["alias_of"] else "")
        out.append(f"| `{{{{###_{s['suffix']}}}}}` | {label} |")
    out.append("")

    not_in_docs = [p for p in catalog["fixed"] if not p["in_documents"]]
    if not_in_docs:
        out += ["### Nicht in Dokumentvorlagen nutzbar", ""]
        for p in not_in_docs:
            out.append(f"- `{{{{{p['name']}}}}}` – {p['label']} (nur E-Mail-/beA-Textbausteine)")
        out.append("")
    for name, note in catalog["documented_unsupported"].items():
        out.append(f"- `{{{{{name}}}}}` – {note}")
    out += ["", "### Skriptfunktionen (Signaturen)", ""]
    for f in catalog["functions"]:
        out.append(f"- `{f['name']}({', '.join(f['params'])})` – Parameteranzahl: {', '.join(map(str, f['arities']))}")
    out += ["", END_MARK]
    return "\n".join(out)


def main():
    catalog = build_catalog()
    os.makedirs(os.path.dirname(CATALOG_JSON), exist_ok=True)
    with open(CATALOG_JSON, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)
        f.write("\n")

    md = read(PLATZHALTER_MD)
    if BEGIN_MARK not in md or END_MARK not in md:
        sys.exit(f"Marker fehlen in {PLATZHALTER_MD}")
    head = md[:md.index(BEGIN_MARK)]
    tail = md[md.index(END_MARK) + len(END_MARK):]
    with open(PLATZHALTER_MD, "w", encoding="utf-8") as f:
        f.write(head + markdown(catalog) + tail)

    print(f"{len(catalog['fixed'])} feste Platzhalter, {len(catalog['party_suffixes'])} Beteiligten-Suffixe, "
          f"{len(catalog['functions'])} Skriptfunktionen -> {os.path.relpath(CATALOG_JSON, ROOT)}")


if __name__ == "__main__":
    main()
