# Add Email Priority Selection

## Why
Currently, the email send dialog in the j-lawyer client does not provide a way to set the email priority (importance level). This is a standard feature in email clients that allows users to mark emails as high priority, normal, or low priority. Legal professionals often need to communicate the urgency of correspondence, especially for time-sensitive matters like court deadlines, urgent client requests, or critical notices.

Without priority setting capability, users cannot:
- Signal urgency to recipients whose email clients support priority flags
- Differentiate urgent legal matters from routine correspondence
- Leverage recipient email client features that sort or highlight priority messages

## What
Add a priority selection dropdown to the `SendEmailFrame` dialog that allows users to choose from:
- **High Priority** (Urgent/Important)
- **Normal Priority** (default)
- **Low Priority**

The selected priority will be applied to outgoing emails using standard JavaMail API headers (`X-Priority`, `Priority`, and `Importance` headers) that are recognized by most email clients including Outlook, Thunderbird, and modern webmail systems.

## Impact
- **Client UI**: Modification to `SendEmailFrame.java` and `SendEmailFrame.form` to add priority selection dropdown
- **Email sending logic**: Update email composition to apply priority headers based on user selection
- **User settings**: Store last-used priority preference per user
- **No server changes required**: This is a client-only feature using standard SMTP/MIME headers
- **No database schema changes**: User preference stored in existing `UserSettings` mechanism
- **Backward compatibility**: Maintained - priority headers are optional and ignored by clients that don't support them

## Scope
### In Scope
- Add priority selection UI component to SendEmailFrame
- Implement priority header application in email sending logic
- Persist user's last selected priority preference
- Support standard priority levels (High, Normal, Low)

### Out of Scope
- Displaying priority of received emails (viewing/reading emails)
- Priority-based filtering or sorting in email folders
- Server-side priority handling or storage
- REST API changes for mobile app priority support
- Priority selection for mass mail functionality (separate feature)

## Non-Functional Requirements
- **Performance**: No measurable impact - adding headers is negligible overhead
- **Usability**: Priority dropdown should be clearly labeled and positioned near other email metadata fields (subject, recipients)
- **Compatibility**: Must work with existing NetBeans GUI Builder (.form file)
- **Accessibility**: Follow existing UI patterns and FlatLaf look-and-feel conventions

## Risks & Mitigations
- **Risk**: Different email clients interpret priority headers differently
  - **Mitigation**: Use standard headers (`X-Priority`, `Priority`, `Importance`) that have broad support
- **Risk**: Users may overuse "high priority" defeating the purpose
  - **Mitigation**: Default to "Normal" priority; education is outside scope of technical implementation

## Related Work
- Email sending implemented in: `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/SendEmailFrame.java`
- Email utilities: `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/EmailUtils.java`
- User settings: `com.jdimension.jlawyer.client.settings.UserSettings`
