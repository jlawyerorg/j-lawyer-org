# Change: Persist AI chats and make them viewable, continuable and deletable

## Why

Chats with Ingo (`AssistantChatPanel`) only live in memory today: `messages` is a field of the
dialog and is lost when the dialog closes. Users cannot look up an earlier answer, cannot pick
up a conversation later, and lose the work done in a case-related chat as soon as they close the
window. Chats started from a case are part of the work on that case and should be found there.

## What Changes

### Persistence (server)
- **Every chat is stored.** A chat record is created when the first message of a chat is sent
  from `AssistantChatPanel`; afterwards the stored message list is replaced with the current
  conversation state after every completed request (success or error). This includes user,
  assistant and tool messages, so a continued chat has the full LLM context.
- **Case reference.** A chat started from `ArchiveFilePanel` stores the id of that case. All
  other chats are stored without a case reference.
- **Deletion with the case.** `ai_chats.case_id` references `cases.id` with
  `ON DELETE CASCADE`, and `ai_chat_messages.chat_id` references `ai_chats.id` with
  `ON DELETE CASCADE`. Deleting a case removes its chats and their messages.
- **Continuation metadata.** Each chat stores the assistant configuration id, request type,
  action id, model reference and capability name so the chat can be continued with the same
  Ingo capability.
- **Deletion with the user.** Deleting a user deletes the user's chats without case reference;
  their case chats stay with the case.
- **No lost messages on concurrent continuation.** Each chat carries a message version. If a
  save is based on an outdated version (another user or dialog saved in between), the server
  stores the conversation as a new chat with the same case reference instead of overwriting, and
  the user is informed.
- **Word count.** The number of words over all messages of a chat is stored and shown in the
  list, the transcript and live in the chat dialog.
- **Rename and assign.** Chats can be renamed (empty title → derived title again). A chat
  without case can be assigned to a case afterwards (one-way).
- **No case history entries** are written for chats.
- **Visibility and permissions.**
  - Chats without a case are private: only their creator can list, read, continue and delete
    them.
  - Chats with a case are visible to every user with access to that case (group check as for
    other case data). Every such user can continue them; deleting requires
    `writeArchiveFileRole`, as does renaming.
  - Viewing and managing chats requires neither an Ingo configuration nor `aiAgentRole`.
- **New EJB service `AiChatService`** with Remote and Local interfaces (save chat with version
  check, list chats of a case, list the caller's chats without case, load messages, rename,
  assign to case, delete chat; local: delete private chats of a user).
- **Database:** new tables `ai_chats` and `ai_chat_messages` (Flyway, utf8mb4 content columns).

### Desktop client
- **`AssistantChatPanel`** persists the conversation through `AiChatService` and can be opened
  with a stored chat to continue it. Resetting the chat ("Chat zurücksetzen") starts a new
  stored chat; the previous one stays stored.
- **New reusable view `AiChatHistoryPanel`** (with `.form`):
  - left: list of chats, sorted by last activity, newest first. The title is the first
    80 characters of the first user message (whitespace collapsed); the tooltip shows the
    complete first message.
  - right: read-only transcript of the selected chat.
  - actions: "Fortsetzen" opens `AssistantChatPanel` with the stored chat, "Umbenennen",
    "Akte zuordnen" (only for chats without case), "Löschen" deletes the selected chat(s) after
    confirmation, "Aktualisieren".
- **Live refresh within the client** via the existing `EventBroker`: `AssistantChatPanel`
  publishes `AiChatSavedEvent` after every save, chat views publish `AiChatDeletedEvent` after
  deleting. Every event carries the chat id and the case id (or none). A view re-renders its
  transcript **only if the event's chat id equals the chat it currently displays**; the list
  only adds, moves or removes the affected entry when the event belongs to the view's scope.
  Changes made by other users are picked up with "Aktualisieren".
- **New main navigation entry "AI"** in the category "Recherche" (after "Auswertungen"). It
  shows the caller's chats without case reference. Always visible.
- **New tab "AI"** in `ArchiveFilePanel`, between "Falldaten" and "Historie". It shows the chats
  of the case. Always visible, no count in the tab title.

## Impact

- **Affected specs:** `ai-chat-history` (new)
- **Affected code (new):**
  - `j-lawyer-server-entities`: `AiChat`, `AiChatMessage`, Flyway migration (next free
    number at implementation time, currently `V3_6_0_53`; coordinate with other open changes)
  - `j-lawyer-server-api`: `AiChatServiceRemote` (JavaDoc in English), `AiChatSaveResult`
  - `j-lawyer-fax` (`com.jdimension.jlawyer.ai`): `ChatWordCounter` next to `Message`
  - `j-lawyer-server-ejb`: `AiChatService`, `AiChatServiceLocal`, facades
  - `j-lawyer-client`: `AiChatHistoryPanel` (+ `.form`), `AiChatsEditorPanel` (+ `.form`) for
    the main navigation
- **Affected code (modified):**
  - `AssistantChatPanel` (save after each request, publish `AiChatSavedEvent`, load stored
    chat, reset → new chat)
  - `AssistantAccess` (resolve a stored chat's capability; open a stored chat)
  - `ArchiveFilePanel` + `ArchiveFilePanel.form` (new tab)
  - `SystemManagement.deleteUser` (delete private chats)
  - `Main` (module bar entry), `Modules*.properties`
  - `JLawyerServiceLocator` (lookup for `AiChatServiceRemote`)
  - `Event` (new event types), new `AiChatSavedEvent` and `AiChatDeletedEvent`
- **Operational:** chat contents (which may contain document text sent as input) are stored in
  the database in plain text. Like the Lucene index they are not covered by
  `add-document-encryption-at-rest`.
- **No breaking changes.** Existing chats are not affected; storing starts with this version.
