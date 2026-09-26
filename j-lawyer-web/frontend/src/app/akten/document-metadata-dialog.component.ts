import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Observable } from 'rxjs';
import { IconComponent } from '../shared/icon.component';
import {
  CaseDocument, CORRESPONDENT_IN, CORRESPONDENT_NONE, CORRESPONDENT_OUT, docDisplayTitle, DocMetadataPatch,
  DocMetadataWrite, KeywordOperation, Party,
} from './case.models';
import { CasesService } from './cases.service';
import { toLocalDate, toServerDate } from './document-actions.util';

/**
 * "Eigenschaften" dialog for the extended document metadata (title, keywords, received date,
 * correspondent "Von/An" and parent document), mirroring the desktop properties dialog.
 *
 * - One document: every field is shown with its current value and saved as a whole
 *   (PUT /v8/cases/documents/{id}/metadata).
 * - Several documents: each field is only changed when its checkbox is activated; keywords can be
 *   added, removed or replaced (PUT /v8/cases/documents/metadata). The parent is not offered in
 *   bulk mode.
 *
 * The correspondent is picked from the case parties (linked to the contact) or typed as free text.
 * Keywords get auto completion from the keywords used in the case. No AI in the web client.
 */
@Component({
  selector: 'jl-document-metadata-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="closed.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>
          @if (bulk()) {
            {{ 'akten.docs.meta.titleBulk' | transloco: { n: docs().length } }}
          } @else {
            {{ 'akten.docs.meta.title' | transloco }}
          }
        </h2>
        <button type="button" class="x" (click)="closed.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>
      <div class="db">
        @if (!bulk()) {
          <p class="fname" [title]="docs()[0].name"><jl-icon name="file-text" [size]="14" /> {{ docs()[0].name }}</p>
        }

        <!-- Bezeichnung -->
        <div class="fld">
          <label class="lbl-row">
            @if (bulk()) { <input type="checkbox" [ngModel]="chgTitle()" (ngModelChange)="chgTitle.set($event)" /> }
            <span class="lbl">{{ 'akten.docs.meta.docTitle' | transloco }}</span>
          </label>
          <input type="text" maxlength="250" [disabled]="bulk() && !chgTitle()" [ngModel]="title()" (ngModelChange)="title.set($event)"
                 [placeholder]="bulk() ? '' : docs()[0].name" />
        </div>

        <!-- Schlagworte -->
        <div class="fld">
          <label class="lbl-row">
            @if (bulk()) { <input type="checkbox" [ngModel]="chgKeywords()" (ngModelChange)="chgKeywords.set($event)" /> }
            <span class="lbl">{{ 'akten.docs.meta.keywords' | transloco }}</span>
          </label>
          @if (bulk()) {
            <select [disabled]="!chgKeywords()" [ngModel]="keywordOp()" (ngModelChange)="keywordOp.set($event)">
              <option value="ADD">{{ 'akten.docs.meta.kwAdd' | transloco }}</option>
              <option value="REMOVE">{{ 'akten.docs.meta.kwRemove' | transloco }}</option>
              <option value="SET">{{ 'akten.docs.meta.kwSet' | transloco }}</option>
            </select>
          }
          <div class="chips" [class.disabled]="bulk() && !chgKeywords()">
            @for (k of keywords(); track k) {
              <span class="chip">{{ k }}
                <button type="button" (click)="removeKeyword(k)" [disabled]="bulk() && !chgKeywords()" [attr.aria-label]="'akten.docs.meta.removeKeyword' | transloco">
                  <jl-icon name="close" [size]="10" />
                </button>
              </span>
            }
            <input type="text" class="kw-in" list="jl-doc-kw-list" [disabled]="bulk() && !chgKeywords()"
                   [ngModel]="keywordDraft()" (ngModelChange)="onKeywordInput($event)"
                   (keydown.enter)="$event.preventDefault(); commitKeyword()" (blur)="commitKeyword()"
                   [placeholder]="'akten.docs.meta.keywordPlaceholder' | transloco" />
            <datalist id="jl-doc-kw-list">
              @for (k of keywordOptions(); track k) { <option [value]="k"></option> }
            </datalist>
          </div>
        </div>

        <!-- Eingang -->
        <div class="fld">
          <label class="lbl-row">
            @if (bulk()) { <input type="checkbox" [ngModel]="chgReceived()" (ngModelChange)="chgReceived.set($event)" /> }
            <span class="lbl">{{ 'akten.docs.meta.received' | transloco }}</span>
          </label>
          <input type="date" [disabled]="bulk() && !chgReceived()" [ngModel]="received()" (ngModelChange)="received.set($event)" />
        </div>

        <!-- Von / An -->
        <div class="fld">
          <label class="lbl-row">
            @if (bulk()) { <input type="checkbox" [ngModel]="chgCorrespondent()" (ngModelChange)="chgCorrespondent.set($event)" /> }
            <span class="lbl">{{ 'akten.docs.meta.correspondent' | transloco }}</span>
          </label>
          <div class="row">
            <select class="dir" [disabled]="bulk() && !chgCorrespondent()" [ngModel]="direction()" (ngModelChange)="direction.set(+$event)">
              <option [ngValue]="dirNone">—</option>
              <option [ngValue]="dirIn">↘ {{ 'akten.docs.meta.dirIn' | transloco }}</option>
              <option [ngValue]="dirOut">↗ {{ 'akten.docs.meta.dirOut' | transloco }}</option>
            </select>
            <input type="text" list="jl-doc-party-list" [disabled]="(bulk() && !chgCorrespondent()) || direction() === dirNone"
                   [ngModel]="correspondentName()" (ngModelChange)="onCorrespondentInput($event)"
                   [placeholder]="'akten.docs.meta.correspondentPlaceholder' | transloco" />
            <datalist id="jl-doc-party-list">
              @for (p of partyOptions(); track p.addressId) { <option [value]="p.name">{{ p.role }}</option> }
            </datalist>
          </div>
          @if (direction() !== dirNone && correspondentName().trim()) {
            <span class="hint">
              @if (correspondentId()) {
                <jl-icon name="contacts" [size]="12" /> {{ 'akten.docs.meta.linkedContact' | transloco }}
              } @else {
                {{ 'akten.docs.meta.freeText' | transloco }}
              }
            </span>
          }
        </div>

        <!-- Parent (single document only) -->
        @if (!bulk()) {
          <div class="fld">
            <span class="lbl">{{ 'akten.docs.meta.parent' | transloco }}</span>
            <select [ngModel]="parentId()" (ngModelChange)="parentId.set($event)">
              <option value="">{{ 'akten.docs.meta.noParent' | transloco }}</option>
              @for (d of parentOptions(); track d.id) { <option [value]="d.id">{{ label(d) }}</option> }
            </select>
          </div>
        }
        @if (error()) { <p class="err">{{ error() }}</p> }
      </div>
      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="closed.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="busy() || !canSave()" (click)="save()">{{ 'akten.editor.save' | transloco }}</button>
      </footer>
    </div>
  `,
  styles: [`
    .backdrop { position: fixed; inset: 0; z-index: 60; background: rgba(6,14,22,.5); }
    .dialog { position: fixed; z-index: 61; top: 50%; left: 50%; transform: translate(-50%,-50%); width: min(520px, 94vw); max-height: 90dvh;
      display: flex; flex-direction: column; background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { display: flex; align-items: center; gap: 10px; padding: 13px 16px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1rem; font-weight: 800; }
    .x { margin-left: auto; display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { padding: 14px 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }
    .fname { display: flex; align-items: center; gap: 6px; margin: 0; font-size: .82rem; color: var(--jl-ink-soft); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .fld { display: flex; flex-direction: column; gap: 6px; }
    .lbl-row { display: flex; align-items: center; gap: 8px; }
    .lbl-row input[type=checkbox] { width: auto; margin: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; box-sizing: border-box; }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    input:disabled, select:disabled { opacity: .55; }
    .row { display: flex; gap: 8px; }
    .row .dir { flex: 0 0 120px; }
    .chips { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; padding: 6px; border: 1px solid var(--jl-line-strong); border-radius: 8px; }
    .chips.disabled { opacity: .55; }
    .chip { display: inline-flex; align-items: center; gap: 4px; padding: 2px 4px 2px 9px; border-radius: 999px; font-size: .78rem; font-weight: 600;
      background: color-mix(in srgb, var(--jl-blue) 12%, transparent); color: var(--jl-blue); }
    .chip button { display: inline-grid; place-items: center; width: 16px; height: 16px; padding: 0; border: 0; border-radius: 50%; background: transparent; color: inherit; cursor: pointer; }
    .chip button:hover:not(:disabled) { background: color-mix(in srgb, var(--jl-blue) 22%, transparent); }
    .kw-in { flex: 1 1 120px; min-width: 120px; border: 0; padding: 4px; }
    .hint { font-size: .76rem; color: var(--jl-ink-faint); display: inline-flex; align-items: center; gap: 5px; }
    .err { margin: 0; color: var(--jl-red, #d14343); font-size: .84rem; }
    .df { display: flex; align-items: center; gap: 10px; padding: 11px 16px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 15px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class DocumentMetadataDialogComponent implements OnInit {
  /** The documents to edit; one = full edit, several = bulk edit. */
  readonly docs = input.required<CaseDocument[]>();
  /** All documents of the case (parent candidates). */
  readonly allDocs = input<CaseDocument[]>([]);
  readonly caseId = input.required<string>();
  /** The case parties, offered as correspondents. */
  readonly parties = input<Party[]>([]);

  /** Emitted after a successful save; the parent reloads the documents. */
  readonly saved = output<void>();
  readonly closed = output<void>();

  private readonly cases = inject(CasesService);
  private readonly transloco = inject(TranslocoService);

  protected readonly dirNone = CORRESPONDENT_NONE;
  protected readonly dirIn = CORRESPONDENT_IN;
  protected readonly dirOut = CORRESPONDENT_OUT;

  protected readonly bulk = computed(() => this.docs().length > 1);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly title = signal('');
  protected readonly keywords = signal<string[]>([]);
  protected readonly keywordDraft = signal('');
  protected readonly received = signal('');
  protected readonly direction = signal(CORRESPONDENT_NONE);
  protected readonly correspondentName = signal('');
  protected readonly correspondentId = signal('');
  protected readonly parentId = signal('');

  // Bulk activation flags ("only change this field when activated").
  protected readonly chgTitle = signal(false);
  protected readonly chgKeywords = signal(false);
  protected readonly keywordOp = signal<KeywordOperation>('ADD');
  protected readonly chgReceived = signal(false);
  protected readonly chgCorrespondent = signal(false);

  private readonly caseKeywords = signal<string[]>([]);
  /** The original received date (ISO), kept so an unchanged date keeps its time of day. */
  private originalReceived = '';

  protected readonly keywordOptions = computed(() => {
    const have = new Set(this.keywords().map((k) => k.toLowerCase()));
    return this.caseKeywords().filter((k) => !have.has(k.toLowerCase()));
  });

  /** Parties with a linked contact, by display name (duplicates collapsed). */
  protected readonly partyOptions = computed(() => {
    const seen = new Set<string>();
    const out: { addressId: string; name: string; role: string }[] = [];
    for (const p of this.parties()) {
      const name = (p.contact || p.contactName || '').trim();
      if (!p.addressId || !name || seen.has(p.addressId)) { continue; }
      seen.add(p.addressId);
      out.push({ addressId: p.addressId, name, role: p.involvementType });
    }
    return out;
  });

  /** Parent candidates: every other document except the edited one's descendants (no cycles). */
  protected readonly parentOptions = computed(() => {
    const self = this.docs()[0];
    if (!self) { return []; }
    const all = this.allDocs();
    const excluded = new Set<string>([self.id]);
    let grew = true;
    while (grew) {
      grew = false;
      for (const d of all) {
        if (d.parentId && excluded.has(d.parentId) && !excluded.has(d.id)) { excluded.add(d.id); grew = true; }
      }
    }
    return all.filter((d) => !excluded.has(d.id))
      .sort((a, b) => docDisplayTitle(a).localeCompare(docDisplayTitle(b), undefined, { sensitivity: 'base' }));
  });

  protected readonly canSave = computed(() => !this.bulk()
    || this.chgTitle() || this.chgKeywords() || this.chgReceived() || this.chgCorrespondent());

  ngOnInit(): void {
    const docs = this.docs();
    if (docs.length === 1) {
      const d = docs[0];
      this.title.set(d.title);
      this.keywords.set([...d.keywords]);
      this.originalReceived = d.receivedDate;
      this.received.set(toLocalDate(d.receivedDate));
      this.direction.set(d.correspondentDirection || CORRESPONDENT_NONE);
      this.correspondentName.set(d.correspondentName);
      this.correspondentId.set(d.correspondentId);
      this.parentId.set(d.parentId);
    } else {
      this.direction.set(CORRESPONDENT_IN);
    }
    this.cases.caseDocumentKeywords(this.caseId()).subscribe((k) => this.caseKeywords.set(k));
  }

  protected label(d: CaseDocument): string {
    const t = docDisplayTitle(d);
    return t === d.name ? t : `${t} (${d.name})`;
  }

  // ----- keywords -----

  /** Commits the draft when a separator is typed or an auto-completion entry was picked. */
  protected onKeywordInput(value: string): void {
    if (/[,;]/.test(value)) {
      value.split(/[,;]/).slice(0, -1).forEach((k) => this.addKeyword(k));
      this.keywordDraft.set(value.split(/[,;]/).pop() ?? '');
      return;
    }
    this.keywordDraft.set(value);
    if (this.caseKeywords().includes(value)) { this.commitKeyword(); }
  }

  protected commitKeyword(): void {
    this.addKeyword(this.keywordDraft());
    this.keywordDraft.set('');
  }

  private addKeyword(raw: string): void {
    const k = raw.trim().replace(/\s+/g, ' ');
    if (!k) { return; }
    if (this.keywords().some((x) => x.toLowerCase() === k.toLowerCase())) { return; }
    this.keywords.update((ks) => [...ks, k]);
  }

  protected removeKeyword(k: string): void {
    this.keywords.update((ks) => ks.filter((x) => x !== k));
  }

  // ----- correspondent -----

  /** Links the correspondent to a party's contact when the typed name matches one exactly. */
  protected onCorrespondentInput(value: string): void {
    this.correspondentName.set(value);
    const hit = this.partyOptions().find((p) => p.name === value.trim());
    this.correspondentId.set(hit ? hit.addressId : '');
  }

  // ----- save -----

  /** The received date as epoch ms; an unchanged date keeps its original time of day. */
  private receivedMillis(): number | null {
    const local = this.received();
    if (!local) { return null; }
    if (this.originalReceived && toLocalDate(this.originalReceived) === local) {
      return new Date(this.originalReceived).getTime();
    }
    const iso = toServerDate(local);
    return iso ? new Date(iso).getTime() : null;
  }

  private correspondentValue(): { id: string | null; name: string | null; direction: number } {
    const name = this.correspondentName().trim();
    if (this.direction() === CORRESPONDENT_NONE || !name) {
      return { id: null, name: null, direction: CORRESPONDENT_NONE };
    }
    return { id: this.correspondentId() || null, name, direction: this.direction() };
  }

  protected save(): void {
    if (this.busy() || !this.canSave()) { return; }
    this.commitKeyword();
    const c = this.correspondentValue();
    let call: Observable<unknown>;
    if (this.bulk()) {
      const patch: DocMetadataPatch = {
        documentIds: this.docs().map((d) => d.id),
        changeTitle: this.chgTitle(),
        title: this.title().trim() || null,
        keywordOperation: this.chgKeywords() ? this.keywordOp() : 'UNCHANGED',
        keywords: this.keywords(),
        changeReceivedDate: this.chgReceived(),
        receivedDate: this.receivedMillis(),
        changeCorrespondent: this.chgCorrespondent(),
        correspondentId: c.id,
        correspondentName: c.name,
        correspondentDirection: c.direction,
      };
      call = this.cases.patchDocumentsMetadata(patch);
    } else {
      const data: DocMetadataWrite = {
        title: this.title().trim() || null,
        keywords: this.keywords(),
        receivedDate: this.receivedMillis(),
        correspondentId: c.id,
        correspondentName: c.name,
        correspondentDirection: c.direction,
        parentId: this.parentId() || null,
      };
      call = this.cases.updateDocumentMetadataV8(this.docs()[0].id, data);
    }
    this.busy.set(true);
    this.error.set(null);
    call.subscribe({
      next: () => { this.busy.set(false); this.saved.emit(); },
      error: () => { this.busy.set(false); this.error.set(this.transloco.translate('akten.docs.writeError')); },
    });
  }
}
