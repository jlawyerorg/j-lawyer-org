## 1. Data model

- [x] 1.1 Add Flyway migration
      `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_30__AssistantPromptSubMenu.sql`
      (re-check the highest existing version first; `V3_6_0_29` is the latest today):
      `ALTER TABLE assistant_prompts ADD COLUMN sub_menu VARCHAR(250) BINARY DEFAULT NULL;`
      plus the `server_settings` version bump to `3.6.0.30` and `commit;`, following
      `V3_5_0_5__AddAssistantPromptModelConfig.sql`
- [x] 1.2 Add field `subMenu` (`@Column(name = "sub_menu")`) with getter/setter to
      `j-lawyer-server-entities/.../persistence/AssistantPrompt.java`, in the JavaDoc style of
      the existing accessors

## 2. Carrying the label into the menus

- [x] 2.1 Add field `subMenu` with getter/setter to `j-lawyer-fax/.../ai/AiCapability.java`
      and copy it in `clone()` — the clone is what reaches the menu builder
- [x] 2.2 Set `clone.setSubMenu(p.getSubMenu())` in
      `AssistantAccess.filterCapabilities(...)`, in the custom-prompt clone block

## 3. Submenu-aware menu building (`AssistantAccess`)

- [x] 3.1 Add `parseSubMenus(String)`: split on `;`, trim, drop empties, collapse duplicates
- [x] 3.2 Add the `MenuTarget` abstraction over `JPopupMenu` (`getComponents()`) and `JMenu`
      (`getMenuComponents()`), plus `findOrCreateSubMenu(...)` which reuses an existing
      `JMenu` with the same text before creating one (icon
      `/icons16/material/baseline_folder_blue_36dp.png`)
- [x] 3.3 Add `addCapabilityItems(...)` taking an item *factory* — a `JMenuItem` has a single
      parent, so a prompt in two submenus needs one instance per submenu. Pass 1 appends
      unlabelled entries, pass 2 adds the alphabetically sorted labels
- [x] 3.4 Rewire all four `populateMenu` overloads onto `addCapabilityItems`, each supplying
      its own factory (flow adapter; dialog parent; frame parent; `JMenu` variant with its
      VISION branch)

## 4. Consolidate the hand-written prompt pickers

- [x] 4.1 Add `populatePromptMenu(JPopupMenu, String requestType, Consumer<AssistantPrompt>)`
      to `AssistantAccess`, using the same submenu logic and `getCompoundIcon(...)`
- [x] 4.2 Replace the duplicated body in `AssistantChatPanel.cmdPromptMouseReleased`
- [x] 4.3 Replace the duplicated body in `AssistantChatDialog.cmdPromptMouseReleased`
- [x] 4.4 Replace the duplicated body in `AssistantGenerateDialog.cmdPromptMouseReleased`
      (keeps its `JOptionPane` error handling). Only method bodies change, so the `.form`
      files of the two dialogs stay untouched

## 5. Prompt editor (`AssistantPromptV2SetupDialog`)

- [x] 5.1 Add the editable `cmbSubMenu` combo box with its `Untermenü:` label and tooltip, and
      insert the row into the hand-written `GroupLayout` below `Name:`
- [x] 5.2 Add `refreshSubMenuModel()` (distinct stored labels, alphabetical, empty entry first,
      preserving the text currently in the editor) and `getSubMenuValue()` (reads the combo
      editor, trims, `null` when blank)
- [x] 5.3 Wire the field into `resetDetails()`, `updatedUI(...)`, `cmdAdd`, `cmdSave`,
      `cmdDuplicate`, `exportPromptToJson` and `cmdImportJson`, and refresh the model after
      every mutation

## 6. Verification

- [x] 6.1 Build manually, start the Docker environment and confirm the migration applied
      (`sub_menu` column present, `jlawyer.server.database.version` = `3.6.0.30`)
- [x] 6.2 Editor round-trip: prompts with no label, with `Klage`, with `Klage;Vorlagen`;
      reopen, duplicate, export by mail and re-import
- [x] 6.3 Capability menus (`ArchiveFilePanel` document popup, `MailContentUI`,
      `SendEmailFrame`): plain entries first, submenus after, the two-label prompt clickable in
      both submenus, one submenu per label across request types
- [x] 6.4 Prompt pickers in `AssistantChatPanel`, `AssistantChatDialog`,
      `AssistantGenerateDialog`: same structure, `{{…}}` placeholder replacement still works
- [x] 6.5 Regression: with every label empty the menus look exactly as before
