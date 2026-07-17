import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CaseDocument, DocMetaWrite, DocTag, MultiValueTagDef } from './case.models';
import { CasesService } from './cases.service';
import { baseMeta, CONVERTIBLE_EXT, HIGHLIGHT_NONE, HIGHLIGHTS, toLocalDate, toServerDate } from './document-actions.util';

/** A folder option for the "move" dropdown (flattened, indented by depth). */
export interface FolderOption {
  id: string;
  name: string;
  depth: number;
  isRoot: boolean;
}

type DialogKind = 'rename' | 'date' | 'move' | 'tags';

/**
 * Per-document overflow ("⋯") menu mirroring the desktop document context menu — but only the
 * actions implementable against the existing REST API: preview, download, rename, set creation
 * date, toggle favorite, highlight (two colour slots), move to folder, edit labels, convert to
 * PDF, OCR and delete. Metadata writes go through {@link CasesService.updateDocumentMetadata},
 * which overwrites all fields, so every write resends the full current metadata and changes one
 * thing. Emits `changed` after any successful write so the parent reloads the document list;
 * preview/download/delete are delegated to the parent's existing handlers.
 */
@Component({
  selector: 'jl-document-actions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <button type="button" class="kebab" [class.on]="open()" (click)="toggle($event)"
            [attr.aria-label]="'akten.docs.actions' | transloco" [disabled]="busy()">
      <jl-icon name="more" [size]="16" />
    </button>

    @if (open()) {
      <div class="menu-backdrop" (click)="open.set(false)"></div>
      <div class="menu" role="menu">
        <div class="grp">{{ 'akten.docs.grpOpen' | transloco }}</div>
        <button type="button" role="menuitem" (click)="emitAnd(preview)">
          <jl-icon name="file-text" [size]="15" /> {{ 'akten.docs.preview' | transloco }}
        </button>
        <button type="button" role="menuitem" (click)="emitAnd(download)">
          <jl-icon name="download" [size]="15" /> {{ 'akten.docs.download' | transloco }}
        </button>

        <div class="sep"></div>
        <div class="grp">{{ 'akten.docs.grpEdit' | transloco }}</div>
        <button type="button" role="menuitem" (click)="openDialog('rename')">
          <jl-icon name="edit" [size]="15" /> {{ 'akten.docs.rename' | transloco }}
        </button>
        <button type="button" role="menuitem" (click)="openDialog('date')">
          <jl-icon name="calendar" [size]="15" /> {{ 'akten.docs.setDate' | transloco }}
        </button>
        <button type="button" role="menuitem" (click)="toggleFavorite()">
          <jl-icon name="star" [size]="15" /> {{ (doc().favorite ? 'akten.docs.unfavorite' : 'akten.docs.favorite') | transloco }}
        </button>
        <div class="hl">
          <span class="hl-lbl"><jl-icon name="palette" [size]="15" /> {{ 'akten.docs.highlight' | transloco }}</span>
          <span class="hl-row">
            @for (h of highlights; track h.hex) {
              <button type="button" class="sw" [style.background]="h.hex"
                      [class.active]="doc().highlight1Value === h.value" (click)="setHighlight(1, h.value)"
                      [attr.aria-label]="('akten.docs.highlight' | transloco) + ' 1'"></button>
            }
            <button type="button" class="sw none" [class.active]="doc().highlight1Value === none"
                    (click)="setHighlight(1, none)" [attr.aria-label]="'akten.docs.highlightNone' | transloco">
              <jl-icon name="close" [size]="12" />
            </button>
          </span>
          <span class="hl-row">
            @for (h of highlights; track h.hex) {
              <button type="button" class="sw" [style.background]="h.hex"
                      [class.active]="doc().highlight2Value === h.value" (click)="setHighlight(2, h.value)"
                      [attr.aria-label]="('akten.docs.highlight' | transloco) + ' 2'"></button>
            }
            <button type="button" class="sw none" [class.active]="doc().highlight2Value === none"
                    (click)="setHighlight(2, none)" [attr.aria-label]="'akten.docs.highlightNone' | transloco">
              <jl-icon name="close" [size]="12" />
            </button>
          </span>
        </div>
        <button type="button" role="menuitem" (click)="openDialog('move')" [disabled]="folders().length < 2">
          <jl-icon name="folder" [size]="15" /> {{ 'akten.docs.move' | transloco }}
        </button>
        <button type="button" role="menuitem" (click)="openDialog('tags')">
          <jl-icon name="tag" [size]="15" /> {{ 'akten.docs.labels' | transloco }}
        </button>

        @if (canConvert() || canOcr()) {
          <div class="sep"></div>
          <div class="grp">{{ 'akten.docs.grpConvert' | transloco }}</div>
          @if (canConvert()) {
            <button type="button" role="menuitem" (click)="toPdf()">
              <jl-icon name="doc" [size]="15" /> {{ 'akten.docs.toPdf' | transloco }}
            </button>
          }
          @if (canOcr()) {
            <button type="button" role="menuitem" (click)="ocr()">
              <jl-icon name="search" [size]="15" /> {{ 'akten.docs.ocr' | transloco }}
            </button>
          }
        }

        <div class="sep"></div>
        <button type="button" role="menuitem" class="danger" (click)="emitAnd(remove)">
          <jl-icon name="trash" [size]="15" /> {{ 'akten.docs.delete' | transloco }}
        </button>
      </div>
    }

    @if (dialog(); as dk) {
      <div class="backdrop" (click)="dialog.set(null)"></div>
      <div class="dialog" role="dialog" aria-modal="true">
        <header class="dh">
          <h2>{{ 'akten.docs.dlg.' + dk | transloco }}</h2>
          <button type="button" class="x" (click)="dialog.set(null)" [attr.aria-label]="'akten.editor.cancel' | transloco">
            <jl-icon name="close" [size]="18" />
          </button>
        </header>
        <div class="db">
          @switch (dk) {
            @case ('rename') {
              <label class="fld">
                <span class="lbl">{{ 'akten.docs.name' | transloco }}</span>
                <input type="text" [ngModel]="renameValue()" (ngModelChange)="renameValue.set($event)"
                       (keydown.enter)="doRename()" />
              </label>
            }
            @case ('date') {
              <label class="fld">
                <span class="lbl">{{ 'akten.docs.creationDate' | transloco }}</span>
                <input type="date" [ngModel]="dateValue()" (ngModelChange)="dateValue.set($event)" />
              </label>
            }
            @case ('move') {
              <label class="fld">
                <span class="lbl">{{ 'akten.docs.targetFolder' | transloco }}</span>
                <select [ngModel]="moveFolderId()" (ngModelChange)="moveFolderId.set($event)">
                  @for (f of folders(); track f.id) {
                    <option [value]="f.id">{{ indent(f) }}{{ f.isRoot ? rootLabel() : f.name }}</option>
                  }
                </select>
              </label>
            }
            @case ('tags') {
              <div class="tags">
                <span class="lbl">{{ 'akten.docs.labels' | transloco }}</span>
                @if (tagOptions().length) {
                  <div class="tag-pick">
                    @for (name of tagOptions(); track name) {
                      <button type="button" class="tag-opt" [class.on]="isActive(name)" [disabled]="busy()" (click)="toggleTag(name)">
                        @if (isActive(name)) { <jl-icon name="check" [size]="12" /> }
                        {{ name }}
                      </button>
                    }
                  </div>
                } @else {
                  <p class="muted">{{ 'akten.docs.noDictionary' | transloco }}</p>
                }

                @if (mvDefs().length) {
                  <span class="lbl mv-head">{{ 'akten.docs.listLabels' | transloco }}</span>
                  @for (def of mvDefs(); track def.tagName) {
                    <label class="fld mv-row">
                      <span class="mv-name">{{ def.tagName }}</span>
                      <select [disabled]="busy()" [value]="mvValue(def.tagName)" (change)="setMvValue(def.tagName, $any($event.target).value)">
                        <option value="">—</option>
                        @for (v of def.values; track v) { <option [value]="v">{{ v }}</option> }
                      </select>
                    </label>
                  }
                }
              </div>
            }
          }
        </div>
        <footer class="df">
          <span class="spacer"></span>
          <button type="button" class="btn" (click)="dialog.set(null)">
            {{ (dk === 'tags' ? 'akten.docs.close' : 'akten.editor.cancel') | transloco }}
          </button>
          @switch (dk) {
            @case ('rename') {
              <button type="button" class="btn primary" [disabled]="!renameValue().trim() || busy()" (click)="doRename()">{{ 'akten.editor.save' | transloco }}</button>
            }
            @case ('date') {
              <button type="button" class="btn primary" [disabled]="!dateValue() || busy()" (click)="doSetDate()">{{ 'akten.editor.save' | transloco }}</button>
            }
            @case ('move') {
              <button type="button" class="btn primary" [disabled]="busy()" (click)="doMove()">{{ 'akten.docs.moveBtn' | transloco }}</button>
            }
          }
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { position: relative; display: inline-flex; }
    .kebab { display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .kebab:hover, .kebab.on { background: var(--jl-surface-alt); color: var(--jl-ink); }
    .kebab:disabled { opacity: .5; cursor: default; }
    .menu-backdrop { position: fixed; inset: 0; z-index: 40; }
    .menu { position: absolute; top: 34px; right: 0; z-index: 41; min-width: 232px; padding: 6px; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 12px; box-shadow: 0 16px 40px rgba(0,0,0,.22); }
    .menu button[role=menuitem] { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border: 0; border-radius: 8px; background: transparent; color: var(--jl-ink); font: inherit; font-size: .86rem; text-align: left; cursor: pointer; }
    .menu button[role=menuitem]:hover:not(:disabled) { background: var(--jl-surface-alt); }
    .menu button[role=menuitem]:disabled { opacity: .45; cursor: default; }
    .menu button.danger { color: var(--jl-red, #d14343); }
    .grp { padding: 6px 10px 3px; font-size: .68rem; font-weight: 800; text-transform: uppercase; letter-spacing: .04em; color: var(--jl-ink-faint); }
    .sep { height: 1px; margin: 4px 6px; background: var(--jl-line); }
    .hl { padding: 4px 10px 6px; display: flex; flex-direction: column; gap: 5px; }
    .hl-lbl { display: flex; align-items: center; gap: 10px; font-size: .86rem; color: var(--jl-ink); }
    .hl-row { display: flex; gap: 6px; padding-left: 25px; }
    .sw { width: 20px; height: 20px; border-radius: 6px; border: 1px solid rgba(0,0,0,.18); cursor: pointer; display: inline-grid; place-items: center; color: var(--jl-ink-soft); padding: 0; }
    .sw.none { background: var(--jl-surface); }
    .sw.active { outline: 2px solid var(--jl-blue); outline-offset: 1px; }
    .backdrop { position: fixed; inset: 0; z-index: 60; background: rgba(6,14,22,.5); display: grid; place-items: center; }
    .dialog { position: fixed; z-index: 61; top: 50%; left: 50%; transform: translate(-50%,-50%); width: min(460px, 94vw); max-height: 90dvh;
      display: flex; flex-direction: column; background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { display: flex; align-items: center; gap: 10px; padding: 13px 16px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1rem; font-weight: 800; }
    .x { margin-left: auto; display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { padding: 14px 16px; overflow-y: auto; }
    .fld { display: flex; flex-direction: column; gap: 5px; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .tags { display: flex; flex-direction: column; gap: 8px; }
    /* Value-set picker: every configured label as a toggle chip (filled = active). */
    .tag-pick { display: flex; flex-wrap: wrap; gap: 6px; }
    .tag-opt { display: inline-flex; align-items: center; gap: 5px; padding: 4px 11px; border-radius: 999px; font: inherit; font-size: .78rem; font-weight: 600;
      border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .tag-opt:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .tag-opt.on { background: color-mix(in srgb, var(--jl-blue) 14%, transparent); border-color: var(--jl-blue); color: var(--jl-blue); }
    .tag-opt:disabled { opacity: .55; cursor: default; }
    .muted { color: var(--jl-ink-faint); font-size: .84rem; margin: 0; }
    .mv-head { margin-top: 6px; }
    .mv-row { flex-direction: row; align-items: center; gap: 10px; }
    .mv-row .mv-name { flex: 0 0 40%; font-size: .84rem; color: var(--jl-ink); }
    .mv-row select { flex: 1 1 auto; }
    .df { display: flex; align-items: center; gap: 10px; padding: 11px 16px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 15px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class DocumentActionsComponent {
  readonly doc = input.required<CaseDocument>();
  readonly caseId = input.required<string>();
  readonly folders = input<FolderOption[]>([]);

  /** Emitted after any successful metadata/tag/PDF/OCR write; the parent reloads the list. */
  readonly changed = output<void>();
  readonly preview = output<void>();
  readonly download = output<void>();
  readonly remove = output<void>();

  private readonly cases = inject(CasesService);
  private readonly transloco = inject(TranslocoService);

  protected readonly none = HIGHLIGHT_NONE;
  protected readonly highlights = HIGHLIGHTS;

  protected readonly open = signal(false);
  protected readonly busy = signal(false);
  protected readonly dialog = signal<DialogKind | null>(null);

  protected readonly renameValue = signal('');
  protected readonly dateValue = signal('');
  protected readonly moveFolderId = signal('');
  protected readonly tagList = signal<DocTag[]>([]);
  protected readonly dictionary = signal<string[]>([]);
  /** Configured multi-value ("Listenetiketten") tag definitions for documents (name + value set). */
  protected readonly mvDefs = signal<MultiValueTagDef[]>([]);

  protected readonly canConvert = computed(() => CONVERTIBLE_EXT.has(this.doc().ext));
  protected readonly canOcr = computed(() => this.doc().ext === 'PDF');

  /**
   * Selectable simple labels: the configured value set plus any already-set label not (any longer)
   * in it — excluding multi-value ("Listenetiketten") tag names, which get their own value pickers.
   */
  protected readonly tagOptions = computed(() => {
    const mv = new Set(this.mvDefs().map((d) => d.tagName));
    const names = new Set(this.dictionary().filter((n) => !mv.has(n)));
    this.tagList().forEach((t) => { if (!mv.has(t.name)) { names.add(t.name); } });
    return [...names].sort((a, b) => a.localeCompare(b));
  });

  /** The currently selected value for a multi-value tag on this document ('' when unset). */
  protected mvValue(name: string): string {
    return this.tagList().find((t) => t.name === name)?.value ?? '';
  }

  /** Sets/clears a multi-value tag's value on the document (empty value removes it). */
  protected setMvValue(name: string, value: string): void {
    const existing = this.tagList().find((t) => t.name === name);
    const call = value
      ? this.cases.addDocumentTag(this.doc().id, name, value)
      : (existing ? this.cases.deleteDocumentTag(existing.id) : null);
    if (!call) { return; }
    this.busy.set(true);
    call.subscribe({
      next: () => { this.busy.set(false); this.reloadTags(); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.writeError')); },
    });
  }

  protected toggle(ev: Event): void {
    ev.stopPropagation();
    this.open.update((o) => !o);
  }

  /** Emits a delegated action (preview/download/delete) and closes the menu. */
  protected emitAnd(out: { emit: (v: void) => void }): void {
    out.emit();
    this.open.set(false);
  }

  protected openDialog(kind: DialogKind): void {
    this.open.set(false);
    const d = this.doc();
    if (kind === 'rename') { this.renameValue.set(d.name); }
    if (kind === 'date') { this.dateValue.set(toLocalDate(d.date)); }
    if (kind === 'move') { this.moveFolderId.set(d.folderId || this.folders().find((f) => f.isRoot)?.id || ''); }
    if (kind === 'tags') {
      this.cases.documentTags(d.id).subscribe((t) => this.tagList.set(t));
      this.cases.documentTagDictionary().subscribe((t) => this.dictionary.set(t));
      this.cases.documentMultiValueTags().subscribe((m) => this.mvDefs.set(m));
    }
    this.dialog.set(kind);
  }

  protected rootLabel(): string {
    return this.transloco.translate('akten.docs.rootFolder');
  }

  protected indent(f: FolderOption): string {
    return '  '.repeat(Math.max(0, f.depth));
  }

  // ----- writes -----

  private base(): DocMetaWrite {
    return baseMeta(this.doc(), this.caseId());
  }

  private run(obs: ReturnType<CasesService['updateDocumentMetadata']>, closeDialog = false): void {
    this.busy.set(true);
    obs.subscribe({
      next: () => { this.busy.set(false); if (closeDialog) { this.dialog.set(null); } this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.writeError')); },
    });
  }

  protected toggleFavorite(): void {
    this.open.set(false);
    this.run(this.cases.updateDocumentMetadata({ ...this.base(), favorite: !this.doc().favorite }));
  }

  protected setHighlight(slot: 1 | 2, value: number): void {
    const meta = this.base();
    if (slot === 1) { meta.highlight1 = value; } else { meta.highlight2 = value; }
    this.run(this.cases.updateDocumentMetadata(meta));
  }

  protected doRename(): void {
    const name = this.renameValue().trim();
    if (!name) { return; }
    this.run(this.cases.updateDocumentMetadata({ ...this.base(), name }), true);
  }

  protected doSetDate(): void {
    const iso = toServerDate(this.dateValue());
    if (!iso) { return; }
    this.run(this.cases.updateDocumentMetadata({ ...this.base(), creationDate: iso }), true);
  }

  protected doMove(): void {
    this.run(this.cases.updateDocumentMetadata({ ...this.base(), folderId: this.moveFolderId() }), true);
  }

  protected toPdf(): void {
    this.open.set(false);
    this.busy.set(true);
    this.cases.convertDocumentToPdf(this.doc().id).subscribe({
      next: () => { this.busy.set(false); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.convertError')); },
    });
  }

  protected ocr(): void {
    this.open.set(false);
    this.busy.set(true);
    this.cases.ocrDocument(this.doc().id).subscribe({
      next: () => { this.busy.set(false); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.convertError')); },
    });
  }

  protected isActive(name: string): boolean {
    return this.tagList().some((t) => t.name === name);
  }

  /** Toggles a label from the value set on the document (add, or remove the matching active tag). */
  protected toggleTag(name: string): void {
    const active = this.tagList().find((t) => t.name === name);
    const call = active ? this.cases.deleteDocumentTag(active.id) : this.cases.addDocumentTag(this.doc().id, name);
    this.busy.set(true);
    call.subscribe({
      next: () => { this.busy.set(false); this.reloadTags(); this.changed.emit(); },
      error: () => { this.busy.set(false); alert(this.transloco.translate('akten.docs.writeError')); },
    });
  }

  private reloadTags(): void {
    this.cases.documentTags(this.doc().id).subscribe((t) => this.tagList.set(t));
  }
}
