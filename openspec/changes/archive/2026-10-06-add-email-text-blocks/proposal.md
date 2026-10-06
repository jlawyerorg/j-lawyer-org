# Change: Text blocks ("Bausteine") for composing e-mails and beA messages

## Why

Users repeatedly type the same passages (standard paragraphs, instructions, closing phrases)
when writing e-mails and beA messages. Post templates ("Post: Vorlagen") only cover complete
messages and replace the whole body. Similar to Outlook's "Quick Parts", users need reusable
text blocks that are inserted at the cursor position of a message that is being written,
including placeholder resolution.

## What Changes

### Persistence (server)
- **New table `text_blocks`** (Flyway migration) and JPA entity `TextBlock` with
  `TextBlockFacade(Local)`. A text block consists of a name, a plain-text variant, an HTML
  variant and an optional folder path.
- **Folder path.** Segments separated by `/` (e.g. `Mandat/Erstgespräch`). Without a folder the
  block is shown on the top level of the insert menu.
- **Server API.** New methods in `IntegrationServiceRemote` (next to the e-mail template
  methods): `getTextBlockSummaries()`, `getTextBlock(id)`, `addTextBlock(TextBlock)`,
  `updateTextBlock(TextBlock)`, `removeTextBlock(id)`, with English JavaDoc.
- **Content on demand.** Lists and the insert menu only load id, name, folder and which variants
  exist (projection `TextBlock.findAllSummaries`). The content of a block is loaded with
  `getTextBlock(id)` only when it is inserted or opened for editing, so many or large blocks do
  not cost bandwidth when the menu is opened.
- **Global and shared.** Text blocks are not user-specific; every logged-in user
  (`loginRole`) can read and maintain them, like post templates.
- **Validation.** Name required, at least one variant (plain text or HTML) must be non-empty,
  the combination of folder and name must be unique.

### Desktop client: maintenance
- New settings module "Post: Bausteine", registered in `Main.java` as child of "Post" directly
  after "Vorlagen", so it appears in the "Vorlagen" popup of the module bar below
  "Post: Vorlagen".
- New panel `EmailTextBlocksPanel` (+ `.form`), modelled on `EmailTemplatesPanel`: list of
  text blocks, fields for name and folder (editable combo box with existing folders), a
  plain-text editor (`TextEditorPanel`) and an HTML editor (`WebViewHtmlEditorPanel`/SunEditor),
  placeholder list with "Einfügen" as in the post template editor. Actions: new, save,
  duplicate, delete.

### Desktop client: inserting
- New helper `TextBlocksMenuBuilder` that builds a hierarchical `JPopupMenu` from all text
  blocks: folder segments become nested `JMenu`s, text blocks become menu items labelled with
  their name; blocks without folder are on the top level.
- `SendEmailFrame` and `SendBeaMessageFrame` get a simple icon button in the toolbar. Clicking
  it opens the hierarchical popup menu; choosing a block inserts it at the cursor position of
  the message body.
- The variant matching the current editor mode is inserted (plain text in text mode and in the
  beA composer, HTML in HTML mode). Blocks without the required variant are shown disabled.
- Placeholders in text blocks are resolved on insert using the same context as post templates
  (case, selected parties, dictation sign, lawyer/assistant, author, form placeholders and
  cloud link in the e-mail composer). `{{CURSOR}}` positions the caret in text mode.

## Impact

- Affected specs: `email-text-blocks` (new)
- New code:
  - j-lawyer-server-entities: `TextBlock`, Flyway migration `V3_6_0_54__TextBlocks.sql`
    (next free number at implementation time)
  - j-lawyer-server-ejb: `TextBlockFacade`, `TextBlockFacadeLocal`
  - j-lawyer-client: `EmailTextBlocksPanel` (+ `.form`), `TextBlocksMenuBuilder`
- Modified code:
  - `IntegrationServiceRemote`, `IntegrationService` (new methods)
  - `Main.java` (module registration), `Modules.properties` (label "Bausteine")
  - `SendEmailFrame` (+ `.form`), `SendBeaMessageFrame` (+ `.form`)
- No breaking changes. Older clients ignore the new table and methods.
