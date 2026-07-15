import { CaseDocument, DocMetaWrite, HIGHLIGHT_NONE } from './case.models';

/** File extensions LibreOffice/Stirling can convert to PDF (mirrors FileTypes.LO_OFFICEFILETYPES). */
export const CONVERTIBLE_EXT = new Set([
  'FODT', 'FODS', 'FODP', 'ODT', 'OTT', 'OTH', 'ODS', 'ODP', 'OTS', 'SXC', 'STC', 'ODM', 'SXW', 'STW', 'SXG',
  'DOC', 'DOCX', 'DOT', 'DOCM', 'DOTX', 'DOTM', 'WPD', 'WPS', 'RTF', 'TXT', 'CSV', 'XLS', 'XLW', 'XLT', 'XLSX',
  'XLSM', 'XLTX', 'XLTM', 'PPT', 'PPS', 'POT', 'PPTX', 'PPTM', 'POTX', 'POTM', 'BMP', 'DXF', 'EMF', 'EPS', 'GIF',
  'JPEG', 'JPG', 'PCX', 'PNG', 'PSD', 'TIF', 'TIFF', 'WMF', 'HTML',
]);

/** Highlight palette (CSS hex + server colour int = 0xFF000000 | rgb, as a signed 32-bit value). */
export const HIGHLIGHTS: { hex: string; value: number }[] = [
  { hex: '#ffd54f', value: (0xff000000 | 0xffd54f) | 0 },
  { hex: '#81c784', value: (0xff000000 | 0x81c784) | 0 },
  { hex: '#64b5f6', value: (0xff000000 | 0x64b5f6) | 0 },
  { hex: '#e57373', value: (0xff000000 | 0xe57373) | 0 },
  { hex: '#ffb74d', value: (0xff000000 | 0xffb74d) | 0 },
  { hex: '#ba68c8', value: (0xff000000 | 0xba68c8) | 0 },
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
