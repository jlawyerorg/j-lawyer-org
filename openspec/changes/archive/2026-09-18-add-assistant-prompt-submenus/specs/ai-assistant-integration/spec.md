## ADDED Requirements

### Requirement: Custom Prompt Submenu Label

A custom assistant prompt (`AssistantPrompt`) SHALL carry an optional free-text submenu label
in the column `sub_menu` (VARCHAR(250)). An empty or absent label SHALL place the prompt on
the top level of every assistant menu, which is the behaviour of all prompts existing before
this change.

A label MAY list several submenu names separated by semicolons. Each segment SHALL be trimmed,
empty segments SHALL be discarded, and duplicate segments within one label SHALL be collapsed.
The label SHALL NOT express nesting — exactly one level of submenu is supported.

#### Scenario: Prompt without a label stays on the top level

- **WHEN** a prompt has no submenu label
- **THEN** it SHALL appear as a direct entry of the assistant menu, as before

#### Scenario: Prompt with one label moves into a submenu

- **GIVEN** a prompt whose submenu label is `Klage`
- **WHEN** an assistant menu containing that prompt is built
- **THEN** the menu SHALL contain a submenu named `Klage`
- **AND** the prompt SHALL appear inside that submenu and not on the top level

#### Scenario: Prompt with several labels appears in each submenu

- **GIVEN** a prompt whose submenu label is `Klage;Vorlagen`
- **WHEN** an assistant menu containing that prompt is built
- **THEN** the prompt SHALL be offered in both the `Klage` and the `Vorlagen` submenu
- **AND** invoking it from either submenu SHALL run the same prompt

### Requirement: Assistant Menu Structuring

Every menu that offers assistant prompts SHALL evaluate the submenu labels — the capability
menus built by `AssistantAccess.populateMenu(...)` as well as the prompt pickers of the chat
and generate dialogs.

Within a menu, entries without a label SHALL be listed before the submenus, and submenus SHALL
be ordered alphabetically, case-insensitively.

A menu SHALL contain at most one submenu per label. Because callers populate a single menu
repeatedly — once per request type, with separators in between — a submenu created by an
earlier population SHALL be reused by later ones rather than created a second time.

#### Scenario: Labels shared across request types collapse into one submenu

- **GIVEN** a `generate` prompt and a `summarize` prompt both labelled `Vorlagen`
- **AND** a menu that is populated once for `generate` and once for `summarize`
- **WHEN** the menu is built
- **THEN** it SHALL contain exactly one `Vorlagen` submenu
- **AND** that submenu SHALL contain both prompts

#### Scenario: Plain entries precede submenus

- **GIVEN** a menu holding both unlabelled prompts and labelled prompts
- **WHEN** the menu is built
- **THEN** the unlabelled entries SHALL appear above the submenus of their section

### Requirement: Submenu Label Editing

The custom prompt editor SHALL offer the submenu label in an editable combo box. The combo box
SHALL be prefilled with the distinct labels stored on the existing prompts exactly as stored —
including combined values such as `Klage;Vorlagen` — sorted alphabetically and preceded by an
empty entry. The user SHALL be able to type a label that does not exist yet.

The label SHALL be preserved when a prompt is duplicated and SHALL be carried by the prompt's
JSON export and import.

#### Scenario: Combo box lists labels in use

- **GIVEN** stored prompts with the labels `Klage;Vorlagen`, `Mandant` and `Vorlagen`
- **WHEN** the prompt editor is opened
- **THEN** the combo box SHALL offer an empty entry followed by `Klage;Vorlagen`, `Mandant`
  and `Vorlagen`

#### Scenario: New label is accepted and offered afterwards

- **WHEN** the user types a label that no prompt uses yet and saves the prompt
- **THEN** the label SHALL be stored with the prompt
- **AND** the combo box SHALL offer it from then on

#### Scenario: Duplicating a prompt keeps its label

- **WHEN** a prompt carrying a submenu label is duplicated
- **THEN** the copy SHALL carry the same label
