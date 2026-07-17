import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from '../akten/cases.service';
import { CaseOverview } from '../akten/case.models';
import { CaseSuggestions, DropscanMailing, DropscanScanbox, DropscanService, SuggestedCase } from './dropscan.service';

interface AssignTarget { id: string; fileNumber: string; name: string; }
type PreviewKind = 'pdf' | 'image' | 'loading' | 'none' | 'error';

/**
 * Dropscan view ("Scans") — master/detail: a mailing list on top, and below it the preview of the
 * selected mailing (envelope image for received items, scanned PDF once scanned) together with the
 * case-assignment suggestions (mirroring the beA / e-mail triage). From the detail the user can
 * request a scan, request destruction, and import the scanned mailing into a suggested or searched
 * case. Scoped to the caller's own Dropscan account; needs a configured API token on the profile.
 */
@Component({
  selector: 'jl-scans',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe, RouterLink],
  template: `
    <div class="sc">
      <header class="sc-head">
        <h1>{{ 'scans.title' | transloco }}</h1>
        <div class="sc-filters">
          <select [value]="filterScanbox()" (change)="filterScanbox.set($any($event.target).value); reloadMailings()">
            <option value="">{{ 'scans.allScanboxes' | transloco }}</option>
            @for (s of scanboxes(); track s.id) { <option [value]="s.id">{{ s.number }}</option> }
          </select>
          <select [value]="filterStatus()" (change)="filterStatus.set($any($event.target).value); reloadMailings()">
            <option value="">{{ 'scans.allStatus' | transloco }}</option>
            @for (st of statuses; track st) { <option [value]="st">{{ ('scans.status.' + st) | transloco }}</option> }
          </select>
          <button type="button" class="btn-ghost" [disabled]="loading()" (click)="reloadMailings()"><jl-icon name="refresh" [size]="15" /><span>{{ 'scans.refresh' | transloco }}</span></button>
        </div>
      </header>

      @if (opError()) { <p class="sc-error">{{ opError() }}</p> }

      <div class="sc-split">
        <!-- master -->
        <section class="sc-list-pane">
          @if (loading()) {
            <p class="sc-muted">{{ 'settings.loading' | transloco }}</p>
          } @else if (loadError()) {
            <p class="sc-error">{{ 'scans.loadError' | transloco }}</p>
          } @else if (mailings().length === 0) {
            <p class="sc-muted">{{ 'scans.empty' | transloco }}</p>
          } @else {
            <ul class="sc-list">
              @for (m of mailings(); track m.uuid) {
                <li class="sc-row" [class.sel]="selected()?.uuid === m.uuid" (click)="select(m)">
                  <span class="sc-box">{{ m.scanboxNumber }}</span>
                  <div class="sc-main">
                    <!-- date as the primary label; recipient omitted (Dropscan returns none, mirrors Swing) -->
                    <span class="sc-name">{{ asDate(m.receivedAt) ? (asDate(m.receivedAt) | date: 'dd.MM.yyyy') : ('scans.item' | transloco) }}</span>
                    @if (m.receivedVia) { <span class="sc-sub">{{ m.receivedVia }}</span> }
                  </div>
                  <span class="sc-status" [class.done]="isScanned(m)">{{ ('scans.status.' + m.status) | transloco }}</span>
                </li>
              }
            </ul>
          }
        </section>

        <!-- detail -->
        <section class="sc-detail">
          @if (selected(); as m) {
            <div class="sc-dhead">
              <div>
                <b>{{ 'scans.item' | transloco }}@if (asDate(m.receivedAt)) { {{ asDate(m.receivedAt) | date: 'dd.MM.yyyy' }} }</b>
                <span class="sc-sub">{{ 'scans.box' | transloco }} {{ m.scanboxNumber }} · {{ ('scans.status.' + m.status) | transloco }}@if (m.receivedVia) { · {{ m.receivedVia }} }</span>
              </div>
              <div class="sc-dactions">
                @if (canScan(m)) {
                  <button type="button" class="btn-ghost" [disabled]="busy()" (click)="scan(m)"><jl-icon name="doc" [size]="15" /><span>{{ 'scans.requestScan' | transloco }}</span></button>
                }
                <button type="button" class="btn-ghost danger" [disabled]="busy()" (click)="destroy(m)"><jl-icon name="trash" [size]="15" /><span>{{ 'scans.destroy' | transloco }}</span></button>
              </div>
            </div>

            <div class="sc-dbody">
              <!-- preview -->
              <div class="sc-preview">
                @switch (previewKind()) {
                  @case ('loading') { <p class="sc-muted">{{ 'settings.loading' | transloco }}</p> }
                  @case ('pdf') { <iframe class="sc-frame" [src]="previewUrl()" title="PDF"></iframe> }
                  @case ('image') { <img class="sc-img" [src]="previewImg()" alt="" /> }
                  @case ('error') { <p class="sc-muted">{{ 'scans.noPreview' | transloco }}</p> }
                  @default { <p class="sc-muted">{{ (isScanned(m) ? 'scans.noPreview' : 'scans.notScanned') | transloco }}</p> }
                }
              </div>

              <!-- assignment -->
              <div class="sc-assign">
                <h3>{{ 'scans.assignTitle' | transloco }}</h3>
                @if (!isScanned(m)) {
                  <p class="sc-muted">{{ 'scans.notScannedHint' | transloco }}</p>
                  @if (canScan(m)) {
                    <button type="button" class="btn-primary" [disabled]="busy()" (click)="scan(m)">
                      <jl-icon name="doc" [size]="15" /><span>{{ 'scans.requestScan' | transloco }}</span>
                    </button>
                  }
                } @else {
                  @if (suggLoading()) { <p class="sc-muted">{{ 'settings.loading' | transloco }}</p> }
                  @else if (suggestions()?.suggestedCases?.length) {
                    <ul class="sug-list">
                      @for (c of suggestions()!.suggestedCases; track c.id) {
                        <li class="sug" [class.on]="target()?.id === c.id">
                          <button type="button" class="sug-main" (click)="pickSuggested(c)">
                            <b>{{ c.fileNumber }}</b> {{ c.name }}
                            <span class="sug-src">{{ ('scans.source.' + (c.source || 'text')) | transloco }}</span>
                          </button>
                          <a class="sug-open" [routerLink]="['/cases', c.id]" [title]="'scans.openCase' | transloco"><jl-icon name="chevron-left" [size]="14" /></a>
                        </li>
                      }
                    </ul>
                  } @else {
                    <p class="sc-muted">{{ 'scans.noSuggestions' | transloco }}</p>
                  }

                  <label class="sc-srch">
                    <span class="sc-lbl">{{ 'scans.searchCase' | transloco }}</span>
                    <input type="search" [value]="caseTerm()" (input)="onCaseSearch($any($event.target).value)" [placeholder]="'scans.searchCaseHint' | transloco" />
                  </label>
                  @if (caseResults().length) {
                    <ul class="sug-list">
                      @for (c of caseResults(); track c.id) {
                        <li class="sug" [class.on]="target()?.id === c.id">
                          <button type="button" class="sug-main" (click)="pickCase(c)"><b>{{ c.fileNumber }}</b> {{ c.name }}</button>
                        </li>
                      }
                    </ul>
                  }

                  @if (suggestions()?.phoneNumbers?.length) {
                    <p class="sc-phones"><jl-icon name="phone" [size]="13" /> {{ suggestions()!.phoneNumbers.join(' · ') }}</p>
                  }

                  <div class="sc-assign-foot">
                    @if (target(); as t) { <span class="sc-target">→ <b>{{ t.fileNumber }}</b> {{ t.name }}</span> }
                    <label class="sc-check"><input type="checkbox" [checked]="destroyAfter()" (change)="destroyAfter.set($any($event.target).checked)" /><span>{{ 'scans.destroyAfter' | transloco }}</span></label>
                    @if (importError()) { <span class="sc-error">{{ importError() }}</span> }
                    <button type="button" class="btn-primary" [disabled]="importing() || !target()" (click)="doImport()">{{ (importing() ? 'scans.importing' : 'scans.importBtn') | transloco }}</button>
                  </div>
                }
              </div>
            </div>
          } @else {
            <p class="sc-placeholder">{{ 'scans.selectHint' | transloco }}</p>
          }
        </section>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    .sc { display: flex; flex-direction: column; gap: 12px; height: 100%; min-height: 0; }
    .sc-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .sc-head h1 { margin: 0; font-size: 1.15rem; font-weight: 700; color: var(--jl-ink); }
    .sc-filters { display: flex; gap: 8px; flex-wrap: wrap; }
    .sc-filters select { font: inherit; font-size: .84rem; padding: 6px 9px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); }
    .sc-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .sc-error { color: var(--jl-red); font-size: .84rem; margin: 0; }
    .sc-split { display: grid; grid-template-rows: minmax(120px, 38%) 1fr; gap: 12px; flex: 1 1 auto; min-height: 0; }
    .sc-list-pane { overflow-y: auto; border: 1px solid var(--jl-line); border-radius: 10px; }
    .sc-list { list-style: none; margin: 0; padding: 0; }
    .sc-row { display: grid; grid-template-columns: 84px 1fr auto; align-items: center; gap: 12px; padding: 9px 12px; border-bottom: 1px solid var(--jl-line); cursor: pointer; }
    .sc-row:hover { background: var(--jl-surface-alt); }
    .sc-row.sel { background: color-mix(in srgb, var(--jl-blue) 12%, transparent); }
    .sc-box { font-size: .72rem; font-weight: 700; color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); border-radius: 6px; padding: 3px 7px; text-align: center; }
    .sc-main { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
    .sc-name { font-weight: 600; color: var(--jl-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .sc-sub { font-size: .8rem; color: var(--jl-ink-faint); }
    .sc-status { font-size: .74rem; font-weight: 600; color: var(--jl-ink-soft); }
    .sc-status.done { color: var(--jl-green, #1a7f37); }
    .sc-detail { border: 1px solid var(--jl-line); border-radius: 10px; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
    .sc-placeholder { margin: auto; color: var(--jl-ink-soft); font-size: .9rem; }
    .sc-dhead { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 10px 14px; border-bottom: 1px solid var(--jl-line); flex-wrap: wrap; }
    .sc-dhead b { font-size: .95rem; } .sc-dhead .sc-sub { display: block; }
    .sc-dactions { display: flex; gap: 8px; }
    .sc-dbody { flex: 1 1 auto; min-height: 0; display: grid; grid-template-columns: 1fr 340px; gap: 0; }
    .sc-preview { border-right: 1px solid var(--jl-line); min-height: 0; display: grid; place-items: center; background: var(--jl-surface-alt); overflow: auto; }
    .sc-frame { width: 100%; height: 100%; border: 0; }
    .sc-img { max-width: 100%; max-height: 100%; object-fit: contain; }
    .sc-assign { padding: 12px 14px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
    .sc-assign h3 { margin: 0; font-size: .8rem; font-weight: 700; text-transform: uppercase; letter-spacing: .03em; color: var(--jl-ink-soft); }
    .sug-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .sug { display: flex; align-items: center; gap: 4px; border: 1px solid var(--jl-line); border-radius: 8px; }
    .sug.on { border-color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 10%, transparent); }
    .sug-main { flex: 1 1 auto; text-align: left; font: inherit; font-size: .84rem; padding: 7px 10px; border: 0; background: transparent; color: var(--jl-ink); cursor: pointer; }
    .sug-src { font-size: .7rem; color: var(--jl-ink-faint); margin-left: 6px; }
    .sug-open { display: inline-grid; place-items: center; width: 30px; align-self: stretch; color: var(--jl-ink-soft); transform: rotate(180deg); }
    .sc-srch { display: flex; flex-direction: column; gap: 4px; font-size: .78rem; color: var(--jl-ink-faint); }
    .sc-srch input { font: inherit; font-size: .86rem; padding: 7px 9px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); }
    .sc-phones { display: flex; align-items: center; gap: 6px; font-size: .8rem; color: var(--jl-ink-soft); margin: 0; }
    .sc-assign-foot { margin-top: auto; display: flex; flex-direction: column; gap: 8px; border-top: 1px solid var(--jl-line); padding-top: 10px; }
    .sc-target { font-size: .82rem; color: var(--jl-ink); }
    .sc-check { display: flex; align-items: center; gap: 8px; font-size: .84rem; color: var(--jl-ink); } .sc-check input { width: 15px; height: 15px; }
    .btn-ghost { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .84rem; padding: 7px 12px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost.danger { color: var(--jl-red); border-color: color-mix(in srgb, var(--jl-red) 40%, var(--jl-line-strong)); }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; align-self: flex-end; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 780px) { .sc-dbody { grid-template-columns: 1fr; grid-template-rows: 240px auto; } .sc-preview { border-right: 0; border-bottom: 1px solid var(--jl-line); } }
  `],
})
export class ScansComponent implements OnInit {
  private readonly api = inject(DropscanService);
  private readonly cases = inject(CasesService);
  private readonly transloco = inject(TranslocoService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statuses = ['received', 'scanned', 'destroyed'];
  protected readonly scanboxes = signal<DropscanScanbox[]>([]);
  protected readonly mailings = signal<DropscanMailing[]>([]);
  protected readonly filterScanbox = signal('');
  protected readonly filterStatus = signal('');
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly selected = signal<DropscanMailing | null>(null);

  // preview
  protected readonly previewKind = signal<PreviewKind>('none');
  protected readonly previewUrl = signal<SafeResourceUrl | null>(null);
  protected readonly previewImg = signal<string | null>(null);
  private objectUrl: string | null = null;

  // suggestions + assignment
  protected readonly suggestions = signal<CaseSuggestions | null>(null);
  protected readonly suggLoading = signal(false);
  protected readonly target = signal<AssignTarget | null>(null);
  protected readonly caseTerm = signal('');
  protected readonly caseResults = signal<CaseOverview[]>([]);
  protected readonly destroyAfter = signal(false);
  protected readonly importing = signal(false);
  protected readonly importError = signal<string | null>(null);
  private readonly caseSearch$ = new Subject<string>();

  ngOnInit(): void {
    this.api.listScanboxes().subscribe({ next: (s) => this.scanboxes.set(s ?? []), error: () => undefined });
    this.reloadMailings();
    this.caseSearch$.pipe(
      debounceTime(250), distinctUntilChanged(),
      switchMap((q) => q.trim().length < 2 ? of([] as CaseOverview[]) : this.cases.searchCases(q.trim(), 15)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((r) => this.caseResults.set(r ?? []));
  }

  protected reloadMailings(): void {
    this.loading.set(true); this.loadError.set(false); this.opError.set(null);
    this.api.listMailings(this.filterScanbox() || undefined, this.filterStatus() || undefined).subscribe({
      next: (m) => {
        this.mailings.set([...(m ?? [])].sort((a, b) => this.ts(b.receivedAt) - this.ts(a.receivedAt)));
        this.loading.set(false);
        const sel = this.selected();
        if (sel && !this.mailings().some((x) => x.uuid === sel.uuid)) { this.clearSelection(); }
      },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected isScanned(m: DropscanMailing): boolean { return m.scanned === true || m.status === 'scanned' || m.status === 'forwarded'; }
  protected canScan(m: DropscanMailing): boolean { return m.status === 'received'; }
  protected asDate(v: number | string | undefined): number | string | null {
    if (v == null) { return null; }
    return typeof v === 'number' ? v : v.replace(/\[[^\]]*\]$/, '');
  }
  private ts(v: number | string | undefined): number {
    if (v == null) { return 0; }
    const n = typeof v === 'number' ? v : Date.parse(v.replace(/\[[^\]]*\]$/, ''));
    return Number.isFinite(n) ? n : 0;
  }

  protected select(m: DropscanMailing): void {
    this.selected.set(m);
    this.target.set(null); this.caseTerm.set(''); this.caseResults.set([]);
    this.destroyAfter.set(false); this.importError.set(null); this.suggestions.set(null);
    this.loadPreview(m);
    if (this.isScanned(m)) { this.loadSuggestions(m); }
  }

  private clearSelection(): void {
    this.selected.set(null);
    this.revoke();
    this.previewKind.set('none'); this.previewUrl.set(null); this.previewImg.set(null);
    this.suggestions.set(null); this.target.set(null);
  }

  private loadPreview(m: DropscanMailing): void {
    this.revoke();
    this.previewKind.set('loading'); this.previewUrl.set(null); this.previewImg.set(null);
    if (this.isScanned(m)) {
      // PDF: a blob: object URL in an <iframe> (CSP frame-src allows blob:)
      this.api.mailingPdf(m.scanboxId, m.uuid).subscribe({
        next: (blob) => {
          if (this.selected()?.uuid !== m.uuid) { return; }
          this.objectUrl = URL.createObjectURL(blob);
          this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl));
          this.previewKind.set('pdf');
        },
        error: () => { if (this.selected()?.uuid === m.uuid) { this.previewKind.set('error'); } },
      });
    } else {
      // Envelope: a data: URL in an <img> (CSP img-src allows 'self' data:, not blob:)
      this.api.envelopeImage(m.scanboxId, m.uuid).subscribe({
        next: (blob) => {
          if (this.selected()?.uuid !== m.uuid) { return; }
          const reader = new FileReader();
          reader.onload = () => { if (this.selected()?.uuid === m.uuid) { this.previewImg.set(String(reader.result)); this.previewKind.set('image'); } };
          reader.onerror = () => { if (this.selected()?.uuid === m.uuid) { this.previewKind.set('error'); } };
          reader.readAsDataURL(blob);
        },
        error: () => { if (this.selected()?.uuid === m.uuid) { this.previewKind.set('error'); } },
      });
    }
  }

  private loadSuggestions(m: DropscanMailing): void {
    this.suggLoading.set(true);
    this.api.caseSuggestions(m.scanboxId, m.uuid).subscribe({
      next: (s) => { if (this.selected()?.uuid === m.uuid) { this.suggestions.set(s); } this.suggLoading.set(false); },
      error: () => { this.suggLoading.set(false); },
    });
  }

  private revoke(): void { if (this.objectUrl) { URL.revokeObjectURL(this.objectUrl); this.objectUrl = null; } }

  protected pickSuggested(c: SuggestedCase): void { this.target.set({ id: c.id, fileNumber: c.fileNumber, name: c.name }); }
  protected pickCase(c: CaseOverview): void { this.target.set({ id: c.id, fileNumber: c.fileNumber, name: c.name }); }
  protected onCaseSearch(v: string): void { this.caseTerm.set(v); this.caseSearch$.next(v); }

  protected scan(m: DropscanMailing): void {
    this.busy.set(true); this.opError.set(null);
    this.api.requestScan(m.scanboxId, m.uuid).subscribe({
      next: () => { this.busy.set(false); this.reloadMailings(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected destroy(m: DropscanMailing): void {
    if (!confirm(this.transloco.translate('scans.destroyConfirm'))) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.requestDestroy(m.scanboxId, m.uuid).subscribe({
      next: () => { this.busy.set(false); this.clearSelection(); this.reloadMailings(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected doImport(): void {
    const m = this.selected();
    const t = this.target();
    if (!m || !t || this.importing() || !this.isScanned(m)) { return; }
    this.importing.set(true); this.importError.set(null);
    this.api.mailingPdf(m.scanboxId, m.uuid).subscribe({
      next: (blob) => {
        const reader = new FileReader();
        reader.onload = () => {
          const result = String(reader.result);
          const comma = result.indexOf(',');
          const b64 = comma >= 0 ? result.slice(comma + 1) : result;
          this.cases.uploadDocument(t.id, this.fileNameFor(m), b64).subscribe({
            next: () => {
              if (this.destroyAfter()) {
                this.api.requestDestroy(m.scanboxId, m.uuid).subscribe({ next: () => this.finishImport(), error: () => this.finishImport() });
              } else { this.finishImport(); }
            },
            error: (e: HttpErrorResponse) => { this.importing.set(false); this.importError.set(this.msg(e)); },
          });
        };
        reader.onerror = () => { this.importing.set(false); this.importError.set(this.transloco.translate('scans.importError')); };
        reader.readAsDataURL(blob);
      },
      error: (e: HttpErrorResponse) => { this.importing.set(false); this.importError.set(this.msg(e)); },
    });
  }

  private finishImport(): void {
    this.importing.set(false);
    this.opError.set(this.transloco.translate('scans.imported'));
    if (this.destroyAfter()) { this.clearSelection(); }
    this.reloadMailings();
  }

  private fileNameFor(m: DropscanMailing): string {
    const d = this.ts(m.receivedAt);
    const stamp = d ? new Date(d).toISOString().slice(0, 10) : 'dropscan';
    // Dropscan returns no recipient, so name by scanbox (like the desktop's Dropscan import).
    const box = String(m.scanboxNumber || '').replace(/[^\wäöüÄÖÜß.-]+/g, '_').trim();
    return `Dropscan_${stamp}${box ? '_' + box : ''}.pdf`;
  }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (body && typeof body === 'object' && typeof body.message === 'string') { return body.message; }
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('scans.loadError');
  }
}
