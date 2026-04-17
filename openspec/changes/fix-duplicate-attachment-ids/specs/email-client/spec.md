## ADDED Requirements

### Requirement: Unique Attachment Identification
The email client SHALL uniquely identify each attachment in a message regardless of whether multiple attachments share the same filename.

#### Scenario: Opening distinct attachments with identical filenames
- **WHEN** a user views a message containing two attachments both named "scan.pdf" with different content
- **AND** the user double-clicks the second attachment in the list
- **THEN** the system SHALL open the content of the second attachment, not the first

#### Scenario: Saving distinct attachments with identical filenames to disk
- **WHEN** a user selects and saves the second of two attachments both named "report.xlsx"
- **THEN** the saved file SHALL contain the content of the second attachment

#### Scenario: Saving distinct attachments with identical filenames to a case
- **WHEN** a user selects and saves an attachment to a case from a message with duplicate filenames
- **THEN** the saved document SHALL contain the content of the selected attachment

#### Scenario: Attachment display shows original filenames
- **WHEN** a message contains multiple attachments with the same filename
- **THEN** the attachment list SHALL display the original filename for each attachment without modification
