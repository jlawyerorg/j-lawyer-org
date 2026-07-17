import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NgTemplateOutlet } from '@angular/common';
import { Observable } from 'rxjs';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { FolderNode, FolderTemplate, FolderTemplateService } from './folder-template.service';

interface NameModal { titleKey: string; run: (name: string) => Observable<unknown>; }
interface ConfirmModal { messageKey: string; message?: string; run: () => Observable<unknown>; }

/**
 * Editor for document-folder templates (Aktenstruktur-Vorlagen): pick a template (create / rename /
 * clone / delete), then edit its folder tree — add a sub-folder to any node, rename or remove folders.
 * The auto-created root folder can only receive children. All mutations need `adminRole` (also
 * enforced server-side); business errors surface inline.
 */
@Component({
  selector: 'jl-folder-templates',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, NgTemplateOutlet, IconComponent],
  template: `
    <div class="ft">
      @if (!canEdit()) { <p class="ft-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (opError()) { <p class="ft-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="ft-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ft-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <div class="ft-bar">
          <select class="ft-select" [disabled]="templates().length === 0"
                  (change)="selectedId.set($any($event.target).value)">
            @for (t of templates(); track t.id) {
              <option [value]="t.id" [selected]="t.id === selectedId()">{{ t.name }}</option>
            }
          </select>
          @if (canEdit()) {
            <button type="button" class="btn-sm" (click)="openNewTemplate()">{{ 'settings.folderTpl.newTemplate' | transloco }}</button>
            <button type="button" class="btn-sm" [disabled]="!selected()" (click)="openRenameTemplate()">{{ 'settings.folderTpl.renameTemplate' | transloco }}</button>
            <button type="button" class="btn-sm" [disabled]="!selected()" (click)="openCloneTemplate()">{{ 'settings.folderTpl.cloneTemplate' | transloco }}</button>
            <button type="button" class="btn-sm danger" [disabled]="!selected()" (click)="askDeleteTemplate()">{{ 'settings.delete' | transloco }}</button>
          }
        </div>

        @if (selected(); as t) {
          <div class="ft-tree">
            <ng-container *ngTemplateOutlet="nodeTpl; context: { $implicit: t.rootFolder, depth: 0 }" />
          </div>
        } @else {
          <p class="ft-muted">{{ 'settings.folderTpl.none' | transloco }}</p>
        }
      }
    </div>

    <ng-template #nodeTpl let-n let-depth="depth">
      <div class="ft-row" [style.padding-left.px]="8 + depth * 20">
        <jl-icon name="folder" [size]="15" />
        <span class="ft-name">{{ n.name }}</span>
        @if (canEdit()) {
          <span class="ft-actions">
            <button type="button" class="icon-btn" [disabled]="busy()" (click)="openAddFolder(n)" [attr.aria-label]="'settings.folderTpl.addFolder' | transloco">
              <jl-icon name="plus" [size]="14" />
            </button>
            @if (n.parentId !== null) {
              <button type="button" class="icon-btn" [disabled]="busy()" (click)="openRenameFolder(n)" [attr.aria-label]="'settings.rename' | transloco">
                <jl-icon name="edit" [size]="14" />
              </button>
              <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="askRemoveFolder(n)" [attr.aria-label]="'settings.delete' | transloco">
                <jl-icon name="trash" [size]="14" />
              </button>
            }
          </span>
        }
      </div>
      @for (c of n.children; track c.id) {
        <ng-container *ngTemplateOutlet="nodeTpl; context: { $implicit: c, depth: depth + 1 }" />
      }
    </ng-template>

    @if (nameModal(); as m) {
      <div class="ed-backdrop" (click)="closeName()"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ m.titleKey | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field">
            <span>{{ 'settings.folderTpl.name' | transloco }}</span>
            <input type="text" [value]="nameValue()" (input)="nameValue.set($any($event.target).value)"
                   (keydown.enter)="submitName()" autofocus />
          </label>
          @if (modalError()) { <p class="ed-error">{{ modalError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="closeName()">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !nameValue().trim()" (click)="submitName()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </footer>
      </div>
    }

    @if (confirmModal(); as c) {
      <div class="ed-backdrop" (click)="closeConfirm()"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ 'settings.confirmTitle' | transloco }}</h2></header>
        <div class="ed-body">
          <p class="ft-confirm">{{ c.message || (c.messageKey | transloco) }}</p>
          @if (modalError()) { <p class="ed-error">{{ modalError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="closeConfirm()">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary danger" [disabled]="saving()" (click)="submitConfirm()">
            {{ (saving() ? 'settings.saving' : 'settings.delete') | transloco }}
          </button>
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .ft { max-width: 640px; }
    .ft-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .ft-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ft-error { color: var(--jl-red); font-size: .84rem; margin: 0 0 10px; }
    .ft-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 14px; }
    .ft-select { font: inherit; font-size: .9rem; padding: 7px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); min-width: 180px; }
    .ft-tree { border: 1px solid var(--jl-line); border-radius: 10px; padding: 6px 0; }
    .ft-row { display: flex; align-items: center; gap: 8px; padding: 5px 10px; min-height: 30px; }
    .ft-row:hover { background: var(--jl-surface-alt); }
    .ft-row:hover .ft-actions { opacity: 1; }
    .ft-name { flex: 1 1 auto; font-size: .9rem; color: var(--jl-ink); }
    .ft-actions { display: inline-flex; gap: 2px; opacity: 0; transition: opacity .1s; }
    .ft-confirm { font-size: .9rem; color: var(--jl-ink); margin: 0; }
    .icon-btn.danger { color: var(--jl-red); }
    .btn-sm { font: inherit; font-size: .82rem; padding: 6px 12px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-sm:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .btn-sm:disabled { opacity: .5; cursor: default; }
    .btn-sm.danger { color: var(--jl-red); }
    .btn-sm.danger:hover:not(:disabled) { border-color: var(--jl-red); color: var(--jl-red); }
    .btn-primary.danger { background: var(--jl-red); }
  `],
  styleUrls: ['./finance-editor.css'],
})
export class FolderTemplatesComponent implements OnInit {
  private readonly api = inject(FolderTemplateService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly templates = signal<FolderTemplate[]>([]);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = computed(() => this.templates().find((t) => t.id === this.selectedId()) ?? null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly nameModal = signal<NameModal | null>(null);
  protected readonly confirmModal = signal<ConfirmModal | null>(null);
  protected readonly nameValue = signal('');
  protected readonly modalError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void { this.reload(); }

  private reload(keepId?: string): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list().subscribe({
      next: (list) => {
        const sorted = [...list].sort((a, b) => a.name.localeCompare(b.name));
        this.templates.set(sorted);
        const want = keepId ?? this.selectedId();
        this.selectedId.set(sorted.some((t) => t.id === want) ? want! : (sorted[0]?.id ?? null));
        this.loading.set(false);
      },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  // --- template-level actions ---

  protected openNewTemplate(): void {
    this.nameValue.set('');
    this.modalError.set(null);
    this.nameModal.set({ titleKey: 'settings.folderTpl.newTemplate', run: (name) => this.api.createTemplate(name) });
  }

  protected openRenameTemplate(): void {
    const t = this.selected();
    if (!t) { return; }
    this.nameValue.set(t.name);
    this.modalError.set(null);
    this.nameModal.set({ titleKey: 'settings.folderTpl.renameTemplate', run: (name) => this.api.renameTemplate(t.id, name) });
  }

  protected openCloneTemplate(): void {
    const t = this.selected();
    if (!t) { return; }
    this.nameValue.set(`${t.name} (Kopie)`);
    this.modalError.set(null);
    this.nameModal.set({ titleKey: 'settings.folderTpl.cloneTemplate', run: (name) => this.api.cloneTemplate(t.name, name) });
  }

  protected askDeleteTemplate(): void {
    const t = this.selected();
    if (!t) { return; }
    this.modalError.set(null);
    this.confirmModal.set({
      messageKey: '', message: this.transloco.translate('settings.folderTpl.confirmDeleteTemplate', { name: t.name }),
      run: () => this.api.deleteTemplate(t.name),
    });
  }

  // --- folder-level actions ---

  protected openAddFolder(parent: FolderNode): void {
    const t = this.selected();
    if (!t) { return; }
    this.nameValue.set('');
    this.modalError.set(null);
    this.nameModal.set({ titleKey: 'settings.folderTpl.addFolder', run: (name) => this.api.addFolder(t.name, parent.id, name) });
  }

  protected openRenameFolder(node: FolderNode): void {
    this.nameValue.set(node.name);
    this.modalError.set(null);
    this.nameModal.set({ titleKey: 'settings.folderTpl.renameFolder', run: (name) => this.api.renameFolder(node.id, name) });
  }

  protected askRemoveFolder(node: FolderNode): void {
    this.modalError.set(null);
    this.confirmModal.set({
      messageKey: '', message: this.transloco.translate('settings.folderTpl.confirmDeleteFolder', { name: node.name }),
      run: () => this.api.removeFolder(node.id),
    });
  }

  // --- modal plumbing ---

  protected submitName(): void {
    const m = this.nameModal();
    const v = this.nameValue().trim();
    if (!m || !v || this.saving()) { return; }
    this.saving.set(true);
    this.modalError.set(null);
    this.opError.set(null);
    m.run(v).subscribe({
      next: () => { this.saving.set(false); this.nameModal.set(null); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.modalError.set(this.msg(e)); },
    });
  }

  protected submitConfirm(): void {
    const c = this.confirmModal();
    if (!c || this.saving()) { return; }
    this.saving.set(true);
    this.modalError.set(null);
    this.opError.set(null);
    c.run().subscribe({
      next: () => { this.saving.set(false); this.confirmModal.set(null); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.modalError.set(this.msg(e)); },
    });
  }

  protected closeName(): void { if (!this.saving()) { this.nameModal.set(null); } }
  protected closeConfirm(): void { if (!this.saving()) { this.confirmModal.set(null); } }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
