# Change: Send e-mails from the AI assistant, optionally filed into a case

## Why

The assistant can find, read and file e-mails (`list_mailboxes`, `search_emails`, `get_email`,
`save_email_to_case`) but cannot send one. A typical request - "schick dem Mandanten den Entwurf
und leg die Mail zur Akte" - therefore ends with the user copying text into the send dialog.

Since `add-graph-api-mail`, every mail leaves through the server (`EmailServiceRemote.sendMail`,
SMTP or Microsoft Graph), so a tool needs no transport of its own. One gap remains: `sendMail`
returns nothing. The send dialog files a sent mail by rebuilding an `.eml` on the client
(`EmailUtils.buildEmlBytes`), which differs from what actually went out - another Message-ID and
date, no `multipart/related` for inline images. A tool that files sent mail should file the real
message, and so should the MCP server (`j-lawyer-mcpserver`), which implements the same tool
independently.

## What Changes

- **Server:** `EmailServiceRemote`/`Local` gain `sendMailReturningEml(...)` - same parameters,
  permissions and behaviour as `sendMail`, returning the sent message as RFC 822 bytes. SMTP returns
  the `MimeMessage` that was transported; Graph returns the message built from the same parts
  (Graph does not hand back the sent message). The copy carries the `Bcc` header, like the copy in
  the Sent folder. `sendMail` is unchanged.
- **Client:** new tool `send_email` in `ToolRegistry`, `RISK_MEDIUM` (approval dialog showing
  recipients, subject and text).
  - Parameters: `mailboxId`, `to`, `subject`, `body` (required); `cc`, `bcc`, `html`,
    `useSignature`, `attachmentDocumentIds`, `caseId`, `folderId`, `tags`, `keywords` (optional).
  - Everything that can fail is checked **before** sending: mailbox ACL, addresses, at most 20
    recipients, attachments readable and at most 20 MB together, and with `caseId` the case
    (exists, not archived), folder and tags.
  - With `caseId` the returned `.eml` is filed like `save_email_to_case` files a message: document
    name template, outgoing correspondent (first recipient, "+N"), title = subject, received date =
    sending time, folder, tags, keywords, case history "E-Mail gesendet an …".
  - Case documents sent as attachments are marked as sent to the first recipient
    (`markDocumentsSent`), as the send dialog does.
  - A failure to file after a successful send is reported as success with `savedToCase: false` and
    the reason - never as an error, so that the model does not send again.
- The MCP server mirrors the tool in a change of its own (`add-send-email-tool` in
  `j-lawyer-mcpserver`), verbatim in id, parameters and description.

## Out of Scope

- Replies with threading (`In-Reply-To`/`References`) - new messages only.
- The send dialog (`SendAction`) keeps its client-side `.eml`; switching it to
  `sendMailReturningEml` is a recommended follow-up.
- Graph sends attachments inline in the `sendMail` request, which Graph limits to roughly 3-4 MB;
  larger attachments on Exchange mailboxes fail with Graph's error. Upload sessions are not part of
  this change.

## Impact

- Affected specs: `email-service` (introduced by `add-graph-api-mail`, not yet archived),
  `ai-assistant-integration`
- Affected code: `EmailServiceRemote`, `EmailServiceLocal`, `EmailService` (SMTP and Graph paths),
  `ToolRegistry` (definition, `executeSendEmail`, `formatToolCallSummary`)
- The MCP WAR must be built against the j-lawyer release that contains `sendMailReturningEml`.
