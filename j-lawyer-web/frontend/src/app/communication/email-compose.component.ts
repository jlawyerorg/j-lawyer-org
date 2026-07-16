import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { EmailService } from './email.service';
import { ComposeAttachment, ComposeMode, Mailbox, MailMessage, SendMailRequest } from './email.models';

/**
 * Modal composer for writing and sending mail (new / reply / reply-all / forward). Bodies are
 * composed as plain text (CSP-safe, no rich-text editor); replies/forwards quote the original as
 * text and carry the threading headers. Sending goes through {@link EmailService.sendMail}, which
 * also copies the message into the mailbox's Sent folder server-side. HTML composition and
 * templates are a later phase.
 */
@Component({
  selector: 'jl-email-compose',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="cmp-backdrop" (click)="tryClose()"></div>
    <div class="cmp-dialog" role="dialog" aria-modal="true">
      <header class="cmp-head">
        <h2>{{ titleKey() | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="tryClose()" [attr.aria-label]="'compose.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="cmp-body">
        <div class="cmp-row">
          <label>{{ 'compose.from' | transloco }}</label>
          @if (mailboxes().length > 1) {
            <select [value]="from()" (change)="from.set($any($event.target).value)">
              @for (mb of mailboxes(); track mb.id) {
                <option [value]="mb.id">{{ mb.displayName || mb.emailAddress }} &lt;{{ mb.emailAddress }}&gt;</option>
              }
            </select>
          } @else {
            <span class="cmp-static">{{ fromLabel() }}</span>
          }
        </div>

        <div class="cmp-row">
          <label>{{ 'compose.to' | transloco }}</label>
          <input type="text" [value]="to()" (input)="to.set($any($event.target).value)"
                 [placeholder]="'compose.toPlaceholder' | transloco" autocomplete="off" />
          @if (!showCc()) {
            <button type="button" class="cmp-link" (click)="showCc.set(true)">{{ 'compose.addCc' | transloco }}</button>
          }
        </div>

        @if (showCc()) {
          <div class="cmp-row">
            <label>{{ 'compose.cc' | transloco }}</label>
            <input type="text" [value]="cc()" (input)="cc.set($any($event.target).value)" autocomplete="off" />
          </div>
          <div class="cmp-row">
            <label>{{ 'compose.bcc' | transloco }}</label>
            <input type="text" [value]="bcc()" (input)="bcc.set($any($event.target).value)" autocomplete="off" />
          </div>
        }

        <div class="cmp-row">
          <label>{{ 'compose.subject' | transloco }}</label>
          <input type="text" [value]="subject()" (input)="subject.set($any($event.target).value)" autocomplete="off" />
        </div>

        <textarea class="cmp-text" [value]="body()" (input)="body.set($any($event.target).value)"
                  [placeholder]="'compose.bodyPlaceholder' | transloco"></textarea>

        @if (attachments().length || loadingAtt()) {
          <div class="cmp-atts">
            @if (loadingAtt()) {
              <span class="muted">{{ 'compose.loadingAtt' | transloco }}</span>
            }
            @for (a of attachments(); track $index) {
              <span class="cmp-att">
                <jl-icon name="paperclip" [size]="13" />
                <span class="cmp-att-name">{{ a.name }}</span>
                <span class="cmp-att-size">{{ formatSize(a.size) }}</span>
                <button type="button" class="cmp-att-x" (click)="removeAttachment($index)" aria-label="remove">✕</button>
              </span>
            }
          </div>
        }

        @if (error()) { <p class="cmp-error">{{ error() }}</p> }
      </div>

      <footer class="cmp-foot">
        <label class="cmp-attach-btn" [class.disabled]="sending()">
          <jl-icon name="paperclip" [size]="15" />
          <span>{{ 'compose.attach' | transloco }}</span>
          <input type="file" multiple hidden [disabled]="sending()" (change)="onFiles($event)" />
        </label>
        <span class="cmp-spacer"></span>
        <button type="button" class="btn-ghost" [disabled]="sending()" (click)="tryClose()">
          {{ 'compose.cancel' | transloco }}
        </button>
        <button type="button" class="btn-primary" [disabled]="sending() || !canSend()" (click)="send()">
          <jl-icon name="send" [size]="15" />
          <span>{{ (sending() ? 'compose.sending' : 'compose.send') | transloco }}</span>
        </button>
      </footer>
    </div>
  `,
  styleUrl: './email-compose.component.css',
})
export class EmailComposeComponent implements OnInit {
  private readonly api = inject(EmailService);
  private readonly transloco = inject(TranslocoService);

  readonly mailboxes = input.required<Mailbox[]>();
  readonly mailboxId = input.required<string>();
  readonly mode = input.required<ComposeMode>();
  readonly seed = input<MailMessage | null>(null);

  readonly sent = output<void>();
  readonly closed = output<void>();

  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly cc = signal('');
  protected readonly bcc = signal('');
  protected readonly showCc = signal(false);
  protected readonly subject = signal('');
  protected readonly body = signal('');
  protected readonly attachments = signal<ComposeAttachment[]>([]);
  protected readonly sending = signal(false);
  protected readonly loadingAtt = signal(false);
  protected readonly error = signal<string | null>(null);

  private inReplyTo = '';
  private references = '';

  protected readonly titleKey = computed(() => {
    switch (this.mode()) {
      case 'reply': return 'compose.titleReply';
      case 'replyAll': return 'compose.titleReplyAll';
      case 'forward': return 'compose.titleForward';
      default: return 'compose.titleNew';
    }
  });

  protected readonly fromLabel = computed(() => {
    const mb = this.mailboxes().find((m) => m.id === this.from());
    return mb ? `${mb.displayName || mb.emailAddress} <${mb.emailAddress}>` : '';
  });

  protected readonly canSend = computed(() =>
    !!(this.to().trim() || this.cc().trim() || this.bcc().trim()));

  ngOnInit(): void {
    this.from.set(this.mailboxId());
    const seed = this.seed();
    const mode = this.mode();
    if (!seed || mode === 'new') { return; }

    const ownAddr = addressOf(this.mailboxes().find((m) => m.id === this.mailboxId())?.emailAddress ?? '');

    if (mode === 'reply' || mode === 'replyAll') {
      this.to.set(seed.from);
      this.subject.set(ensurePrefix('Re: ', seed.subject));
      if (mode === 'replyAll') {
        const others = [...seed.to, ...seed.cc]
          .filter((a) => { const e = addressOf(a); return e && e !== ownAddr && e !== addressOf(seed.from); });
        if (others.length) { this.cc.set(others.join(', ')); this.showCc.set(true); }
      }
      this.body.set('\n\n' + quoteReply(seed, this.transloco));
      this.inReplyTo = seed.messageId;
      this.references = [seed.references, seed.messageId].filter(Boolean).join(' ');
    } else if (mode === 'forward') {
      this.subject.set(ensurePrefix('Fwd: ', seed.subject));
      this.body.set('\n\n' + quoteForward(seed, this.transloco));
      this.loadForwardedAttachments(seed);
    }
  }

  /** Fetches the original message's non-inline attachments so a forward carries them along. */
  private loadForwardedAttachments(seed: MailMessage): void {
    const atts = (seed.attachments ?? []).filter((a) => !(a.inline && a.contentId));
    if (!atts.length) { return; }
    this.loadingAtt.set(true);
    forkJoin(atts.map((a) => this.api.getAttachment(this.mailboxId(), seed.messageRef, a.attachmentId)
      .pipe(catchError(() => of(null))))).subscribe((results) => {
      const fetched: ComposeAttachment[] = [];
      for (const full of results) {
        if (full?.contentBase64) {
          fetched.push({
            name: full.name, contentType: full.contentType, size: full.size,
            contentBase64: full.contentBase64, inline: false, contentId: '',
          });
        }
      }
      this.attachments.update((list) => [...list, ...fetched]);
      this.loadingAtt.set(false);
    });
  }

  protected onFiles(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    for (const f of files) {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = String(reader.result ?? '');
        const base64 = dataUrl.slice(dataUrl.indexOf(',') + 1);
        this.attachments.update((list) => [...list, {
          name: f.name, contentType: f.type || 'application/octet-stream', size: f.size,
          contentBase64: base64, inline: false, contentId: '',
        }]);
      };
      reader.readAsDataURL(f);
    }
  }

  protected removeAttachment(i: number): void {
    this.attachments.update((list) => list.filter((_, idx) => idx !== i));
  }

  protected send(): void {
    if (this.sending() || !this.canSend()) { return; }
    this.error.set(null);
    this.sending.set(true);
    const req: SendMailRequest = {
      to: this.to().trim(),
      cc: this.cc().trim(),
      bcc: this.bcc().trim(),
      subject: this.subject(),
      body: this.body(),
      contentType: 'text/plain',
      attachments: this.attachments(),
      priority: '',
      readReceipt: false,
      inReplyTo: this.inReplyTo,
      references: this.references,
    };
    this.api.sendMail(this.from(), req).subscribe({
      next: () => { this.sending.set(false); this.sent.emit(); },
      error: () => { this.sending.set(false); this.error.set(this.transloco.translate('compose.error')); },
    });
  }

  protected tryClose(): void {
    if (this.sending()) { return; }
    this.closed.emit();
  }

  protected formatSize(bytes: number): string {
    if (!bytes) { return ''; }
    if (bytes < 1024) { return `${bytes} B`; }
    if (bytes < 1024 * 1024) { return `${Math.round(bytes / 1024)} KB`; }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}

// ---------- module-scope helpers ----------

/** Extracts the bare email address (lowercased) from a "Name <a@b.de>" or "a@b.de" string. */
function addressOf(s: string): string {
  const m = /<([^>]+)>/.exec(s);
  return (m ? m[1] : s).trim().toLowerCase();
}

/** Ensures the subject starts with the given prefix (case-insensitive), avoiding "Re: Re:". */
function ensurePrefix(prefix: string, subject: string): string {
  const s = subject ?? '';
  return s.toLowerCase().startsWith(prefix.toLowerCase()) ? s : prefix + s;
}

function bodyAsText(msg: MailMessage): string {
  const isHtml = /html/i.test(msg.bodyContentType);
  return isHtml ? htmlToText(msg.body ?? '') : (msg.body ?? '');
}

/** Crudely converts HTML to readable plain text (strip tags, decode common entities). */
function htmlToText(html: string): string {
  return html
    .replace(/<\s*(br|\/p|\/div|\/tr|\/li)\s*\/?\s*>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/** Reply quote: an attribution line followed by the original body prefixed with "> ". */
function quoteReply(msg: MailMessage, t: TranslocoService): string {
  const header = t.translate('compose.quoteHeader', { from: msg.from, date: formatWhen(msg.date, t) });
  const quoted = bodyAsText(msg).split('\n').map((l) => '> ' + l).join('\n');
  return `${header}\n${quoted}`;
}

/** Forward block: a "forwarded message" banner with the original headers, then the body. */
function quoteForward(msg: MailMessage, t: TranslocoService): string {
  const lines = [
    '---------- ' + t.translate('compose.forwardHeader') + ' ----------',
    `${t.translate('email.from')}: ${msg.from}`,
    `${t.translate('email.date')}: ${formatWhen(msg.date, t)}`,
    `${t.translate('email.to')}: ${msg.to.join(', ')}`,
    `${t.translate('compose.subject')}: ${msg.subject}`,
    '',
    bodyAsText(msg),
  ];
  return lines.join('\n');
}

function formatWhen(iso: string | null, t: TranslocoService): string {
  if (!iso) { return ''; }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) { return ''; }
  return new Intl.DateTimeFormat(t.getActiveLang(), { dateStyle: 'medium', timeStyle: 'short' }).format(d);
}
