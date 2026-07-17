import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { ASSISTANT_REQUEST_TYPES, AssistantConfigService, AssistantModel, AssistantPrompt } from './assistant-config.service';

const EMPTY: AssistantPrompt = { name: '', requestType: 'chat', prompt: '', systemPrompt: '', modelRef: '', configuration: '' };

/**
 * "Eigene Prompts" section: the custom AI assistant prompts (Assistent Ingo). Any logged-in user may
 * create/edit; deleting needs `adminRole` (enforced server-side). The per-model `configuration` is an
 * advanced opaque JSON field, round-tripped verbatim.
 */
@Component({
  selector: 'jl-assistant-prompts',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      <div class="fl-bar">
        <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.aiPrompt.create' | transloco }}</span></button>
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
          @for (p of items(); track p.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(p)">
                <span class="fl-name">{{ p.name }}</span>
                @if (p.modelRef) { <span class="fl-sub">{{ p.modelRef }}</span> }
              </div>
              <span class="fl-badge">{{ ('settings.aiPrompt.type.' + p.requestType) | transloco }}</span>
              <button type="button" class="icon-btn" (click)="openEdit(p)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
              @if (canDelete()) {
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(p)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog wide" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.aiPrompt.edit' : 'settings.aiPrompt.create') | transloco }}</h2></header>
        <div class="ed-body">
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.aiPrompt.name' | transloco }}</span><input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" /></label>
            <label class="ed-field">
              <span>{{ 'settings.aiPrompt.requestType' | transloco }}</span>
              <select [value]="draft().requestType" (change)="patch('requestType', $any($event.target).value)">
                @for (t of requestTypes; track t) { <option [value]="t">{{ ('settings.aiPrompt.type.' + t) | transloco }}</option> }
              </select>
            </label>
          </div>
          <label class="ed-field">
            <span>{{ 'settings.aiPrompt.modelRef' | transloco }}</span>
            <select [value]="draft().modelRef" (change)="patch('modelRef', $any($event.target).value)">
              <option value="">{{ 'settings.aiPrompt.modelDefault' | transloco }}</option>
              @for (m of availableModels(); track m) { <option [value]="m">{{ m }}</option> }
            </select>
          </label>
          @if (selectedModel(); as m) {
            <div class="model-meta">
              <span class="mm-badge"><jl-icon [name]="m.local ? 'desktop' : 'globe'" [size]="14" />{{ (m.local ? 'settings.aiPrompt.modelLocal' : 'settings.aiPrompt.modelRemote') | transloco }}@if (m.provider) { · {{ m.provider }} }</span>
              <span class="mm-badge" [class.on]="m.supportsTools"><jl-icon name="gear" [size]="14" />{{ (m.supportsTools ? 'settings.aiPrompt.agentic' : 'settings.aiPrompt.notAgentic') | transloco }}</span>
              <span class="mm-badge" [class.on]="m.deductTokens"><jl-icon name="euro" [size]="14" />{{ (m.deductTokens ? 'settings.aiPrompt.ingoTokens' : 'settings.aiPrompt.foreignTokens') | transloco }}</span>
              @if (m.description) { <p class="mm-desc">{{ m.description }}</p> }
            </div>
          }
          <label class="ed-field"><span>{{ 'settings.aiPrompt.prompt' | transloco }}</span><textarea rows="5" [value]="draft().prompt" (input)="patch('prompt', $any($event.target).value)"></textarea></label>
          <label class="ed-field"><span>{{ 'settings.aiPrompt.systemPrompt' | transloco }}</span><textarea rows="3" [value]="draft().systemPrompt" (input)="patch('systemPrompt', $any($event.target).value)"></textarea></label>

          <fieldset class="cfg">
            <legend>{{ 'settings.aiPrompt.parameters' | transloco }}</legend>
            @if (!draft().modelRef) {
              <p class="cfg-hint">{{ 'settings.aiPrompt.paramsNoModel' | transloco }}</p>
            } @else if (configParams().length === 0) {
              <p class="cfg-hint">{{ 'settings.aiPrompt.paramsNone' | transloco }}</p>
            } @else {
              @for (p of configParams(); track p.id) {
                <label class="ed-field">
                  <span>{{ p.description || p.id }}</span>
                  <input type="text" [value]="p.value" [attr.placeholder]="p.id" (input)="patchConfigParam(p.id, $any($event.target).value)" />
                </label>
              }
            }
          </fieldset>
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
  styles: [`
    .ed-dialog.wide { width: min(720px, calc(100vw - 32px)); }
    textarea.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .82rem; }
    .model-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: -4px 0 4px; }
    .mm-badge { display: inline-flex; align-items: center; gap: 5px; font-size: .76rem; color: var(--jl-ink-faint);
      background: var(--jl-surface-alt); border-radius: 7px; padding: 3px 8px; }
    .mm-badge.on { color: var(--jl-blue); }
    .mm-desc { flex: 1 1 100%; margin: 2px 0 0; font-size: .78rem; color: var(--jl-ink-soft); }
    .cfg { border: 1px solid var(--jl-line); border-radius: 10px; padding: 10px 12px 12px; margin: 0; display: flex; flex-direction: column; gap: 10px; }
    .cfg legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .03em; }
    .cfg-hint { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
  `],
})
export class AssistantPromptsComponent implements OnInit {
  private readonly api = inject(AssistantConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly requestTypes = ASSISTANT_REQUEST_TYPES;
  protected readonly items = signal<AssistantPrompt[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<AssistantPrompt | null>(null);
  protected readonly draft = signal<AssistantPrompt>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canDelete = computed(() => this.auth.hasRole('adminRole'));

  /** Models discovered from the reachable Ingo servers (deduplicated by name). */
  protected readonly models = signal<AssistantModel[]>([]);

  /**
   * Model names offered for the current request type: those whose server reports support for it (or
   * report no request types at all), plus the prompt's own stored model so an edit never loses it.
   */
  protected readonly availableModels = computed<string[]>(() => {
    const rt = this.draft().requestType;
    const names = new Set<string>();
    for (const m of this.models()) {
      if (m.supportedRequestTypes.length === 0 || m.supportedRequestTypes.includes(rt)) {
        names.add(m.name);
      }
    }
    const current = this.draft().modelRef;
    if (current) { names.add(current); }
    return [...names].sort((a, b) => a.localeCompare(b));
  });

  /** Metadata of the currently selected model (agentic / token type / description), if known. */
  protected readonly selectedModel = computed<AssistantModel | undefined>(() => {
    const ref = this.draft().modelRef;
    return ref ? this.models().find((m) => m.name === ref) : undefined;
  });

  /** Entered values for the model's config parameters, keyed by parameter id. */
  private readonly configValues = signal<Record<string, string>>({});

  /**
   * The configurable parameters of the selected model, each with its current value — so a new prompt
   * shows all parameters (pre-filled empty) and the user sees exactly what can be configured. When
   * the model is unknown (unreachable server) the list is empty and the stored configuration is kept.
   */
  protected readonly configParams = computed<{ id: string; description: string; value: string }[]>(() => {
    const m = this.selectedModel();
    if (!m?.configurations?.length) { return []; }
    const values = this.configValues();
    return m.configurations.map((c) => ({ id: c.id, description: c.description, value: values[c.id] ?? '' }));
  });

  ngOnInit(): void {
    this.reload();
    // best-effort model discovery; failures (unreachable servers) just leave the dropdown minimal
    this.api.getStatus().subscribe({
      next: (statuses) => {
        const byName = new Map<string, AssistantModel>();
        for (const s of statuses ?? []) {
          for (const m of s.models ?? []) { if (!byName.has(m.name)) { byName.set(m.name, m); } }
        }
        this.models.set([...byName.values()]);
      },
      error: () => { /* ignore — dropdown falls back to default + stored value */ },
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listPrompts().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.name.localeCompare(b.name))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.configValues.set({}); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(p: AssistantPrompt): void { this.editItem.set(p); this.draft.set({ ...p }); this.configValues.set(this.parseProps(p.configuration)); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof AssistantPrompt>(key: K, value: AssistantPrompt[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }

  /** Updates one config parameter and re-serializes all non-empty values into the prompt's configuration. */
  protected patchConfigParam(id: string, value: string): void {
    this.configValues.update((v) => ({ ...v, [id]: value }));
    this.patch('configuration', this.serializeProps(this.configValues()));
  }

  /** Parses a Java-Properties string (key=value lines) into a map, ignoring comments. */
  private parseProps(s: string | undefined): Record<string, string> {
    const map: Record<string, string> = {};
    for (const line of (s ?? '').split(/\r?\n/)) {
      const t = line.trim();
      if (!t || t.startsWith('#') || t.startsWith('!')) { continue; }
      const m = /^([^=:]+)[=:](.*)$/.exec(t);
      if (m) { map[m[1].trim()] = m[2].trim(); } else { map[t] = ''; }
    }
    return map;
  }

  /** Serializes non-empty values back into Java-Properties key=value lines. */
  private serializeProps(map: Record<string, string>): string {
    return Object.entries(map).filter(([k, v]) => k && v !== '').map(([k, v]) => `${k}=${v}`).join('\n');
  }

  protected submit(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.updatePrompt(d) : this.api.createPrompt(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(p: AssistantPrompt): void {
    if (this.busy() || !p.id) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.deletePrompt(p.id).subscribe({
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
