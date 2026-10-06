# email-text-blocks Specification

## Purpose
Text blocks ("Bausteine") with plain-text and HTML variants and an optional folder path, maintained
under "Post: Bausteine" and inserted at the cursor of the e-mail and beA composers via a hierarchical
menu, with placeholder resolution and content loaded on demand.
## Requirements
### Requirement: Text Block Data Model
The system SHALL support text blocks consisting of a name, a plain-text variant, an HTML
variant and an optional folder path. The folder path SHALL consist of segments separated by
`/`. Text blocks SHALL be stored in the database.

#### Scenario: Text block with folder
- **WHEN** a user saves a text block named "Vollmacht anfordern" with folder `Mandat/Beginn`
- **THEN** the text block SHALL be stored with name, both variants and the folder path

#### Scenario: Text block without folder
- **WHEN** a user saves a text block without folder
- **THEN** the text block SHALL be stored without folder path and be treated as top level

#### Scenario: Folder path normalization
- **WHEN** a user saves a text block with folder ` Mandat // Beginn/ `
- **THEN** the stored folder path SHALL be `Mandat/Beginn`

### Requirement: Shared Text Blocks
Text blocks SHALL be global: every logged-in user SHALL be able to see, create, change and
delete all text blocks.

#### Scenario: Block created by another user
- **WHEN** user A creates a text block
- **THEN** user B SHALL see it in the maintenance panel and in the insert menu

### Requirement: Text Block Validation
The system SHALL reject saving a text block without a name, a text block whose plain-text and
HTML variants are both empty, and a text block whose combination of folder path and name is
already used by another text block.

#### Scenario: Missing name
- **WHEN** a user saves a text block with an empty name
- **THEN** the save SHALL be rejected with an error message

#### Scenario: No content
- **WHEN** a user saves a text block with empty plain text and empty HTML
- **THEN** the save SHALL be rejected with an error message

#### Scenario: Duplicate name in folder
- **WHEN** a text block "Gruß" exists in folder `Allgemein` and a user saves another text block
  "Gruß" in folder `Allgemein`
- **THEN** the save SHALL be rejected with an error message

#### Scenario: Same name in different folders
- **WHEN** a text block "Gruß" exists in folder `Allgemein` and a user saves a text block
  "Gruß" in folder `Mandat`
- **THEN** the save SHALL succeed

### Requirement: Maintenance Entry In Templates Menu
The desktop client SHALL offer an entry "Post: Bausteine" in the "Vorlagen" popup menu of the
main window, placed directly below "Post: Vorlagen". Selecting it SHALL open the text block
maintenance panel.

#### Scenario: Open maintenance
- **WHEN** the user opens the "Vorlagen" popup menu and selects "Post: Bausteine"
- **THEN** the text block maintenance panel SHALL be shown in the main area

### Requirement: Text Block Maintenance Panel
The maintenance panel SHALL list all text blocks with folder and name and SHALL allow creating,
editing, duplicating and deleting text blocks. It SHALL provide a plain-text editor and an HTML
editor based on SunEditor, as used for post templates, a folder field offering the existing
folder paths while allowing new ones, and a placeholder list from which placeholders can be
inserted into the active editor.

#### Scenario: Edit both variants
- **WHEN** the user edits a text block and enters content in the "Text" and the "HTML" editor
  and saves
- **THEN** both variants SHALL be stored

#### Scenario: Insert placeholder while editing
- **WHEN** the user selects a placeholder in the placeholder list and inserts it
- **THEN** the placeholder SHALL be inserted at the cursor of the active editor

#### Scenario: Delete text block
- **WHEN** the user deletes a text block and confirms
- **THEN** the text block SHALL be removed and no longer appear in the insert menu

### Requirement: Hierarchical Text Block Menu
The e-mail composer and the beA composer SHALL each provide an icon button that opens a popup
menu of all text blocks. Folder path segments SHALL be shown as nested sub menus; text blocks
SHALL be shown as menu items labelled with their name on the level of their last folder
segment. Text blocks without folder SHALL be shown on the top level. On every level sub menus
SHALL be listed before text blocks, each sorted alphabetically. The menu SHALL reflect the
current stored text blocks each time it is opened.

#### Scenario: Nested folders
- **WHEN** a text block "Vollmacht" has folder `Mandat/Beginn` and the user clicks the text
  block button
- **THEN** the popup SHALL contain a sub menu "Mandat" containing a sub menu "Beginn"
  containing the item "Vollmacht"

#### Scenario: Top-level block
- **WHEN** a text block "Gruß" has no folder and the user clicks the text block button
- **THEN** the item "Gruß" SHALL be shown directly in the popup, below the folder sub menus

#### Scenario: No text blocks
- **WHEN** no text blocks exist and the user clicks the text block button
- **THEN** the popup SHALL show a single disabled entry indicating that no text blocks exist

### Requirement: Load Text Block Content On Demand
The system SHALL build the insert menu and the maintenance list from text block names, folders and
the information which variants exist, without transferring the content of the text blocks. The
content of a text block SHALL only be loaded when it is inserted or selected for editing.

#### Scenario: Opening the insert menu
- **WHEN** the user opens the text block menu in a composer
- **THEN** only id, name, folder and the availability of both variants SHALL be loaded for each
  text block

#### Scenario: Inserting a text block
- **WHEN** the user picks a text block in the menu
- **THEN** the content of exactly this text block SHALL be loaded and inserted

#### Scenario: Selecting a text block for editing
- **WHEN** the user selects a text block in the maintenance list
- **THEN** the content of exactly this text block SHALL be loaded and shown in the editors

### Requirement: Insert Text Block At Cursor In E-Mail Composer
Selecting a text block in the e-mail composer SHALL insert it at the current cursor position of
the message body, replacing a selection if present. In text mode the plain-text variant SHALL
be inserted, in HTML mode the HTML variant SHALL be inserted with its formatting.

#### Scenario: Insert in text mode
- **WHEN** the composer is in text mode, the cursor is in the middle of the body and the user
  selects a text block
- **THEN** the plain-text variant SHALL be inserted at the cursor and the existing text before
  and after SHALL remain unchanged

#### Scenario: Insert in HTML mode
- **WHEN** the composer is in HTML mode and the user selects a text block
- **THEN** the HTML variant SHALL be inserted at the cursor of the HTML editor

### Requirement: Insert Text Block At Cursor In beA Composer
Selecting a text block in the beA composer SHALL insert its plain-text variant at the current
cursor position of the message body.

#### Scenario: Insert in beA message
- **WHEN** the user writes a beA message and selects a text block
- **THEN** the plain-text variant SHALL be inserted at the cursor

### Requirement: Unavailable Variant Is Disabled
The insert menu SHALL show a text block disabled if it has no content for the variant
required by the current editor (plain text in text mode and in the beA composer, HTML in HTML
mode).

#### Scenario: HTML-only block in text mode
- **WHEN** a text block has only an HTML variant and the e-mail composer is in text mode
- **THEN** the text block SHALL be shown disabled in the menu

#### Scenario: Text-only block in HTML mode
- **WHEN** a text block has only a plain-text variant and the e-mail composer is in HTML mode
- **THEN** the text block SHALL be shown disabled in the menu

#### Scenario: HTML-only block in beA composer
- **WHEN** a text block has only an HTML variant and the user opens the menu in the beA
  composer
- **THEN** the text block SHALL be shown disabled in the menu

### Requirement: Placeholder Resolution On Insert
Placeholders contained in a text block SHALL be resolved before insertion using the same
context as post templates in the respective composer (case, selected parties, dictation sign,
case lawyer and assistant, author, and in the e-mail composer form placeholders and cloud
link). The `{{CURSOR}}` placeholder SHALL be removed; in text mode and in the beA composer the
caret SHALL be placed at its position.

#### Scenario: Case placeholders resolved
- **WHEN** the composer has a case context and the user inserts a text block containing a case
  file number placeholder
- **THEN** the inserted text SHALL contain the case file number instead of the placeholder

#### Scenario: Cursor placeholder in text mode
- **WHEN** the user inserts a text block containing `{{CURSOR}}` in text mode
- **THEN** `{{CURSOR}}` SHALL NOT appear in the body and the caret SHALL be placed at its
  position within the inserted text

#### Scenario: No case context
- **WHEN** the composer has no case context and the user inserts a text block with placeholders
- **THEN** the text block SHALL be inserted with placeholders resolved as far as context is
  available, as for post templates

