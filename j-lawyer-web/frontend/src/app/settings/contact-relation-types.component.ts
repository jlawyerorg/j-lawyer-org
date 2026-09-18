import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ContactRelationType } from '../contacts/contact.models';
import { ContactRelationTypeService } from './contact-relation-type.service';
import { ContactRelationTypeEditorComponent } from './contact-relation-type-editor.component';

/**
 * "Kontaktbeziehungstypen" section: lists the relationship types between contacts in their
 * configured order (inactive ones marked as such) and hosts the create/edit modal. As in the party
 * types section the order is not an editable field — the rows are drag-and-drop reorderable and a
 * drop renumbers the affected types' `sequenceNumber`. Deleting a type that relationships still use
 * is refused by the server; its message (naming how many) is shown as it comes.
 */
@Component({
  selector: 'jl-contact-relation-types',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, ContactRelationTypeEditorComponent],
  template: `
    <div class="fl">
      <div class="fl-bar">
        <button type="button" class="btn-primary" (click)="openNew()">
          <jl-icon name="plus" [size]="15" /><span>{{ 'settings.relationType.create' | transloco }}</span>
        </button>
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
          @for (t of items(); track t.id; let i = $index) {
            <li class="fl-row" draggable="true" [class.inactive]="!t.active"
                [class.dragging]="dragIndex() === i"
                [class.drop-target]="overIndex() === i && dragIndex() !== null && dragIndex() !== i"
                (dragstart)="onDragStart(i, $event)" (dragend)="onDragEnd()"
                (dragover)="onDragOver(i, $event)" (dragleave)="onDragLeave(i)" (drop)="onDrop(i, $event)">
              <span class="fl-grip" [attr.aria-label]="'settings.relationType.dragHint' | transloco" title="{{ 'settings.relationType.dragHint' | transloco }}">
                <jl-icon name="grip" [size]="15" />
              </span>
              <span class="fl-swatch" [style.background]="swatch(t)"></span>
              <div class="fl-main" (click)="openEdit(t)">
                <span class="fl-name">
                  {{ t.name }}
                  @if (!t.active) { <span class="fl-off">{{ 'settings.relationType.inactive' | transloco }}</span> }
                </span>
                <span class="fl-sub">{{ labels(t) }}</span>
              </div>
              @if (t.category) { <span class="fl-badge">{{ t.category }}</span> }
              <button type="button" class="icon-btn" [disabled]="busy()" (click)="toggleActive(t)"
                      [attr.aria-label]="(t.active ? 'settings.relationType.deactivate' : 'settings.relationType.activate') | transloco"
                      title="{{ (t.active ? 'settings.relationType.deactivate' : 'settings.relationType.activate') | transloco }}">
                <jl-icon [name]="t.active ? 'eye' : 'eye-off'" [size]="15" />
              </button>
              <button type="button" class="icon-btn" (click)="openEdit(t)" [attr.aria-label]="'settings.rename' | transloco">
                <jl-icon name="edit" [size]="15" />
              </button>
              <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(t)" [attr.aria-label]="'settings.delete' | transloco">
                <jl-icon name="trash" [size]="15" />
              </button>
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <jl-contact-relation-type-editor [relationType]="editItem()" [categories]="categories()"
                                       [saving]="saving()" [error]="saveError()"
                                       (save)="onSave($event)" (close)="editorOpen.set(false)" />
    }
  `,
  styles: [`
    .fl-grip { flex: 0 0 auto; display: inline-flex; color: var(--jl-ink-faint); cursor: grab; }
    .fl-swatch { flex: 0 0 auto; width: 14px; height: 14px; border-radius: 4px; border: 1px solid var(--jl-line-strong); }
    .fl-row { cursor: default; }
    .fl-row.dragging { opacity: .45; }
    .fl-row.drop-target { box-shadow: inset 0 2px 0 var(--jl-blue); }
    .fl-row.inactive .fl-name, .fl-row.inactive .fl-sub { opacity: .6; }
    .fl-off { margin-left: 6px; font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); }
  `],
  styleUrls: ['./finance-list.css'],
})
export class ContactRelationTypesComponent implements OnInit {
  private readonly api = inject(ContactRelationTypeService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<ContactRelationType[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly dragIndex = signal<number | null>(null);
  protected readonly overIndex = signal<number | null>(null);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<ContactRelationType | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  /** The categories already in use, offered as suggestions in the editor. */
  protected readonly categories = computed(() =>
    [...new Set(this.items().map((t) => t.category).filter(Boolean))].sort((a, b) => a.localeCompare(b)));

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list().subscribe({
      next: (t) => {
        this.items.set([...t].sort((a, b) => (a.sequenceNumber - b.sequenceNumber) || a.name.localeCompare(b.name)));
        this.loading.set(false);
      },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected swatch(t: ContactRelationType): string {
    return '#' + ((t.color & 0xffffff) >>> 0).toString(16).padStart(6, '0');
  }

  /** Both directions of the type as one line; a symmetric type reads the same both ways. */
  protected labels(t: ContactRelationType): string {
    return t.symmetric ? t.labelFrom : [t.labelFrom, t.labelTo].filter(Boolean).join('  ⇄  ');
  }

  // --- drag-and-drop reordering ---

  protected onDragStart(i: number, event: DragEvent): void {
    this.dragIndex.set(i);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      // Firefox requires data to be set for a drag to start.
      event.dataTransfer.setData('text/plain', String(i));
    }
  }

  protected onDragOver(i: number, event: DragEvent): void {
    if (this.dragIndex() === null) { return; }
    event.preventDefault();
    if (event.dataTransfer) { event.dataTransfer.dropEffect = 'move'; }
    this.overIndex.set(i);
  }

  protected onDragLeave(i: number): void {
    if (this.overIndex() === i) { this.overIndex.set(null); }
  }

  protected onDragEnd(): void {
    this.dragIndex.set(null);
    this.overIndex.set(null);
  }

  protected onDrop(i: number, event: DragEvent): void {
    event.preventDefault();
    const from = this.dragIndex();
    this.dragIndex.set(null);
    this.overIndex.set(null);
    if (from === null || from === i) { return; }
    const arr = [...this.items()];
    const [moved] = arr.splice(from, 1);
    arr.splice(i, 0, moved);
    this.persistOrder(arr);
  }

  /** Renumbers the reordered list (1-based) and persists only the types whose sequence changed. */
  private persistOrder(newOrder: ContactRelationType[]): void {
    const origSeq = new Map(this.items().map((t) => [t.id, t.sequenceNumber]));
    const renumbered = newOrder.map((t, idx) => ({ ...t, sequenceNumber: idx + 1 }));
    this.items.set(renumbered); // optimistic
    const changed = renumbered.filter((t) => origSeq.get(t.id) !== t.sequenceNumber);
    if (changed.length === 0) { return; }
    this.busy.set(true);
    this.opError.set(null);
    forkJoin(changed.map((t) => this.api.update(t))).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); this.reload(); },
    });
  }

  // --- create / edit / (de)activate / delete ---

  protected openNew(): void { this.editItem.set(null); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(t: ContactRelationType): void { this.editItem.set(t); this.saveError.set(null); this.editorOpen.set(true); }

  protected onSave(t: ContactRelationType): void {
    this.saving.set(true);
    this.saveError.set(null);
    this.opError.set(null);
    const editing = this.editItem();
    // New types go to the end of the order; existing ones keep their sequence.
    const payload = editing ? t : { ...t, sequenceNumber: this.nextSequence() };
    const req = editing ? this.api.update(payload) : this.api.create(payload);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  private nextSequence(): number {
    return this.items().reduce((max, t) => Math.max(max, t.sequenceNumber), 0) + 1;
  }

  /**
   * Activates or deactivates a type. An inactive type is no longer offered for new relationships,
   * while existing relationships keep working and keep their wording.
   */
  protected toggleActive(t: ContactRelationType): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.opError.set(null);
    this.api.update({ ...t, active: !t.active }).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected remove(t: ContactRelationType): void {
    if (this.busy()) { return; }
    if (!confirm(this.transloco.translate('settings.relationType.deleteConfirm', { name: t.name }))) { return; }
    this.busy.set(true);
    this.opError.set(null);
    this.api.delete(t).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  /**
   * The server's own refusal ("still in use", duplicate name), taken from a plain-text body or from
   * the v8 error envelope {status, error, message} whose message is prefixed by the exception type.
   */
  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    let raw = '';
    if (body && typeof body === 'object' && typeof body.message === 'string') {
      raw = body.message;
    } else if (typeof body === 'string') {
      raw = body;
    }
    const msg = raw.replace(/^\w*(?:Exception|Error):\s*/, '').trim();
    return msg || this.transloco.translate('settings.saveError');
  }
}
