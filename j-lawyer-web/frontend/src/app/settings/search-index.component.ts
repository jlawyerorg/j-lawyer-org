import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { SearchIndexStatus, SystemSettingsService } from './system-settings.service';

/**
 * Full-text (Lucene) search-index maintenance: shows how many documents are indexed vs. how many
 * exist, and lets an administrator trigger an asynchronous full rebuild. Needs `sysAdminRole`.
 */
@Component({
  selector: 'jl-search-index',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="si">
      @if (!canEdit()) { <p class="si-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loadError()) { <p class="si-error">{{ 'settings.loadError' | transloco }}</p> }

      <div class="si-stats">
        <div class="si-stat">
          <span class="si-num">{{ loading() ? '…' : status().totalDocuments }}</span>
          <span class="si-lbl">{{ 'settings.searchIndex.total' | transloco }}</span>
        </div>
        <div class="si-stat">
          <span class="si-num">{{ loading() ? '…' : status().indexedDocuments }}</span>
          <span class="si-lbl">{{ 'settings.searchIndex.indexed' | transloco }}</span>
        </div>
        <button type="button" class="si-refresh" [disabled]="loading()" (click)="refresh()" [title]="'settings.searchIndex.refresh' | transloco">
          <jl-icon name="refresh" [size]="16" />
        </button>
      </div>

      @if (reindexStarted()) { <p class="si-ok">{{ 'settings.searchIndex.reindexStarted' | transloco }}</p> }
      @if (reindexError()) { <p class="si-error">{{ 'settings.saveError' | transloco }}</p> }

      <div class="si-foot">
        <button type="button" class="btn-primary" [disabled]="!canEdit() || reindexing()" (click)="reindex()">
          <jl-icon name="refresh" [size]="15" />
          <span>{{ (reindexing() ? 'settings.searchIndex.reindexing' : 'settings.searchIndex.reindex') | transloco }}</span>
        </button>
      </div>
      <p class="si-note">{{ 'settings.searchIndex.hint' | transloco }}</p>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .si { max-width: 560px; }
    .si-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .si-error { color: var(--jl-red); font-size: .84rem; }
    .si-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .si-stats { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .si-stat { display: flex; flex-direction: column; gap: 3px; padding: 14px 18px; border: 1px solid var(--jl-line);
      border-radius: 10px; background: var(--jl-surface); min-width: 130px; }
    .si-num { font-size: 1.5rem; font-weight: 700; color: var(--jl-ink); font-variant-numeric: tabular-nums; }
    .si-lbl { font-size: .78rem; color: var(--jl-ink-faint); }
    .si-refresh { display: inline-grid; place-items: center; width: 34px; height: 34px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .si-refresh:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .si-refresh:disabled { opacity: .5; cursor: default; }
    .si-foot { display: flex; }
    .si-note { margin: 10px 0 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .btn-primary { display: inline-flex; align-items: center; gap: 7px; font: inherit; font-size: .86rem; font-weight: 600;
      padding: 9px 16px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class SearchIndexComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly status = signal<SearchIndexStatus>({ indexedDocuments: 0, totalDocuments: 0 });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly reindexing = signal(false);
  protected readonly reindexStarted = signal(false);
  protected readonly reindexError = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  ngOnInit(): void { this.refresh(); }

  protected refresh(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.getSearchIndex().subscribe({
      next: (s) => { this.status.set(s); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected reindex(): void {
    if (!this.canEdit() || this.reindexing()) { return; }
    this.reindexing.set(true);
    this.reindexStarted.set(false);
    this.reindexError.set(false);
    this.api.reindexSearchIndex().subscribe({
      next: () => { this.reindexing.set(false); this.reindexStarted.set(true); },
      error: () => { this.reindexing.set(false); this.reindexError.set(true); },
    });
  }
}
