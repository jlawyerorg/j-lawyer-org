## MODIFIED Requirements

### Requirement: Fielded Search for Document Metadata
Full-text search SHALL support a `field:value` query prefix for the document metadata
fields filename (`dateiname`), case name (`akte`), case number (`az`), document title
(`bezeichnung`), document keywords (`schlagwort`) and correspondent name (`von`), in
addition to the default full-text search over document content. Metadata fielded matches
SHALL be case-insensitive and support `*`/`?` wildcards. The `schlagwort` field SHALL match
against each keyword of a document individually.

A metadata value without wildcards SHALL match as a substring of the field, because the
keyword index fields hold the whole filename/case name as a single term and an exact match
would otherwise require the user to type the complete value. Enclosing the value in double
quotes SHALL request an exact match of the whole field value instead. Double quotes SHALL
be treated as syntax and never as part of the searched value; wildcards inside quotes SHALL
still apply.

Any query without a recognized field prefix SHALL continue to be treated as literal
full-text against the document content, and SHALL additionally match the document title
and keywords, with query-syntax special characters escaped so arbitrary input cannot cause
a parse error. Metadata fielded search relies on non-analyzed keyword index fields, so it
takes effect only for documents indexed after the change (a full re-index is required).
When the metadata of a document changes, its index entry SHALL be updated without
re-extracting the document text.

#### Scenario: Search by filename
- **WHEN** a user searches for `dateiname:quittung`
- **THEN** documents whose filename contains `quittung` (case-insensitive) are returned, and documents that merely mention it in their content are not

#### Scenario: Exact filename search
- **WHEN** a user searches for `dateiname:"test.pdf"`
- **THEN** only documents whose filename is exactly `test.pdf` (case-insensitive) are returned

#### Scenario: Wildcard filename search
- **WHEN** a user searches for `dateiname:*.pdf`
- **THEN** documents whose filename ends with `.pdf` (case-insensitive) are returned

#### Scenario: Quoted wildcard pattern
- **WHEN** a user searches for `dateiname:"*2026-08-??_Quittung*"`
- **THEN** the quotes are stripped, the wildcards still apply, and the result is identical to the same pattern without quotes

#### Scenario: Plain text search is unchanged and robust
- **WHEN** a user searches without a recognized field prefix (e.g. `Vertrag 2024` or text containing special characters like `:` or `(`)
- **THEN** the input is searched as literal full-text against the document content and does not raise a query parse error

#### Scenario: Search by keyword
- **WHEN** a user searches for `schlagwort:frist`
- **THEN** documents having a keyword containing `frist` (case-insensitive) are returned

#### Scenario: Search by title
- **WHEN** a user searches for `bezeichnung:"Klageerwiderung Beklagter"`
- **THEN** only documents whose title is exactly "Klageerwiderung Beklagter" (case-insensitive) are returned

#### Scenario: Plain search finds title
- **WHEN** a user searches for `Klageerwiderung` and a document has this word only in its title
- **THEN** the document is returned

#### Scenario: Metadata change updates the index
- **WHEN** a user adds the keyword `Beweis` to an indexed document
- **THEN** a subsequent search for `schlagwort:beweis` returns the document without a full re-index
