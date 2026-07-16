/**
 * Declarative catalog of settings sections per role-scoped screen. Each screen renders its sections
 * through the shared {@link SettingsScreenComponent}, which groups them by `groupKey`, lets the user
 * search by title/keywords, and renders the matching editor for the selected section's `kind`.
 *
 * Collapsing the desktop's ~65 menu items into these lists is the whole point: most are just option
 * groups ("Wertevorrat"), edited by one generic editor keyed on `optionGroup`.
 */

/** Which editor renders a section's detail pane. */
export type SectionKind = 'optionGroup' | 'users' | 'groups' | 'firmProfile'
  | 'invoiceTypes' | 'invoicePools' | 'invoicePositions' | 'financeSettings';

export interface SettingsSection {
  /** Stable id (also the list-selection key). */
  id: string;
  /** i18n key for the section title. */
  titleKey: string;
  /** i18n key for the group subheading it sits under. */
  groupKey: string;
  /** Extra raw search terms (space-separated), matched alongside the translated title. */
  keywords?: string;
  kind: SectionKind;
  /** Option-group key for `kind === 'optionGroup'`. */
  optionGroup?: string;
}

/** Helper for the many option-group value-list sections. */
function optGroup(id: string, titleKey: string, groupKey: string, optionGroup: string, keywords = ''): SettingsSection {
  return { id, titleKey, groupKey, kind: 'optionGroup', optionGroup, keywords };
}

const G_ADDR = 'settings.group.addresses';
const G_CASE = 'settings.group.cases';
const G_DOC = 'settings.group.documents';
const G_FIN = 'settings.group.finance';
const G_TIME = 'settings.group.timeTracking';
const G_USERS = 'settings.group.users';
const G_FIRM = 'settings.group.firm';
const G_INVOICING = 'settings.group.invoicing';

/** "Allgemein" screen — dictionaries/value lists open to any user (edit needs option-group roles). */
export const GENERAL_SECTIONS: SettingsSection[] = [
  optGroup('salutation', 'settings.section.salutation', G_ADDR, 'address.salutation', 'anrede'),
  optGroup('title', 'settings.section.title', G_ADDR, 'address.title', 'anrede titel'),
  optGroup('titleInAddress', 'settings.section.titleInAddress', G_ADDR, 'address.titleinaddress', 'briefkopf'),
  optGroup('complimentaryClose', 'settings.section.complimentaryClose', G_ADDR, 'address.complimentaryclose', 'grussformel'),
  optGroup('degreePrefix', 'settings.section.degreePrefix', G_ADDR, 'address.degreeprefix', 'akademisch grad'),
  optGroup('degreeSuffix', 'settings.section.degreeSuffix', G_ADDR, 'address.degreesuffix', 'akademisch grad'),
  optGroup('profession', 'settings.section.profession', G_ADDR, 'address.profession', 'beruf'),
  optGroup('contactRole', 'settings.section.contactRole', G_ADDR, 'address.role', 'rolle funktion'),
  optGroup('legalForm', 'settings.section.legalForm', G_ADDR, 'address.legalform', 'rechtsform'),
  optGroup('nationality', 'settings.section.nationality', G_ADDR, 'address.nationality', 'staatsangehoerigkeit'),
  optGroup('state', 'settings.section.state', G_ADDR, 'address.state', 'bundesland'),
  optGroup('country', 'settings.section.country', G_ADDR, 'address.country', 'land'),
  optGroup('addressTags', 'settings.section.addressTags', G_ADDR, 'address.tags', 'etikett label'),

  optGroup('subjectField', 'settings.section.subjectField', G_CASE, 'archiveFile.subjectField', 'sachgebiet'),
  optGroup('caseTags', 'settings.section.caseTags', G_CASE, 'archiveFile.tags', 'etikett label akte'),

  optGroup('documentTags', 'settings.section.documentTags', G_DOC, 'document.tags', 'etikett label dokument'),
  optGroup('pdfStamps', 'settings.section.pdfStamps', G_DOC, 'document.pdfstamps', 'stempel'),

  optGroup('currency', 'settings.section.currency', G_FIN, 'invoice.currency', 'waehrung'),
  optGroup('taxRates', 'settings.section.taxRates', G_FIN, 'invoice.taxrates', 'steuersatz mwst'),

  optGroup('timeIncrements', 'settings.section.timeIncrements', G_TIME, 'timesheet.intervalminutes', 'taktung zeiterfassung'),
];

/** "Administration" screen — needs adminRole. */
export const ADMINISTRATION_SECTIONS: SettingsSection[] = [
  { id: 'firmProfile', titleKey: 'settings.section.firmProfile', groupKey: G_FIRM, kind: 'firmProfile', keywords: 'kanzlei kanzleidaten firma stammdaten bank' },
  { id: 'invoiceTypes', titleKey: 'settings.section.invoiceTypes', groupKey: G_INVOICING, kind: 'invoiceTypes', keywords: 'belegart rechnung typ beleg' },
  { id: 'invoicePools', titleKey: 'settings.section.invoicePools', groupKey: G_INVOICING, kind: 'invoicePools', keywords: 'belegnummer nummernkreis rechnungsnummer' },
  { id: 'invoicePositions', titleKey: 'settings.section.invoicePositions', groupKey: G_INVOICING, kind: 'invoicePositions', keywords: 'belegposition vorlage position rechnung' },
  { id: 'financeSettings', titleKey: 'settings.section.financeSettings', groupKey: G_INVOICING, kind: 'financeSettings', keywords: 'girocode epc qr finanzen einstellungen' },
  { id: 'users', titleKey: 'settings.section.users', groupKey: G_USERS, kind: 'users', keywords: 'nutzer benutzer rollen' },
  { id: 'groups', titleKey: 'settings.section.groups', groupKey: G_USERS, kind: 'groups', keywords: 'gruppen' },
];

/** "System" screen — needs sysAdminRole. Empty in Wave 1 (all sections need new endpoints). */
export const SYSTEM_SECTIONS: SettingsSection[] = [];
