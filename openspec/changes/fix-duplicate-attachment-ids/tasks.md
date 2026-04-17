## 1. Server: Unique IMAP attachmentId
- [ ] 1.1 In `EmailService.extractAttachments()`: change `setAttachmentId` calls to use `String.valueOf(result.size())` (lines 1734, 1763)
- [ ] 1.2 In `EmailService.extractAttachmentsMetadata()`: same change (lines 1838, 1867)
- [ ] 1.3 Add code comment noting that both methods must use identical traversal order

## 2. Client: AttachmentListEntry wrapper
- [ ] 2.1 Create `AttachmentListEntry` inner class in `MailContentUI` with `displayName`, `attachmentId`, `index`, and `toString()` returning `displayName`
- [ ] 2.2 Change server-based list population (~line 2813-2827) to add `AttachmentListEntry` objects instead of plain strings
- [ ] 2.3 Change `loadSingleAttachmentContent` to accept `attachmentId` and match by `att.getAttachmentId()` instead of `att.getName()`

## 3. Client: Update consumer sites
- [ ] 3.1 Double-click open handler (~line 2116-2149): extract `AttachmentListEntry` from selection, use `attachmentId` for server path, `index` for local/Outlook paths
- [ ] 3.2 Save-as-file handler (~line 2155-2236): same pattern
- [ ] 3.3 Save-to-case handler (~line 2238-2310): same pattern
- [ ] 3.4 ICS calendar handling (~line 1027): update if it searches by name

## 4. Client: Local EML index-based retrieval
- [ ] 4.1 Add `EmailUtils.getAttachmentBytes(int index, MessageContainer)` overload
- [ ] 4.2 Add corresponding `getAttachmentPart(int index, Object partObject, Folder folder)` that counts attachments positionally
- [ ] 4.3 Update local EML list population to create `AttachmentListEntry` with index

## 5. Client: Outlook MSG index-based retrieval
- [ ] 5.1 Update Outlook MSG retrieval in consumer sites to use `entry.getIndex()` for positional lookup instead of filename matching

## 6. Verification
- [ ] 6.1 Test with IMAP message containing 2+ attachments with identical filenames
- [ ] 6.2 Test with IMAP message with unique filenames (regression)
- [ ] 6.3 Test save-as-file and save-to-case with duplicate filenames
- [ ] 6.4 Test inline CID images are still correctly skipped
- [ ] 6.5 Test ICS calendar attachment handling
