import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { AssistantConfigService, AssistantReplacement } from './assistant-config.service';

const EMPTY: AssistantReplacement = { searchString: '', replaceWith: '', caseInsensitive: false };

/**
 * "Automatische Ersetzungen" section: text replacements applied to transcription / dictation output
 * (Assistent Ingo). Any logged-in user may create/edit; deleting needs `adminRole` (enforced server-side).
 */
@Component({
  selector: 'jl-assistant-replacements',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      <div class="fl-bar">
        <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.aiRepl.create' | transloco }}</span></button>
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
          @for (r of items(); track r.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(r)">
                <span class="fl-name">{{ r.searchString }} → {{ r.replaceWith }}</span>
                @if (r.caseInsensitive) { <span class="fl-sub">{{ 'settings.aiRepl.caseInsensitive' | transloco }}</span> }
              </div>
              <button type="button" class="icon-btn" (click)="openEdit(r)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
              @if (canDelete()) {
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(r)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.aiRepl.edit' : 'settings.aiRepl.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field"><span>{{ 'settings.aiRepl.search' | transloco }}</span><textarea rows="2" [value]="draft().searchString" (input)="patch('searchString', $any($event.target).value)"></textarea></label>
          <label class="ed-field"><span>{{ 'settings.aiRepl.replaceWith' | transloco }}</span><textarea rows="2" [value]="draft().replaceWith" (input)="patch('replaceWith', $any($event.target).value)"></textarea></label>
          <label class="ed-check"><input type="checkbox" [checked]="draft().caseInsensitive" (change)="patch('caseInsensitive', $any($event.target).checked)" /><span>{{ 'settings.aiRepl.caseInsensitive' | transloco }}</span></label>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().searchString.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styleUrls: ['./finance-list.css', './finance-editor.css'],
  styles: [`.ed-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); } .ed-check input { width: 16px; height: 16px; }`],
})
export class AssistantReplacementsComponent implements OnInit {
  private readonly api = inject(AssistantConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<AssistantReplacement[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<AssistantReplacement | null>(null);
  protected readonly draft = signal<AssistantReplacement>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canDelete = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listReplacements().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.searchString.localeCompare(b.searchString))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(r: AssistantReplacement): void { this.editItem.set(r); this.draft.set({ ...r }); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof AssistantReplacement>(key: K, value: AssistantReplacement[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }

  protected submit(): void {
    const d = this.draft();
    if (!d.searchString.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.updateReplacement(d) : this.api.createReplacement(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(r: AssistantReplacement): void {
    if (this.busy() || !r.id) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.deleteReplacement(r.id).subscribe({
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
