import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { ScanSettings, SystemSettingsService } from './system-settings.service';

/**
 * Editor for the server scan / OCR settings: the observed server directory and the OCR command line.
 * Editing needs `sysAdminRole` (also enforced server-side; the System screen is already gated).
 */
@Component({
  selector: 'jl-scan-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="ss">
      @if (!canEdit()) { <p class="ss-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="ss-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ss-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <label class="ss-field">
          <span>{{ 'settings.scan.serverDirectory' | transloco }}</span>
          <input type="text" [value]="draft().serverDirectory" [disabled]="!canEdit() || saving()"
                 (input)="patch('serverDirectory', $any($event.target).value)" />
        </label>
        <label class="ss-field">
          <span>{{ 'settings.scan.ocrCommand' | transloco }}</span>
          <input type="text" [value]="draft().ocrCommand" [disabled]="!canEdit() || saving()"
                 (input)="patch('ocrCommand', $any($event.target).value)" />
        </label>
        <div class="ss-foot">
          @if (savedOk()) { <span class="ss-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ss-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ss { max-width: 640px; display: flex; flex-direction: column; gap: 14px; }
    .ss-hint { margin: 0; font-size: .82rem; color: #b26a00; }
    .ss-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ss-error { color: var(--jl-red); font-size: .84rem; }
    .ss-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .ss-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .ss-field input:focus { border-color: var(--jl-blue); }
    .ss-field input:disabled { opacity: .6; }
    .ss-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .ss-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class ScanSettingsComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<ScanSettings>({ serverDirectory: '', ocrCommand: '' });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getScan().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch(key: keyof ScanSettings, value: string): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.saveScan(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
