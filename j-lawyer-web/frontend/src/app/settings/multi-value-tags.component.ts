import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { MultiValueTagService, MvEntityType, MvTagDefinition } from './multi-value-tag.service';

interface TagModal { titleKey: string; f1Key: string; f2Key?: string; run: (v1: string, v2: string) => Observable<unknown>; }

/**
 * Editor for multi-value tags (Listenetiketten) of one entity type (case/address/document): a list of
 * tags on the left, the selected tag's allowed values on the right. Supports creating tags, adding /
 * renaming / removing values, and renaming / removing whole tags. Removals and renames cascade to tags
 * already attached to entities (server-side). Adds/renames need `createOptionGroupRole`, removals
 * `deleteOptionGroupRole`.
 */
@Component({
  selector: 'jl-multi-value-tags',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="mv">
      @if (!canCreate() && !canDelete()) { <p class="mv-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (opError()) { <p class="mv-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="mv-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="mv-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <div class="mv-grid">
          <div class="mv-col">
            @if (canCreate()) {
              <button type="button" class="btn-primary mv-new" (click)="openNewTag()">
                <jl-icon name="plus" [size]="15" /><span>{{ 'settings.mvTags.newTag' | transloco }}</span>
              </button>
            }
            @if (tags().length === 0) {
              <p class="mv-muted">{{ 'settings.empty' | transloco }}</p>
            } @else {
              <ul class="mv-list">
                @for (t of tags(); track t.tagName) {
                  <li class="mv-row" [class.sel]="selectedTag() === t.tagName" (click)="selectedTag.set(t.tagName)">
                    <span class="mv-name">{{ t.tagName }}</span>
                    <span class="mv-badge">{{ t.values.length }}</span>
                    @if (canCreate()) {
                      <button type="button" class="icon-btn" (click)="openRenameTag(t); $event.stopPropagation()" [attr.aria-label]="'settings.rename' | transloco">
                        <jl-icon name="edit" [size]="14" />
                      </button>
                    }
                    @if (canDelete()) {
                      <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="removeTag(t); $event.stopPropagation()" [attr.aria-label]="'settings.delete' | transloco">
                        <jl-icon name="trash" [size]="14" />
                      </button>
                    }
                  </li>
                }
              </ul>
            }
          </div>

          <div class="mv-col">
            @if (selected(); as t) {
              <div class="mv-vhead">
                <h3>{{ 'settings.mvTags.values' | transloco }} — {{ t.tagName }}</h3>
                @if (canCreate()) {
                  <button type="button" class="btn-sm" (click)="openAddValue()">{{ 'settings.mvTags.addValue' | transloco }}</button>
                }
              </div>
              @if (t.values.length === 0) {
                <p class="mv-muted">{{ 'settings.empty' | transloco }}</p>
              } @else {
                <ul class="mv-list">
                  @for (v of t.values; track v) {
                    <li class="mv-row">
                      <span class="mv-name">{{ v }}</span>
                      @if (canCreate()) {
                        <button type="button" class="icon-btn" (click)="openRenameValue(v)" [attr.aria-label]="'settings.rename' | transloco">
                          <jl-icon name="edit" [size]="14" />
                        </button>
                      }
                      @if (canDelete()) {
                        <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="removeValue(v)" [attr.aria-label]="'settings.delete' | transloco">
                          <jl-icon name="trash" [size]="14" />
                        </button>
                      }
                    </li>
                  }
                </ul>
              }
            } @else {
              <p class="mv-muted">{{ 'settings.mvTags.selectTag' | transloco }}</p>
            }
          </div>
        </div>
      }
    </div>

    @if (modal(); as m) {
      <div class="ed-backdrop" (click)="closeModal()"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ m.titleKey | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field">
            <span>{{ m.f1Key | transloco }}</span>
            <input type="text" [value]="mv1()" (input)="mv1.set($any($event.target).value)" (keydown.enter)="submitModal()" autofocus />
          </label>
          @if (m.f2Key) {
            <label class="ed-field">
              <span>{{ m.f2Key | transloco }}</span>
              <input type="text" [value]="mv2()" (input)="mv2.set($any($event.target).value)" (keydown.enter)="submitModal()" />
            </label>
          }
          @if (modalError()) { <p class="ed-error">{{ modalError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="closeModal()">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !mv1().trim() || (!!m.f2Key && !mv2().trim())" (click)="submitModal()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .mv { max-width: 760px; }
    .mv-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .mv-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .mv-error { color: var(--jl-red); font-size: .84rem; margin: 0 0 10px; }
    .mv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .mv-new { margin-bottom: 10px; }
    .mv-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .mv-row { display: flex; align-items: center; gap: 8px; padding: 8px 11px; border: 1px solid var(--jl-line);
      border-radius: 8px; background: var(--jl-surface); cursor: pointer; }
    .mv-row:hover { border-color: var(--jl-line-strong); }
    .mv-row.sel { border-color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .mv-name { flex: 1 1 auto; font-size: .9rem; color: var(--jl-ink); }
    .mv-badge { font-size: .74rem; color: var(--jl-ink-faint); background: var(--jl-surface-alt); border-radius: 999px; padding: 1px 8px; }
    .mv-vhead { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
    .mv-vhead h3 { margin: 0; font-size: .9rem; font-weight: 700; color: var(--jl-ink); }
    .icon-btn.danger { color: var(--jl-red); }
    .btn-sm { font: inherit; font-size: .82rem; padding: 6px 12px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-sm:hover { border-color: var(--jl-blue); color: var(--jl-blue); }
    @media (max-width: 620px) { .mv-grid { grid-template-columns: 1fr; } }
  `],
  styleUrls: ['./finance-editor.css'],
})
export class MultiValueTagsComponent {
  private readonly api = inject(MultiValueTagService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  readonly entityType = input.required<MvEntityType>();

  protected readonly tags = signal<MvTagDefinition[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly selectedTag = signal<string | null>(null);
  protected readonly selected = computed(() => this.tags().find((t) => t.tagName === this.selectedTag()) ?? null);

  protected readonly modal = signal<TagModal | null>(null);
  protected readonly mv1 = signal('');
  protected readonly mv2 = signal('');
  protected readonly modalError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.auth.hasRole('createOptionGroupRole'));
  protected readonly canDelete = computed(() => this.auth.hasRole('deleteOptionGroupRole'));

  constructor() {
    // Reload whenever the entity type changes — the component instance is reused across the
    // address/case/document sections (same @case), so we react to the input rather than ngOnInit.
    effect(() => {
      this.entityType();
      this.selectedTag.set(null);
      this.reload();
    });
  }

  private reload(keep?: string): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list(this.entityType()).subscribe({
      next: (list) => {
        this.tags.set([...list].sort((a, b) => a.tagName.localeCompare(b.tagName)));
        const want = keep ?? this.selectedTag();
        this.selectedTag.set(this.tags().some((t) => t.tagName === want) ? want! : (this.tags()[0]?.tagName ?? null));
        this.loading.set(false);
      },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNewTag(): void {
    this.mv1.set(''); this.mv2.set(''); this.modalError.set(null);
    this.modal.set({
      titleKey: 'settings.mvTags.newTag', f1Key: 'settings.mvTags.tagName', f2Key: 'settings.mvTags.firstValue',
      run: (name, value) => this.api.addValue(this.entityType(), name, value),
    });
  }

  protected openRenameTag(t: MvTagDefinition): void {
    this.mv1.set(t.tagName); this.modalError.set(null);
    this.modal.set({
      titleKey: 'settings.mvTags.renameTag', f1Key: 'settings.mvTags.tagName',
      run: (name) => this.api.renameTag(this.entityType(), t.tagName, name),
    });
  }

  protected openAddValue(): void {
    const tag = this.selectedTag();
    if (!tag) { return; }
    this.mv1.set(''); this.modalError.set(null);
    this.modal.set({
      titleKey: 'settings.mvTags.addValue', f1Key: 'settings.mvTags.value',
      run: (value) => this.api.addValue(this.entityType(), tag, value),
    });
  }

  protected openRenameValue(value: string): void {
    const tag = this.selectedTag();
    if (!tag) { return; }
    this.mv1.set(value); this.modalError.set(null);
    this.modal.set({
      titleKey: 'settings.mvTags.renameValue', f1Key: 'settings.mvTags.value',
      run: (nv) => this.api.renameValue(this.entityType(), tag, value, nv),
    });
  }

  protected submitModal(): void {
    const m = this.modal();
    const v1 = this.mv1().trim();
    if (!m || !v1 || (m.f2Key && !this.mv2().trim()) || this.saving()) { return; }
    this.saving.set(true);
    this.modalError.set(null);
    this.opError.set(null);
    m.run(v1, this.mv2().trim()).subscribe({
      next: () => { this.saving.set(false); this.modal.set(null); this.reload(v1); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.modalError.set(this.msg(e)); },
    });
  }

  protected removeTag(t: MvTagDefinition): void {
    if (this.busy()) { return; }
    if (!confirm(this.transloco.translate('settings.mvTags.confirmDeleteTag', { name: t.tagName }))) { return; }
    this.mutate(this.api.removeTag(this.entityType(), t.tagName), () => {
      if (this.selectedTag() === t.tagName) { this.selectedTag.set(null); }
    });
  }

  protected removeValue(value: string): void {
    const tag = this.selectedTag();
    if (!tag || this.busy()) { return; }
    if (!confirm(this.transloco.translate('settings.mvTags.confirmDeleteValue', { name: value }))) { return; }
    this.mutate(this.api.removeValue(this.entityType(), tag, value));
  }

  private mutate(req: Observable<unknown>, before?: () => void): void {
    this.busy.set(true);
    this.opError.set(null);
    req.subscribe({
      next: () => { this.busy.set(false); if (before) { before(); } this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected closeModal(): void { if (!this.saving()) { this.modal.set(null); } }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
