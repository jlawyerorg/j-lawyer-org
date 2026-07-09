/**
 * Shared document-preview primitives, used by any module that shows a document viewer
 * (the case detail's Dokumente tab and the global fulltext-search results). Extracted so
 * the preview overlay is a single reusable component (OpenSpec change add-web-client).
 */

/** A document identified just enough to fetch + preview it. */
export interface PreviewDoc {
  id: string;
  name: string;
  /** Upper-case file extension (e.g. "PDF"). */
  ext: string;
}

/** Response of GET /v1/cases/document/{id}/content. */
export interface DocumentContentDto {
  id: string;
  fileName: string;
  caseId: string;
  base64content: string;
}

/** How a document can be previewed in the browser (constrained by the app CSP). */
export type DocPreviewKind = 'pdf' | 'image' | 'text' | 'none';

const IMAGE_EXTS = new Set(['PNG', 'JPG', 'JPEG', 'GIF', 'WEBP', 'BMP']);
const TEXT_EXTS = new Set(['TXT', 'MD', 'CSV', 'LOG', 'XML', 'JSON', 'HTML', 'HTM', 'EML', 'YML', 'YAML']);

/** Decides how (or whether) a document can be previewed, based on its extension. */
export function previewKindOf(ext: string): DocPreviewKind {
  const e = (ext || '').toUpperCase();
  if (e === 'PDF') {
    return 'pdf';
  }
  if (IMAGE_EXTS.has(e)) {
    return 'image';
  }
  if (TEXT_EXTS.has(e)) {
    return 'text';
  }
  return 'none';
}

/** Best-effort MIME type for building a Blob / data URL from a document's bytes. */
export function mimeOf(ext: string): string {
  switch ((ext || '').toUpperCase()) {
    case 'PDF': return 'application/pdf';
    case 'PNG': return 'image/png';
    case 'JPG':
    case 'JPEG': return 'image/jpeg';
    case 'GIF': return 'image/gif';
    case 'WEBP': return 'image/webp';
    case 'BMP': return 'image/bmp';
    case 'TXT':
    case 'LOG': return 'text/plain';
    case 'MD': return 'text/markdown';
    case 'CSV': return 'text/csv';
    case 'XML': return 'application/xml';
    case 'JSON': return 'application/json';
    case 'HTML':
    case 'HTM': return 'text/html';
    default: return 'application/octet-stream';
  }
}

/** Decodes a (whitespace-stripped) Base64 string into raw bytes. */
export function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i);
  }
  return bytes;
}

/** Decodes UTF-8 bytes into a string (for text previews). */
export function bytesToText(bytes: Uint8Array): string {
  return new TextDecoder('utf-8').decode(bytes);
}
