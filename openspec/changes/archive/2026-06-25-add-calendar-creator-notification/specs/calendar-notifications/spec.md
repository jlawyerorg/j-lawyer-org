## ADDED Requirements

### Requirement: Calendar Entry Creator Tracking

The system SHALL record the user who created a calendar entry (Termin/Frist/Wiedervorlage)
so that the creator can be identified for later notifications. The creator SHALL be the
authenticated principal who performed the creation and SHALL be persisted on the entry. The
creator SHALL be immutable after creation: updating an entry SHALL NOT change or clear its
recorded creator, regardless of the value supplied by the client.

#### Scenario: Creator recorded on creation

- **WHEN** a user creates a calendar entry
- **THEN** the entry stores that user's identifier as its creator

#### Scenario: Creator preserved on update

- **WHEN** a calendar entry is updated
- **AND** the client does not supply (or supplies an empty) creator value
- **THEN** the persisted creator is preserved unchanged

#### Scenario: Legacy entries without a creator

- **WHEN** a calendar entry has no recorded creator (created before this capability existed)
- **THEN** the system treats the creator as unknown and sends no creator notification for it

### Requirement: Notify Creator on Modification or Completion

The system SHALL send a mail notification to the creator of a calendar entry when a
different user modifies or completes that entry. The notification SHALL identify the entry
and its case, and SHALL distinguish a completion ("erledigt") from a generic modification
("geändert") in its subject and body. Both the editing-an-entry path and the
mark-as-done path SHALL trigger this notification.

#### Scenario: Entry modified by another user

- **WHEN** user A modifies a calendar entry created by user B
- **AND** B is not the same user as A
- **THEN** B receives a mail notification stating the entry was modified

#### Scenario: Entry completed by another user

- **WHEN** user A marks as done a calendar entry created by user B
- **AND** B is not the same user as A
- **THEN** B receives a mail notification stating the entry was completed

#### Scenario: Creator notification is independent of assignee notification

- **WHEN** user A modifies an entry created by user B and assigned to user C
- **AND** A, B, and C are distinct users
- **THEN** C receives the existing assignee notification
- **AND** B receives the creator notification

### Requirement: Creator Notification Suppression

The system SHALL suppress the creator notification when it would be redundant or unwanted,
to avoid duplicate or self-directed mail.

#### Scenario: Creator edits their own entry

- **WHEN** a user modifies or completes an entry they created themselves
- **THEN** no creator notification is sent

#### Scenario: Creator is also the assignee

- **WHEN** an entry's creator and assignee are the same user
- **AND** another user modifies or completes the entry
- **THEN** that user receives only one notification (the assignee notification) and no
  duplicate creator notification

#### Scenario: Creator has no email or unknown creator

- **WHEN** the creator has no email address configured, or the creator is unknown
- **THEN** no creator notification is sent

### Requirement: Creator Notification User Preference

The system SHALL provide a per-user preference controlling whether the user receives
notifications about entries they created being modified or completed. The preference SHALL
default to disabled (opt-in) and SHALL be configurable in the user profile.

#### Scenario: Preference disabled (default)

- **WHEN** a user has not changed the preference
- **AND** another user modifies or completes an entry that user created
- **THEN** no creator notification is sent to that user

#### Scenario: Preference explicitly enabled

- **WHEN** a user has enabled the creator-notification preference
- **AND** another user modifies or completes an entry that user created
- **THEN** the creator notification is sent
