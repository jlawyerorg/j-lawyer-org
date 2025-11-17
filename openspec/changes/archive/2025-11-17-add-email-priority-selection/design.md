# Design: Email Priority Selection

## Technical Approach

### UI Component Design
Add a `JComboBox` component to `SendEmailFrame` for priority selection:
- **Component name**: `cmbPriority`
- **Options**: "Normal", "Hoch (High)", "Niedrig (Low)"
- **Default**: "Normal"
- **Placement**: Near the subject field, aligned with other metadata controls
- **Label**: "Priorität:" (consistent with German UI labels used throughout)

### JavaMail Priority Headers
Apply these standard headers when sending email:

**High Priority:**
```
X-Priority: 1
Priority: Urgent
Importance: high
```

**Normal Priority:**
```
X-Priority: 3
Priority: Normal
Importance: normal
```

**Low Priority:**
```
X-Priority: 5
Priority: Non-Urgent
Importance: low
```

These headers are recognized by:
- Microsoft Outlook (`Importance` header)
- Mozilla Thunderbird (`X-Priority` header)
- Gmail and other webmail (`X-Priority` and `Priority`)

### Implementation Locations

**1. SendEmailFrame.java / SendEmailFrame.form**
- Add `cmbPriority` JComboBox component
- Position near subject field (after `txtSubject`)
- Add label "Priorität:"
- Initialize with three options: "Normal", "Hoch (High)", "Niedrig (Low)"
- Load last-used priority from UserSettings on dialog initialization
- Save selected priority to UserSettings when email is successfully sent

**2. Email Sending Logic (in SendEmailFrame.java)**
In the `cmdSend` button action or the method that constructs the `MimeMessage`:
- Read selected value from `cmbPriority`
- Map selection to priority headers:
  - "Hoch (High)" → high priority headers
  - "Normal" → normal priority headers (or omit headers)
  - "Niedrig (Low)" → low priority headers
- Apply headers to `MimeMessage` before calling `msg.saveChanges()`

**3. UserSettings Integration**
- **Setting key**: `CONF_MAIL_PRIORITY` (new constant)
- **Values**: "NORMAL", "HIGH", "LOW"
- **Load on init**: Read from settings and set combobox selection
- **Save on send**: Store selected priority after successful send

### Code Changes Required

**File**: `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/SendEmailFrame.java`
- Add `cmbPriority` field declaration
- In `initComponents()` or constructor: initialize combobox, load last-used priority from settings
- In send method (around line 1240-1270 where `MimeMessage` is constructed):
  - After `msg.setSubject()` and before `msg.saveChanges()`, add priority headers based on `cmbPriority.getSelectedItem()`
- After successful send: save priority preference to UserSettings

**File**: `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/SendEmailFrame.form`
- Add JComboBox component `cmbPriority`
- Add JLabel "Priorität:"
- Position in layout near subject field

**File**: `com.jdimension.jlawyer.client.settings.UserSettings` (if needed)
- Add constant: `public static final String CONF_MAIL_PRIORITY = "jlawyer.mail.priority";`

### Priority Header Application
```java
String priority = (String) cmbPriority.getSelectedItem();
if (priority != null) {
    if (priority.startsWith("Hoch")) {
        msg.setHeader("X-Priority", "1");
        msg.setHeader("Priority", "Urgent");
        msg.setHeader("Importance", "high");
    } else if (priority.startsWith("Niedrig")) {
        msg.setHeader("X-Priority", "5");
        msg.setHeader("Priority", "Non-Urgent");
        msg.setHeader("Importance", "low");
    } else {
        // Normal priority - set explicit headers
        msg.setHeader("X-Priority", "3");
        msg.setHeader("Priority", "Normal");
        msg.setHeader("Importance", "normal");
    }
}
```

### NetBeans GUI Builder Compatibility
Since `SendEmailFrame.form` exists, the JComboBox must be added through NetBeans GUI Builder to maintain .form file consistency. The manual code changes will be limited to:
- Business logic for reading the selected value
- Applying headers to the MimeMessage
- UserSettings integration

## Testing Strategy
- **Manual testing**: Send emails with each priority level to different email clients (Outlook, Thunderbird, Gmail)
- **Verification**: Check received email headers and display in recipient clients
- **Settings persistence**: Verify last-used priority is remembered across dialog opens
- **Default behavior**: Confirm "Normal" is the default for new users

## Alternative Approaches Considered

**Alternative 1: Radio buttons instead of dropdown**
- **Rejected**: Takes more UI space; dropdown is more compact and conventional for priority selection

**Alternative 2: Priority buttons in toolbar**
- **Rejected**: Less discoverable; dropdown near subject is more intuitive

**Alternative 3: Support additional priority levels (5 levels instead of 3)**
- **Rejected**: Most email clients recognize 3 levels; more levels add complexity without clear benefit

## Assumptions
- Users are familiar with email priority concepts from other email clients
- German UI labels are appropriate (existing pattern in codebase)
- Standard MIME headers are sufficient (no custom server-side processing needed)
- Priority selection is per-email, not per-mailbox or per-case
