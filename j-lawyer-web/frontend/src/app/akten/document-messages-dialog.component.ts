import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { CaseDocument, docDisplayTitle, DocMessage } from './case.models';
import { CasesService } from './cases.service';

/**
 * Shows the instant messages linked to one document (newest first) with a compose box on top,
 * like the desktop "Nachrichten" tab. A new message is linked to the case and the document.
 * Emits `sent` after a message was posted so the parent can refresh the message counter.
 */
@Component({
  selector: 'jl-document-messages-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="closed.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <jl-icon name="message" [size]="16" />
        <h2 [title]="doc().name">{{ 'akten.docs.msgs.title' | transloco: { name: displayTitle() } }}</h2>
        <button type="button" class="x" (click)="closed.emit()" [attr.aria-label]="'akten.docs.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>
      <div class="db">
        <form class="composer" (submit)="$event.preventDefault(); send()">
          <input type="text" [value]="draft()" (input)="draft.set($any($event.target).value)"
                 [placeholder]="'akten.msg.placeholder' | transloco" [disabled]="sending()" />
          <button type="submit" class="send-btn" [disabled]="!draft().trim() || sending()">
            <jl-icon name="send" [size]="14" /> {{ 'akten.msg.send' | transloco }}
          </button>
        </form>
        @if (messages() === null) {
          <p class="muted">{{ 'akten.loading' | transloco }}</p>
        } @else {
          <div class="chat">
            @for (m of messages(); track m.id) {
              <div class="bubble" [class.mine]="m.sender === currentUser()">
                <span class="bubble-meta">{{ m.sender }} · {{ m.sent | date: 'dd.MM.yyyy HH:mm' }}</span>
                <span class="bubble-text">{{ m.content }}</span>
              </div>
            } @empty {
              <p class="muted">{{ 'akten.docs.msgs.none' | transloco }}</p>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .backdrop { position: fixed; inset: 0; z-index: 60; background: rgba(6,14,22,.5); }
    .dialog { position: fixed; z-index: 61; top: 50%; left: 50%; transform: translate(-50%,-50%); width: min(520px, 94vw); max-height: 86dvh;
      display: flex; flex-direction: column; background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { display: flex; align-items: center; gap: 10px; padding: 13px 16px; border-bottom: 1px solid var(--jl-line); color: var(--jl-ink); }
    .dh h2 { margin: 0; font-size: 1rem; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .x { margin-left: auto; flex: 0 0 auto; display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { padding: 14px 16px; overflow-y: auto; }
    .composer { display: flex; gap: 8px; margin-bottom: 12px; }
    .composer input { flex: 1; min-width: 0; font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); }
    .composer input:focus { outline: none; border-color: var(--jl-blue); }
    .send-btn { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 13px; border-radius: 8px;
      border: 1px solid var(--jl-blue); background: var(--jl-blue); color: #fff; cursor: pointer; }
    .send-btn:disabled { opacity: .5; cursor: default; }
    .chat { display: flex; flex-direction: column; gap: 8px; }
    .bubble { max-width: 82%; align-self: flex-start; display: flex; flex-direction: column; gap: 3px; padding: 8px 11px;
      background: var(--jl-surface-alt); border: 1px solid var(--jl-line); border-radius: 12px 12px 12px 4px; }
    .bubble.mine { align-self: flex-end; background: color-mix(in srgb, var(--jl-blue) 12%, var(--jl-surface)); border-color: color-mix(in srgb, var(--jl-blue) 30%, transparent); border-radius: 12px 12px 4px 12px; }
    .bubble-meta { font-size: .72rem; color: var(--jl-ink-faint); font-variant-numeric: tabular-nums; }
    .bubble-text { font-size: .88rem; color: var(--jl-ink); word-break: break-word; white-space: pre-wrap; }
    .muted { color: var(--jl-ink-faint); font-size: .84rem; margin: 0; }
  `],
})
export class DocumentMessagesDialogComponent implements OnInit {
  readonly doc = input.required<CaseDocument>();
  readonly caseId = input.required<string>();

  readonly sent = output<void>();
  readonly closed = output<void>();

  private readonly cases = inject(CasesService);
  private readonly auth = inject(AuthService);

  protected readonly messages = signal<DocMessage[] | null>(null);
  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly currentUser = computed(() => this.auth.user()?.username ?? '');
  protected readonly displayTitle = computed(() => docDisplayTitle(this.doc()));

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.cases.documentMessages(this.doc().id).subscribe((m) => this.messages.set(m));
  }

  protected send(): void {
    const content = this.draft().trim();
    const sender = this.currentUser();
    if (!content || !sender || this.sending()) { return; }
    this.sending.set(true);
    this.cases.sendDocumentMessage(this.caseId(), this.doc().id, sender, content).subscribe({
      next: () => { this.sending.set(false); this.draft.set(''); this.load(); this.sent.emit(); },
      error: () => this.sending.set(false),
    });
  }
}
