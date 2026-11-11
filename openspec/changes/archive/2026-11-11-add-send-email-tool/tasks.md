## 1. Server

- [x] 1.1 `sendMailReturningEml(...)` in `EmailServiceRemote` and `EmailServiceLocal`
- [x] 1.2 `EmailService`: SMTP path returns the transported `MimeMessage` (`writeTo`), Graph path the
      message built with `buildMimeMultipart`; both with `Bcc`; shared code with `sendMail`, which
      keeps its behaviour
- [x] 1.3 Same `loginRole` and `checkMailboxAccess`, folder cache invalidation as `sendMail`

## 2. Client tool

- [x] 2.1 `send_email` definition in `ToolRegistry` (`RISK_MEDIUM`, German texts)
- [x] 2.2 `executeSendEmail`: validation before sending (mailbox, addresses, recipient cap,
      attachments and size, case / folder / tags), signature for text or HTML, attachments from case
      documents
- [x] 2.3 Filing with `caseId`: `buildEmlDocumentName`, name clash suffix, outgoing metadata via
      `MailDocumentOrigins`, folder, tags, keywords, case history, `DocumentAddedEvent`
- [x] 2.4 `markDocumentsSent` for case documents sent as attachments
- [x] 2.5 Partial success: sent but not filed returns `sent: true, savedToCase: false`
- [x] 2.6 `formatToolCallSummary`: "E-Mail an … senden"

## 3. Verification

- [ ] 3.1 SMTP mailbox: send to an own test address with attachment and `caseId`; message in the
      Sent folder, `.eml` in the case identical to the received message, metadata and history
- [ ] 3.2 Graph mailbox: same, `.eml` equal in content
- [ ] 3.3 Refusals before sending: foreign mailbox, invalid address, 21 recipients, archived case,
      unknown tag, attachment over the limit - nothing sent, nothing stored
- [ ] 3.4 Approval dialog shows recipients, subject and text; denial sends nothing
