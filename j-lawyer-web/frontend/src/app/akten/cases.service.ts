import { computed, Injectable, signal } from '@angular/core';
import { CaseDetail, CaseOverview } from './case.models';

/**
 * Case data access.
 *
 * TEMPORARY: backed by in-memory SAMPLE DATA so the Akten UI can be built and verified
 * before the authentication flow lands (the REST API needs a browser login — see
 * gap-analysis.md, design.md "Authentifizierung"). Swap to real REST here without
 * touching the components: replace the seed with HttpClient calls to
 * GET /j-lawyer-io/rest/v7/cases/list and …/{id} (+ parties, duedates, documents),
 * e.g. via httpResource/toSignal. The models already match the REST DTOs.
 */
@Injectable({ providedIn: 'root' })
export class CasesService {
  private readonly _cases = signal<CaseDetail[]>(SAMPLE_CASES);

  /** List rows (overview projection of the full cases). */
  readonly overviews = computed<CaseOverview[]>(() =>
    this._cases().map((c) => ({
      id: c.id,
      fileNumber: c.fileNumber,
      name: c.name,
      subjectField: c.subjectField,
      lawyer: c.lawyer,
      status: c.status,
      lastChanged: c.lastChanged,
    })),
  );

  find(id: string): CaseDetail | undefined {
    return this._cases().find((c) => c.id === id);
  }
}

const SAMPLE_CASES: CaseDetail[] = [
  {
    id: '00142-25', fileNumber: '00142/25', name: 'Müller ./. Schneider GmbH',
    subjectField: 'Verkehrsrecht', lawyer: 'RA Dr. Kunze', status: 'open', lastChanged: '2026-07-07',
    claimNumber: 'VK-2025-142', claimValue: 4850, assistant: 'RAin Bauer',
    reason: 'Schadensersatz nach Verkehrsunfall', notice: 'Mandant meldet Nutzungsausfall für 9 Tage. Vergleichsbereitschaft der Gegenseite signalisiert.',
    created: '2026-06-12',
    parties: [
      { id: 'p1', involvementType: 'client', contact: 'Thomas Müller' },
      { id: 'p2', involvementType: 'opponent', contact: 'Schneider GmbH' },
      { id: 'p3', involvementType: 'court', contact: 'Amtsgericht München' },
      { id: 'p4', involvementType: 'insurer', contact: 'HUK-Coburg' },
    ],
    dueDates: [
      { id: 'd1', reason: 'Klageerwiderung', dueDate: '2026-07-14', done: false, assignee: 'RA Dr. Kunze', type: 'deadline' },
      { id: 'd2', reason: 'Vergleichsangebot prüfen', dueDate: '2026-07-21', done: false, assignee: 'RAin Bauer', type: 'followup' },
    ],
    documents: [
      { id: 'doc1', name: 'Klageschrift_Müller.pdf', date: '2026-07-01', author: 'RA Dr. Kunze', size: '248 KB', ext: 'PDF' },
      { id: 'doc2', name: 'Unfallbericht_Entwurf.docx', date: '2026-06-28', author: 'RAin Bauer', size: '54 KB', ext: 'DOC' },
      { id: 'doc3', name: 'Foto_Unfallstelle_01.jpg', date: '2026-06-20', author: 'Import Scanner', size: '2,1 MB', ext: 'JPG' },
    ],
  },
  {
    id: '00139-25', fileNumber: '00139/25', name: 'Nachlass Weber', subjectField: 'Erbrecht',
    lawyer: 'RAin Bauer', status: 'dueToday', lastChanged: '2026-07-07',
    claimNumber: 'ER-2025-139', claimValue: 0, assistant: 'RA Dr. Kunze',
    reason: 'Nachlassabwicklung', notice: 'Erbschein beantragt.', created: '2026-05-30',
    parties: [
      { id: 'p1', involvementType: 'client', contact: 'Erbengemeinschaft Weber' },
      { id: 'p2', involvementType: 'court', contact: 'Nachlassgericht München' },
    ],
    dueDates: [{ id: 'd1', reason: 'Frist Erbausschlagung', dueDate: '2026-07-07', done: false, assignee: 'RAin Bauer', type: 'deadline' }],
    documents: [{ id: 'doc1', name: 'Testament_Kopie.pdf', date: '2026-06-02', author: 'RAin Bauer', size: '120 KB', ext: 'PDF' }],
  },
  {
    id: '00131-25', fileNumber: '00131/25', name: 'Bauvorhaben Lindenstraße 12', subjectField: 'Baurecht',
    lawyer: 'RA Dr. Kunze', status: 'open', lastChanged: '2026-07-05',
    claimNumber: 'BR-2025-131', claimValue: 32000, assistant: 'RAin Bauer',
    reason: 'Mängelrüge Bauträger', notice: '', created: '2026-05-18',
    parties: [
      { id: 'p1', involvementType: 'client', contact: 'Eheleute Lindner' },
      { id: 'p2', involvementType: 'opponent', contact: 'BauTrend AG' },
    ],
    dueDates: [{ id: 'd1', reason: 'Wiedervorlage Gutachten', dueDate: '2026-07-18', done: false, assignee: 'RA Dr. Kunze', type: 'followup' }],
    documents: [{ id: 'doc1', name: 'Mängelrüge.pdf', date: '2026-06-30', author: 'RA Dr. Kunze', size: '96 KB', ext: 'PDF' }],
  },
  {
    id: '00128-25', fileNumber: '00128/25', name: 'Kündigung Bauer', subjectField: 'Arbeitsrecht',
    lawyer: 'RAin Bauer', status: 'waiting', lastChanged: '2026-07-02',
    claimNumber: 'AR-2025-128', claimValue: 12500, assistant: 'RA Dr. Kunze',
    reason: 'Kündigungsschutzklage', notice: 'Wartet auf Gütetermin.', created: '2026-04-22',
    parties: [
      { id: 'p1', involvementType: 'client', contact: 'Sabine Bauer' },
      { id: 'p2', involvementType: 'opponent', contact: 'Logistik Süd GmbH' },
      { id: 'p3', involvementType: 'court', contact: 'Arbeitsgericht München' },
    ],
    dueDates: [{ id: 'd1', reason: 'Gütetermin', dueDate: '2026-08-05', done: false, assignee: 'RAin Bauer', type: 'deadline' }],
    documents: [{ id: 'doc1', name: 'Kündigungsschutzklage.pdf', date: '2026-06-15', author: 'RAin Bauer', size: '180 KB', ext: 'PDF' }],
  },
  {
    id: '00120-25', fileNumber: '00120/25', name: 'Meier GbR – Vertragsprüfung', subjectField: 'Vertragsrecht',
    lawyer: 'RA Dr. Kunze', status: 'closed', lastChanged: '2026-06-28',
    claimNumber: 'VR-2025-120', claimValue: 0, assistant: 'RAin Bauer',
    reason: 'Prüfung Kooperationsvertrag', notice: 'Abgeschlossen, Mandant informiert.', created: '2026-03-10',
    parties: [{ id: 'p1', involvementType: 'client', contact: 'Meier GbR' }],
    dueDates: [],
    documents: [{ id: 'doc1', name: 'Vertragspruefung_final.pdf', date: '2026-06-27', author: 'RA Dr. Kunze', size: '210 KB', ext: 'PDF' }],
  },
  {
    id: '00118-25', fileNumber: '00118/25', name: 'Sorgerecht Fischer', subjectField: 'Familienrecht',
    lawyer: 'RAin Bauer', status: 'open', lastChanged: '2026-06-26',
    claimNumber: 'FA-2025-118', claimValue: 0, assistant: 'RA Dr. Kunze',
    reason: 'Umgangs- und Sorgerecht', notice: '', created: '2026-02-14',
    parties: [
      { id: 'p1', involvementType: 'client', contact: 'Markus Fischer' },
      { id: 'p2', involvementType: 'court', contact: 'Familiengericht München' },
    ],
    dueDates: [{ id: 'd1', reason: 'Stellungnahme Jugendamt', dueDate: '2026-07-30', done: false, assignee: 'RAin Bauer', type: 'followup' }],
    documents: [{ id: 'doc1', name: 'Antrag_Sorgerecht.pdf', date: '2026-06-10', author: 'RAin Bauer', size: '140 KB', ext: 'PDF' }],
  },
];
