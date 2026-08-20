import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { TimesheetConfigService, TimesheetSettings } from './timesheet-config.service';

/**
 * Global time-tracking settings: whether to warn when starting a parallel recording, and how a bare
 * number is interpreted during manual booking (minutes / hours / reject). Needs `adminRole`.
 */
@Component({
  selector: 'jl-timesheet-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="ts">
      @if (!canEdit()) { <p class="ts-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="ts-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ts-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="ts-sec">
          <legend>{{ 'settings.tsSettings.parallelTitle' | transloco }}</legend>
          <label class="ts-check">
            <input type="checkbox" [checked]="draft().parallelLogsWarning" [disabled]="!canEdit()"
                   (change)="patchParallel($any($event.target).checked)" />
            <span>{{ 'settings.tsSettings.parallelWarning' | transloco }}</span>
          </label>
          <p class="ts-note">{{ 'settings.tsSettings.parallelHint' | transloco }}</p>
        </fieldset>

        <fieldset class="ts-sec">
          <legend>{{ 'settings.tsSettings.inputTitle' | transloco }}</legend>
          <p class="ts-note">{{ 'settings.tsSettings.inputHint' | transloco }}</p>
          @for (m of modes; track m) {
            <label class="ts-radio">
              <input type="radio" name="numericInput" [value]="m" [checked]="draft().numericInput === m" [disabled]="!canEdit()"
                     (change)="patchInput(m)" />
              <span>{{ 'settings.tsSettings.mode.' + m | transloco }}</span>
            </label>
          }
        </fieldset>

        <div class="ts-foot">
          @if (savedOk()) { <span class="ts-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ts-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ts { max-width: 560px; }
    .ts-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .ts-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ts-error { color: var(--jl-red); font-size: .84rem; }
    .ts-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .ts-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px; display: flex; flex-direction: column; gap: 8px; }
    .ts-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .ts-check, .ts-radio { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .ts-check input, .ts-radio input { width: 16px; height: 16px; }
    .ts-note { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .ts-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class TimesheetSettingsComponent implements OnInit {
  private readonly api = inject(TimesheetConfigService);
  private readonly auth = inject(AuthService);

  protected readonly modes: TimesheetSettings['numericInput'][] = ['minutes', 'hours', 'reject'];
  protected readonly draft = signal<TimesheetSettings>({ parallelLogsWarning: false, numericInput: 'minutes' });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getSettings().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patchParallel(v: boolean): void { this.draft.update((d) => ({ ...d, parallelLogsWarning: v })); this.savedOk.set(false); }
  protected patchInput(m: TimesheetSettings['numericInput']): void { this.draft.update((d) => ({ ...d, numericInput: m })); this.savedOk.set(false); }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(false); this.savedOk.set(false);
    this.api.saveSettings(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
