# Design: AI chat history

## Context

`AssistantChatPanel` (a `JDialog`) holds the conversation in `List<Message> messages`
(`com.jdimension.jlawyer.ai.Message`: role, content, toolCallId, toolName, modelRef). Each
request sends the whole list to `IntegrationServiceRemote.submitAssistantRequest`. With tool
calling the server may return the full, updated conversation (`toolResponse.getMessages()`),
which replaces the local list. The first user message is the prompt plus the input text
(e.g. selected document text), so it can be large.

The chat is opened via `AssistantAccess.populateMenu(...)`; when opened from a case view the
`AssistantInputAdapter` is the `ArchiveFilePanel` and `selectedCase` is set.

## Goals / Non-Goals

- Goals: store all chats, attach case chats to their case, show/continue/rename/delete chats in
  a main navigation view and in a case tab, assign a chat without case to a case afterwards,
  show the word count of a chat, delete case chats together with the case and private chats
  together with their user, never lose messages when a chat is continued concurrently.
- Non-Goals: server push of changes made by other users/clients, REST API or web client access,
  full-text search over chats, case history entries for chats (none are written for creating,
  continuing, renaming, assigning or deleting chats), export of chats, migration of chats from
  before this change, persisting tool approvals ("für diese Sitzung erlauben") across sessions,
  removing a case reference or moving a chat between cases, a chat count in the tab title.

## Decisions

### Data model
`ai_chats`
- `id` VARCHAR(50) PK (UUID as elsewhere)
- `case_id` VARCHAR(50) NULL, FK → `cases.id` ON DELETE CASCADE, indexed
- `owner` VARCHAR(50) principal id of the creator, indexed
- `title` VARCHAR(255): derived title or user-defined title
- `title_custom` BOOLEAN: true once the user renamed the chat
- `first_message` LONGTEXT utf8mb4: the complete first user message (tooltip)
- `word_count` INT: number of words of all stored messages
- `message_version` INT: incremented by every message save (optimistic concurrency, see below)
- `assistant_config_id`, `request_type`, `action_id`, `model_ref`: what the next request is sent to
- `system_prompt` TEXT, `configuration_values` TEXT (utf8mb4): sent with every request, not part of
  the message list
- `capability_name`: display only (e.g. the name of a custom prompt)
- `created`, `last_activity` DATETIME

`ai_chat_messages`
- `id` VARCHAR(50) PK, `chat_id` FK → `ai_chats.id` ON DELETE CASCADE
- `seq` INT (order), `role`, `content` LONGTEXT utf8mb4, `tool_call_id`, `tool_name`,
  `model_ref`
- `principal_id` VARCHAR(50): the user whose request produced the message

The case is referenced by id only (no JPA relationship to `ArchiveFileBean`), so case deletion
in `ArchiveFileService.removeArchiveFile` needs no code change: the database cascade removes the
chats. utf8mb4 is required because LLM output regularly contains emoji (see
`V3_6_0_7__InstantMessageContentUtf8mb4`).

### Save = replace the message list
`saveChat(chat, expectedVersion, List<Message>)` creates the chat if the id is unknown,
otherwise replaces all messages, increments `message_version`, recomputes `word_count` and
updates `last_activity` and the continuation metadata. Replacing instead of appending is robust
against the server-side rewrite of the conversation during tool calling and costs no relevant
extra traffic, because the client sends the full list to the LLM backend on every request
anyway. The client calls it after every completed request (also on error, so the user's question
is not lost) in the existing `SwingWorker`, after the AI call. A failing save is logged and
shown in the status bar; it never interrupts the chat.

A save never changes `case_id`, `owner`, `title` or `title_custom`; these are changed only by
the dedicated metadata operations below. While `title_custom` is false, the title is derived on
creation: the first 80 characters of the first user message, whitespace collapsed.

### Message authors
Every message stores the principal id of the user whose request produced it (the user message and
the assistant/tool messages of that request). Because a save replaces the whole list, the server
keeps the author of a message if the previously stored message at the same position has the same
role and content; all other messages are new and get the caller as author
(`AiChatService.attributeAuthors`). For a fork, the authors of the stored chat it diverged from are
kept the same way; for a chat that was deleted meanwhile, all messages get the caller. The client
never sets authors; `Message.principalId` is filled only when reading stored messages and is not
sent to the AI backend. The transcript of a case chat shows the author above each user message.

### Concurrent continuation (optimistic concurrency with fork)
A case chat can be continued by several users at the same time, and one user can open the same
chat twice. With replace-all saves the last writer would silently drop the other's messages.

- The client remembers the `message_version` it loaded (0 for a new chat) and sends it as
  `expectedVersion`.
- The server loads the chat row with `LockModeType.PESSIMISTIC_WRITE` in the save transaction
  and compares versions. On a match it saves as described above.
- On a mismatch, or if the chat was deleted in the meantime, the server stores the submitted
  messages as a **new chat** (new id, same case reference and continuation metadata, owner =
  caller, derived title) and returns it flagged as forked. The stored chat stays untouched.
- If the case of the chat no longer exists, nothing is saved and the save fails (status bar
  message); deleted case data is not resurrected.
- The client continues with the returned chat id and version and shows once:
  "Der Chat wurde zwischenzeitlich an anderer Stelle geändert. Dieser Verlauf wurde als neuer
  Chat gespeichert."

The result is a small DTO `AiChatSaveResult` (chat summary + `forked` flag). Metadata
operations (rename, assign) do not increment `message_version`, so renaming a chat while it is
open in a dialog does not cause a fork.

### Word count
The word count is the number of whitespace-separated tokens over the content of all stored
messages (user, assistant and tool messages), i.e. of everything that is sent to the model when
the chat is continued. It is an indication of the chat's size, not a token count. A shared
helper next to `Message` (`com.jdimension.jlawyer.ai.ChatWordCounter`) is used by the server
(persisted in `word_count`) and by the client (live display in `AssistantChatPanel`).

Shown:
- in each list entry ("1.234 Wörter"),
- in the header of the transcript,
- in `AssistantChatPanel`, updated after each request.

### Case reference
The case id is stored on creation if and only if the chat was started from `ArchiveFilePanel`
(`inputAdapter instanceof ArchiveFilePanel`, which `AssistantChatPanel` already detects as
`caseView`). Other entry points that happen to know a case (e.g. an e-mail dialog) store chats
without case reference.

A chat without case reference can be assigned to a case afterwards
(`assignChatToCase(chatId, caseId)`), using the existing `SearchAndAssignDialog` for case
selection (folder selection is ignored). The confirmation states that the chat then becomes
visible to all users with access to the case. Assignment is one-way: an assigned chat cannot be
detached or moved to another case.

### Renaming
`renameChat(chatId, title)` sets `title` and `title_custom = true`. An empty title resets the
chat to the derived title (`title_custom = false`). Titles are trimmed and limited to 255
characters. The tooltip always shows the first message, independent of the title.

### Permissions
| Operation | Chat without case | Case chat |
|---|---|---|
| list, read, save (create/continue) | `loginRole`, caller = owner | `readArchiveFileRole` + `SecurityUtils.checkGroupsForCase` |
| rename | caller = owner | `writeArchiveFileRole` + group check |
| delete | caller = owner | `writeArchiveFileRole` + group check |
| assign to case | caller = owner, plus `readArchiveFileRole` + group check on the target case | not allowed |

A fork created by a different user than the owner of the original case chat gets the caller as
owner. Neither Ingo configuration nor `aiAgentRole` is required to view, rename, assign or
delete chats.

### Deleting a user
`SystemManagement.deleteUser` calls `AiChatServiceLocal.removePrivateChatsOfUser(principalId)`
before removing the user: all chats with `owner = principalId` and no case reference are
deleted (messages via cascade). Case chats of that user stay with the case; the creator column
shows the stored principal id. All server paths that delete users must use this method.

### Sorting
Lists are sorted by `last_activity` descending, so a continued chat moves to the top. The list
row shows the title, the date/time of the last activity and the word count; in the case tab
additionally the creator.

### Continuing a chat
"Fortsetzen" opens `AssistantChatPanel` with the stored chat:
1. The capability is rebuilt from what the next request needs: assistant configuration, request
   type, action and model (all stored in `ai_chats`) plus system prompt and prompt configuration
   (`system_prompt`, `configuration_values`, stored because they are sent separately with every
   request and are not part of the message list). The stored assistant configuration must still
   offer a capability with the stored request type and action, and the stored model must still
   be listed for it by `getAssistantModels()`. The chat continues with a copy of that capability
   carrying the stored model, system prompt and configuration; asynchronous execution and
   parameters come from the capability. The name of a custom prompt (`capability_name`) is for
   display only and plays no role in the lookup.
2. If it is no longer available (configuration removed, action or model no longer offered), the
   user picks one of the chat entries the menus currently offer; the chat continues with it and the
   chat's continuation metadata is updated on the next save. If no chat capability is available
   at all, a message says so and the dialog is not opened.
3. The stored messages are rendered as in a live chat; `isFirstMessage` is false, so the input
   text is not sent again.
4. From the case tab, the `ArchiveFilePanel` is passed as input adapter (so "neues Dokument"
   works); for case chats `selectedCase` is that case.

### Live refresh via EventBroker
The chat dialog is non-modal, so a history view stays visible while a chat is continued or a new
chat is started from the Ingo menu. Views are kept current with client-side events on the
existing `EventBroker`, like `DocumentAddedEvent`/`DocumentUpdatedEvent`:

- `AiChatSavedEvent(chatId, caseId, title, lastActivity, owner, wordCount)`: published by
  `AssistantChatPanel` after every successful save (including a fork, with the new chat's data)
  and by `AiChatHistoryPanel` after renaming or assigning a chat.
- `AiChatDeletedEvent(chatIds, caseId)`: published by `AiChatHistoryPanel` after deleting.

`AiChatHistoryPanel` implements `EventConsumer`, subscribes when it is created and unsubscribes
when it is disposed (case closed / editor removed). Handling, always on the EDT:

1. **Transcript (mandatory id check):** the consumer compares the event's chat id with the id
   of the chat currently displayed on the right. Only on a match it reloads that chat (saved
   and still in scope) or clears the transcript (deleted, or no longer in scope after an
   assignment). Without a match the transcript is not touched, so an unrelated save never
   re-renders or scrolls the visible chat.
2. **List (scope-based):** case mode: scope = same case id; own-chats mode: scope = no case id.
   For a saved chat in scope the entry is inserted or updated and repositioned by
   `last_activity`; for a saved chat that is no longer in scope (assigned to a case) or a
   deleted chat, the entry is removed. The list is not re-fetched from the server and the
   selection is preserved where the entry still exists. Other events are ignored.

The event carries the summary fields so the list can be updated without a server call. Changes by
other users or other clients are not pushed; "Aktualisieren" re-fetches the list.

### UI composition
`AiChatHistoryPanel` (`JPanel` + `.form`) takes a mode (case id or "own chats without case") and
is used twice:
- inside `ArchiveFilePanel` as the tab "AI" (only a tab is added to `ArchiveFilePanel.form`;
  the panel is loaded lazily when the tab is selected for the first time),
- inside a thin editor `AiChatsEditorPanel` (`ThemeableEditor`, like `ReportingPanel`)
  registered in `Main` as module "AI" with module name "Recherche" after "Auswertungen".

Both are **always visible**, independent of whether Ingo is configured and of `aiAgentRole`.

Actions (buttons and context menu): "Fortsetzen", "Umbenennen", "Akte zuordnen" (own-chats mode
only), "Löschen", "Aktualisieren". Actions the caller is not permitted to perform are disabled.

The transcript on the right shows a header (title, word count, model, created) and reuses
`AiChatMessageMarkdownPanel` for user/assistant messages and the existing compact rendering for
tool messages.

## Risks / Trade-offs

- **Large first messages.** The first message can contain long document text. The list DTO
  carries the title only; the tooltip text is loaded on first hover via
  `getFirstMessage(chatId)` and cached, and rendered as HTML with a fixed width so it wraps.
  This keeps the list fast while still showing the complete first message.
- **Long chats.** Continuing a chat re-sends its whole history; very long chats can exceed the
  model's context window or become expensive. The word count makes the size visible; errors
  from the backend are shown as in any chat.
- **Forks instead of merges.** A concurrent continuation produces two chats instead of a merged
  one. This is deliberate: merging two diverging LLM conversations is not meaningful, and no
  message is lost.
- **Storage growth.** Every chat is stored, including document inputs. There is no automatic
  retention; users delete chats manually, chats of deleted cases and private chats of deleted
  users are removed.
- **Confidentiality.** Case chats (including chats assigned afterwards) are visible to all users
  with access to the case. A hint is shown in the empty state of the case tab and in the
  assignment confirmation.

## Migration Plan

One Flyway migration creates both tables. No data migration. Rollback: drop both tables.

## Open Questions

- None blocking. Possible follow-ups: REST endpoints for the web client, search over chats.
