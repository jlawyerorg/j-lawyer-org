---
name: j-lawyer-vorlagen
description: Creates, derives and edits j-lawyer.org document templates (Dokumentvorlagen) in DOCX and ODT format with j-lawyer placeholders such as {{MANDANT_NAME}}, {{AKTE_ZEICHEN}} and [[SCRIPT:...]] logic. Use when the user wants a new j-lawyer template (Brief-, Schriftsatz-, Rechnungsvorlage), wants to turn an example letter/document into a template by replacing concrete names, addresses, dates, file numbers etc. with placeholders, or wants to change, fix or check an existing j-lawyer template. Trigger on "j-lawyer", "Vorlage", "Dokumentvorlage", "Platzhalter", "Briefvorlage", "Schriftsatzvorlage", "Rechnungsvorlage", or a .docx/.odt containing {{...}} placeholders.
---

# j-lawyer document templates (DOCX / ODT)

j-lawyer.org fills templates on the server when a user creates a document from a case: every
`{{PLACEHOLDER}}` is replaced by case / party / office data, every `[[SCRIPT:…]]` is evaluated.
This skill builds such templates and makes sure the server will actually resolve them.

**Talk to the user in German** (they are German law-firm staff). Keep technical details (runs, XML)
out of the conversation unless asked.

## Tool

All work goes through `scripts/jlvorlage.py` (Python 3, standard library only). `SKILL_DIR` below
means the directory containing this file.

| Command | Purpose |
|---|---|
| `python3 SKILL_DIR/scripts/jlvorlage.py inspect IN` | paragraph text with location (table, header/footer, text box), placeholders and scripts found |
| `… replace IN OUT --map map.json` | replace text, also across formatting boundaries; each replacement lands in one run with the formatting of the original text |
| `… normalize IN OUT` | put every placeholder/script into a single run (fixes what Word splits); `--aenderungen-annehmen`, `--steuerelemente-aufloesen` for DOCX tracked changes / content controls |
| `… validate IN [--parteien MANDANT,GEGNER,…] [--formulare PREFIX,…]` | check against the server's rules; exit code 1 on errors |
| `… build SPEC.md OUT.docx/.odt` | create a new template from a Markdown subset (see references/muster.md) |
| `… placeholders --suche TEXT [--partei MANDANT]` | search the placeholder catalog |

Never overwrite the input file; always write a new output file. Run `validate` on every result
before handing it over and fix all FEHLER; explain remaining WARNUNGEN to the user briefly.

## References (read when needed)

- `references/platzhalter.md` – full placeholder catalog generated from the server source (authoritative,
  partly deviates from the user manual), party/case-form prefixes, table placeholders, invoice table.
- `references/skripte.md` – `[[SCRIPT:…]]` syntax and every function (WENNGLEICH, MWDJU, FRIST, …).
- `references/engine-regeln.md` – how the server processes templates; what breaks resolution.
- `references/muster.md` – `build` input format and reusable building blocks (address block,
  salutation, reference line, deadlines, invoice table).

## Core rules

1. Use only placeholders from the catalog. Party placeholders are `<PREFIX>_<FIELD>`; default prefixes
   are MANDANT, GEGNER, DRITTE – other party types (e.g. VERSICHERUNG) and case-form (Falldatenblatt)
   prefixes exist only if the firm created them: ask the user, then pass them to `validate`.
2. The server inserts a space after each value unless followed by `. , ; : ! ? ' " / ) ] -` or a
   space, so `{{MANDANT_VORNAME}}{{MANDANT_NAME}}` and `{{MANDANT_VORNAME}} {{MANDANT_NAME}}` both
   work. Never put a placeholder that may be empty between two spaces (double space): write
   `{{MANDANT_AGRAD1}}{{MANDANT_NAME}}`, not `{{MANDANT_AGRAD1}} {{MANDANT_NAME}}` after a space.
3. Prefer current names over aliases: `_BEGRUESSUNG` (not `_ANREDE`), `_ANREDE1` (not `_TITEL`),
   `_UNTERNEHMEN` (not `_FIRMA`).
4. Table placeholders (`TABELLE_1`, `BEL_TABELLE`, `ZE_TABELLE`, `ZE_SUMMEN`, `BEL_GIROCODE`) stand
   alone in a table cell with the required table shape; `BELP_*` rows go into a body table.
5. A script must open and close within one paragraph (line breaks only via Shift+Enter), must not
   contain `]`, and names placeholders without braces.
6. Keep the original layout and formatting; change only what is needed.

## Workflow A – new template

1. Clarify briefly (one message, sensible defaults offered): purpose and content, DOCX or ODT
   (default DOCX), recipient party type, case-form prefixes needed, whether the firm uses a
   j-lawyer letterhead (Briefkopf). With a letterhead the template contains only the body – no
   sender block, header or footer (in DOCX the letterhead merge drops template headers/footers).
2. Write the spec in the Markdown subset from `references/muster.md`, using its building blocks.
   Draft real legal text the user can adapt; mark open points for the user rather than inventing facts.
3. `build`, then `validate`. Fix and rebuild until there are no errors.
4. Deliver the file and a short list of the placeholders/scripts used.

## Workflow B – template from an example document

1. `inspect` the example. Identify every concrete, case-specific value: names, company, address,
   salutation, own and opposing file numbers, court and court file number, dates, amounts, bank
   details, insurance numbers, signatory, dictation mark.
2. Present a mapping table to the user **before changing anything**:
   `Fundstelle | Wert im Beispiel | vorgeschlagener Platzhalter / Skript`. Include proposals such as
   the salutation via `{{MANDANT_BEGRUESSUNG}} {{MANDANT_NAME}},` (BEGRUESSUNG holds only
   "Sehr geehrter Herr" without the name) or `MWDJU`, today's date via `{{KURZDATUM}}`,
   deadlines via `FRIST`/`FRISTBANKTAG`, gendered terms via `GENDERN`. Mark values with no matching
   placeholder (they stay as text or become a case-form field) and ask which party type each person is.
3. After confirmation write `map.json` and run `replace`. Order rules from long to short
   (full name before surname). Use `"vorkommen": [n]` or `"regex": true` where a plain search is
   ambiguous (e.g. dates: the letter date becomes `{{KURZDATUM}}`, others may stay).
4. `normalize`, then `validate`. The HINWEIS lines list remaining dates, IBANs, e-mails etc. – go
   through them with the user so no personal data of the example case stays in the template.

`map.json` format:
```json
[{"find": "Max Mustermann", "replace": "{{MANDANT_VORNAME}}{{MANDANT_NAME}}"},
 {"find": "Sehr geehrter Herr Mustermann,", "replace": "{{MANDANT_BEGRUESSUNG}} {{MANDANT_NAME}},"},
 {"find": "\\d{2}\\.\\d{2}\\.\\d{4}", "replace": "{{KURZDATUM}}", "regex": true, "vorkommen": [1]},
 {"find": "Mustermann", "replace": "{{MANDANT_NAME}}", "bereich": "text"}]
```
`bereich`: `alle` (default) | `text` | `kopf` | `fuss`. `vorkommen`: 1-based occurrences to replace.

## Workflow C – edit an existing template

1. `inspect` (and `validate` to see existing problems – mention them, fix them only if wanted).
2. Text changes, new or changed placeholders and scripts: `replace` with a map; anchor on unique
   surrounding text. Inserting text = replacing an anchor with anchor + new text.
3. Structural changes (new paragraphs, table rows, moved blocks): edit the XML directly (DOCX
   `word/document.xml`, ODT `content.xml`) following `references/engine-regeln.md`, or use a
   docx skill if one is available – then always `normalize` + `validate`.
4. Deliver with a short change summary.

## Optional: j-lawyer connector

If tools of the j-lawyer MCP connector are available (names like `list_form_types`,
`search_templates`, `create_document_from_template`):
- `list_form_types` → case-form placeholder prefixes for `validate --formulare`.
- `search_templates` → check whether a similar template already exists.
- Test run: the connector cannot upload templates – ask the user to upload the file in j-lawyer
  (Einstellungen → Dokumentvorlagen), then create a document from it in a **test case** with
  `create_document_from_template` and check `get_document_text` for leftover `{{`, `[[SCRIPT` or
  `SCRIPTFEHLER`. Only do this with the user's consent, never in a real client case.

## Output

- claude.ai / Claude Desktop: save results to `/mnt/user-data/outputs/` (if that directory exists)
  and offer them for download.
- Claude Code: next to the input file with suffix `-vorlage` (e.g. `brief-vorlage.docx`), or where the user says.
