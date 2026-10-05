# ai-chat-history Specification

## Purpose
TBD - created by archiving change add-ai-chat-history. Update Purpose after archive.
## Requirements
### Requirement: Persist All AI Chats
The system SHALL persist every chat conducted in `AssistantChatPanel`. A chat record SHALL be
created when the first message of a chat is sent. After every completed request (successful or
failed) the stored message list SHALL be replaced with the current conversation state,
including user, assistant and tool messages with role, content, tool call id, tool name and
model reference. Each message SHALL also store the principal id of the user whose request produced
it. Messages that were stored before SHALL keep their author when the message list is replaced;
new messages SHALL be attributed to the user saving the chat. Authors sent by the client SHALL be
ignored. The chat SHALL also store the assistant configuration id, request type, action
id, model reference and capability name, its creator, the creation time and the time of the last
activity. A failure to save SHALL be logged and reported in the status bar and SHALL NOT
interrupt the chat.

#### Scenario: First message creates a stored chat
- **WHEN** a user sends the first message in `AssistantChatPanel`
- **THEN** a chat record SHALL be stored with the user as creator
- **AND** the stored messages SHALL contain the user message and, once received, the assistant response

#### Scenario: Tool calls are stored
- **WHEN** the assistant calls tools during a chat and the conversation completes
- **THEN** the stored messages SHALL contain the tool call and tool result messages in conversation order

#### Scenario: Failed request keeps the question
- **WHEN** the AI request fails after the user sent a message
- **THEN** the user message SHALL still be stored

#### Scenario: Messages record who asked
- **WHEN** user A starts a case chat and user B later continues it
- **THEN** the messages of A's requests SHALL be stored with A as author
- **AND** the messages of B's requests SHALL be stored with B as author
- **AND** the transcript of the case chat SHALL show the author of each user message

#### Scenario: Resetting starts a new chat
- **WHEN** the user resets the chat in `AssistantChatPanel` and sends a new message
- **THEN** a new chat record SHALL be created
- **AND** the previous chat SHALL remain stored unchanged

### Requirement: Case Reference For Chats Started From A Case
A chat started from `ArchiveFilePanel` SHALL store a reference to that case. Chats started
from any other place SHALL be stored without case reference. Saving messages SHALL NOT change
the case reference. A case reference SHALL only be added by assigning a chat without case to a
case (see "Assign A Chat To A Case Afterwards"); it SHALL never be removed or changed to another
case.

#### Scenario: Chat started in the case view
- **WHEN** a user starts a chat from the Ingo menu of an open case in `ArchiveFilePanel`
- **THEN** the stored chat SHALL reference that case

#### Scenario: Chat started outside a case view
- **WHEN** a user starts a chat from a place other than `ArchiveFilePanel`
- **THEN** the stored chat SHALL have no case reference

### Requirement: Chats Are Deleted With Their Case
When a case is deleted, the system SHALL delete all chats referencing that case together with
their messages.

#### Scenario: Case deletion removes chats
- **WHEN** a case with stored chats is deleted
- **THEN** none of its chats and none of their messages SHALL remain in the database

### Requirement: Private Chats Are Deleted With Their User
When a user is deleted, the system SHALL delete all chats without case reference created by that
user together with their messages. Case chats created by that user SHALL remain with their case.

#### Scenario: User deletion removes private chats
- **WHEN** an administrator deletes a user who has chats without case reference and case chats
- **THEN** the user's chats without case reference and their messages SHALL be deleted
- **AND** the user's case chats SHALL remain visible in their cases

### Requirement: Concurrent Continuation Without Message Loss
The system SHALL never lose stored messages when the same chat is continued concurrently by
several users or in several dialogs. Each chat SHALL carry a message version that every message
save increments. A save SHALL state the version it is based on. If the stored version differs,
or the chat was deleted in the meantime, the server SHALL store the submitted messages as a new
chat with the same case reference and the caller as creator, leave the stored chat unchanged, and
report the fork to the client. The client SHALL continue with the new chat and SHALL inform the
user once. If the referenced case no longer exists, nothing SHALL be saved. Renaming or assigning
a chat SHALL NOT change the message version.

#### Scenario: Two users continue the same case chat
- **WHEN** user A and user B both continue chat X based on the same version and A saves first
- **THEN** A's messages SHALL be stored in chat X
- **AND** B's save SHALL create a new chat with B's complete conversation and the same case reference
- **AND** B SHALL be informed that the chat was saved as a new chat
- **AND** chat X SHALL still contain A's messages

#### Scenario: Rename while the chat is open
- **WHEN** a chat is open in the chat dialog and the chat is renamed in a chat view
- **THEN** the next save from the dialog SHALL update the same chat without creating a new one

### Requirement: Chat Visibility And Permissions
Chats without case reference SHALL be visible only to their creator; only the creator SHALL be
able to read, continue and delete them. Chats with case reference SHALL be visible to every user
with access to the case (`readArchiveFileRole` and case group permissions); such users SHALL be
able to read and continue them. Renaming and deleting a case chat SHALL require
`writeArchiveFileRole` and case group permissions. Viewing, renaming, assigning and deleting
chats SHALL NOT require an Ingo configuration or `aiAgentRole`. The server SHALL enforce these
rules.

#### Scenario: Another user's private chat is not accessible
- **WHEN** a user requests a chat without case reference created by another user
- **THEN** the server SHALL deny access

#### Scenario: Colleague sees case chats
- **WHEN** a user with access to a case opens the case's "AI" tab
- **THEN** the chats of that case SHALL be listed regardless of who created them

#### Scenario: User without case access
- **WHEN** a user without group permission for a case requests its chats
- **THEN** the server SHALL deny access

### Requirement: Chat List Presentation
Every chat view SHALL show a list of chats on the left, sorted by last activity in descending
order, and a read-only transcript of the selected chat on the right. Each list entry SHALL use
the user-defined title or, if none is set, the first 80 characters of the first user message
(whitespace collapsed) as title, SHALL show the date of the last activity and the word count of
the chat, and SHALL show the complete first user message as tooltip. The transcript SHALL show
the title and the word count of the chat.

#### Scenario: Newest chat first
- **WHEN** a chat view lists several chats
- **THEN** the chat with the most recent activity SHALL be shown first

#### Scenario: Title and tooltip
- **WHEN** a chat's first user message is longer than 80 characters
- **THEN** the list entry SHALL show its first 80 characters as title
- **AND** hovering the entry SHALL show the complete first message as tooltip

#### Scenario: Selecting a chat shows its transcript
- **WHEN** the user selects a chat in the list
- **THEN** its stored messages SHALL be displayed on the right in conversation order

### Requirement: Chat Word Count
The system SHALL determine the word count of a chat as the number of whitespace-separated words
over the content of all its messages, including tool messages. The word count SHALL be stored
with every save and displayed in the chat list, in the transcript header, and in
`AssistantChatPanel`, where it SHALL be updated after every request.

#### Scenario: Word count grows when a chat is continued
- **WHEN** the user continues a chat and receives an answer
- **THEN** the word count shown in `AssistantChatPanel` SHALL include the new messages
- **AND** the list entry of the chat SHALL show the updated word count

### Requirement: Rename A Chat
Every chat view SHALL allow renaming the selected chat, subject to the permissions of the "Chat
Visibility And Permissions" requirement. A user-defined title SHALL be trimmed and limited to 255
characters. An empty title SHALL reset the chat to the title derived from the first user message.

#### Scenario: Rename a chat
- **WHEN** the user renames a chat to "Fristberechnung Berufung"
- **THEN** the list entry SHALL show "Fristberechnung Berufung"
- **AND** the tooltip SHALL still show the complete first user message

#### Scenario: Reset the title
- **WHEN** the user renames a chat with an empty title
- **THEN** the title SHALL again be derived from the first user message

### Requirement: Assign A Chat To A Case Afterwards
The main navigation view "AI" SHALL allow its creator to assign a chat without case reference to
a case the user has access to. The user SHALL confirm that the chat then becomes visible to all
users with access to the case. After the assignment the chat SHALL be a case chat in every
respect. An assigned chat SHALL NOT be detached from the case or moved to another case.

#### Scenario: Assign a chat to a case
- **WHEN** the user selects a chat in the main navigation view "AI", chooses "Akte zuordnen", selects a case and confirms
- **THEN** the chat SHALL reference that case
- **AND** it SHALL disappear from the main navigation view "AI"
- **AND** it SHALL appear in the "AI" tab of that case

#### Scenario: Case chats cannot be reassigned
- **WHEN** a chat already references a case
- **THEN** the system SHALL NOT offer or allow assigning it to a case

### Requirement: Main Navigation Entry For Chats Without Case
The client SHALL provide a main navigation entry "AI" in the category "Recherche" that lists the
current user's chats without case reference. The entry SHALL always be visible, independent of
the Ingo configuration and of `aiAgentRole`.

#### Scenario: Opening the AI module
- **WHEN** the user opens "AI" in the main navigation
- **THEN** the user's own chats without case reference SHALL be listed
- **AND** chats with a case reference SHALL NOT be listed

### Requirement: Case Tab For Case Chats
`ArchiveFilePanel` SHALL provide a tab "AI" positioned between "Falldaten" and "Historie" that
lists the chats referencing the open case. The tab SHALL always be visible, independent of the
Ingo configuration and of `aiAgentRole`, and its title SHALL NOT contain a chat count.

#### Scenario: Opening the AI tab of a case
- **WHEN** the user selects the "AI" tab of an open case
- **THEN** the chats referencing this case SHALL be listed

### Requirement: Continue A Stored Chat
Every chat view SHALL allow continuing the selected chat. Continuing SHALL open
`AssistantChatPanel` with the stored messages; new messages SHALL be saved to the same chat. A chat
SHALL store everything its next request needs: assistant configuration, request type, action,
model, system prompt and prompt configuration. Continuing SHALL use the stored values if the
assistant configuration still offers the request type and action and the model still exists;
the name of the custom prompt the chat was started with SHALL NOT be required to exist. Input text SHALL NOT be sent again. If the stored capability is no
longer available, the user SHALL be able to pick another available chat capability to
continue with; if no chat capability is available, the user SHALL be informed and the chat
SHALL NOT be opened.

#### Scenario: Continue a chat
- **WHEN** the user continues a stored chat and sends a new message
- **THEN** the new user and assistant messages SHALL be appended to the same stored chat
- **AND** the chat SHALL move to the top of the list of every open view showing it, while the dialog is still open

#### Scenario: Continue a case chat from the case tab
- **WHEN** the user continues a chat from the case's "AI" tab
- **THEN** the chat dialog SHALL be connected to the open case view, so case-related actions such as creating a document are available

#### Scenario: Custom prompt renamed or deleted
- **WHEN** the user continues a chat started with a custom prompt that was renamed or deleted since, and its assistant configuration, request type, action and model still exist
- **THEN** the chat SHALL continue with the stored model, system prompt and prompt configuration without asking the user

#### Scenario: Capability no longer available
- **WHEN** the user continues a chat whose assistant configuration or action no longer exists
- **THEN** the user SHALL be offered the currently available chat capabilities
- **AND** the chat SHALL continue with the selected capability

### Requirement: Live Refresh Of Chat Views
The client SHALL publish an `AiChatSavedEvent` on the `EventBroker` after every successful save,
rename or assignment of a chat and an `AiChatDeletedEvent` after deleting chats. Both events SHALL carry the chat
id(s) and the case id (or none). `AiChatHistoryPanel` SHALL consume these events and SHALL
compare the chat id of every event with the id of the chat currently displayed in its
transcript. It SHALL reload or clear the transcript only if the ids match. Independently, it
SHALL update only the affected list entry if the event belongs to its scope (case mode: same
case; own-chats mode: no case), and SHALL remove the entry of a chat that left its scope,
preserving the selection. Changes made by other users or other
clients SHALL be loaded via "Aktualisieren".

#### Scenario: Continued chat updates the displayed transcript
- **WHEN** chat A is displayed in a view and the user continues chat A in the chat dialog and a message is saved
- **THEN** the view SHALL reload the transcript of chat A
- **AND** chat A SHALL move to the top of the list

#### Scenario: Save of another chat does not touch the transcript
- **WHEN** chat A is displayed in a view and chat B of the same scope is saved
- **THEN** the transcript of chat A SHALL NOT be reloaded or re-rendered
- **AND** chat B SHALL be inserted or moved to the top of the list
- **AND** chat A SHALL stay selected

#### Scenario: New chat from the case appears in the open case tab
- **WHEN** the "AI" tab of a case is open and the user starts a new chat from the Ingo menu of that case
- **THEN** the new chat SHALL appear at the top of the list after its first save

#### Scenario: Event outside the scope is ignored
- **WHEN** a view shows the chats of case X and a chat of case Y is saved
- **THEN** neither the list nor the transcript of the view SHALL change

#### Scenario: Displayed chat deleted in another view
- **WHEN** chat A is displayed in a view and chat A is deleted in another view
- **THEN** the view SHALL clear its transcript
- **AND** remove chat A from its list

### Requirement: Delete Chats From Chat Views
Every chat view SHALL allow deleting one or more selected chats after a confirmation, subject
to the permissions of the "Chat Visibility And Permissions" requirement.

#### Scenario: Delete a chat
- **WHEN** the user selects a chat, chooses "Löschen" and confirms
- **THEN** the chat and its messages SHALL be deleted
- **AND** the chat SHALL disappear from the list

#### Scenario: Deletion cancelled
- **WHEN** the user cancels the confirmation
- **THEN** the chat SHALL remain stored

### Requirement: No Case History Entries For Chats
The system SHALL NOT write case history entries for creating, continuing, renaming, assigning or
deleting chats.

#### Scenario: Deleting a case chat
- **WHEN** a user deletes a case chat
- **THEN** no entry SHALL be added to the case history

