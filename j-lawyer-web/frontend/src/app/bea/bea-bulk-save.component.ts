import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { defer, forkJoin, from, Observable, of, Subject, throwError } from 'rxjs';
import { catchError, debounceTime, map, mergeMap, switchMap } from 'rxjs/operators';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { LabelEditorComponent, LabelValue } from '../shared/label-editor.component';
import { fileKind, kindGlyph } from '../shared/document-preview.models';
import { CasesService } from '../akten/cases.service';
import {
  CaseOverview, CaseTag, DocFolder, DocumentNameTemplate, HIGHLIGHT_NONE, MultiValueTagDef,
} from '../akten/case.models';
import { EventEditorComponent } from '../calendar/event-editor.component';
import { CalendarService } from '../calendar/calendar.service';
import { CaseRef, EventDraft } from '../calendar/calendar.models';
import { CaseSuggestions } from '../communication/email.models';
import { BeaService } from './bea.service';
import { BeaMessage } from './bea.models';

/** A case chosen as the save target. */
export interface TargetCase { id: string; fileNumber: string; name: string; }

interface FolderOption { id: string; name: string; depth: number; isRoot: boolean; }
interface TypeChip { ext: string; kind: string; count: number; allIncluded: boolean; }

/** One document to store: the whole message (.bea) or a single attachment. */
interface SaveEntry {
  key: string;
  kind: 'bea' | 'attachment';
  /** For attachments: the beA attachment name used to fetch its content. */
  attachmentName?: string;
  /** Inline attachment content when the message already carried it. */
  inlineContent?: string | null;
  origName: string;
  ext: string;
  include: boolean;
  fileName: string;
  folderId: string;
  favorite: boolean;
  tags: LabelValue[];
  templateId: string;
  sizeBytes: number;
  status: 'idle' | 'saving' | 'done' | 'error';
}

const CONCURRENCY = 3;

/**
 * Rich "In Akte speichern" dialog for an opened beA message — the beA counterpart of the e-mail
 * bulk-save dialog. It lists the whole message as a {@code .bea} file (the server-side XML export)
 * plus each attachment as its own entry, each with a type-specific icon, include toggle, an editable
 * file name driven by a per-document naming template, a target folder, a favorite flag and document
 * labels. Case-level labels can be maintained inline, duplicate names block saving, and a calendar
 * entry can be created for the case.
 */
@Component({
  selector: 'jl-bea-bulk-save',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, LabelEditorComponent, EventEditorComponent],
  template: `
    <div class="bs-backdrop" (click)="tryClose()"></div>
    <div class="bs-dialog" role="dialog" aria-modal="true">
      <header class="bs-head">
        <h2>{{ 'bulkSave.title' | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="tryClose()" [attr.aria-label]="'bulkSave.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="bs-body">
        @if (target(); as t) {
          <div class="bs-chosen">
            <jl-icon name="cases" [size]="14" />
            <span class="bs-fn">{{ t.fileNumber }}</span>
            <span class="bs-name">{{ t.name }}</span>
            <button type="button" class="bs-link" [disabled]="saving()" (click)="clearTarget()">{{ 'bulkSave.change' | transloco }}</button>
          </div>
        } @else {
          @if (suggestedCases().length) {
            <div class="bs-block">
              <span class="bs-label">{{ 'bulkSave.suggestions' | transloco }}</span>
              <div class="bs-chips">
                @for (c of suggestedCases(); track c.id) {
                  <button type="button" class="bs-chip" (click)="pick({ id: c.id, fileNumber: c.fileNumber, name: c.name })">
                    <span class="bs-fn">{{ c.fileNumber }}</span><span class="bs-name">{{ c.name }}</span>
                  </button>
                }
              </div>
            </div>
          }
          <div class="bs-search">
            <jl-icon name="search" [size]="14" />
            <input type="search" [value]="term()" (input)="onSearch($event)"
                   [placeholder]="'bulkSave.searchPlaceholder' | transloco" autocomplete="off" />
          </div>
          @if (results().length) {
            <ul class="bs-results">
              @for (r of results(); track r.id) {
                <li (click)="pick({ id: r.id, fileNumber: r.fileNumber, name: r.name })">
                  <span class="bs-fn">{{ r.fileNumber }}</span><span class="bs-name">{{ r.name }}</span>
                </li>
              }
            </ul>
          }
        }

        @if (target()) {
          <div class="bs-section">
            <span class="bs-label">{{ 'bulkSave.caseLabels' | transloco }}</span>
            <jl-label-editor [labels]="caseLabels()" [singleDict]="caseSingleDict()" [listDefs]="caseListDefs()"
                             [disabled]="saving()" (changed)="onCaseLabelsChanged($event)" />
          </div>

          <div class="bs-apply">
            <div class="bs-apply-row">
              <label>{{ 'bulkSave.nameTemplate' | transloco }}</label>
              <select [value]="applyTemplate()" (change)="applyTemplate.set($any($event.target).value)" [disabled]="saving() || busy()">
                @for (tpl of templates(); track tpl.id) {
                  <option [value]="tpl.id">{{ tpl.displayName }}{{ tpl.defaultTemplate ? ' ★' : '' }}</option>
                }
              </select>
              <button type="button" class="bs-apply-btn" [disabled]="saving() || busy() || !templates().length" (click)="applyTemplateToAll()">
                {{ 'bulkSave.applyAll' | transloco }}
              </button>
            </div>
            <div class="bs-apply-row">
              <label>{{ 'bulkSave.folder' | transloco }}</label>
              <select [value]="applyFolder()" (change)="applyFolder.set($any($event.target).value)" [disabled]="saving()">
                @for (f of folderOptions(); track f.id) {
                  <option [value]="f.id">{{ indent(f.depth) }}{{ f.name }}</option>
                }
              </select>
              <button type="button" class="bs-apply-btn" [disabled]="saving()" (click)="applyFolderToAll()">
                {{ 'bulkSave.applyAll' | transloco }}
              </button>
            </div>
            @if (docSingleDict().length || docListDefs().length) {
              <div class="bs-apply-row bs-apply-labels">
                <label>{{ 'bulkSave.docLabels' | transloco }}</label>
                <jl-label-editor [labels]="applyTags()" [singleDict]="docSingleDict()" [listDefs]="docListDefs()"
                                 [disabled]="saving()" (changed)="applyTags.set($event)" />
                <button type="button" class="bs-apply-btn" [disabled]="saving() || !applyTags().length" (click)="applyTagsToAll()">
                  {{ 'bulkSave.applyAll' | transloco }}
                </button>
              </div>
            }
          </div>

          @if (typeChips().length > 1) {
            <div class="bs-typechips">
              @for (c of typeChips(); track c.ext) {
                <button type="button" class="bs-typechip" [class.on]="c.allIncluded" [disabled]="saving()"
                        (click)="toggleType(c.ext)">
                  <span class="ftype small" [class]="'kind-' + c.kind"><jl-icon [name]="glyph(c.kind)" [size]="11" /></span>
                  {{ c.ext || '—' }} <span class="bs-typecount">{{ c.count }}</span>
                </button>
              }
            </div>
          }

          <div class="bs-totals">
            <span>{{ 'bulkSave.selectedCount' | transloco: { included: includedCount(), total: entries().length } }}</span>
            <span>{{ totalSizeLabel() }}</span>
          </div>

          <ul class="bs-entries">
            @for (e of entries(); track e.key) {
              <li class="bs-entry" [class.excluded]="!e.include" [class.conflict]="conflictKeys().has(e.key) && e.include">
                <input type="checkbox" [checked]="e.include" (change)="toggleInclude(e.key, $any($event.target).checked)" [disabled]="saving()" />
                <span class="ftype" [class]="'kind-' + fileKindOf(e.ext)" [title]="e.ext">
                  <jl-icon [name]="glyph(fileKindOf(e.ext))" [size]="14" />
                  <span class="ext-lbl">{{ e.ext || '—' }}</span>
                </span>
                <div class="bs-entry-main">
                  <input class="bs-fname" type="text" [value]="e.fileName" [disabled]="saving() || !e.include"
                         (input)="setFileName(e.key, $any($event.target).value)" (blur)="sanitizeName(e.key)" />
                  <div class="bs-entry-meta">
                    @if (templates().length) {
                      <select class="bs-tpl" [value]="e.templateId" [disabled]="saving() || !e.include"
                              (change)="setEntryTemplate(e.key, $any($event.target).value)"
                              [title]="'bulkSave.nameTemplate' | transloco">
                        @for (tpl of templates(); track tpl.id) {
                          <option [value]="tpl.id">{{ tpl.displayName }}</option>
                        }
                      </select>
                    }
                    <select class="bs-folder" [value]="e.folderId" [disabled]="saving() || !e.include"
                            (change)="setFolder(e.key, $any($event.target).value)">
                      @for (f of folderOptions(); track f.id) {
                        <option [value]="f.id">{{ indent(f.depth) }}{{ f.name }}</option>
                      }
                    </select>
                    <button type="button" class="bs-fav" [class.on]="e.favorite" [disabled]="saving() || !e.include"
                            (click)="toggleFavorite(e.key)" [title]="'bulkSave.favorite' | transloco">
                      <jl-icon name="star" [size]="15" />
                    </button>
                    @if (e.sizeBytes >= 0) { <span class="bs-size">{{ sizeLabel(e.sizeBytes) }}</span> }
                    @switch (e.status) {
                      @case ('saving') { <jl-icon name="refresh" [size]="14" class="spin" /> }
                      @case ('done') { <span class="bs-ok"><jl-icon name="check" [size]="14" /></span> }
                      @case ('error') { <span class="bs-err"><jl-icon name="close" [size]="14" /></span> }
                    }
                  </div>
                  @if ((docSingleDict().length || docListDefs().length) && e.include) {
                    <jl-label-editor class="bs-entry-labels" [labels]="e.tags" [singleDict]="docSingleDict()"
                                     [listDefs]="docListDefs()" [disabled]="saving()"
                                     (changed)="setEntryTags(e.key, $event)" />
                  }
                </div>
              </li>
            }
          </ul>

          @if (hasConflicts()) { <p class="bs-warn">{{ 'bulkSave.duplicateWarning' | transloco }}</p> }
          @if (error()) { <p class="bs-error">{{ error() }}</p> }
          @if (allDone()) { <p class="bs-success">{{ 'bulkSave.saved' | transloco }}</p> }
        }
      </div>

      <footer class="bs-foot">
        @if (target()) {
          <button type="button" class="bs-followup" [disabled]="saving()" (click)="followupOpen.set(true)">
            <jl-icon name="calendar" [size]="15" /> <span>{{ 'bulkSave.createFollowup' | transloco }}</span>
          </button>
        }
        <span class="bs-foot-spacer"></span>
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="tryClose()">
          {{ (allDone() ? 'bulkSave.done' : 'bulkSave.cancel') | transloco }}
        </button>
        @if (!allDone()) {
          <button type="button" class="btn-primary"
                  [disabled]="saving() || busy() || !target() || includedCount() === 0 || hasConflicts()" (click)="save()">
            <jl-icon name="inbox" [size]="15" />
            <span>{{ (saving() ? 'bulkSave.saving' : 'bulkSave.save') | transloco }}</span>
          </button>
        }
      </footer>
    </div>

    @if (followupOpen() && target()) {
      <jl-event-editor [presetCase]="followupCase()" (save)="onFollowupSave($event)" (close)="followupOpen.set(false)" />
    }
  `,
  styleUrl: '../communication/email-bulk-save.component.css',
})
export class BeaBulkSaveComponent implements OnInit {
  private readonly api = inject(BeaService);
  private readonly cases = inject(CasesService);
  private readonly calendar = inject(CalendarService);
  private readonly transloco = inject(TranslocoService);

  readonly safeId = input.required<string>();
  readonly message = input.required<BeaMessage>();
  readonly suggestions = input<CaseSuggestions | null>(null);
  readonly preselect = input<TargetCase | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  protected readonly target = signal<TargetCase | null>(null);
  protected readonly term = signal('');
  protected readonly results = signal<CaseOverview[]>([]);

  protected readonly entries = signal<SaveEntry[]>([]);
  protected readonly folderOptions = signal<FolderOption[]>([]);
  protected readonly templates = signal<DocumentNameTemplate[]>([]);
  private readonly existingNames = signal<Set<string>>(new Set());

  protected readonly docSingleDict = signal<string[]>([]);
  protected readonly docListDefs = signal<MultiValueTagDef[]>([]);
  protected readonly applyTags = signal<LabelValue[]>([]);

  protected readonly caseTags = signal<CaseTag[]>([]);
  protected readonly caseSingleDict = signal<string[]>([]);
  protected readonly caseListDefs = signal<MultiValueTagDef[]>([]);
  protected readonly caseLabels = computed<LabelValue[]>(() =>
    this.caseTags().map((t) => ({ name: t.name, value: t.value })));

  protected readonly applyTemplate = signal('');
  protected readonly applyFolder = signal('');

  protected readonly saving = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly followupOpen = signal(false);

  private readonly search$ = new Subject<string>();

  protected readonly suggestedCases = computed(() => this.suggestions()?.suggestedCases ?? []);
  protected readonly includedCount = computed(() => this.entries().filter((e) => e.include).length);
  protected readonly allDone = computed(() => {
    const es = this.entries().filter((e) => e.include);
    return es.length > 0 && es.every((e) => e.status === 'done');
  });

  protected readonly typeChips = computed<TypeChip[]>(() => {
    const groups = new Map<string, SaveEntry[]>();
    for (const e of this.entries()) {
      const list = groups.get(e.ext) ?? [];
      list.push(e);
      groups.set(e.ext, list);
    }
    return [...groups.entries()]
      .map(([ext, es]) => ({ ext, kind: fileKind(ext), count: es.length, allIncluded: es.every((e) => e.include) }))
      .sort((a, b) => a.ext.localeCompare(b.ext));
  });

  protected readonly conflictKeys = computed(() => {
    const seen = new Map<string, number>();
    const existing = this.existingNames();
    for (const e of this.entries()) {
      if (!e.include) { continue; }
      const n = e.fileName.trim().toLowerCase();
      if (!n) { continue; }
      seen.set(n, (seen.get(n) ?? 0) + 1);
    }
    const bad = new Set<string>();
    for (const e of this.entries()) {
      if (!e.include) { continue; }
      const n = e.fileName.trim().toLowerCase();
      if (!n || existing.has(n) || (seen.get(n) ?? 0) > 1) { bad.add(e.key); }
    }
    return bad;
  });
  protected readonly hasConflicts = computed(() => {
    const bad = this.conflictKeys();
    return this.entries().some((e) => e.include && (bad.has(e.key) || !e.fileName.trim()));
  });

  protected readonly followupCase = computed<CaseRef | null>(() => {
    const t = this.target();
    return t ? { id: t.id, fileNumber: t.fileNumber, name: t.name } : null;
  });

  constructor() {
    this.search$.pipe(
      debounceTime(250),
      switchMap((q) => q.trim().length < 2 ? of([] as CaseOverview[]) : this.cases.searchCases(q.trim(), 15)),
      takeUntilDestroyed(),
    ).subscribe((rows) => this.results.set(rows));

    this.cases.documentTagDictionary().subscribe((d) => this.docSingleDict.set(d));
    this.cases.documentMultiValueTags().subscribe((d) => this.docListDefs.set(d));
    this.cases.tagTemplates().subscribe((t) => this.caseSingleDict.set(t));
    this.cases.multiValueTags().subscribe((d) => this.caseListDefs.set(d));
  }

  ngOnInit(): void {
    const pre = this.preselect();
    if (pre) { this.pick(pre); return; }
    const cs = this.suggestedCases();
    if (cs.length === 1) { this.pick({ id: cs[0].id, fileNumber: cs[0].fileNumber, name: cs[0].name }); }
  }

  protected onSearch(ev: Event): void {
    const v = (ev.target as HTMLInputElement).value;
    this.term.set(v);
    this.search$.next(v);
  }

  protected pick(c: TargetCase): void {
    this.target.set(c);
    this.results.set([]);
    this.term.set('');
    this.buildEntries();
    this.loadCaseContext(c.id);
  }

  protected clearTarget(): void {
    if (this.saving()) { return; }
    this.target.set(null);
    this.entries.set([]);
    this.folderOptions.set([]);
    this.existingNames.set(new Set());
    this.caseTags.set([]);
    this.error.set(null);
  }

  /** Builds the .bea (whole message) entry plus one entry per attachment. */
  private buildEntries(): void {
    const msg = this.message();
    const beaName = ensureExt(safeName(msg.subject) || 'beA-Nachricht', '.bea');
    const es: SaveEntry[] = [{
      key: 'bea', kind: 'bea', origName: beaName, ext: 'BEA', include: true,
      fileName: beaName, folderId: '', favorite: false, tags: [], templateId: '', sizeBytes: -1, status: 'idle',
    }];
    for (const a of (msg.attachments ?? [])) {
      const name = safeName(a.alias || a.name) || 'anhang';
      es.push({
        key: 'att:' + a.name, kind: 'attachment', attachmentName: a.name, inlineContent: a.content,
        origName: a.alias || a.name || name, ext: extOf(a.alias || a.name || name),
        include: !a.technicalAttachment, fileName: name, folderId: '', favorite: false,
        tags: [], templateId: '', sizeBytes: a.size ?? -1, status: 'idle',
      });
    }
    this.entries.set(es);
  }

  private loadCaseContext(caseId: string): void {
    this.cases.caseFolders(caseId).subscribe((root) => {
      this.folderOptions.set(flattenFolders(root, this.transloco.translate('bulkSave.rootFolder')));
    });
    this.cases.caseDocumentNames(caseId).subscribe((names) =>
      this.existingNames.set(new Set(names.map((n) => n.toLowerCase()))));
    this.cases.tags(caseId).subscribe((t) => this.caseTags.set(t));
    this.cases.documentNameTemplates().subscribe((tpls) => {
      this.templates.set(tpls);
      const def = tpls.find((t) => t.defaultTemplate) ?? tpls[0];
      if (def) {
        this.applyTemplate.set(def.id);
        this.entries.update((es) => es.map((e) => ({ ...e, templateId: def.id })));
        this.applyTemplateToAll();
      }
    });
  }

  protected applyTemplateToAll(): void {
    const t = this.target();
    const tplId = this.applyTemplate();
    if (!t || !tplId) { return; }
    this.entries.update((es) => es.map((e) => e.include ? { ...e, templateId: tplId } : e));
    const date = this.messageDate();
    const included = this.entries().filter((e) => e.include);
    if (!included.length) { return; }
    this.busy.set(true);
    forkJoin(included.map((e) => this.cases.computeDocumentName(t.id, e.origName, date, tplId).pipe(
      map((name) => ({ key: e.key, name })),
    ))).subscribe({
      next: (rows) => { for (const r of rows) { this.patch(r.key, { fileName: r.name }); } this.busy.set(false); },
      error: () => this.busy.set(false),
    });
  }

  protected applyFolderToAll(): void {
    const fid = this.applyFolder();
    this.entries.update((es) => es.map((e) => e.include ? { ...e, folderId: fid } : e));
  }

  protected applyTagsToAll(): void {
    const tags = this.applyTags();
    this.entries.update((es) => es.map((e) => e.include ? { ...e, tags: tags.map((x) => ({ ...x })) } : e));
  }

  protected toggleType(ext: string): void {
    const chip = this.typeChips().find((c) => c.ext === ext);
    const next = !(chip?.allIncluded ?? false);
    this.entries.update((es) => es.map((e) => e.ext === ext ? { ...e, include: next } : e));
  }

  protected toggleInclude(key: string, on: boolean): void { this.patch(key, { include: on }); }
  protected setFileName(key: string, v: string): void { this.patch(key, { fileName: v }); }
  protected setFolder(key: string, v: string): void { this.patch(key, { folderId: v }); }
  protected setEntryTags(key: string, tags: LabelValue[]): void { this.patch(key, { tags }); }
  protected toggleFavorite(key: string): void {
    const e = this.entries().find((x) => x.key === key);
    if (e) { this.patch(key, { favorite: !e.favorite }); }
  }

  protected setEntryTemplate(key: string, tplId: string): void {
    const t = this.target();
    const e = this.entries().find((x) => x.key === key);
    if (!t || !e) { return; }
    this.patch(key, { templateId: tplId });
    this.busy.set(true);
    this.cases.computeDocumentName(t.id, e.origName, this.messageDate(), tplId).subscribe({
      next: (name) => { this.patch(key, { fileName: name }); this.busy.set(false); },
      error: () => this.busy.set(false),
    });
  }

  protected sanitizeName(key: string): void {
    const e = this.entries().find((x) => x.key === key);
    if (!e) { return; }
    const cleaned = preserveExtension(e.origName, safeName(e.fileName));
    if (cleaned !== e.fileName) { this.patch(key, { fileName: cleaned }); }
  }

  private patch(key: string, p: Partial<SaveEntry>): void {
    this.entries.update((es) => es.map((e) => e.key === key ? { ...e, ...p } : e));
  }

  protected onCaseLabelsChanged(next: LabelValue[]): void {
    const t = this.target();
    if (!t) { return; }
    const prev = this.caseLabels();
    const prevByName = new Map(prev.map((p) => [p.name, p.value]));
    const nextByName = new Map(next.map((n) => [n.name, n.value]));
    const calls: Observable<unknown>[] = [];
    for (const n of next) {
      if (prevByName.get(n.name) !== n.value) { calls.push(this.cases.addTag(t.id, n.name, n.value || undefined)); }
    }
    for (const p of prev) {
      if (!nextByName.has(p.name)) {
        const tag = this.caseTags().find((x) => x.name === p.name);
        if (tag) { calls.push(this.cases.removeTag(tag.id)); }
      }
    }
    this.caseTags.update((tags) => {
      const kept = tags.filter((x) => nextByName.has(x.name)).map((x) => ({ ...x, value: nextByName.get(x.name) ?? x.value }));
      for (const n of next) {
        if (!kept.some((m) => m.name === n.name)) { kept.push({ id: 'pending', name: n.name, value: n.value }); }
      }
      return kept;
    });
    if (calls.length) {
      forkJoin(calls).subscribe({ next: () => this.reloadCaseTags(t.id), error: () => this.reloadCaseTags(t.id) });
    }
  }

  private reloadCaseTags(caseId: string): void {
    this.cases.tags(caseId).subscribe((t) => { if (this.target()?.id === caseId) { this.caseTags.set(t); } });
  }

  protected save(): void {
    const t = this.target();
    if (this.saving() || !t || this.hasConflicts()) { return; }
    const included = this.entries().filter((e) => e.include);
    if (!included.length) { return; }
    this.error.set(null);
    this.saving.set(true);
    this.entries.update((es) => es.map((e) => e.include ? { ...e, status: 'saving' } : e));

    let failures = 0;
    from(included).pipe(
      mergeMap((entry) => defer(() => this.saveEntry(t.id, entry)).pipe(
        map(() => ({ key: entry.key, ok: true })),
        catchError(() => of({ key: entry.key, ok: false })),
      ), CONCURRENCY),
    ).subscribe({
      next: (r) => { if (!r.ok) { failures++; } this.patch(r.key, { status: r.ok ? 'done' : 'error' }); },
      complete: () => {
        this.saving.set(false);
        if (failures > 0) { this.error.set(this.transloco.translate('bulkSave.partialError', { failed: failures })); }
        else { this.saved.emit(); }
      },
    });
  }

  private saveEntry(caseId: string, entry: SaveEntry): Observable<unknown> {
    const sid = this.safeId();
    const msgId = this.message().id;
    const bytes$: Observable<string> = entry.kind === 'bea'
      ? this.api.exportMessage(sid, msgId).pipe(map((e) => e.contentBase64))
      : entry.inlineContent
        ? of(entry.inlineContent)
        : this.api.getAttachment(sid, msgId, entry.attachmentName!).pipe(
          switchMap((full) => full.content ? of(full.content) : throwError(() => new Error('no content'))));

    const dateIso = this.messageDateIso();
    return bytes$.pipe(
      switchMap((b64) => this.cases.uploadDocument(caseId, entry.fileName, b64, entry.folderId || undefined)),
      switchMap((created) => {
        const ops: Observable<unknown>[] = [];
        if (entry.favorite || dateIso) {
          ops.push(this.cases.updateDocumentMetadata({
            id: created.id, caseId, name: entry.fileName,
            creationDate: dateIso, changeDate: dateIso,
            favorite: entry.favorite, highlight1: HIGHLIGHT_NONE, highlight2: HIGHLIGHT_NONE,
            externalId: null, ...(entry.folderId ? { folderId: entry.folderId } : {}),
          }));
        }
        for (const tag of entry.tags) { ops.push(this.cases.addDocumentTag(created.id, tag.name, tag.value || undefined)); }
        return ops.length ? forkJoin(ops) : of(null);
      }),
    );
  }

  protected onFollowupSave(draft: EventDraft): void {
    this.calendar.save(draft).subscribe({
      next: () => this.followupOpen.set(false),
      error: () => this.followupOpen.set(false),
    });
  }

  protected tryClose(): void {
    if (this.saving()) { return; }
    this.closed.emit();
  }

  private messageDate(): Date | null {
    const d = this.message().receptionTime || this.message().createdTime;
    return d ? new Date(d) : null;
  }

  private messageDateIso(): string {
    const d = this.messageDate();
    return (d ?? new Date()).toISOString();
  }

  protected indent(depth: number): string { return depth > 0 ? '  '.repeat(depth) : ''; }
  protected fileKindOf(ext: string): string { return fileKind(ext); }
  protected glyph(kind: string): string { return kindGlyph(kind as ReturnType<typeof fileKind>); }
  protected sizeLabel(bytes: number): string { return humanSize(bytes); }

  protected totalSizeLabel(): string {
    const known = this.entries().filter((e) => e.include && e.sizeBytes >= 0);
    if (!known.length) { return ''; }
    return humanSize(known.reduce((s, e) => s + e.sizeBytes, 0));
  }
}

// ---------- module-scope helpers ----------

function flattenFolders(root: DocFolder | null, rootLabel: string): FolderOption[] {
  const out: FolderOption[] = [];
  const visit = (f: DocFolder, depth: number, isRoot: boolean): void => {
    out.push({ id: isRoot ? '' : f.id, name: isRoot ? (f.name || rootLabel) : f.name, depth, isRoot });
    [...(f.children ?? [])]
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
      .forEach((c) => visit(c, depth + 1, false));
  };
  if (root) { visit(root, 0, true); } else { out.push({ id: '', name: rootLabel, depth: 0, isRoot: true }); }
  return out;
}

function safeName(s: string): string {
  return (s ?? '').replace(/[\\/:*?"<>|]+/g, '_').trim().slice(0, 120);
}

function ensureExt(name: string, ext: string): string {
  return name.toLowerCase().endsWith(ext) ? name : name + ext;
}

function extOf(name: string): string {
  const dot = (name ?? '').lastIndexOf('.');
  return dot > 0 ? name.slice(dot + 1).toUpperCase() : '';
}

function preserveExtension(origName: string, newName: string): string {
  const dot = origName.lastIndexOf('.');
  if (dot < 0) { return newName; }
  const ext = origName.slice(dot);
  return newName.toLowerCase().endsWith(ext.toLowerCase()) ? newName : newName + ext;
}

function humanSize(bytes: number): string {
  if (bytes < 1024) { return `${bytes} B`; }
  const kb = bytes / 1024;
  if (kb < 1024) { return `${Math.round(kb)} KB`; }
  return `${(kb / 1024).toFixed(1)} MB`;
}
