## 1. Persistence

- [x] 1.1 Create JPA entity `TextBlock` (table `text_blocks`): id, name, folder (nullable),
      content_text (MEDIUMTEXT), content_html (MEDIUMTEXT)
- [x] 1.2 Create `TextBlockFacade` / `TextBlockFacadeLocal` with the summary projection
      "all ordered by folder, name, without content" and "by name"
- [x] 1.3 Write the Flyway migration (next free number at implementation time, currently
      `V3_6_0_54__TextBlocks.sql`): table `text_blocks`, free-text columns utf8mb4, id column
      `VARCHAR(50) BINARY`
- [x] 1.4 Register the entity in the persistence unit if required by the current setup

## 2. Server API

- [x] 2.1 Add to `IntegrationServiceRemote` with English JavaDoc: `getTextBlockSummaries()`,
      `getTextBlock(String id)`, `addTextBlock(TextBlock)`, `updateTextBlock(TextBlock)`,
      `removeTextBlock(String id)`
- [x] 2.2 Implement in `IntegrationService` with `@RolesAllowed("loginRole")`: folder path
      normalization (trim segments, drop empty segments, `/` separator, empty → null),
      validation (name required, at least one non-empty variant, folder+name unique)
- [x] 2.3 Store variants without content as null; summaries report the existing variants
      via `@Transient` flags; cover the projection in `NamedQueryProjectionTest`
- [x] 2.4 Unit test for the folder path normalization helper (`TextBlockTest` in
      j-lawyer-server-entities, also covering `hasText()` / `hasHtml()`)

## 3. Client: maintenance

- [x] 3.1 Add key for "Bausteine" to `Modules.properties` and register a new settings module
      under "Post" in `Main.java` directly after "Vorlagen" (editor class
      `com.jdimension.jlawyer.client.mail.EmailTextBlocksPanel`)
- [x] 3.2 Create `EmailTextBlocksPanel` (+ `.form`) modelled on `EmailTemplatesPanel`:
      list of blocks (folder + name), name field, editable folder combo with existing
      folders, tabs "Text" (`TextEditorPanel`) and "HTML" (`WebViewHtmlEditorPanel`)
- [x] 3.3 Placeholder list and "Einfügen" inserting into the editor of the active tab
      (`PlaceHolders.getAllPlaceHolders`, `PlaceHolders.insertAt`)
- [x] 3.4 Actions new, save, duplicate, delete (with confirmation); show server validation
      errors to the user
- [x] 3.5 Verify "Post: Bausteine" appears in the "Vorlagen" popup below "Post: Vorlagen"

## 4. Client: inserting

- [x] 4.1 Create `TextBlocksMenuBuilder` building a nested `JPopupMenu` from folder paths
      (sub menus first, then blocks, alphabetical; blocks without required variant disabled;
      empty state entry)
- [x] 4.2 `SendEmailFrame`: extract placeholder context collection from
      `cmbTemplatesActionPerformed` into a reusable private method; templates keep working
      unchanged
- [x] 4.3 `SendEmailFrame` (+ `.form`): toolbar button `cmdInsertTextBlock` after
      `cmdInsertSignature`; load blocks, show popup, resolve placeholders, insert at cursor
      in text or HTML mode, handle `{{CURSOR}}`
- [x] 4.4 `SendBeaMessageFrame`: extract placeholder context collection as in 4.2
- [x] 4.5 `SendBeaMessageFrame` (+ `.form`): toolbar button after `cmdSaveDraft`; plain-text
      insert at cursor, handle `{{CURSOR}}`

## 5. Validation

- [x] 5.1 Create blocks with and without folder, with nested folders, text-only, HTML-only
      and both variants; verify list, edit, duplicate, delete
- [x] 5.2 Validation: empty name, no variant, duplicate folder+name are rejected
- [x] 5.3 E-mail composer, text mode: menu hierarchy matches folders, HTML-only blocks are
      disabled, block is inserted at the cursor
- [x] 5.4 E-mail composer, HTML mode: text-only blocks are disabled, HTML block is inserted at
      the cursor with formatting preserved
- [x] 5.5 beA composer: plain-text insertion at the cursor, HTML-only blocks disabled
- [x] 5.6 Placeholders: block with case and party placeholders resolved with case context;
      `{{CURSOR}}` positions the caret in text mode; post templates still work
- [x] 5.7 Opening the insert menu and the maintenance list transfers no content; the content
      of a block is loaded only when it is inserted or selected
- [x] 5.8 Changes made in the maintenance panel are visible the next time the menu is opened
- [x] 5.9 Run `openspec validate add-email-text-blocks --strict`
