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
