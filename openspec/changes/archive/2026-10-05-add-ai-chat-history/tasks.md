## 1. Persistence

- [x] 1.1 Create JPA entity `AiChat` (table `ai_chats`): id, case_id (nullable, plain
      column), owner, title, title_custom, first_message (LONGTEXT), word_count,
      message_version, assistant_config_id, request_type, action_id, model_ref,
      capability_name, system_prompt, configuration_values, created, last_activity
- [x] 1.2 Create JPA entity `AiChatMessage` (table `ai_chat_messages`): id, chat_id, seq,
      role, content (LONGTEXT), tool_call_id, tool_name, model_ref, principal_id
- [x] 1.3 Create facades `AiChatFacade(Local)` and `AiChatMessageFacade(Local)` with
      queries "by case ordered by last_activity desc", "by owner without case ordered by
      last_activity desc", "messages by chat ordered by seq", "delete messages by chat",
      "delete chats by owner without case"
- [x] 1.4 Write the Flyway migration (next free number at implementation time, currently
      `V3_6_0_53`): both tables, `ai_chats.case_id` FK → `cases.id` ON DELETE CASCADE,
      `ai_chat_messages.chat_id` FK → `ai_chats.id` ON DELETE CASCADE, indexes on `case_id`,
      `owner`, `chat_id`; content columns utf8mb4; id column types matching `cases.id`
- [x] 1.5 Register the entities in the persistence unit if required by the current setup

## 2. Shared helper

- [x] 2.1 Create `ChatWordCounter` in `com.jdimension.jlawyer.ai` (next to `Message`): word
      count over the content of a message list (whitespace-separated tokens, null-safe)
- [x] 2.2 Unit tests for `ChatWordCounter` (empty list, null content, multiple whitespace,
      tool messages)

## 3. Server service

- [x] 3.1 Add `AiChatServiceRemote` in `j-lawyer-server-api` with English JavaDoc:
      `saveChat(AiChat, int expectedVersion, List<Message>)` returning `AiChatSaveResult`
      (chat summary + `forked`), `getChatsForCase(caseId)`, `getOwnChatsWithoutCase()`,
      `getChat(chatId)`, `getMessages(chatId)`, `getFirstMessage(chatId)`,
      `renameChat(chatId, title)`, `assignChatToCase(chatId, caseId)`,
      `removeChats(List<String> chatIds)`
- [x] 3.2 Implement `AiChatService` + `AiChatServiceLocal`, save: create on unknown id;
      otherwise lock the row (`PESSIMISTIC_WRITE`), compare `message_version`; on match replace
      messages, increment version, recompute `word_count`, update `last_activity` and
      continuation metadata; on mismatch or deleted chat store as new chat (same case
      reference, caller as owner, derived title) and flag `forked`; fail if the case no longer
      exists; derive title (first 80 chars of the first user message, whitespace collapsed) and
      `first_message` on creation; never change `case_id`, `owner`, `title`, `title_custom` in a
      save
- [x] 3.3 Implement `renameChat` (trim, max 255 chars, empty → derived title and
      `title_custom = false`) and `assignChatToCase` (only chats without case; one-way); neither
      changes `message_version`
- [x] 3.4 Add `removePrivateChatsOfUser(principalId)` to `AiChatServiceLocal` and call it from
      `SystemManagement.deleteUser`; check for other server paths that delete users and call it
      there as well
- [x] 3.5 Enforce permissions as in design.md (owner for chats without case;
      `readArchiveFileRole` + `SecurityUtils.checkGroupsForCase` for reading/saving case chats;
      `writeArchiveFileRole` + group check for renaming/deleting case chats; owner + read access
      to the target case for assigning)
- [x] 3.6 Store the author (`principal_id`) per message: keep authors of unchanged earlier
      messages (`attributeAuthors`), attribute new ones to the caller, return them with the
      messages; show the author above user messages in the case transcript; unit test
      `AiChatAuthorshipTest`
- [x] 3.7 Unit tests for title derivation (short, long, whitespace, empty first message) and
      rename normalisation
- [x] 3.8 Verify that deleting a case removes its chats (DB cascade; no change to
      `ArchiveFileService.removeArchiveFile` expected)

## 4. Client: storing and continuing chats

- [x] 4.1 Add `lookupAiChatServiceRemote()` to `JLawyerServiceLocator`
- [x] 4.2 Add event types to `Event` and create `AiChatSavedEvent` (chatId, caseId, title,
      lastActivity, owner, wordCount) and `AiChatDeletedEvent` (chatIds, caseId)
- [x] 4.3 `AssistantChatPanel`: hold chat id and loaded `message_version`; create the chat on
      the first send; set the case id only when started from `ArchiveFilePanel`; save the full
      message list with the expected version after every completed request (success and error)
      in the worker; take over id/version from the result; on `forked` show the fork notice
      once; log and show a status message on save failure; publish `AiChatSavedEvent` after
      every successful save
- [x] 4.4 `AssistantChatPanel`: show the word count of the chat (via `ChatWordCounter`),
      updated after each request
- [x] 4.5 `AssistantChatPanel`: "Chat zurücksetzen" clears the chat id so the next message
      starts a new stored chat
- [x] 4.6 `AssistantChatPanel`: add a way to open with a stored chat (`AiChat` + messages):
      render stored messages like live ones, `isFirstMessage = false`, keep chat id and version
- [x] 4.7 `AssistantAccess`: rebuild a stored chat's capability (config must offer request type
      + action, model must exist via `getAssistantModels`; copy with stored model, system prompt
      and configuration; prompt name irrelevant); save system prompt and configuration with the
      chat; if not resolvable, let the user pick from the chat entries of the menus; if none
      exist, show a message; open `AssistantChatPanel` for the stored chat

## 5. Client: views

- [x] 5.1 Create `AiChatHistoryPanel` (+ `.form`): list left (title, last activity, word count,
      creator in case mode), transcript right (header with title, word count, model, created;
      read-only, `AiChatMessageMarkdownPanel`); buttons and context menu "Fortsetzen",
      "Umbenennen", "Akte zuordnen" (own-chats mode only), "Löschen", "Aktualisieren", disabled
      when not permitted; multi-select delete with confirmation; tooltip loaded lazily via
      `getFirstMessage` and rendered as wrapped HTML
- [x] 5.2 `AiChatHistoryPanel`: "Umbenennen" dialog (empty input resets to the derived title);
      "Akte zuordnen" via `SearchAndAssignDialog` with a confirmation that the chat becomes
      visible to all users with access to the case; publish `AiChatSavedEvent` afterwards
- [x] 5.3 `AiChatHistoryPanel` as `EventConsumer`: subscribe on creation, unsubscribe on
      dispose; on the EDT, compare the event's chat id(s) with the currently displayed chat and
      reload/clear the transcript only on a match; for the list, insert/update/reposition saved
      chats in scope and remove deleted chats and chats that left the scope (keep selection, no
      server re-fetch); ignore other events; publish `AiChatDeletedEvent` after deleting
- [x] 5.4 `ArchiveFilePanel` + `ArchiveFilePanel.form`: add tab "AI" between "Falldaten" and
      "Historie" (always visible, no count in the title) containing `AiChatHistoryPanel` in case
      mode, loaded on first selection and reset when another case is opened; pass the
      `ArchiveFilePanel` as input adapter when continuing
- [x] 5.5 Create `AiChatsEditorPanel` (+ `.form`, `ThemeableEditor`) wrapping
      `AiChatHistoryPanel` in "own chats without case" mode
- [x] 5.6 `Main`: register module "AI" (module name "Recherche") after "Auswertungen", always
      visible; add the label to `Modules*.properties`; choose blue/green 32px icons consistent
      with the others

## 6. Validation

- [x] 6.1 Manual test: chat from a case → appears in the case "AI" tab, not in the main "AI"
      module; chat from elsewhere → appears in the main "AI" module only
- [x] 6.2 Manual test: continue a chat (incl. tool calls) and verify the new messages are stored,
      the word count grows and the chat moves to the top
- [x] 6.3 Manual test concurrency: two users (or two dialogs) continue the same case chat; the
      second save creates a new chat with the fork notice, both histories are complete
- [x] 6.4 Manual test: rename a chat (and reset by empty title); assign a chat without case to a
      case → it leaves the main "AI" module and appears in the case tab
- [x] 6.5 Manual test: delete a chat in both views; delete a case and verify its chats are gone;
      delete a user and verify their private chats are gone and their case chats remain
- [x] 6.6 Manual test live refresh: transcript of chat A updates while A is continued in the
      dialog; saving chat B leaves the transcript of A untouched and keeps A selected; a new
      chat from the Ingo menu appears in the open case tab; chats of another case are ignored
- [x] 6.7 Manual test: a second user sees and continues case chats, but not the first user's
      chats without case; without Ingo configuration both views are visible and usable
- [x] 6.8 Run `openspec validate add-ai-chat-history --strict`
