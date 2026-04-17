## Context
Email messages can have multiple attachments sharing the same filename (e.g. two files both named "scan.pdf"). The system currently uses the filename as the attachment identifier on both server and client, making it impossible to distinguish between them.

Three code paths are affected: server-based IMAP, server-based Graph API (already correct), local EML files, and Outlook MSG files.

## Goals / Non-Goals
- Goals: Every attachment in a message is uniquely addressable regardless of filename
- Non-Goals: Changing the Graph API path (already uses Microsoft's unique IDs)

## Decisions

### Server: Use positional index as IMAP attachmentId
- Decision: Set `attachmentId` to `String.valueOf(result.size())` — the zero-based position at which each attachment is added to the result list
- Why: MIME tree traversal is deterministic for a given message. Both `extractAttachments()` and `extractAttachmentsMetadata()` use identical traversal logic, so the same attachment always gets the same index. No content reading required (preserves lazy loading).
- Alternatives considered:
  - Content hash: Requires reading attachment bytes during metadata-only extraction, defeating lazy loading
  - UUID: Not reproducible across calls for the same message — `imapGetSingleAttachment` re-fetches all attachments and searches by ID, so IDs must be stable

### Client: Wrapper object in JList instead of plain String
- Decision: Introduce an `AttachmentListEntry` inner class with `displayName`, `attachmentId`, and `index` fields. `toString()` returns `displayName` so the JList renders filenames as before.
- Why: The JList selection must carry enough information to uniquely identify an attachment across all three retrieval paths (server IMAP, local EML, Outlook MSG).

### Client: Index-based retrieval for local EML
- Decision: Add `EmailUtils.getAttachmentBytes(int index, MessageContainer)` overload that returns the nth attachment by position in the MIME tree
- Why: Local EML files have no server-assigned attachmentId; positional index is the only stable identifier

## Risks / Trade-offs
- Index stability depends on MIME tree traversal consistency between metadata and full-load paths. Both methods use identical recursive logic, so this is reliable. If the traversal logic is ever changed in one method but not the other, indices could mismatch — a code comment will flag this dependency.
