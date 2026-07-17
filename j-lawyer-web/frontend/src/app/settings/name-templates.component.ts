import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { NameTemplate, NameTemplateService } from './name-template.service';

/**
 * "Benennungsschemata" section: lists document name templates and hosts a create/edit modal with a
 * debounced live preview. Exactly one template can be the default. Writes need `adminRole`.
 */
@Component({
  selector: 'jl-name-templates',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()">
            <jl-icon name="plus" [size]="15" /><span>{{ 'settings.nameTpl.create' | transloco }}</span>
          </button>
        }
      </div>
      @if (opError()) { <p class="fl-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="fl-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="fl-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (items().length === 0) {
        <p class="fl-muted">{{ 'settings.empty' | transloco }}</p>
      } @else {
        <ul class="fl-list">
          @for (t of items(); track t.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(t)">
                <span class="fl-name">{{ t.displayName }}</span>
                <span class="fl-sub">{{ t.pattern }}</span>
              </div>
              @if (t.defaultTemplate) { <span class="fl-badge">{{ 'settings.nameTpl.default' | transloco }}</span> }
              @if (canEdit()) {
                <button type="button" class="icon-btn" (click)="openEdit(t)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(t)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.nameTpl.edit' : 'settings.nameTpl.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field">
            <span>{{ 'settings.nameTpl.name' | transloco }}</span>
            <input type="text" [value]="draft().displayName" (input)="patch('displayName', $any($event.target).value)" />
          </label>
          <label class="ed-field">
            <span>{{ 'settings.nameTpl.pattern' | transloco }}</span>
            <input type="text" [value]="draft().pattern" (input)="patch('pattern', $any($event.target).value)" />
          </label>
          <p class="ed-hint">{{ 'settings.nameTpl.patternHint' | transloco }}</p>
          <label class="ed-check">
            <input type="checkbox" [checked]="draft().defaultTemplate" (change)="patch('defaultTemplate', $any($event.target).checked)" />
            <span>{{ 'settings.nameTpl.isDefault' | transloco }}</span>
          </label>
          <div class="ed-preview">
            <span class="ed-preview-label">{{ 'settings.nameTpl.preview' | transloco }}</span>
            @if (previewItems().length) {
              <ul class="ed-preview-list">@for (s of previewItems(); track $index) { <li>{{ s }}</li> }</ul>
            } @else { <span class="ed-value">…</span> }
          </div>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().displayName.trim() || !draft().pattern.trim()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </footer>
      </div>
    }
  `,
  styles: [`
    .ed-preview-list { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 2px 14px; }
    .ed-preview-list li { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: .85rem; color: var(--jl-ink); }
  `],
  styleUrls: ['./finance-list.css', './finance-editor.css'],
})
export class NameTemplatesComponent implements OnInit {
  private readonly api = inject(NameTemplateService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<NameTemplate[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<NameTemplate | null>(null);
  protected readonly draft = signal<NameTemplate>({ displayName: '', pattern: '', defaultTemplate: false });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly previewItems = signal<string[]>([]);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  private readonly previewInput = new Subject<NameTemplate>();

  constructor() {
    this.previewInput.pipe(
      debounceTime(300),
      switchMap((t) => this.api.preview(t)),
      takeUntilDestroyed(),
    ).subscribe({
      next: (s) => this.previewItems.set(s),
      error: () => this.previewItems.set([]),
    });
  }

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list().subscribe({
      next: (t) => { this.items.set([...t].sort((a, b) => a.displayName.localeCompare(b.displayName))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void {
    this.editItem.set(null);
    this.draft.set({ displayName: '', pattern: '', defaultTemplate: false });
    this.saveError.set(null); this.previewItems.set([]);
    this.editorOpen.set(true);
  }

  protected openEdit(t: NameTemplate): void {
    this.editItem.set(t);
    this.draft.set({ ...t });
    this.saveError.set(null);
    this.editorOpen.set(true);
    this.previewInput.next(this.draft());
  }

  protected patch<K extends keyof NameTemplate>(key: K, value: NameTemplate[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.previewInput.next(this.draft());
  }

  protected submit(): void {
    const d = this.draft();
    if (!d.displayName.trim() || !d.pattern.trim() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(null);
    this.opError.set(null);
    const req = this.editItem() ? this.api.update(d) : this.api.create(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(t: NameTemplate): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.opError.set(null);
    this.api.delete(t).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
