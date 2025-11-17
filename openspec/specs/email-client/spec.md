# email-client Specification

## Purpose
TBD - created by archiving change add-email-priority-selection. Update Purpose after archive.
## Requirements
### Requirement: Email Priority Selection
The email send dialog SHALL provide a priority selection control allowing users to set the importance level of outgoing emails.

#### Scenario: User selects high priority for urgent email
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user selects "Hoch (High)" from the priority dropdown
**And** the user sends the email
**Then** the outgoing email SHALL include headers `X-Priority: 1`, `Priority: Urgent`, and `Importance: high`
**And** recipient email clients SHALL display the email as high priority/urgent

#### Scenario: User sends email with normal priority (default)
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user has not changed the priority selection (defaults to "Normal")
**And** the user sends the email
**Then** the outgoing email SHALL include headers `X-Priority: 3`, `Priority: Normal`, and `Importance: normal`
**And** recipient email clients SHALL display the email as normal priority

#### Scenario: User selects low priority for non-urgent email
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user selects "Niedrig (Low)" from the priority dropdown
**And** the user sends the email
**Then** the outgoing email SHALL include headers `X-Priority: 5`, `Priority: Non-Urgent`, and `Importance: low`
**And** recipient email clients SHALL display the email as low priority

### Requirement: Priority Selection UI Component
The SendEmailFrame dialog SHALL display a priority dropdown positioned near the subject field with three options.

#### Scenario: Priority dropdown is visible and accessible
**Given** a user opens the SendEmailFrame dialog
**Then** a priority dropdown labeled "Priorität:" SHALL be visible
**And** the dropdown SHALL be positioned near the subject field
**And** the dropdown SHALL contain exactly three options: "Normal", "Hoch (High)", "Niedrig (Low)"
**And** "Normal" SHALL be the default selected option

#### Scenario: Priority dropdown maintains consistent UI style
**Given** the SendEmailFrame uses NetBeans GUI Builder and FlatLaf look-and-feel
**When** the priority dropdown is rendered
**Then** it SHALL use a JComboBox component
**And** it SHALL follow the same styling as other dropdown controls in the dialog
**And** the .form file SHALL be updated to maintain GUI Builder compatibility

### Requirement: Priority Preference Persistence
The application SHALL remember the user's last selected email priority and restore it when opening the send dialog.

#### Scenario: Last used priority is restored on dialog open
**Given** a user previously sent an email with "Hoch (High)" priority
**When** the user opens the SendEmailFrame dialog again
**Then** the priority dropdown SHALL be pre-selected to "Hoch (High)"

#### Scenario: Priority preference is saved after successful send
**Given** a user selects "Niedrig (Low)" priority
**When** the user successfully sends the email
**Then** the application SHALL save "LOW" to UserSettings with key `CONF_MAIL_PRIORITY`
**And** this preference SHALL persist across application restarts

#### Scenario: Default priority for new users
**Given** a user who has never sent an email with priority selection
**When** the user opens the SendEmailFrame dialog for the first time
**Then** the priority dropdown SHALL default to "Normal"
**And** the UserSettings SHALL have no saved priority preference

### Requirement: Standard Email Priority Headers
The application SHALL apply email priority headers using standard MIME header conventions recognized by major email clients.

#### Scenario: High priority headers conform to standards
**Given** a user sends an email with "Hoch (High)" priority
**Then** the MimeMessage SHALL include header `X-Priority` with value `1`
**And** SHALL include header `Priority` with value `Urgent`
**And** SHALL include header `Importance` with value `high`

#### Scenario: Normal priority headers conform to standards
**Given** a user sends an email with "Normal" priority
**Then** the MimeMessage SHALL include header `X-Priority` with value `3`
**And** SHALL include header `Priority` with value `Normal`
**And** SHALL include header `Importance` with value `normal`

#### Scenario: Low priority headers conform to standards
**Given** a user sends an email with "Niedrig (Low)" priority
**Then** the MimeMessage SHALL include header `X-Priority` with value `5`
**And** SHALL include header `Priority` with value `Non-Urgent`
**And** SHALL include header `Importance` with value `low`

#### Scenario: Priority headers are applied before message finalization
**Given** a user is sending an email with any priority level
**When** the MimeMessage is being constructed
**Then** priority headers SHALL be set after subject and recipients
**And** SHALL be set before `msg.saveChanges()` is called
**And** SHALL be included in the final transmitted email

