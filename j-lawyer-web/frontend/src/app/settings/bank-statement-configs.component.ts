import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { BankStatementConfigService, BankStatementCsvConfig } from './bank-statement-config.service';

const EMPTY: BankStatementCsvConfig = {
  configurationName: '', delimiter: ';', decimalFormat: '#,##0.00', decimalSeparator: ',',
  decimalGroupingCharacter: '.', decimalGrouping: true, locale: 'de_DE', headerLines: 1, footerLines: 0,
  columnDate: 0, columnName: 1, columnBookingType: 2, columnIban: 3, columnPurpose: 4, columnAmount: 5, columnCurrency: 6,
};

interface ColField { key: keyof BankStatementCsvConfig; labelKey: string; }
const COLS: ColField[] = [
  { key: 'columnDate', labelKey: 'settings.bankCsv.columnDate' },
  { key: 'columnName', labelKey: 'settings.bankCsv.columnName' },
  { key: 'columnBookingType', labelKey: 'settings.bankCsv.columnBookingType' },
  { key: 'columnIban', labelKey: 'settings.bankCsv.columnIban' },
  { key: 'columnPurpose', labelKey: 'settings.bankCsv.columnPurpose' },
  { key: 'columnAmount', labelKey: 'settings.bankCsv.columnAmount' },
  { key: 'columnCurrency', labelKey: 'settings.bankCsv.columnCurrency' },
];

/**
 * "Kontoauszug-Import" section: manages CSV bank-statement import profiles (list + create/edit modal
 * with parsing options and per-field column indices). Writes need `adminRole`.
 */
@Component({
  selector: 'jl-bank-statement-configs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.bankCsv.create' | transloco }}</span></button>
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
          @for (c of items(); track c.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(c)">
                <span class="fl-name">{{ c.configurationName }}</span>
                <span class="fl-sub">{{ 'settings.bankCsv.delimiter' | transloco }} „{{ c.delimiter }}" · {{ c.locale }}</span>
              </div>
              @if (canEdit()) {
                <button type="button" class="icon-btn" (click)="openEdit(c)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(c)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog cf-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.bankCsv.edit' : 'settings.bankCsv.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field">
            <span>{{ 'settings.bankCsv.name' | transloco }}</span>
            <input type="text" [value]="draft().configurationName" (input)="patch('configurationName', $any($event.target).value)" />
          </label>

          <fieldset class="ed-sec">
            <legend>{{ 'settings.bankCsv.parsing' | transloco }}</legend>
            <div class="ed-grid">
              <label class="ed-field"><span>{{ 'settings.bankCsv.delimiter' | transloco }}</span><input type="text" maxlength="1" [value]="draft().delimiter" (input)="patch('delimiter', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.bankCsv.locale' | transloco }}</span><input type="text" [value]="draft().locale" (input)="patch('locale', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.bankCsv.headerLines' | transloco }}</span><input type="number" min="0" [value]="draft().headerLines" (input)="patchNum('headerLines', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.bankCsv.footerLines' | transloco }}</span><input type="number" min="0" [value]="draft().footerLines" (input)="patchNum('footerLines', $any($event.target).value)" /></label>
            </div>
          </fieldset>

          <fieldset class="ed-sec">
            <legend>{{ 'settings.bankCsv.decimals' | transloco }}</legend>
            <div class="ed-grid">
              <label class="ed-field"><span>{{ 'settings.bankCsv.decimalFormat' | transloco }}</span><input type="text" [value]="draft().decimalFormat" (input)="patch('decimalFormat', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.bankCsv.decimalSeparator' | transloco }}</span><input type="text" maxlength="1" [value]="draft().decimalSeparator" (input)="patch('decimalSeparator', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.bankCsv.decimalGroupingCharacter' | transloco }}</span><input type="text" maxlength="1" [value]="draft().decimalGroupingCharacter" (input)="patch('decimalGroupingCharacter', $any($event.target).value)" /></label>
            </div>
            <label class="ed-check"><input type="checkbox" [checked]="draft().decimalGrouping" (change)="patch('decimalGrouping', $any($event.target).checked)" /><span>{{ 'settings.bankCsv.decimalGrouping' | transloco }}</span></label>
          </fieldset>

          <fieldset class="ed-sec">
            <legend>{{ 'settings.bankCsv.columns' | transloco }}</legend>
            <p class="ed-hint">{{ 'settings.bankCsv.columnsHint' | transloco }}</p>
            <div class="ed-grid">
              @for (col of cols; track col.key) {
                <label class="ed-field"><span>{{ col.labelKey | transloco }}</span><input type="number" min="0" [value]="numVal(col.key)" (input)="patchNum(col.key, $any($event.target).value)" /></label>
              }
            </div>
          </fieldset>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().configurationName.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styles: [`
    .cf-dialog { width: min(620px, calc(100vw - 32px)); }
    .ed-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 10px 12px 12px; margin: 4px 0 0; display: flex; flex-direction: column; gap: 10px; }
    .ed-sec legend { font-size: .76rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .03em; }
    .ed-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  `],
  styleUrls: ['./finance-list.css', './finance-editor.css'],
})
export class BankStatementConfigsComponent implements OnInit {
  private readonly api = inject(BankStatementConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly cols = COLS;
  protected readonly items = signal<BankStatementCsvConfig[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<BankStatementCsvConfig | null>(null);
  protected readonly draft = signal<BankStatementCsvConfig>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.configurationName.localeCompare(b.configurationName))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected numVal(key: keyof BankStatementCsvConfig): number { return this.draft()[key] as number; }
  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(c: BankStatementCsvConfig): void { this.editItem.set(c); this.draft.set({ ...c }); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof BankStatementCsvConfig>(key: K, value: BankStatementCsvConfig[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }
  protected patchNum(key: keyof BankStatementCsvConfig, value: string): void {
    const n = parseInt(value, 10);
    this.patch(key, (Number.isFinite(n) ? n : 0) as BankStatementCsvConfig[typeof key]);
  }

  protected submit(): void {
    const d = this.draft();
    if (!d.configurationName.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.update(d) : this.api.create(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(c: BankStatementCsvConfig): void {
    if (this.busy()) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.delete(c).subscribe({
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
