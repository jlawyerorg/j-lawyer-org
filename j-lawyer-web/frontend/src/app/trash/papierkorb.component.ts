import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { BinDocument, DocumentBin, TrashService } from './trash.service';

type Filter = 'all' | 'case' | 'address';

/**
 * The document recycle bin ("Papierkorb") — the web equivalent of the desktop `DocumentsBinDialog`.
 * Lists soft-deleted case and contact documents, restores or permanently deletes them (with a
 * confirm), can empty the whole bin, shows the total size and the auto-purge retention period
 * (editable with `adminRole`). Owner links deep-link to the case/contact.
 */
@Component({
  selector: 'jl-papierkorb',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink, DatePipe],
  template: `
    <div class="tr">
      <header class="tr-head">
        <div>
          <h1>{{ 'trash.title' | transloco }}</h1>
          <p class="tr-sub">{{ 'trash.summary' | transloco: { count: documents().length, size: sizeStr(bin()?.totalBytes ?? 0) } }}</p>
        </div>
        <div class="tr-actions">
          <button type="button" class="btn-ghost" [disabled]="loading()" (click)="reload()"><jl-icon name="refresh" [size]="15" /><span>{{ 'trash.refresh' | transloco }}</span></button>
          @if (documents().length) {
            <button type="button" class="btn-ghost danger" (click)="askEmpty()"><jl-icon name="trash" [size]="15" /><span>{{ 'trash.empty' | transloco }}</span></button>
          }
        </div>
      </header>

      <div class="tr-bar">
        <div class="tr-filters">
          @for (f of filters; track f) {
            <button type="button" class="chip" [class.on]="filter() === f" (click)="filter.set(f)">{{ ('trash.filter.' + f) | transloco }}</button>
          }
        </div>
        <div class="tr-retention">
          <span>{{ 'trash.retention' | transloco }}</span>
          @if (canAdmin()) {
            <input type="number" min="0" [value]="retention()" (input)="retention.set(toInt($any($event.target).value))" />
            <span>{{ 'trash.days' | transloco }}</span>
            <button type="button" class="btn-ghost sm" [disabled]="retentionSaving()" (click)="saveRetention()">{{ (retentionSaving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
            @if (retentionOk()) { <span class="ok">✓</span> }
          } @else {
            <b>{{ retention() }}</b> <span>{{ 'trash.days' | transloco }}</span>
          }
        </div>
      </div>

      @if (opError()) { <p class="tr-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="tr-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="tr-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (filtered().length === 0) {
        <p class="tr-muted">{{ 'trash.emptyState' | transloco }}</p>
      } @else {
        <ul class="tr-list">
          @for (d of filtered(); track d.type + d.id) {
            <li class="tr-row">
              <span class="tr-type" [class.addr]="d.type === 'address'">{{ ('trash.type.' + d.type) | transloco }}</span>
              <div class="tr-main">
                <span class="tr-name">{{ d.name }}</span>
                @if (d.ownerId) {
                  <a class="tr-owner" [routerLink]="['/', d.type === 'case' ? 'cases' : 'contacts', d.ownerId]">
                    {{ d.ownerReference ? (d.ownerReference + ' · ') : '' }}{{ d.ownerLabel }}
                  </a>
                }
              </div>
              <span class="tr-meta">{{ d.deletionDate ? (d.deletionDate | date: 'dd.MM.yyyy HH:mm') : '—' }}@if (d.deletedBy) { · {{ d.deletedBy }} }</span>
              <span class="tr-size">{{ sizeStr(d.size) }}</span>
              <button type="button" class="icon-btn" [disabled]="busy()" (click)="restore(d)" [title]="'trash.restore' | transloco" [attr.aria-label]="'trash.restore' | transloco"><jl-icon name="reply" [size]="15" /></button>
              <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="askDelete(d)" [title]="'trash.deleteForever' | transloco" [attr.aria-label]="'trash.deleteForever' | transloco"><jl-icon name="trash" [size]="15" /></button>
            </li>
          }
        </ul>
      }
    </div>

    @if (confirm(); as c) {
      <div class="cf-backdrop" (click)="confirm.set(null)"></div>
      <div class="cf-dialog" role="dialog" aria-modal="true">
        <h2>{{ (c.kind === 'empty' ? 'trash.confirmEmptyTitle' : 'trash.confirmDeleteTitle') | transloco }}</h2>
        <p>{{ (c.kind === 'empty' ? 'trash.confirmEmpty' : 'trash.confirmDelete') | transloco: { name: c.doc?.name } }}</p>
        <div class="cf-foot">
          <button type="button" class="btn-ghost" [disabled]="busy()" (click)="confirm.set(null)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-danger" [disabled]="busy()" (click)="doConfirm()">{{ (busy() ? 'settings.saving' : 'trash.deleteForever') | transloco }}</button>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .tr { display: flex; flex-direction: column; gap: 14px; }
    .tr-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .tr-head h1 { margin: 0; font-size: 1.15rem; font-weight: 700; color: var(--jl-ink); }
    .tr-sub { margin: 3px 0 0; font-size: .84rem; color: var(--jl-ink-soft); }
    .tr-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .tr-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .tr-filters { display: flex; gap: 6px; }
    .chip { font: inherit; font-size: .82rem; padding: 6px 12px; border: 1px solid var(--jl-line-strong); border-radius: 999px; background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .chip.on { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    .tr-retention { display: flex; align-items: center; gap: 8px; font-size: .82rem; color: var(--jl-ink-faint); }
    .tr-retention input { width: 68px; font: inherit; padding: 5px 8px; border: 1px solid var(--jl-line-strong); border-radius: 7px; background: var(--jl-surface); color: var(--jl-ink); }
    .tr-retention .ok { color: var(--jl-green, #1a7f37); }
    .tr-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .tr-error { color: var(--jl-red); font-size: .84rem; margin: 0; }
    .tr-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
    .tr-row { display: grid; grid-template-columns: 84px 1fr auto auto auto auto; align-items: center; gap: 12px; padding: 9px 6px; border-bottom: 1px solid var(--jl-line); }
    .tr-type { font-size: .7rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; text-align: center; padding: 3px 6px; border-radius: 6px; background: color-mix(in srgb, var(--jl-blue) 15%, transparent); color: var(--jl-blue); }
    .tr-type.addr { background: color-mix(in srgb, var(--jl-green, #1a7f37) 16%, transparent); color: var(--jl-green, #1a7f37); }
    .tr-main { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
    .tr-name { font-weight: 600; color: var(--jl-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .tr-owner { font-size: .8rem; color: var(--jl-blue); text-decoration: none; }
    .tr-owner:hover { text-decoration: underline; }
    .tr-meta { font-size: .8rem; color: var(--jl-ink-faint); white-space: nowrap; }
    .tr-size { font-size: .8rem; color: var(--jl-ink-soft); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .icon-btn { display: inline-grid; place-items: center; width: 32px; height: 32px; border: 0; border-radius: 8px; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .icon-btn:hover { background: var(--jl-surface-alt); color: var(--jl-ink); }
    .icon-btn.danger:hover { color: var(--jl-red); }
    .icon-btn:disabled { opacity: .5; cursor: default; }
    .btn-ghost { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .84rem; padding: 7px 12px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost.sm { padding: 5px 10px; font-size: .8rem; }
    .btn-ghost.danger { color: var(--jl-red); border-color: color-mix(in srgb, var(--jl-red) 40%, var(--jl-line-strong)); }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    .cf-backdrop { position: fixed; inset: 0; background: rgba(4, 12, 20, .45); z-index: 42; }
    .cf-dialog { position: fixed; z-index: 43; left: 50%; top: 50%; transform: translate(-50%, -50%); width: min(440px, calc(100vw - 32px));
      background: var(--jl-surface); color: var(--jl-ink); border: 1px solid var(--jl-line-strong); border-radius: 14px; box-shadow: 0 24px 60px rgba(4, 12, 20, .4); padding: 18px 20px; }
    .cf-dialog h2 { margin: 0 0 8px; font-size: 1rem; font-weight: 700; }
    .cf-dialog p { margin: 0 0 16px; font-size: .88rem; color: var(--jl-ink-soft); }
    .cf-foot { display: flex; justify-content: flex-end; gap: 8px; }
    .btn-danger { font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px; background: var(--jl-red); color: #fff; cursor: pointer; }
    .btn-danger:disabled { opacity: .55; cursor: default; }
    @media (max-width: 720px) {
      .tr-row { grid-template-columns: 64px 1fr auto auto; }
      .tr-meta, .tr-size { display: none; }
    }
  `],
})
export class PapierkorbComponent implements OnInit {
  private readonly api = inject(TrashService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly filters: Filter[] = ['all', 'case', 'address'];
  protected readonly filter = signal<Filter>('all');
  protected readonly bin = signal<DocumentBin | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly confirm = signal<{ kind: 'delete' | 'empty'; doc?: BinDocument } | null>(null);
  protected readonly retention = signal(7);
  protected readonly retentionSaving = signal(false);
  protected readonly retentionOk = signal(false);
  protected readonly canAdmin = computed(() => this.auth.hasRole('adminRole'));

  protected readonly documents = computed(() => this.bin()?.documents ?? []);
  protected readonly filtered = computed(() => {
    const f = this.filter();
    const docs = this.documents();
    return (f === 'all' ? docs : docs.filter((d) => d.type === f))
      .slice().sort((a, b) => b.deletionDate - a.deletionDate);
  });

  ngOnInit(): void { this.reload(); }

  protected reload(): void {
    this.loading.set(true); this.loadError.set(false); this.opError.set(null);
    this.api.getBin().subscribe({
      next: (b) => { this.bin.set(b); this.retention.set(b.retentionDays); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected toInt(v: string): number { const n = parseInt(v, 10); return Number.isFinite(n) ? n : 0; }
  protected sizeStr(bytes: number): string {
    if (bytes >= 1024 * 1024) { return (bytes / (1024 * 1024)).toFixed(1) + ' MB'; }
    if (bytes >= 1024) { return Math.round(bytes / 1024) + ' KB'; }
    return bytes + ' B';
  }

  protected restore(d: BinDocument): void {
    if (this.busy()) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.restore(d.type, d.id).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected askDelete(d: BinDocument): void { this.confirm.set({ kind: 'delete', doc: d }); }
  protected askEmpty(): void { this.confirm.set({ kind: 'empty' }); }

  protected doConfirm(): void {
    const c = this.confirm();
    if (!c || this.busy()) { return; }
    if (c.kind === 'delete' && c.doc) {
      this.busy.set(true); this.opError.set(null);
      this.api.remove(c.doc.type, c.doc.id).subscribe({
        next: () => { this.busy.set(false); this.confirm.set(null); this.reload(); },
        error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
      });
    } else if (c.kind === 'empty') {
      this.emptyAll();
    }
  }

  /** Empties the bin by deleting every listed document (per-doc, so per-type write roles apply). */
  private emptyAll(): void {
    const docs = this.documents();
    if (docs.length === 0) { this.confirm.set(null); return; }
    this.busy.set(true); this.opError.set(null);
    let remaining = docs.length;
    let failed = 0;
    for (const d of docs) {
      this.api.remove(d.type, d.id).subscribe({
        next: () => { if (--remaining === 0) { this.finishEmpty(failed); } },
        error: () => { failed++; if (--remaining === 0) { this.finishEmpty(failed); } },
      });
    }
  }

  private finishEmpty(failed: number): void {
    this.busy.set(false);
    this.confirm.set(null);
    if (failed > 0) { this.opError.set(this.transloco.translate('trash.emptyPartial', { n: failed })); }
    this.reload();
  }

  protected saveRetention(): void {
    if (this.retentionSaving()) { return; }
    this.retentionSaving.set(true); this.retentionOk.set(false); this.opError.set(null);
    this.api.setRetention(this.retention()).subscribe({
      next: () => { this.retentionSaving.set(false); this.retentionOk.set(true); },
      error: (e: HttpErrorResponse) => { this.retentionSaving.set(false); this.opError.set(this.msg(e)); },
    });
  }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
