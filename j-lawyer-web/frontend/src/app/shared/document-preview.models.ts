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
  /** Which content endpoint holds the bytes: case documents (default) or contact documents. */
  source?: 'case' | 'contact';
}

/** Response of GET /v1/cases/document/{id}/content. */
export interface DocumentContentDto {
  id: string;
  fileName: string;
  caseId: string;
  base64content: string;
}

/**
 * How a document can be previewed in the browser (constrained by the app CSP). `office` documents
 * are rendered via a server-produced PDF (StirlingPDF); `eml`/`bea` via a server-parsed structured view.
 */
export type DocPreviewKind = 'pdf' | 'image' | 'text' | 'html' | 'office' | 'eml' | 'bea' | 'none';

const IMAGE_EXTS = new Set(['PNG', 'JPG', 'JPEG', 'GIF', 'WEBP', 'BMP']);
const TEXT_EXTS = new Set(['TXT', 'MD', 'CSV', 'LOG', 'XML', 'JSON', 'YML', 'YAML']);
/** HTML documents get an editable rich-text (WYSIWYG) editor rather than a read-only viewer. */
const HTML_EXTS = new Set(['HTML', 'HTM']);
/** Office formats previewed via server-side conversion to PDF (mirrors FileTypes.LO_OFFICEFILETYPES, minus text/image). */
const OFFICE_EXTS = new Set([
  'DOC', 'DOCX', 'DOT', 'DOCM', 'DOTX', 'DOTM', 'ODT', 'OTT', 'FODT', 'SXW', 'STW', 'RTF', 'WPD', 'WPS',
  'ODS', 'OTS', 'FODS', 'SXC', 'STC', 'XLS', 'XLSX', 'XLSM', 'XLT', 'XLTX', 'XLW',
  'ODP', 'OTP', 'FODP', 'PPT', 'PPS', 'PPTX', 'PPTM', 'POT', 'POTX',
]);

/**
 * Office preview from the server (GET /v8/cases/document/{id}/preview-pdf): a PDF rendering when
 * available (StirlingPDF), else the extracted text (Tika) as a fallback.
 */
export interface OfficePreview {
  kind: 'pdf' | 'text';
  fileName: string;
  /** Base64 PDF when kind === 'pdf'. */
  base64content?: string;
  /** Extracted text when kind === 'text'. */
  text?: string;
}

/** An attachment shown in the EML / beA preview. */
export interface PreviewAttachment {
  name: string;
  size: number;
}

/** Parsed email (.eml) for preview — from the server (GET /v8/cases/document/{id}/eml). */
export interface EmailPreview {
  subject: string;
  from: string;
  to: string;
  cc: string;
  date: string;
  /** Sanitised HTML body when the mail has one, else ''. */
  htmlBody: string;
  /** Plain-text body when there is no HTML body, else ''. */
  textBody: string;
  attachments: PreviewAttachment[];
}

/** Parsed beA message (.bea) for preview — from the server (GET /v8/cases/document/{id}/bea). */
export interface BeaPreview {
  subject: string;
  from: string;
  to: string;
  sent: string;
  caseNumber: string;
  justizReference: string;
  body: string;
  attachments: PreviewAttachment[];
}

/** Decides how (or whether) a document can be previewed, based on its extension. */
export function previewKindOf(ext: string): DocPreviewKind {
  const e = (ext || '').toUpperCase();
  if (e === 'PDF') {
    return 'pdf';
  }
  if (e === 'EML') {
    return 'eml';
  }
  if (e === 'BEA') {
    return 'bea';
  }
  if (HTML_EXTS.has(e)) {
    return 'html';
  }
  if (IMAGE_EXTS.has(e)) {
    return 'image';
  }
  if (TEXT_EXTS.has(e)) {
    return 'text';
  }
  if (OFFICE_EXTS.has(e)) {
    return 'office';
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

/**
 * Decodes a (whitespace-stripped) Base64 string into raw bytes.
 *
 * The return type is pinned to `Uint8Array<ArrayBuffer>` (not the default
 * `Uint8Array<ArrayBufferLike>` since TypeScript 5.7): the bytes are backed by a
 * freshly allocated `ArrayBuffer`, and `BlobPart` / `new Blob([...])` requires an
 * `ArrayBuffer`-backed view — a plain `Uint8Array` no longer satisfies it.
 */
export function base64ToBytes(b64: string): Uint8Array<ArrayBuffer> {
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

/** Encodes a string as Base64 of its UTF-8 bytes (for saving edited text/HTML content). */
export function textToBase64(text: string): string {
  const bytes = new TextEncoder().encode(text);
  let bin = '';
  for (const b of bytes) {
    bin += String.fromCharCode(b);
  }
  return btoa(bin);
}

/** Display category derived from a file extension (drives the file-type badge icon + colour). */
export type FileKind = 'pdf' | 'word' | 'excel' | 'ppt' | 'image' | 'archive' | 'email' | 'text' | 'generic';

/** Icon glyph (IconComponent) for each file-type category; generic 'doc' is the fallback. */
const KIND_ICON: Record<FileKind, string> = {
  pdf: 'file-text', word: 'file-text', text: 'file-text',
  excel: 'sheet', ppt: 'presentation', image: 'image', archive: 'archive', email: 'mail', generic: 'doc',
};

/** Groups a file extension into a display category. */
export function fileKind(ext: string): FileKind {
  switch ((ext || '').toLowerCase()) {
    case 'pdf': return 'pdf';
    case 'doc': case 'docx': case 'odt': case 'rtf': return 'word';
    case 'xls': case 'xlsx': case 'ods': case 'csv': return 'excel';
    case 'ppt': case 'pptx': case 'odp': return 'ppt';
    case 'png': case 'jpg': case 'jpeg': case 'gif': case 'bmp': case 'tif': case 'tiff': case 'webp': case 'svg':
      return 'image';
    case 'zip': case 'rar': case '7z': case 'tar': case 'gz': return 'archive';
    case 'eml': case 'msg': return 'email';
    case 'txt': case 'md': case 'log': return 'text';
    default: return 'generic';
  }
}

/** The icon glyph for a file-type category. */
export function kindGlyph(kind: FileKind): string {
  return KIND_ICON[kind] ?? 'doc';
}

/** Convenience: the icon glyph directly from a file extension. */
export function fileKindIcon(ext: string): string {
  return kindGlyph(fileKind(ext));
}
