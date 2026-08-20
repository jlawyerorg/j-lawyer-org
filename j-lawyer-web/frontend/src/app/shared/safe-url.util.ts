import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Wraps a same-origin `blob:` / object URL (created locally via `URL.createObjectURL`)
 * as a trusted `SafeResourceUrl` so it can be bound to an `<iframe [src]>`.
 *
 * This is the single, reviewed choke point for bypassing Angular's resource-URL
 * sanitization. Angular blocks *all* resource URLs (iframe/embed/object src) unless
 * explicitly trusted — there is no alternative for embedding a locally-generated blob.
 * The bypass is safe here because:
 *   - the URL is a `blob:` created by our own code from bytes we fetched over the
 *     authenticated API (never attacker-supplied, never remote), and
 *   - every embedded document is additionally sandboxed at the call site (a sandboxed
 *     iframe and/or a restrictive per-document Content-Security-Policy), so even
 *     untrusted HTML bodies (email, beA messages) cannot execute or exfiltrate.
 *
 * Keeping this in one place means the S6268 review lives on exactly one line instead of
 * being scattered across every viewer component.
 */
export function trustBlobResourceUrl(sanitizer: DomSanitizer, blobUrl: string): SafeResourceUrl {
  return sanitizer.bypassSecurityTrustResourceUrl(blobUrl); // NOSONAR typescript:S6268 — reviewed: same-origin local blob, sandboxed at call site
}
