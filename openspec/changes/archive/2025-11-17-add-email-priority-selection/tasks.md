# Implementation Tasks

## Prerequisites
- [ ] Read this proposal and design documents
- [ ] Ensure development environment has NetBeans IDE installed (for GUI Builder)
- [ ] Backup SendEmailFrame.form and SendEmailFrame.java before modifications

## UI Implementation

- [ ] Open `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/SendEmailFrame.form` in NetBeans GUI Builder
- [ ] Add JLabel component with text "Priorität:" positioned near the subject field
- [ ] Add JComboBox component named `cmbPriority` positioned next to the label
- [ ] Configure layout constraints to align with existing metadata fields (subject, recipients)
- [ ] Save the form - NetBeans will update both .form and .java files

## Code Implementation

- [ ] In `SendEmailFrame.java`, add constant for UserSettings key: `private static final String CONF_MAIL_PRIORITY = "jlawyer.mail.priority";` (or add to UserSettings class if preferred)
- [ ] In constructor or initialization method, populate `cmbPriority` with three items: "Normal", "Hoch (High)", "Niedrig (Low)"
- [ ] In constructor or initialization method, load last-used priority from UserSettings and set combobox selection (default to "Normal" if not found)
- [ ] Locate the email sending method where `MimeMessage` is constructed (around line 1240-1270)
- [ ] After `msg.setSubject()` call, add logic to read `cmbPriority.getSelectedItem()` and apply priority headers
- [ ] Implement priority header mapping: High → (X-Priority:1, Priority:Urgent, Importance:high), Normal → (X-Priority:3, Priority:Normal, Importance:normal), Low → (X-Priority:5, Priority:Non-Urgent, Importance:low)
- [ ] After successful email send, save selected priority to UserSettings using the setting key

## Testing

- [ ] Compile and run the j-lawyer client application
- [ ] Open the SendEmailFrame dialog and verify priority dropdown is visible and properly positioned
- [ ] Verify dropdown contains three options with "Normal" as default
- [ ] Send a test email with "Normal" priority and verify headers in received email (use email client or check raw message)
- [ ] Send a test email with "Hoch (High)" priority and verify high priority display in Outlook/Thunderbird/Gmail
- [ ] Send a test email with "Niedrig (Low)" priority and verify low priority display in recipient client
- [ ] Close and reopen SendEmailFrame - verify last selected priority is restored
- [ ] Restart j-lawyer client and verify priority preference persists across application restarts
- [ ] Verify .form file is valid by reopening in NetBeans GUI Builder without errors

## Documentation

- [ ] Add JavaDoc comments to any new methods related to priority handling
- [ ] Update any relevant user documentation or help files (if applicable)

## Dependencies
This change has no dependencies on other modules or changes. It is self-contained within the client module.

## Validation Checklist
- [ ] Priority dropdown displays correctly in SendEmailFrame
- [ ] Three priority options are available: Normal, High, Low
- [ ] Default selection is "Normal"
- [ ] Priority headers are correctly applied to sent emails (verified by checking email headers)
- [ ] Major email clients (Outlook, Thunderbird, Gmail) display priority indicators correctly
- [ ] User preference is saved after sending email
- [ ] User preference is restored when reopening dialog
- [ ] User preference persists across application restarts
- [ ] .form file remains compatible with NetBeans GUI Builder
- [ ] No regression in existing email sending functionality
