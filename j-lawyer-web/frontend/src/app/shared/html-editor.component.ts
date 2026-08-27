import {
  ChangeDetectionStrategy, Component, DestroyRef, effect, ElementRef, inject, input, signal,
  ViewEncapsulation, viewChild,
} from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoModule } from '@jsverse/transloco';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Link from '@tiptap/extension-link';
import TextAlign from '@tiptap/extension-text-align';
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableHeader } from '@tiptap/extension-table-header';
import { TableCell } from '@tiptap/extension-table-cell';
import Image from '@tiptap/extension-image';
import TextStyle from '@tiptap/extension-text-style';
import Color from '@tiptap/extension-color';
import FontFamily from '@tiptap/extension-font-family';
import Highlight from '@tiptap/extension-highlight';
import { DocumentContentService } from './document-content.service';
import { base64ToBytes, bytesToText, textToBase64 } from './document-preview.models';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

/**
 * Rich-text (WYSIWYG) editor for HTML case documents, backed by TipTap/ProseMirror. Loads the
 * document's HTML, lets the user edit it with formatting, tables, links and undo/redo, and
 * auto-saves (debounced) via {@link DocumentContentService.updateContent} — which writes a
 * document-history entry server-side. The saved output is wrapped in a minimal, well-formed HTML
 * document (UTF-8). Used embedded in the document preview and as a full-page pop-out.
 */
@Component({
  selector: 'jl-html-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [TranslocoModule],
  template: `
    <div class="jl-html-editor" [class.embedded]="embedded()">
      @if (loading()) {
        <p class="he-msg">{{ 'editor.loading' | transloco }}</p>
      } @else if (error()) {
        <p class="he-msg">{{ 'editor.loadError' | transloco }}</p>
      }
      <div class="he-toolbar" [hidden]="loading() || error()" role="toolbar">
        <button type="button" class="he-b" (click)="cmd('undo')" [title]="'editor.undo' | transloco">↶</button>
        <button type="button" class="he-b" (click)="cmd('redo')" [title]="'editor.redo' | transloco">↷</button>
        <span class="he-sep"></span>
        <button type="button" class="he-b bold" [class.on]="isActive('bold')" (click)="cmd('bold')" [title]="'editor.bold' | transloco">B</button>
        <button type="button" class="he-b ital" [class.on]="isActive('italic')" (click)="cmd('italic')" [title]="'editor.italic' | transloco">I</button>
        <button type="button" class="he-b undl" [class.on]="isActive('underline')" (click)="cmd('underline')" [title]="'editor.underline' | transloco">U</button>
        <button type="button" class="he-b strk" [class.on]="isActive('strike')" (click)="cmd('strike')" [title]="'editor.strike' | transloco">S</button>
        <span class="he-sep"></span>
        <button type="button" class="he-b" [class.on]="isActiveHeading(1)" (click)="heading(1)" [title]="'editor.h1' | transloco">H1</button>
        <button type="button" class="he-b" [class.on]="isActiveHeading(2)" (click)="heading(2)" [title]="'editor.h2' | transloco">H2</button>
        <button type="button" class="he-b" [class.on]="isActiveHeading(3)" (click)="heading(3)" [title]="'editor.h3' | transloco">H3</button>
        <button type="button" class="he-b" [class.on]="isActive('paragraph')" (click)="cmd('paragraph')" [title]="'editor.paragraph' | transloco">¶</button>
        <span class="he-sep"></span>
        <button type="button" class="he-b" [class.on]="isActive('bulletList')" (click)="cmd('bulletList')" [title]="'editor.bulletList' | transloco">•</button>
        <button type="button" class="he-b" [class.on]="isActive('orderedList')" (click)="cmd('orderedList')" [title]="'editor.orderedList' | transloco">1.</button>
        <button type="button" class="he-b" [class.on]="isActive('link')" (click)="setLink()" [title]="'editor.link' | transloco">🔗</button>
        <span class="he-sep"></span>
        <button type="button" class="he-b" [class.on]="isAlign('left')" (click)="align('left')" [title]="'editor.alignLeft' | transloco">⯇</button>
        <button type="button" class="he-b" [class.on]="isAlign('center')" (click)="align('center')" [title]="'editor.alignCenter' | transloco">≡</button>
        <button type="button" class="he-b" [class.on]="isAlign('right')" (click)="align('right')" [title]="'editor.alignRight' | transloco">⯈</button>
        <span class="he-sep"></span>
        <select class="he-sel" [title]="'editor.fontFamily' | transloco" [value]="currentFont()" (change)="setFont($event)">
          <option value="">{{ 'editor.fontDefault' | transloco }}</option>
          @for (f of fonts; track f.css) { <option [value]="f.css">{{ f.label }}</option> }
        </select>
        <label class="he-color" [title]="'editor.textColor' | transloco">
          <span class="cg">A</span>
          <input type="color" [value]="currentColor()" (input)="setColor($event)" />
        </label>
        <label class="he-color" [title]="'editor.highlight' | transloco">
          <span class="cg hl">A</span>
          <input type="color" [value]="currentHighlight()" (input)="setHighlight($event)" />
        </label>
        <button type="button" class="he-b" (click)="clearFormatting()" [title]="'editor.clearFormat' | transloco">⌫</button>
        <span class="he-sep"></span>
        <button type="button" class="he-b" (click)="insertTable()" [title]="'editor.table' | transloco">▦</button>
        <span class="he-spacer"></span>
        <span class="he-status" [class.err]="saveState() === 'error'">
          @switch (saveState()) {
            @case ('saving') { {{ 'editor.saving' | transloco }} }
            @case ('saved') { ✓ {{ 'editor.saved' | transloco }} }
            @case ('error') { {{ 'editor.saveError' | transloco }} }
          }
        </span>
      </div>
      <div class="he-host" #host [hidden]="loading() || error()"></div>
    </div>
  `,
  styles: [`
    .jl-html-editor { display: flex; flex-direction: column; height: 100%; min-height: 0; background: var(--jl-surface); }
    .jl-html-editor .he-msg { padding: 28px; margin: auto; color: var(--jl-ink-soft); }
    .jl-html-editor .he-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 3px; padding: 6px 8px;
      border-bottom: 1px solid var(--jl-line); background: var(--jl-surface-alt); flex: none; }
    .jl-html-editor .he-b { min-width: 28px; height: 28px; padding: 0 6px; display: inline-flex; align-items: center;
      justify-content: center; font: inherit; font-size: .82rem; line-height: 1; cursor: pointer; color: var(--jl-ink);
      background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 6px; }
    .jl-html-editor .he-b:hover { border-color: var(--jl-blue); color: var(--jl-blue); }
    .jl-html-editor .he-b.on { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    .jl-html-editor .he-b.bold { font-weight: 800; }
    .jl-html-editor .he-b.ital { font-style: italic; }
    .jl-html-editor .he-b.undl { text-decoration: underline; }
    .jl-html-editor .he-b.strk { text-decoration: line-through; }
    .jl-html-editor .he-sel { height: 28px; max-width: 140px; font: inherit; font-size: .8rem; color: var(--jl-ink);
      background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 6px; padding: 0 4px; cursor: pointer; }
    .jl-html-editor .he-sel:hover { border-color: var(--jl-blue); }
    .jl-html-editor .he-color { display: inline-flex; align-items: center; justify-content: center; position: relative;
      width: 28px; height: 28px; border: 1px solid var(--jl-line-strong); border-radius: 6px; cursor: pointer; background: var(--jl-surface); }
    .jl-html-editor .he-color:hover { border-color: var(--jl-blue); }
    .jl-html-editor .he-color .cg { font-size: .82rem; font-weight: 700; color: var(--jl-ink); line-height: 1; pointer-events: none; }
    .jl-html-editor .he-color .cg.hl { background: #ffe066; padding: 0 2px; border-radius: 2px; }
    .jl-html-editor .he-color input[type=color] { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; border: 0; padding: 0; }
    .jl-html-editor .he-sep { width: 1px; height: 20px; background: var(--jl-line); margin: 0 3px; }
    .jl-html-editor .he-spacer { flex: 1; }
    .jl-html-editor .he-status { font-size: .76rem; color: var(--jl-ink-soft); padding-right: 6px; white-space: nowrap; }
    .jl-html-editor .he-status.err { color: var(--jl-red); }
    .jl-html-editor .he-host { flex: 1 1 auto; min-height: 0; overflow: auto; background: #fff; }
    .jl-html-editor .he-host .ProseMirror { min-height: 100%; padding: 18px 22px; outline: none; color: #1a1a1a;
      font-size: .92rem; line-height: 1.55; }
    .jl-html-editor .he-host .ProseMirror:focus { outline: none; }
    .jl-html-editor .he-host .ProseMirror p { margin: 0 0 .6em; }
    .jl-html-editor .he-host .ProseMirror h1 { font-size: 1.5rem; margin: .6em 0 .3em; }
    .jl-html-editor .he-host .ProseMirror h2 { font-size: 1.25rem; margin: .6em 0 .3em; }
    .jl-html-editor .he-host .ProseMirror h3 { font-size: 1.08rem; margin: .6em 0 .3em; }
    .jl-html-editor .he-host .ProseMirror ul, .jl-html-editor .he-host .ProseMirror ol { padding-left: 1.4em; margin: 0 0 .6em; }
    .jl-html-editor .he-host .ProseMirror a { color: #1560d0; text-decoration: underline; }
    .jl-html-editor .he-host .ProseMirror img { max-width: 100%; height: auto; }
    .jl-html-editor .he-host .ProseMirror table { border-collapse: collapse; margin: .4em 0; width: auto; }
    .jl-html-editor .he-host .ProseMirror td, .jl-html-editor .he-host .ProseMirror th {
      border: 1px solid #c7ccd1; padding: 5px 8px; min-width: 3em; vertical-align: top; }
    .jl-html-editor .he-host .ProseMirror th { background: #eef1f4; font-weight: 700; }
  `],
})
export class HtmlEditorComponent {
  /** The case document id to load and (auto-)save. */
  readonly documentId = input.required<string>();
  /** When true, styling adapts to an in-flow side-panel (vs full-page pop-out). */
  readonly embedded = input(false);

  private readonly content = inject(DocumentContentService);
  private readonly host = viewChild<ElementRef<HTMLDivElement>>('host');

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly saveState = signal<SaveState>('idle');
  /** Bumped on every editor transaction so the OnPush toolbar re-evaluates active states. */
  protected readonly tick = signal(0);

  private editor: Editor | null = null;
  /** The loaded HTML, held until the host element exists so the editor mounts with content. */
  private readonly contentHtml = signal<string | null>(null);
  private dirty = false;
  private readonly saveTrigger = new Subject<void>();

  constructor() {
    this.saveTrigger.pipe(debounceTime(1500), takeUntilDestroyed()).subscribe(() => this.doSave());

    // (Re)load whenever the document id changes.
    effect(() => {
      const id = this.documentId();
      this.destroyEditor();
      this.contentHtml.set(null);
      this.dirty = false;
      this.saveState.set('idle');
      this.error.set(false);
      this.loading.set(true);
      this.load(id);
    });

    // Mount the editor once both the host element and the loaded content are available.
    effect(() => {
      const el = this.host()?.nativeElement;
      const html = this.contentHtml();
      if (el && html != null && !this.editor) {
        this.createEditor(el, html);
      }
    });

    inject(DestroyRef).onDestroy(() => {
      this.flush();
      this.destroyEditor();
    });
  }

  private load(id: string): void {
    this.content.content(id, 'case').subscribe({
      next: (dto) => {
        if (id !== this.documentId()) {
          return; // a newer document was requested meanwhile
        }
        const html = bytesToText(base64ToBytes((dto.base64content ?? '').replace(/\s/g, '')));
        this.contentHtml.set(html);
        this.loading.set(false);
      },
      error: () => {
        if (id === this.documentId()) {
          this.error.set(true);
          this.loading.set(false);
        }
      },
    });
  }

  private createEditor(el: HTMLElement, html: string): void {
    this.editor = new Editor({
      element: el,
      extensions: [
        StarterKit,
        Underline,
        Link.configure({ openOnClick: false, autolink: true }),
        TextAlign.configure({ types: ['heading', 'paragraph'] }),
        Table.configure({ resizable: true }),
        TableRow,
        TableHeader,
        TableCell,
        Image.configure({ allowBase64: true }),
        TextStyle,
        Color,
        FontFamily,
        Highlight.configure({ multicolor: true }),
      ],
      content: html,
      onUpdate: () => {
        this.dirty = true;
        this.saveState.set('idle');
        this.saveTrigger.next();
        this.tick.update((v) => v + 1);
      },
      onSelectionUpdate: () => this.tick.update((v) => v + 1),
      onTransaction: () => this.tick.update((v) => v + 1),
    });
  }

  private doSave(): void {
    if (!this.editor || !this.dirty) {
      return;
    }
    this.saveState.set('saving');
    this.dirty = false;
    const full = this.wrap(this.editor.getHTML());
    this.content.updateContent(this.documentId(), textToBase64(full)).subscribe({
      next: () => {
        // Only report "saved" if no further edits arrived while the request was in flight.
        this.saveState.set(this.dirty ? 'idle' : 'saved');
      },
      error: () => {
        this.dirty = true; // keep the change pending for the next save
        this.saveState.set('error');
      },
    });
  }

  /** Fire a final save on teardown (pop-out closed / preview switched) if there are unsaved edits. */
  private flush(): void {
    if (this.editor && this.dirty) {
      const full = this.wrap(this.editor.getHTML());
      this.content.updateContent(this.documentId(), textToBase64(full)).subscribe({ error: () => {} });
      this.dirty = false;
    }
  }

  /** Wraps the editor's body HTML in a minimal, well-formed UTF-8 HTML document. */
  private wrap(body: string): string {
    return `<!DOCTYPE html>\n<html>\n<head>\n<meta charset="utf-8">\n</head>\n<body>\n${body}\n</body>\n</html>\n`;
  }

  private destroyEditor(): void {
    this.editor?.destroy();
    this.editor = null;
  }

  // --- toolbar -----------------------------------------------------------------------------

  protected cmd(name: 'undo' | 'redo' | 'bold' | 'italic' | 'underline' | 'strike'
    | 'paragraph' | 'bulletList' | 'orderedList'): void {
    const c = this.editor?.chain().focus();
    if (!c) {
      return;
    }
    switch (name) {
      case 'undo': c.undo().run(); break;
      case 'redo': c.redo().run(); break;
      case 'bold': c.toggleBold().run(); break;
      case 'italic': c.toggleItalic().run(); break;
      case 'underline': c.toggleUnderline().run(); break;
      case 'strike': c.toggleStrike().run(); break;
      case 'paragraph': c.setParagraph().run(); break;
      case 'bulletList': c.toggleBulletList().run(); break;
      case 'orderedList': c.toggleOrderedList().run(); break;
    }
  }

  protected heading(level: 1 | 2 | 3): void {
    this.editor?.chain().focus().toggleHeading({ level }).run();
  }

  protected align(dir: 'left' | 'center' | 'right'): void {
    this.editor?.chain().focus().setTextAlign(dir).run();
  }

  protected insertTable(): void {
    this.editor?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
  }

  /** Selectable font families (label shown in the dropdown, css applied via the FontFamily mark). */
  protected readonly fonts: ReadonlyArray<{ label: string; css: string }> = [
    { label: 'Arial', css: 'Arial, sans-serif' },
    { label: 'Verdana', css: 'Verdana, sans-serif' },
    { label: 'Trebuchet MS', css: '"Trebuchet MS", sans-serif' },
    { label: 'Times New Roman', css: '"Times New Roman", serif' },
    { label: 'Georgia', css: 'Georgia, serif' },
    { label: 'Courier New', css: '"Courier New", monospace' },
  ];

  protected setFont(event: Event): void {
    const css = (event.target as HTMLSelectElement).value;
    const c = this.editor?.chain().focus();
    if (!c) {
      return;
    }
    if (css === '') {
      c.unsetFontFamily().run();
    } else {
      c.setFontFamily(css).run();
    }
  }

  protected setColor(event: Event): void {
    this.editor?.chain().focus().setColor((event.target as HTMLInputElement).value).run();
  }

  protected setHighlight(event: Event): void {
    this.editor?.chain().focus().setHighlight({ color: (event.target as HTMLInputElement).value }).run();
  }

  /** Removes font family, text colour and highlight from the current selection. */
  protected clearFormatting(): void {
    this.editor?.chain().focus().unsetColor().unsetHighlight().unsetFontFamily().run();
  }

  protected currentFont(): string {
    this.tick();
    return (this.editor?.getAttributes('textStyle')['fontFamily'] as string) ?? '';
  }

  protected currentColor(): string {
    this.tick();
    return (this.editor?.getAttributes('textStyle')['color'] as string) ?? '#1a1a1a';
  }

  protected currentHighlight(): string {
    this.tick();
    return (this.editor?.getAttributes('highlight')['color'] as string) ?? '#ffe066';
  }

  protected setLink(): void {
    if (!this.editor) {
      return;
    }
    const prev = (this.editor.getAttributes('link')['href'] as string) ?? '';
    const url = window.prompt('URL', prev);
    if (url === null) {
      return; // cancelled
    }
    if (url.trim() === '') {
      this.editor.chain().focus().extendMarkRange('link').unsetLink().run();
      return;
    }
    this.editor.chain().focus().extendMarkRange('link').setLink({ href: url.trim() }).run();
  }

  protected isActive(name: string): boolean {
    this.tick();
    return this.editor?.isActive(name) ?? false;
  }

  protected isActiveHeading(level: number): boolean {
    this.tick();
    return this.editor?.isActive('heading', { level }) ?? false;
  }

  protected isAlign(dir: string): boolean {
    this.tick();
    return this.editor?.isActive({ textAlign: dir }) ?? false;
  }
}
