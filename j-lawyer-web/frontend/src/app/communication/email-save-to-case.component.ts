import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, from, Observable, of, Subject } from 'rxjs';
import { debounceTime, map, switchMap } from 'rxjs/operators';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from '../akten/cases.service';
import { CaseOverview } from '../akten/case.models';
import { EmailService } from './email.service';
import { CaseSuggestions, MailMessage } from './email.models';

/** What to store in the case. */
type SaveMode = 'full' | 'separate' | 'attachmentsOnly';

/** A case chosen as the save target (from a suggestion or a search hit). */
export interface TargetCase { id: string; fileNumber: string; name: string; }

/**
 * Modal for storing an opened email in a case as document(s). Mirrors the desktop client's
 * "save to case": the whole message as an .eml (which already embeds its attachments), optionally
 * plus each attachment as a separate document, or just the attachments. The case is picked from the
 * server-computed suggestions or a case search. Uses existing endpoints only (EML/attachment
 * download + PUT /v1/cases/document/create); no server change.
 */
@Component({
  selector: 'jl-email-save-to-case',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="stc-backdrop" (click)="tryClose()"></div>
    <div class="stc-dialog" role="dialog" aria-modal="true">
      <header class="stc-head">
        <h2>{{ 'saveToCase.title' | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="tryClose()" [attr.aria-label]="'saveToCase.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="stc-body">
        <!-- Case picker -->
        @if (target(); as t) {
          <div class="stc-chosen">
            <jl-icon name="cases" [size]="14" />
            <span class="stc-fn">{{ t.fileNumber }}</span>
            <span class="stc-name">{{ t.name }}</span>
            <button type="button" class="stc-link" (click)="clearTarget()">{{ 'saveToCase.change' | transloco }}</button>
          </div>
        } @else {
          @if (suggestedCases().length) {
            <div class="stc-block">
              <span class="stc-label">{{ 'saveToCase.suggestions' | transloco }}</span>
              <div class="stc-chips">
                @for (c of suggestedCases(); track c.id) {
                  <button type="button" class="stc-chip" (click)="pick({ id: c.id, fileNumber: c.fileNumber, name: c.name })">
                    <span class="stc-fn">{{ c.fileNumber }}</span><span class="stc-name">{{ c.name }}</span>
                  </button>
                }
              </div>
            </div>
          }
          <div class="stc-search">
            <jl-icon name="search" [size]="14" />
            <input type="search" [value]="term()" (input)="onSearch($event)"
                   [placeholder]="'saveToCase.searchPlaceholder' | transloco" autocomplete="off" />
          </div>
          @if (results().length) {
            <ul class="stc-results">
              @for (r of results(); track r.id) {
                <li (click)="pick({ id: r.id, fileNumber: r.fileNumber, name: r.name })">
                  <span class="stc-fn">{{ r.fileNumber }}</span><span class="stc-name">{{ r.name }}</span>
                </li>
              }
            </ul>
          }
        }

        <!-- What to save -->
        @if (target()) {
          <div class="stc-modes">
            <label class="stc-mode">
              <input type="radio" name="stcmode" [checked]="mode() === 'full'" (change)="mode.set('full')" />
              <span>{{ 'saveToCase.modeFull' | transloco }}</span>
            </label>
            @if (attachmentCount() > 0) {
              <label class="stc-mode">
                <input type="radio" name="stcmode" [checked]="mode() === 'separate'" (change)="mode.set('separate')" />
                <span>{{ 'saveToCase.modeSeparate' | transloco }} ({{ attachmentCount() }})</span>
              </label>
              <label class="stc-mode">
                <input type="radio" name="stcmode" [checked]="mode() === 'attachmentsOnly'" (change)="mode.set('attachmentsOnly')" />
                <span>{{ 'saveToCase.modeAttachmentsOnly' | transloco }} ({{ attachmentCount() }})</span>
              </label>
            }
          </div>
        }

        @if (error()) { <p class="stc-error">{{ error() }}</p> }
      </div>

      <footer class="stc-foot">
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="tryClose()">
          {{ 'saveToCase.cancel' | transloco }}
        </button>
        <button type="button" class="btn-primary" [disabled]="saving() || !target()" (click)="save()">
          <jl-icon name="inbox" [size]="15" />
          <span>{{ (saving() ? 'saveToCase.saving' : 'saveToCase.save') | transloco }}</span>
        </button>
      </footer>
    </div>
  `,
  styleUrl: './email-save-to-case.component.css',
})
export class EmailSaveToCaseComponent implements OnInit {
  private readonly api = inject(EmailService);
  private readonly cases = inject(CasesService);
  private readonly transloco = inject(TranslocoService);

  readonly mailboxId = input.required<string>();
  readonly message = input.required<MailMessage>();
  readonly suggestions = input<CaseSuggestions | null>(null);
  /** Preselected target case (e.g. when opened from a specific suggestion chip). */
  readonly preselect = input<TargetCase | null>(null);

  readonly saved = output<void>();
  readonly closed = output<void>();

  protected readonly target = signal<TargetCase | null>(null);
  protected readonly mode = signal<SaveMode>('full');
  protected readonly term = signal('');
  protected readonly results = signal<CaseOverview[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly search$ = new Subject<string>();

  /** Non-inline attachments — those a user would want stored separately. */
  protected readonly attachmentCount = computed(() =>
    (this.message().attachments ?? []).filter((a) => !(a.inline && a.contentId)).length);

  protected readonly suggestedCases = computed(() => this.suggestions()?.suggestedCases ?? []);

  constructor() {
    this.search$.pipe(
      debounceTime(250),
      switchMap((q) => q.trim().length < 2 ? of([] as CaseOverview[]) : this.cases.searchCases(q.trim(), 15)),
      takeUntilDestroyed(),
    ).subscribe((rows) => this.results.set(rows));
  }

  ngOnInit(): void {
    // An explicit preselection (from a chip) wins; otherwise preselect a lone obvious match.
    const pre = this.preselect();
    if (pre) { this.target.set(pre); return; }
    const cs = this.suggestedCases();
    if (cs.length === 1) {
      this.target.set({ id: cs[0].id, fileNumber: cs[0].fileNumber, name: cs[0].name });
    }
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
  }

  protected clearTarget(): void {
    this.target.set(null);
  }

  protected save(): void {
    const t = this.target();
    if (this.saving() || !t) { return; }
    const mid = this.mailboxId();
    const msg = this.message();
    const mode = this.mode();
    this.error.set(null);
    this.saving.set(true);

    const tasks: Observable<unknown>[] = [];

    if (mode !== 'attachmentsOnly') {
      const emlName = ensureExt(safeName(msg.subject) || 'Email', '.eml');
      tasks.push(this.api.getEml(mid, msg.messageRef).pipe(
        switchMap((blob) => from(blobToBase64(blob))),
        switchMap((b64) => this.cases.uploadDocument(t.id, emlName, b64)),
      ));
    }

    if (mode === 'attachmentsOnly' || mode === 'separate') {
      for (const a of (msg.attachments ?? []).filter((x) => !(x.inline && x.contentId))) {
        tasks.push(this.api.getAttachment(mid, msg.messageRef, a.attachmentId).pipe(
          switchMap((full) => full.contentBase64
            ? this.cases.uploadDocument(t.id, safeName(full.name || a.name) || 'attachment', full.contentBase64)
            : of(null)),
        ));
      }
    }

    if (!tasks.length) { this.saving.set(false); this.saved.emit(); return; }

    forkJoin(tasks).subscribe({
      next: () => { this.saving.set(false); this.saved.emit(); },
      error: () => { this.saving.set(false); this.error.set(this.transloco.translate('saveToCase.error')); },
    });
  }

  protected tryClose(): void {
    if (this.saving()) { return; }
    this.closed.emit();
  }
}

// ---------- module-scope helpers ----------

function safeName(s: string): string {
  return (s ?? '').replace(/[\\/:*?"<>|]+/g, '_').trim().slice(0, 120);
}

function ensureExt(name: string, ext: string): string {
  return name.toLowerCase().endsWith(ext) ? name : name + ext;
}

/** Reads a Blob as raw Base64 (no data: prefix), for the document-create payload. */
function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const s = String(reader.result ?? '');
      resolve(s.slice(s.indexOf(',') + 1));
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(blob);
  });
}
