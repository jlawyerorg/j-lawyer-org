import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { TimesheetConfigService, TimesheetPositionTemplate } from './timesheet-config.service';

const EMPTY: TimesheetPositionTemplate = { name: '', description: '', unitPrice: 0, taxRate: 19 };

/**
 * "Zeiterfassungspositionen" section: manages the global pool of timesheet position templates
 * (list + create/edit modal). Writes need `adminRole`.
 */
@Component({
  selector: 'jl-timesheet-templates',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.tsPos.create' | transloco }}</span></button>
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
                <span class="fl-name">{{ t.name }}</span>
                @if (t.description) { <span class="fl-sub">{{ t.description }}</span> }
              </div>
              <span class="fl-badge">{{ t.unitPrice }} · {{ t.taxRate }} %</span>
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
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.tsPos.edit' : 'settings.tsPos.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field"><span>{{ 'settings.tsPos.name' | transloco }}</span><input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" /></label>
          <label class="ed-field"><span>{{ 'settings.tsPos.description' | transloco }}</span><textarea rows="2" [value]="draft().description" (input)="patch('description', $any($event.target).value)"></textarea></label>
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.tsPos.unitPrice' | transloco }}</span><input type="number" step="0.01" [value]="draft().unitPrice" (input)="patchNum('unitPrice', $any($event.target).value)" /></label>
            <label class="ed-field"><span>{{ 'settings.tsPos.taxRate' | transloco }}</span><input type="number" step="0.1" [value]="draft().taxRate" (input)="patchNum('taxRate', $any($event.target).value)" /></label>
          </div>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().name.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styleUrls: ['./finance-list.css', './finance-editor.css'],
})
export class TimesheetTemplatesComponent implements OnInit {
  private readonly api = inject(TimesheetConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<TimesheetPositionTemplate[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<TimesheetPositionTemplate | null>(null);
  protected readonly draft = signal<TimesheetPositionTemplate>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listTemplates().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.name.localeCompare(b.name))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(t: TimesheetPositionTemplate): void { this.editItem.set(t); this.draft.set({ ...t }); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof TimesheetPositionTemplate>(key: K, value: TimesheetPositionTemplate[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }
  protected patchNum(key: 'unitPrice' | 'taxRate', value: string): void { const n = parseFloat(value); this.patch(key, Number.isFinite(n) ? n : 0); }

  protected submit(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.updateTemplate(d) : this.api.createTemplate(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(t: TimesheetPositionTemplate): void {
    if (this.busy() || !t.id) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.deleteTemplate(t.id).subscribe({
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
