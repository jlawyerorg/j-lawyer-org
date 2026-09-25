# Change: Submenus for custom Ingo prompts

## Why

Custom Ingo prompts (`AssistantPrompt`, table `assistant_prompts`) are rendered **flat** in
every context menu that offers the assistant. The entity carries no grouping attribute at all
— `id, prompt_name, request_type, prompt_text, model_ref, configuration, system_prompt` — so
`AssistantAccess.populateMenu(...)` has nothing to group by and simply appends one
`JMenuItem` per prompt.

A firm that actually uses the assistant ends up with dozens of prompts. The document context
menu in `ArchiveFilePanel`, the mail popups in `MailContentUI` and `SendEmailFrame` and the
prompt pickers in the chat and generate dialogs then present one long, unscannable list that
can outgrow the screen. There is no way for a firm to express "these six prompts belong to
dunning, those four to employment law".

## What Changes

- **New optional field `subMenu` on `AssistantPrompt`** (column `sub_menu`): a free-text
  label chosen by the firm. Empty means the prompt stays on the top level — the behaviour
  every existing prompt keeps after the migration.
- **Semicolon-separated labels** put the same prompt into several submenus at once
  (`Klage;Vorlagen`), so a prompt that belongs to two topics does not have to be duplicated.
- **One level of nesting only.** No path syntax, no nested submenus — the label is the
  submenu name.
- **Every assistant context menu evaluates the label.** Both menu-building paths are covered:
  the `AiCapability`-clone path used by `AssistantAccess.populateMenu(...)` (six client files,
  18 call sites) and the three hand-written prompt pickers in `AssistantChatPanel`,
  `AssistantChatDialog` and `AssistantGenerateDialog`, which are consolidated into one shared
  helper in the process.
- **Editable combo box in the prompt editor** (`AssistantPromptV2SetupDialog`), prefilled with
  the distinct labels already stored on any prompt, sorted alphabetically, with an empty entry
  on top. The user can pick an existing label or type a new one.

Menu layout rules:

- Entries without a label come first, submenus follow, submenus sorted alphabetically.
- A menu holds **one submenu per label**. Callers invoke `populateMenu` repeatedly on the same
  menu (once per request type, separated by `JSeparator`); a submenu created by an earlier
  call is found and reused by the later ones instead of being created twice.

Out of scope: the REST v8 DTO `RestfulAssistantPromptV8` and the Angular web client. The
column survives a REST update untouched because `AssistantEndpointV8.updatePrompt` merges the
DTO into the loaded entity — but it is not editable there. Adding the DTO field alone would be
actively harmful (a web client that does not send it would null the value), so DTO and web
input have to be added together in a later change.

## Impact

- Affected specs: `ai-assistant-integration`
- Affected code:
  - `j-lawyer-server-entities/.../persistence/AssistantPrompt.java` + Flyway migration
    `V3_6_0_30__AssistantPromptSubMenu.sql`
  - `j-lawyer-fax/.../ai/AiCapability.java` (new field, carried through `clone()`)
  - `j-lawyer-client/.../assistant/AssistantAccess.java` (submenu-aware menu building,
    new `populatePromptMenu` helper)
  - `j-lawyer-client/.../assistant/AssistantChatPanel.java`, `AssistantChatDialog.java`,
    `AssistantGenerateDialog.java` (three duplicated handlers replaced by the helper)
  - `j-lawyer-client/.../configuration/AssistantPromptV2SetupDialog.java` (new combo box)
- No EJB interface signatures change — `IntegrationServiceRemote` passes the whole entity.
- No behaviour change for installations that leave every label empty.
