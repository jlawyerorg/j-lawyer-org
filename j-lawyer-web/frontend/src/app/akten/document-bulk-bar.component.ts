import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { forkJoin, Observable } from 'rxjs';
import { IconComponent } from '../shared/icon.component';
import { CaseDocument } from './case.models';
import { CasesService } from './cases.service';
import { FolderOption } from './document-actions.component';
import { baseMeta, HIGHLIGHT_NONE, HIGHLIGHTS, isConvertible, isPdf, toServerDate } from './document-actions.util';

type BulkDialog = 'date' | 'move' | 'label';

/**
 * Bulk-action bar shown while one or more documents are selected. Offers the document actions the
 * Swing client applies across a multi-selection (favorite, highlight, creation date, move to
 * folder, add label, convert to PDF, OCR, delete, download) — each fans out over the selection via
 * forkJoin against the existing per-document REST endpoints. Emits `changed` after a successful
 * batch (parent reloads + clears) and `download` (parent downloads each selected document).
 */
@Component({
  selector: 'jl-document-bulk-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="bar">
      <span class="count">{{ 'akten.docs.bulk.selected' | transloco: { n: docs().length } }}</span>

      <button type="button" class="ba" (click)="download.emit()">
        <jl-icon name="download" [size]="15" /> {{ 'akten.docs.download' | transloco }}
      </button>

      <div class="wrap">
        <button type="button" class="ba" [class.on]="menu()" (click)="menu.set(!menu())" [disabled]="busy()">
          <jl-icon name="edit" [size]="15" /> {{ 'akten.docs.bulk.edit' | transloco }} <span class="caret">▾</span>
        </button>
        @if (menu()) {
          <div class="menu-backdrop" (click)="menu.set(false)"></div>
          <div class="menu" role="menu">
            <button type="button" role="menuitem" (click)="setFavorite(true)"><jl-icon name="star" [size]="15" /> {{ 'akten.docs.favorite' | transloco }}</button>
            <button type="button" role="menuitem" (click)="setFavorite(false)"><jl-icon name="star" [size]="15" /> {{ 'akten.docs.unfavorite' | transloco }}</button>
            <div class="hl">
              <span class="hl-lbl"><jl-icon name="palette" [size]="15" /> {{ 'akten.docs.highlight' | transloco }}</span>
              <span class="hl-row">
                @for (h of highlights; track h.hex) {
                  <button type="button" class="sw" [style.background]="h.hex" (click)="setHighlight(h.value)"></button>
                }
                <button type="button" class="sw none" (click)="setHighlight(none)" [attr.aria-label]="'akten.docs.highlightNone' | transloco"><jl-icon name="close" [size]="12" /></button>
              </span>
            </div>
            <button type="button" role="menuitem" (click)="openDialog('date')"><jl-icon name="calendar" [size]="15" /> {{ 'akten.docs.setDate' | transloco }}</button>
            <button type="button" role="menuitem" (click)="openDialog('move')" [disabled]="folders().length < 2"><jl-icon name="folder" [size]="15" /> {{ 'akten.docs.move' | transloco }}</button>
            <button type="button" role="menuitem" (click)="openDialog('label')"><jl-icon name="tag" [size]="15" /> {{ 'akten.docs.bulk.addLabel' | transloco }}</button>
            @if (anyConvertible()) {
              <button type="button" role="menuitem" (click)="toPdf()"><jl-icon name="doc" [size]="15" /> {{ 'akten.docs.toPdf' | transloco }}</button>
            }
            @if (anyPdf()) {
              <button type="button" role="menuitem" (click)="ocr()"><jl-icon name="search" [size]="15" /> {{ 'akten.docs.ocr' | transloco }}</button>
            }
          </div>
        }
      </div>

      <button type="button" class="ba danger" (click)="removeAll()" [disabled]="busy()">
        <jl-icon name="trash" [size]="15" /> {{ 'akten.docs.delete' | transloco }}
      </button>

      <span class="spacer"></span>
      <button type="button" class="ba ghost" (click)="clear.emit()" [attr.aria-label]="'akten.docs.bulk.clear' | transloco">
        <jl-icon name="close" [size]="15" /> {{ 'akten.docs.bulk.clear' | transloco }}
      </button>
    </div>

    @if (dialog(); as dk) {
      <div class="backdrop" (click)="dialog.set(null)"></div>
      <div class="dialog" role="dialog" aria-modal="true">
        <header class="dh">
          <h2>{{ 'akten.docs.bulk.dlg.' + dk | transloco: { n: docs().length } }}</h2>
          <button type="button" class="x" (click)="dialog.set(null)" [attr.aria-label]="'akten.editor.cancel' | transloco"><jl-icon name="close" [size]="18" /></button>
        </header>
        <div class="db">
          @switch (dk) {
            @case ('date') {
              <label class="fld"><span class="lbl">{{ 'akten.docs.creationDate' | transloco }}</span>
                <input type="date" [ngModel]="dateValue()" (ngModelChange)="dateValue.set($event)" /></label>
            }
            @case ('move') {
              <label class="fld"><span class="lbl">{{ 'akten.docs.targetFolder' | transloco }}</span>
                <select [ngModel]="moveFolderId()" (ngModelChange)="moveFolderId.set($event)">
                  @for (f of folders(); track f.id) { <option [value]="f.id">{{ f.isRoot ? rootLabel() : f.name }}</option> }
                </select></label>
            }
            @case ('label') {
              @if (dictionary().length) {
                <label class="fld"><span class="lbl">{{ 'akten.docs.addLabel' | transloco }}</span>
                  <select [ngModel]="labelValue()" (ngModelChange)="labelValue.set($event)">
                    @for (d of dictionary(); track d) { <option [value]="d">{{ d }}</option> }
                  </select></label>
              } @else {
                <p class="muted">{{ 'akten.docs.noDictionary' | transloco }}</p>
              }
            }
          }
        </div>
        <footer class="df">
          <span class="spacer"></span>
          <button type="button" class="btn" (click)="dialog.set(null)">{{ 'akten.editor.cancel' | transloco }}</button>
          @switch (dk) {
            @case ('date') { <button type="button" class="btn primary" [disabled]="!dateValue() || busy()" (click)="applyDate()">{{ 'akten.editor.save' | transloco }}</button> }
            @case ('move') { <button type="button" class="btn primary" [disabled]="busy()" (click)="applyMove()">{{ 'akten.docs.moveBtn' | transloco }}</button> }
            @case ('label') { <button type="button" class="btn primary" [disabled]="!labelValue().trim() || busy()" (click)="applyLabel()">{{ 'akten.docs.addBtn' | transloco }}</button> }
          }
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .bar { position: sticky; top: 0; z-index: 5; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 8px 10px; margin-bottom: 8px;
      background: color-mix(in srgb, var(--jl-blue) 10%, var(--jl-surface)); border: 1px solid var(--jl-blue); border-radius: 10px; }
    .count { font-size: .82rem; font-weight: 700; color: var(--jl-blue); padding: 0 4px; }
    .spacer { flex: 1; }
    .wrap { position: relative; display: inline-flex; }
    .ba { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .82rem; font-weight: 600; padding: 6px 11px; border-radius: 8px;
      border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .ba:hover:not(:disabled) { border-color: var(--jl-blue); }
    .ba:disabled { opacity: .5; cursor: default; }
    .ba.on { border-color: var(--jl-blue); color: var(--jl-blue); }
    .ba.danger { color: var(--jl-red, #d14343); }
    .ba.danger:hover:not(:disabled) { border-color: var(--jl-red, #d14343); }
    .ba.ghost { border-color: transparent; background: transparent; color: var(--jl-ink-soft); }
    .ba.ghost:hover { background: var(--jl-surface); }
    .caret { font-size: .7rem; }
    .menu-backdrop { position: fixed; inset: 0; z-index: 40; }
    .menu { position: absolute; top: 36px; left: 0; z-index: 41; min-width: 220px; padding: 6px; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 12px; box-shadow: 0 16px 40px rgba(0,0,0,.22); }
    .menu button[role=menuitem] { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border: 0; border-radius: 8px; background: transparent; color: var(--jl-ink); font: inherit; font-size: .86rem; text-align: left; cursor: pointer; }
    .menu button[role=menuitem]:hover:not(:disabled) { background: var(--jl-surface-alt); }
    .menu button[role=menuitem]:disabled { opacity: .45; cursor: default; }
    .hl { padding: 4px 10px 6px; display: flex; flex-direction: column; gap: 5px; }
    .hl-lbl { display: flex; align-items: center; gap: 10px; font-size: .86rem; }
    .hl-row { display: flex; gap: 6px; padding-left: 25px; }
    .sw { width: 20px; height: 20px; border-radius: 6px; border: 1px solid rgba(0,0,0,.18); cursor: pointer; display: inline-grid; place-items: center; color: var(--jl-ink-soft); padding: 0; }
    .sw.none { background: var(--jl-surface); }
    .backdrop { position: fixed; inset: 0; z-index: 60; background: rgba(6,14,22,.5); }
    .dialog { position: fixed; z-index: 61; top: 50%; left: 50%; transform: translate(-50%,-50%); width: min(440px, 94vw); display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { display: flex; align-items: center; gap: 10px; padding: 13px 16px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1rem; font-weight: 800; }
    .x { margin-left: auto; display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .db { padding: 14px 16px; }
    .muted { color: var(--jl-ink-faint); font-size: .84rem; margin: 0; }
    .fld { display: flex; flex-direction: column; gap: 5px; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .df { display: flex; align-items: center; gap: 10px; padding: 11px 16px; border-top: 1px solid var(--jl-line); }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 15px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class DocumentBulkBarComponent {
  readonly docs = input.required<CaseDocument[]>();
  readonly caseId = input.required<string>();
  readonly folders = input<FolderOption[]>([]);

  readonly changed = output<void>();
  readonly clear = output<void>();
  readonly download = output<void>();

  private readonly cases = inject(CasesService);
  private readonly transloco = inject(TranslocoService);

  protected readonly none = HIGHLIGHT_NONE;
  protected readonly highlights = HIGHLIGHTS;

  protected readonly busy = signal(false);
  protected readonly menu = signal(false);
  protected readonly dialog = signal<BulkDialog | null>(null);
  protected readonly dateValue = signal('');
  protected readonly moveFolderId = signal('');
  protected readonly labelValue = signal('');
  protected readonly dictionary = signal<string[]>([]);

  protected readonly anyConvertible = computed(() => this.docs().some((d) => isConvertible(d.ext)));
  protected readonly anyPdf = computed(() => this.docs().some((d) => isPdf(d.ext)));

  protected rootLabel(): string {
    return this.transloco.translate('akten.docs.rootFolder');
  }

  protected openDialog(kind: BulkDialog): void {
    this.menu.set(false);
    if (kind === 'move') { this.moveFolderId.set(this.folders().find((f) => f.isRoot)?.id ?? ''); }
    if (kind === 'date') { this.dateValue.set(''); }
    if (kind === 'label') {
      this.labelValue.set('');
      this.cases.documentTagDictionary().subscribe((d) => {
        this.dictionary.set(d);
        if (d.length && !this.labelValue()) { this.labelValue.set(d[0]); }
      });
    }
    this.dialog.set(kind);
  }

  /** Runs `op` for every selected document; on success emits `changed` (and closes the dialog). */
  private fanOut(op: (d: CaseDocument) => Observable<unknown>, closeDialog = false): void {
    const docs = this.docs();
    if (!docs.length) { return; }
    this.busy.set(true);
    forkJoin(docs.map(op)).subscribe({
      next: () => { this.busy.set(false); this.menu.set(false); if (closeDialog) { this.dialog.set(null); } this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.writeError')); },
    });
  }

  protected setFavorite(favorite: boolean): void {
    this.fanOut((d) => this.cases.updateDocumentMetadata({ ...baseMeta(d, this.caseId()), favorite }));
  }

  protected setHighlight(value: number): void {
    this.fanOut((d) => this.cases.updateDocumentMetadata({ ...baseMeta(d, this.caseId()), highlight1: value }));
  }

  protected applyDate(): void {
    const iso = toServerDate(this.dateValue());
    if (!iso) { return; }
    this.fanOut((d) => this.cases.updateDocumentMetadata({ ...baseMeta(d, this.caseId()), creationDate: iso }), true);
  }

  protected applyMove(): void {
    const folderId = this.moveFolderId();
    this.fanOut((d) => this.cases.updateDocumentMetadata({ ...baseMeta(d, this.caseId()), folderId }), true);
  }

  protected applyLabel(): void {
    const name = this.labelValue().trim();
    if (!name) { return; }
    this.fanOut((d) => this.cases.addDocumentTag(d.id, name), true);
  }

  protected toPdf(): void {
    const eligible = this.docs().filter((d) => isConvertible(d.ext));
    if (!eligible.length) { return; }
    this.menu.set(false);
    this.busy.set(true);
    forkJoin(eligible.map((d) => this.cases.convertDocumentToPdf(d.id))).subscribe({
      next: () => { this.busy.set(false); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.convertError')); },
    });
  }

  protected ocr(): void {
    const eligible = this.docs().filter((d) => isPdf(d.ext));
    if (!eligible.length) { return; }
    this.menu.set(false);
    this.busy.set(true);
    forkJoin(eligible.map((d) => this.cases.ocrDocument(d.id))).subscribe({
      next: () => { this.busy.set(false); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.convertError')); },
    });
  }

  protected removeAll(): void {
    const docs = this.docs();
    if (!docs.length || this.busy()) { return; }
    if (!confirm(this.transloco.translate('akten.docs.bulk.deleteConfirm', { n: docs.length }))) { return; }
    this.busy.set(true);
    forkJoin(docs.map((d) => this.cases.deleteDocument(d.id))).subscribe({
      next: () => { this.busy.set(false); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.writeError')); },
    });
  }
}
