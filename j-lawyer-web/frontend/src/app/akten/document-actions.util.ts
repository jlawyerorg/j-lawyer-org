import { CaseDocument, DocMetaWrite, HIGHLIGHT_NONE } from './case.models';

/** File extensions LibreOffice/Stirling can convert to PDF (mirrors FileTypes.LO_OFFICEFILETYPES). */
export const CONVERTIBLE_EXT = new Set([
  'FODT', 'FODS', 'FODP', 'ODT', 'OTT', 'OTH', 'ODS', 'ODP', 'OTS', 'SXC', 'STC', 'ODM', 'SXW', 'STW', 'SXG',
  'DOC', 'DOCX', 'DOT', 'DOCM', 'DOTX', 'DOTM', 'WPD', 'WPS', 'RTF', 'TXT', 'CSV', 'XLS', 'XLW', 'XLT', 'XLSX',
  'XLSM', 'XLTX', 'XLTM', 'PPT', 'PPS', 'POT', 'PPTX', 'PPTM', 'POTX', 'POTM', 'BMP', 'DXF', 'EMF', 'EPS', 'GIF',
  'JPEG', 'JPG', 'PCX', 'PNG', 'PSD', 'TIF', 'TIFF', 'WMF', 'HTML',
]);

/**
 * Highlight palette (CSS hex + server colour int = 0xFF000000 | rgb, as a signed 32-bit value).
 * Mirrors the desktop client's picker — keep in sync with
 * `themes.colors.DefaultColorTheme.getHighlightColors()` (same colours, same order).
 */
export const HIGHLIGHTS: { hex: string; value: number }[] = [
  // red / orange / yellow
  { hex: '#cc0033', value: (0xff000000 | 0xcc0033) | 0 },
  { hex: '#f08000', value: (0xff000000 | 0xf08000) | 0 },
  { hex: '#b5510e', value: (0xff000000 | 0xb5510e) | 0 },
  { hex: '#ffcc00', value: (0xff000000 | 0xffcc00) | 0 },
  // green / petrol / turquoise
  { hex: '#1e9e4a', value: (0xff000000 | 0x1e9e4a) | 0 },
  { hex: '#006400', value: (0xff000000 | 0x006400) | 0 },
  { hex: '#0e8c8c', value: (0xff000000 | 0x0e8c8c) | 0 },
  { hex: '#31ded5', value: (0xff000000 | 0x31ded5) | 0 },
  // blue / violet / pink
  { hex: '#330dbf', value: (0xff000000 | 0x330dbf) | 0 },
  { hex: '#aa1eca', value: (0xff000000 | 0xaa1eca) | 0 },
  { hex: '#e63b8c', value: (0xff000000 | 0xe63b8c) | 0 },
  // neutral
  { hex: '#ffffff', value: (0xff000000 | 0xffffff) | 0 },
  { hex: '#aaaaaa', value: (0xff000000 | 0xaaaaaa) | 0 },
  { hex: '#666666', value: (0xff000000 | 0x666666) | 0 },
  { hex: '#000000', value: (0xff000000 | 0x000000) | 0 },
];

export function isConvertible(ext: string): boolean {
  return CONVERTIBLE_EXT.has(ext);
}

export function isPdf(ext: string): boolean {
  return ext === 'PDF';
}

/**
 * Builds the full current metadata for a document. The update-metadata endpoint overwrites every
 * field, so callers spread this and change only what they intend (leaving `folderId` out unless
 * moving). See {@link DocMetaWrite}.
 */
export function baseMeta(doc: CaseDocument, caseId: string): DocMetaWrite {
  return {
    id: doc.id,
    caseId,
    name: doc.name,
    creationDate: toServerInstant(doc.date),
    changeDate: toServerInstant(doc.changeDate),
    favorite: doc.favorite,
    highlight1: doc.highlight1Value,
    highlight2: doc.highlight2Value,
    externalId: doc.externalId || null,
    version: doc.version,
  };
}

export { HIGHLIGHT_NONE };

/** ISO instant → "yyyy-MM-dd" for a date input (local); '' when empty/invalid. */
export function toLocalDate(iso: string): string {
  if (!iso) { return ''; }
  const d = new Date(iso);
  if (isNaN(d.getTime())) { return ''; }
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** "yyyy-MM-dd" → ISO instant (UTC 'Z') the server's date parser accepts; '' when empty. */
export function toServerDate(local: string): string {
  if (!local) { return ''; }
  const d = new Date(`${local}T00:00:00`);
  return isNaN(d.getTime()) ? '' : d.toISOString();
}

/** A stored ISO string (or empty) → an instant the server requires non-null; falls back to now. */
export function toServerInstant(iso: string): string {
  if (!iso) { return new Date().toISOString(); }
  const d = new Date(iso);
  return isNaN(d.getTime()) ? new Date().toISOString() : d.toISOString();
}
