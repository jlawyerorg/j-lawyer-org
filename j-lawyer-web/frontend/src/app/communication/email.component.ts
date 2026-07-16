import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { base64ToBytes } from '../shared/document-preview.models';
import { EmailService } from './email.service';
import { EmailComposeComponent } from './email-compose.component';
import { EmailSaveToCaseComponent, TargetCase } from './email-save-to-case.component';
import {
  CaseSuggestions, ComposeMode, FolderNode, MailScope, Mailbox, MailFolder, MailMessage, PAGE_SIZE,
  TIME_RANGES, TimeRange, wellKnownOrder,
} from './email.models';

/** A mailbox header or an (indented) folder row in the left navigation. */
type NavRow =
  | { kind: 'mailbox'; mailbox: Mailbox }
  | { kind: 'folder'; mailboxId: string; folder: FolderNode };

/** The folder tree of one mailbox. */
interface MailboxFolders {
  mailbox: Mailbox;
  nodes: FolderNode[];
}

/**
 * E-Mail module — the web equivalent of the Swing client's EmailInboxPanel (read & triage scope).
 * Three panes: mailbox/folder tree, message list (with download-restriction presets and folder
 * search), and a reader that renders the body in a sandboxed same-origin blob iframe
 * (CSP frame-src 'self' blob:) with remote content blocked and inline cid: images inlined as
 * data URIs. Triage actions: mark read/unread, delete, move, download attachment/EML. Compose and
 * send are a later phase. All data comes from /v7/email/** (EmailService).
 */
@Component({
  selector: 'jl-email',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, EmailComposeComponent, EmailSaveToCaseComponent, RouterLink],
  template: `
    <div class="mail" [class.show-reader]="selectedRef()"
         [class.m-folders]="mobilePane() === 'folders'"
         [class.m-list]="mobilePane() === 'list'"
         [class.m-reader]="mobilePane() === 'reader'">
      <!-- ---------- Left: mailboxes + folders ---------- -->
      <aside class="folders">
        <header class="col-head">
          <h1>{{ 'email.title' | transloco }}</h1>
          <button type="button" class="icon-btn" [disabled]="structureLoading()"
                  (click)="refresh()" [attr.aria-label]="'email.refresh' | transloco" [title]="'email.refresh' | transloco">
            <jl-icon name="refresh" [size]="15" />
          </button>
        </header>
        <div class="col-body">
          @if (structureLoading() && !mailboxes()) {
            <p class="muted pad">{{ 'email.loading' | transloco }}</p>
          } @else if (structureError()) {
            <p class="pad">
              {{ 'email.error' | transloco }}
              <button type="button" class="btn-retry" (click)="refresh()">{{ 'email.retry' | transloco }}</button>
            </p>
          } @else if (!mailboxes()?.length) {
            <p class="muted pad">{{ 'email.noMailboxes' | transloco }}</p>
          } @else {
            @for (row of navRows(); track rowKey(row)) {
              @if (row.kind === 'mailbox') {
                <div class="mbx" [title]="row.mailbox.emailAddress">
                  <jl-icon name="mail" [size]="13" />
                  <span class="mbx-name">{{ row.mailbox.displayName || row.mailbox.emailAddress }}</span>
                  <button type="button" class="mbx-hidden" (click)="openHidden(row.mailbox.id)"
                          [title]="'email.hidden.manage' | transloco" [attr.aria-label]="'email.hidden.manage' | transloco">
                    <jl-icon name="eye" [size]="13" />
                  </button>
                </div>
              } @else {
                <div class="fld-row">
                  <button type="button" class="fld"
                          [class.sel]="row.mailboxId === selectedMailboxId() && row.folder.folderId === selectedFolderId()"
                          [style.padding-left.px]="18 + row.folder.depth * 14"
                          (click)="selectFolder(row.mailboxId, row.folder)">
                    <span class="fld-name">{{ folderLabel(row.folder) }}</span>
                    @if (row.folder.unreadCount > 0) { <span class="fld-badge">{{ row.folder.unreadCount }}</span> }
                  </button>
                  @if (!isWellKnown(row.folder)) {
                    <button type="button" class="fld-hide" [disabled]="folderBusy() === row.folder.folderId"
                            (click)="hideFolder(row.mailboxId, row.folder)" [title]="'email.hidden.hide' | transloco">
                      <jl-icon name="eye-off" [size]="13" />
                    </button>
                  }
                </div>
              }
            }
          }
        </div>
      </aside>

      <!-- ---------- Middle: message list ---------- -->
      <section class="list">
        <header class="col-head list-head">
          <div class="list-title">
            <button type="button" class="to-folders" (click)="mobilePane.set('folders')">‹ {{ 'email.navBack' | transloco }}</button>
            <h2>{{ selectedFolder() ? folderLabel(selectedFolder()!) : ('email.selectFolder' | transloco) }}</h2>
            @if (messages()?.length) { <span class="count">{{ messages()!.length }}</span> }
            <button type="button" class="compose-btn" [disabled]="!composeFromId()" (click)="composeNew()"
                    [title]="'compose.new' | transloco">
              <jl-icon name="edit" [size]="14" /><span>{{ 'compose.new' | transloco }}</span>
            </button>
          </div>
          <div class="list-tools">
            <div class="search">
              <button type="button" class="search-btn" [disabled]="!selectedFolderId()"
                      (click)="applySearch()" [title]="'email.search' | transloco"
                      [attr.aria-label]="'email.search' | transloco">
                <jl-icon name="search" [size]="14" />
              </button>
              <input type="search" [value]="searchInput()" [placeholder]="'email.searchPlaceholder' | transloco"
                     [disabled]="!selectedFolderId()"
                     (input)="searchInput.set($any($event.target).value)"
                     (keydown.enter)="applySearch()" />
              @if (search()) {
                <button type="button" class="clear" (click)="clearSearch()" aria-label="clear">✕</button>
              }
            </div>
            <div class="seg" role="group" [attr.aria-label]="'email.scope.label' | transloco">
              <button type="button" [class.on]="scope() === 'all'" [disabled]="!selectedFolderId()"
                      (click)="setScope('all')">{{ 'email.scope.all' | transloco }}</button>
              <button type="button" [class.on]="scope() === 'unread'" [disabled]="!selectedFolderId()"
                      (click)="setScope('unread')">{{ 'email.scope.unread' | transloco }}</button>
            </div>
            <select class="range" [disabled]="!selectedFolderId()"
                    [value]="rangeId()" (change)="changeRange($any($event.target).value)">
              @for (r of ranges; track r.id) {
                <option [value]="r.id">{{ r.labelKey | transloco }}</option>
              }
            </select>
          </div>
        </header>
        <div class="col-body" (scroll)="onListScroll($event)">
          @if (!selectedFolderId()) {
            <p class="muted pad">{{ 'email.selectFolderHint' | transloco }}</p>
          } @else if (messagesLoading()) {
            <p class="muted pad">{{ 'email.loading' | transloco }}</p>
          } @else if (messagesError()) {
            <p class="pad">
              {{ 'email.listError' | transloco }}
              <button type="button" class="btn-retry" (click)="loadMessages()">{{ 'email.retry' | transloco }}</button>
            </p>
          } @else if (!messages()?.length) {
            <p class="muted pad">{{ 'email.empty' | transloco }}</p>
          } @else {
            @for (m of messages()!; track m.messageRef) {
              <button type="button" class="msg" [class.unread]="!m.read" [class.sel]="m.messageRef === selectedRef()"
                      (click)="selectMessage(m)">
                @if (!m.read) { <span class="dot" aria-hidden="true"></span> }
                <div class="msg-main">
                  <div class="msg-top">
                    <span class="msg-from">{{ m.from || ('email.noSender' | transloco) }}</span>
                    <span class="msg-date">{{ formatDate(m.date) }}</span>
                  </div>
                  <div class="msg-sub">
                    @if (m.hasAttachments) { <jl-icon name="download" [size]="12" /> }
                    <span class="msg-subject">{{ m.subject || ('email.noSubject' | transloco) }}</span>
                  </div>
                </div>
              </button>
            }
            @if (paging()) {
              <p class="muted pad load-more">{{ 'email.loadingMore' | transloco }}</p>
            }
          }
        </div>
      </section>

      <!-- ---------- Right: reader ---------- -->
      <section class="reader">
        @if (selectedRef()) {
          <header class="col-head reader-head">
            <button type="button" class="back" (click)="closeReader()">‹ {{ 'email.back' | transloco }}</button>
            <div class="reader-actions">
              <button type="button" class="icon-btn" [disabled]="acting() || !message()" (click)="reply()"
                      [title]="'compose.reply' | transloco">
                <jl-icon name="reply" [size]="15" />
              </button>
              <button type="button" class="icon-btn" [disabled]="acting() || !message()" (click)="replyAll()"
                      [title]="'compose.replyAll' | transloco">
                <jl-icon name="reply-all" [size]="15" />
              </button>
              <button type="button" class="icon-btn" [disabled]="acting() || !message()" (click)="forward()"
                      [title]="'compose.forward' | transloco">
                <jl-icon name="forward" [size]="15" />
              </button>
              <button type="button" class="icon-btn" [disabled]="acting() || !message()" (click)="openSaveToCase(null)"
                      [title]="'saveToCase.title' | transloco">
                <jl-icon name="inbox" [size]="15" />
              </button>
              <button type="button" class="icon-btn" [disabled]="acting()" (click)="toggleRead()"
                      [title]="(message()?.read ? 'email.markUnread' : 'email.markRead') | transloco">
                <jl-icon name="check" [size]="15" />
              </button>
              <div class="menu-wrap">
                <button type="button" class="icon-btn" [disabled]="acting() || !moveTargets().length"
                        (click)="moveOpen.set(!moveOpen())" [title]="'email.move' | transloco">
                  <jl-icon name="cases" [size]="15" />
                </button>
                @if (moveOpen()) {
                  <div class="menu" (mouseleave)="moveOpen.set(false)">
                    @for (t of moveTargets(); track t.folderId) {
                      <button type="button" class="menu-item" (click)="move(t)">{{ folderLabel(t) }}</button>
                    }
                  </div>
                }
              </div>
              <button type="button" class="icon-btn" [disabled]="acting()" (click)="downloadEml()"
                      [title]="'email.downloadEml' | transloco">
                <jl-icon name="download" [size]="15" />
              </button>
              <button type="button" class="icon-btn danger" [disabled]="acting()" (click)="remove()"
                      [title]="'email.delete' | transloco">✕</button>
            </div>
          </header>
          <div class="col-body reader-body">
            @if (messageLoading()) {
              <p class="muted pad">{{ 'email.loadingMessage' | transloco }}</p>
            } @else if (messageError()) {
              <p class="pad">
                {{ 'email.messageError' | transloco }}
                <button type="button" class="btn-retry" (click)="reloadMessage()">{{ 'email.retry' | transloco }}</button>
              </p>
            } @else if (message()) {
              @let msg = message()!;
              <div class="headers">
                <h3 class="subject">{{ msg.subject || ('email.noSubject' | transloco) }}</h3>
                <dl class="hdr-grid">
                  <dt>{{ 'email.from' | transloco }}</dt><dd>{{ msg.from }}</dd>
                  @if (msg.to.length) { <dt>{{ 'email.to' | transloco }}</dt><dd>{{ msg.to.join(', ') }}</dd> }
                  @if (msg.cc.length) { <dt>{{ 'email.cc' | transloco }}</dt><dd>{{ msg.cc.join(', ') }}</dd> }
                  <dt>{{ 'email.date' | transloco }}</dt><dd>{{ formatDateTime(msg.date) }}</dd>
                </dl>
              </div>

              @if (suggestions(); as sg) {
                @if (sg.suggestedCases.length || sg.contacts.length || sg.phoneNumbers.length) {
                  <div class="suggest">
                    @if (sg.suggestedCases.length) {
                      <div class="suggest-group">
                        <span class="suggest-label">{{ 'email.suggest.cases' | transloco }}</span>
                        <div class="suggest-items">
                          @for (c of sg.suggestedCases; track c.id) {
                            <span class="suggest-case">
                              <a class="sc-open" [routerLink]="['/cases', c.id]"
                                 [title]="c.name" (click)="mobilePane.set('reader')">
                                <jl-icon name="cases" [size]="13" />
                                <span class="sc-fn">{{ c.fileNumber }}</span>
                                <span class="sc-name">{{ c.name }}</span>
                                @if (c.archived) { <span class="sc-arch">{{ 'email.suggest.archived' | transloco }}</span> }
                              </a>
                              <button type="button" class="sc-save"
                                      (click)="openSaveToCase({ id: c.id, fileNumber: c.fileNumber, name: c.name })"
                                      [title]="'saveToCase.title' | transloco" [attr.aria-label]="'saveToCase.title' | transloco">
                                <jl-icon name="inbox" [size]="13" />
                              </button>
                            </span>
                          }
                        </div>
                      </div>
                    }
                    @if (sg.contacts.length) {
                      <div class="suggest-group">
                        <span class="suggest-label">{{ 'email.suggest.contacts' | transloco }}</span>
                        <div class="suggest-items">
                          @for (ct of sg.contacts; track ct.id) {
                            <a class="suggest-contact" [routerLink]="['/contacts', ct.id]" [title]="ct.email">
                              <jl-icon name="contacts" [size]="12" />
                              <span class="sc-name">{{ ct.displayName }}</span>
                            </a>
                          }
                        </div>
                      </div>
                    }
                    @if (sg.phoneNumbers.length) {
                      <div class="suggest-group">
                        <span class="suggest-label">{{ 'email.suggest.phones' | transloco }}</span>
                        <div class="suggest-items">
                          @for (p of sg.phoneNumbers; track p) {
                            <a class="suggest-phone" [href]="'tel:' + p"><jl-icon name="phone" [size]="12" />{{ p }}</a>
                          }
                        </div>
                      </div>
                    }
                  </div>
                }
              }

              <div class="body">
                @if (bodyUrl()) {
                  <iframe class="body-frame" sandbox="allow-popups allow-popups-to-escape-sandbox"
                          [src]="bodyUrl()" [title]="msg.subject"></iframe>
                } @else {
                  <pre class="body-text">{{ bodyText() }}</pre>
                }
              </div>

              @if (visibleAttachments().length) {
                <div class="atts">
                  <h4>{{ 'email.attachments' | transloco }} ({{ visibleAttachments().length }})</h4>
                  <ul>
                    @for (a of visibleAttachments(); track a.attachmentId) {
                      <li>
                        <button type="button" class="att" [disabled]="downloadingAtt() === a.attachmentId"
                                (click)="downloadAttachment(a)">
                          <jl-icon name="download" [size]="14" />
                          <span class="att-name">{{ a.name }}</span>
                          <span class="att-size">{{ formatSize(a.size) }}</span>
                        </button>
                      </li>
                    }
                  </ul>
                </div>
              }
            }
          </div>
        } @else {
          <p class="empty reader-empty">{{ 'email.selectMessageHint' | transloco }}</p>
        }
      </section>
    </div>

    @if (hiddenDialog(); as hd) {
      <div class="hf-backdrop" (click)="hiddenDialog.set(null)"></div>
      <div class="hf-dialog" role="dialog" aria-modal="true">
        <header class="hf-head">
          <h2>{{ 'email.hidden.title' | transloco }}</h2>
          <button type="button" class="icon-btn" (click)="hiddenDialog.set(null)" [attr.aria-label]="'email.hidden.close' | transloco">
            <jl-icon name="close" [size]="18" />
          </button>
        </header>
        <div class="hf-body">
          @if (hiddenList() === null) {
            <p class="muted">{{ 'email.loading' | transloco }}</p>
          } @else if (!hiddenList()!.length) {
            <p class="muted">{{ 'email.hidden.none' | transloco }}</p>
          } @else {
            @for (f of hiddenList()!; track f.folderId) {
              <div class="hf-row">
                <jl-icon name="eye-off" [size]="14" />
                <span class="hf-name">{{ f.displayName }}</span>
                <button type="button" class="hf-unhide" [disabled]="folderBusy() === f.folderId"
                        (click)="unhide(hd.mailboxId, f)">{{ 'email.hidden.unhide' | transloco }}</button>
              </div>
            }
          }
        </div>
      </div>
    }

    @if (compose(); as c) {
      <jl-email-compose [mailboxes]="mailboxes() ?? []" [mailboxId]="composeFromId()!"
                        [mode]="c.mode" [seed]="c.seed"
                        (sent)="onSent()" (closed)="compose.set(null)" />
    }

    @if (saveToCaseOpen() && message() && selectedMailboxId()) {
      <jl-email-save-to-case [mailboxId]="selectedMailboxId()!" [message]="message()!" [suggestions]="suggestions()"
                             [preselect]="saveToCasePreselect()"
                             (saved)="onSavedToCase()" (closed)="saveToCaseOpen.set(false)" />
    }
  `,
  styleUrl: './email.component.css',
})
export class EmailComponent {
  private readonly api = inject(EmailService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly transloco = inject(TranslocoService);
  private readonly router = inject(Router);

  protected readonly ranges = TIME_RANGES;

  // Mailbox/folder structure is cached in the service (survives view open/close); the component
  // just reads it and derives the folder tree. Only the message list is component-local state.
  protected readonly mailboxes = this.api.mailboxes;
  protected readonly structureLoading = this.api.structureLoading;
  protected readonly structureError = this.api.structureError;

  protected readonly selectedMailboxId = signal<string | null>(null);
  protected readonly selectedFolderId = signal<string | null>(null);

  protected readonly messages = signal<MailMessage[] | null>(null);
  protected readonly messagesLoading = signal(false);
  protected readonly messagesError = signal(false);
  /** Scope toggle (all / unread only) and optional time-range filter. */
  protected readonly scope = signal<MailScope>('all');
  protected readonly rangeId = signal<string>(TIME_RANGES[0].id);
  /** Infinite-scroll paging state: loading a subsequent page, and "no more pages". */
  protected readonly paging = signal(false);
  protected readonly exhausted = signal(false);
  protected readonly searchInput = signal('');
  protected readonly search = signal('');

  protected readonly selectedRef = signal<string | null>(null);
  protected readonly message = signal<MailMessage | null>(null);
  protected readonly messageLoading = signal(false);
  protected readonly messageError = signal(false);
  protected readonly bodyUrl = signal<SafeResourceUrl | null>(null);
  protected readonly bodyText = signal('');
  /** Server-computed case/contact/phone suggestions for the opened message. */
  protected readonly suggestions = signal<CaseSuggestions | null>(null);
  protected readonly acting = signal(false);
  protected readonly moveOpen = signal(false);
  protected readonly downloadingAtt = signal<string | null>(null);
  /** Which pane is shown on phones (drill-down: folder tree → message list → reader). */
  protected readonly mobilePane = signal<'folders' | 'list' | 'reader'>('folders');

  /** Open composer (new/reply/forward), or null when closed. `seed` is the original for reply/forward. */
  protected readonly compose = signal<{ mode: ComposeMode; seed: MailMessage | null } | null>(null);
  /** Whether the "save to case" dialog is open, and an optional case preselected from a chip. */
  protected readonly saveToCaseOpen = signal(false);
  protected readonly saveToCasePreselect = signal<TargetCase | null>(null);

  /** Folder-hide state: id of the folder being hidden/unhidden, and the "manage hidden" dialog. */
  protected readonly folderBusy = signal<string | null>(null);
  protected readonly hiddenDialog = signal<{ mailboxId: string } | null>(null);
  protected readonly hiddenList = signal<MailFolder[] | null>(null);

  private msgSeq = 0;
  /** Guards against a folder switch / filter change landing an in-flight page in the wrong list. */
  private listSeq = 0;
  private bodyBlobUrl: string | null = null;

  /** Folder trees derived from the cached (flat) folder lists — one per mailbox, in order. */
  private readonly folderTrees = computed<MailboxFolders[]>(() => {
    const boxes = this.mailboxes() ?? [];
    const raw = this.api.folders();
    return boxes.map((mb) => ({ mailbox: mb, nodes: buildTree(raw[mb.id] ?? []) }));
  });

  /** Flattened left navigation: each mailbox header followed by its folder rows. */
  protected readonly navRows = computed<NavRow[]>(() => {
    const rows: NavRow[] = [];
    for (const mf of this.folderTrees()) {
      rows.push({ kind: 'mailbox', mailbox: mf.mailbox });
      flatten(mf.nodes, (f) => rows.push({ kind: 'folder', mailboxId: mf.mailbox.id, folder: f }));
    }
    return rows;
  });

  protected readonly selectedFolder = computed<FolderNode | null>(() => {
    const mid = this.selectedMailboxId(); const fid = this.selectedFolderId();
    if (!mid || !fid) { return null; }
    const mf = this.folderTrees().find((x) => x.mailbox.id === mid);
    return mf ? findFolder(mf.nodes, fid) : null;
  });

  /** Folders of the current mailbox other than the current one — valid move targets. */
  protected readonly moveTargets = computed<FolderNode[]>(() => {
    const mid = this.selectedMailboxId(); const fid = this.selectedFolderId();
    const mf = this.folderTrees().find((x) => x.mailbox.id === mid);
    if (!mf) { return []; }
    const flat: FolderNode[] = [];
    flatten(mf.nodes, (f) => flat.push(f));
    return flat.filter((f) => f.folderId !== fid);
  });

  /** Attachments worth listing: skip inline images already rendered in the body. */
  protected readonly visibleAttachments = computed(() =>
    (this.message()?.attachments ?? []).filter((a) => !(a.inline && a.contentId)),
  );

  /** Mailbox used as the sender: the selected one, else the first available. */
  protected readonly composeFromId = computed(() =>
    this.selectedMailboxId() ?? this.mailboxes()?.[0]?.id ?? null);

  private autoSelected = false;

  constructor() {
    // Load the structure only if not already cached; the effect opens the first inbox as soon as
    // the (cached or freshly fetched) tree is available — once per component instance.
    this.api.ensureStructure();
    effect(() => {
      const trees = this.folderTrees();
      if (!this.autoSelected && trees.length && !this.selectedFolderId()) {
        this.autoSelected = true;
        this.autoSelectInbox();
      }
    });
    inject(DestroyRef).onDestroy(() => this.revokeBody());
  }

  /** Opens the first mailbox's inbox so the module isn't empty. */
  private autoSelectInbox(): void {
    if (this.selectedFolderId()) { return; }
    for (const mf of this.folderTrees()) {
      const inbox = firstMatch(mf.nodes, (f) => f.wellKnownName === 'inbox') ?? mf.nodes[0];
      if (inbox) {
        this.selectFolder(mf.mailbox.id, inbox);
        return;
      }
    }
  }

  protected selectFolder(mailboxId: string, folder: FolderNode): void {
    this.selectedMailboxId.set(mailboxId);
    this.selectedFolderId.set(folder.folderId);
    this.search.set('');
    this.searchInput.set('');
    this.closeReader();
    this.mobilePane.set('list');
    this.loadMessages();
  }

  /** (Re)loads the first page of the current folder with the active scope/range/search filter. */
  protected loadMessages(): void {
    const mid = this.selectedMailboxId(); const fid = this.selectedFolderId();
    if (!mid || !fid) { return; }
    const seq = ++this.listSeq;
    this.messages.set(null);
    this.messagesLoading.set(true);
    this.messagesError.set(false);
    this.paging.set(false);
    this.exhausted.set(false);
    this.fetchPage(mid, fid, 0, seq, false);
  }

  /** Loads the next page (older messages) when the list is scrolled near its end. */
  protected loadMore(): void {
    const mid = this.selectedMailboxId(); const fid = this.selectedFolderId();
    if (!mid || !fid) { return; }
    if (this.messagesLoading() || this.paging() || this.exhausted() || this.messagesError()) { return; }
    const loaded = this.messages()?.length ?? 0;
    if (!loaded) { return; }
    this.paging.set(true);
    this.fetchPage(mid, fid, loaded, this.listSeq, true);
  }

  /** Fetches one page at `offset`; appends (infinite scroll) or replaces (fresh load). */
  private fetchPage(mid: string, fid: string, offset: number, seq: number, append: boolean): void {
    const sinceDays = this.currentRange().sinceDays;
    this.api.listMessages(mid, fid, {
      top: PAGE_SIZE,
      offset,
      sinceDate: sinceDays != null ? isoDaysAgo(sinceDays) : undefined,
      unreadOnly: this.scope() === 'unread',
      search: this.search() || undefined,
    }).subscribe({
      next: (rows) => {
        if (seq !== this.listSeq) { return; }
        if (append) {
          // Dedupe by ref in case the folder shifted between pages (avoids @for track collisions).
          this.messages.update((list) => {
            const seen = new Set((list ?? []).map((m) => m.messageRef));
            return [...(list ?? []), ...rows.filter((m) => !seen.has(m.messageRef))];
          });
        } else {
          this.messages.set(rows);
        }
        if (rows.length < PAGE_SIZE) { this.exhausted.set(true); }
        this.messagesLoading.set(false);
        this.paging.set(false);
      },
      error: () => {
        if (seq !== this.listSeq) { return; }
        if (!append) { this.messagesError.set(true); }
        this.messagesLoading.set(false);
        this.paging.set(false);
      },
    });
  }

  /** Fires from the list body's scroll; triggers the next page shortly before the end. */
  protected onListScroll(ev: Event): void {
    const el = ev.target as HTMLElement;
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 240) {
      this.loadMore();
    }
  }

  protected setScope(s: MailScope): void {
    if (this.scope() === s) { return; }
    this.scope.set(s);
    this.loadMessages();
  }

  protected changeRange(id: string): void {
    this.rangeId.set(id);
    this.loadMessages();
  }

  protected applySearch(): void {
    this.search.set(this.searchInput().trim());
    this.loadMessages();
  }

  protected clearSearch(): void {
    this.search.set('');
    this.searchInput.set('');
    this.loadMessages();
  }

  protected selectMessage(m: MailMessage): void {
    this.selectedRef.set(m.messageRef);
    this.mobilePane.set('reader');
    this.reloadMessage();
    if (!m.read) {
      // Optimistically reflect the read state in the list and folder badge.
      this.patchMessage(m.messageRef, { read: true });
      this.bumpUnread(-1);
      this.api.markAsRead(this.selectedMailboxId()!, m.messageRef, true).subscribe({ error: () => {} });
    }
  }

  protected reloadMessage(): void {
    const mid = this.selectedMailboxId(); const ref = this.selectedRef();
    if (!mid || !ref) { return; }
    const seq = ++this.msgSeq;
    this.messageLoading.set(true);
    this.messageError.set(false);
    this.message.set(null);
    this.suggestions.set(null);
    this.revokeBody();
    this.bodyUrl.set(null);
    this.bodyText.set('');
    this.api.getMessage(mid, ref, true).subscribe({
      next: (msg) => {
        if (seq !== this.msgSeq) { return; }
        this.message.set(msg);
        this.renderBody(msg);
        this.messageLoading.set(false);
        this.loadSuggestions(seq, mid, msg);
      },
      error: () => {
        if (seq !== this.msgSeq) { return; }
        this.messageError.set(true);
        this.messageLoading.set(false);
      },
    });
  }

  /**
   * Renders the body. HTML goes into a blob iframe carrying a strict CSP meta (no remote loads;
   * only data: images) with cid: references inlined from the message's inline attachments; the
   * sandbox attribute additionally disables scripts. Plain text is shown verbatim in a <pre>.
   */
  private renderBody(msg: MailMessage): void {
    const isHtml = /html/i.test(msg.bodyContentType);
    if (!isHtml) {
      this.bodyText.set(msg.body ?? '');
      return;
    }
    const html = inlineCidImages(msg.body ?? '', msg.attachments);
    const doc =
      '<!doctype html><html><head><meta charset="utf-8">' +
      '<meta http-equiv="Content-Security-Policy" ' +
      `content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:">` +
      '<base target="_blank">' +
      '<style>html,body{margin:0;padding:12px;font:14px/1.5 system-ui,-apple-system,Segoe UI,Roboto,sans-serif;' +
      'color:#16232e;word-break:break-word;} img{max-width:100%;height:auto;} a{color:#0b5cad;}</style>' +
      `</head><body>${html}</body></html>`;
    this.bodyBlobUrl = URL.createObjectURL(new Blob([doc], { type: 'text/html' }));
    // iframe [src] is a RESOURCE_URL context; the blob is our own same-origin data. CSP
    // frame-src 'self' blob: permits embedding it; sandbox="" keeps scripts/same-origin off.
    this.bodyUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.bodyBlobUrl));
  }

  protected toggleRead(): void {
    const mid = this.selectedMailboxId(); const msg = this.message();
    if (!mid || !msg) { return; }
    const next = !msg.read;
    this.acting.set(true);
    this.api.markAsRead(mid, msg.messageRef, next).subscribe({
      next: () => {
        this.message.update((m) => (m ? { ...m, read: next } : m));
        this.patchMessage(msg.messageRef, { read: next });
        this.bumpUnread(next ? -1 : 1);
        this.acting.set(false);
      },
      error: () => this.acting.set(false),
    });
  }

  protected move(target: FolderNode): void {
    const mid = this.selectedMailboxId(); const ref = this.selectedRef();
    if (!mid || !ref) { return; }
    this.moveOpen.set(false);
    this.acting.set(true);
    this.api.moveMessage(mid, ref, target.folderId).subscribe({
      next: () => { this.acting.set(false); this.removeFromList(ref); this.closeReader(); },
      error: () => this.acting.set(false),
    });
  }

  protected remove(): void {
    const mid = this.selectedMailboxId(); const ref = this.selectedRef();
    if (!mid || !ref) { return; }
    this.acting.set(true);
    this.api.deleteMessage(mid, ref).subscribe({
      next: () => { this.acting.set(false); this.removeFromList(ref); this.closeReader(); },
      error: () => this.acting.set(false),
    });
  }

  protected downloadEml(): void {
    const mid = this.selectedMailboxId(); const msg = this.message();
    if (!mid || !msg) { return; }
    this.acting.set(true);
    this.api.getEml(mid, msg.messageRef).subscribe({
      next: (blob) => {
        triggerDownload(blob, `${safeName(msg.subject) || 'message'}.eml`);
        this.acting.set(false);
      },
      error: () => this.acting.set(false),
    });
  }

  protected downloadAttachment(att: { attachmentId: string; name: string; contentType: string; contentBase64: string | null }): void {
    const mid = this.selectedMailboxId(); const ref = this.selectedRef();
    if (!mid || !ref) { return; }
    if (att.contentBase64) {
      triggerDownload(bytesBlob(att.contentBase64, att.contentType), att.name);
      return;
    }
    this.downloadingAtt.set(att.attachmentId);
    this.api.getAttachment(mid, ref, att.attachmentId).subscribe({
      next: (full) => {
        if (full.contentBase64) {
          triggerDownload(bytesBlob(full.contentBase64, full.contentType), full.name || att.name);
        }
        this.downloadingAtt.set(null);
      },
      error: () => this.downloadingAtt.set(null),
    });
  }

  protected refresh(): void {
    this.api.ensureStructure(true);
    if (this.selectedFolderId()) { this.loadMessages(); }
  }

  /** Lazily fetches server-side case/contact/phone suggestions once a message has loaded. */
  private loadSuggestions(seq: number, mailboxId: string, msg: MailMessage): void {
    this.api.caseSuggestions(mailboxId, { subject: msg.subject, body: msg.body, from: msg.from }).subscribe({
      next: (sg) => { if (seq === this.msgSeq) { this.suggestions.set(sg); } },
      error: () => { /* suggestions are best-effort; ignore */ },
    });
  }

  protected composeNew(): void {
    if (!this.composeFromId()) { return; }
    this.compose.set({ mode: 'new', seed: null });
  }

  protected reply(): void { this.openReply('reply'); }
  protected replyAll(): void { this.openReply('replyAll'); }
  protected forward(): void { this.openReply('forward'); }

  private openReply(mode: ComposeMode): void {
    const msg = this.message();
    if (!msg) { return; }
    this.compose.set({ mode, seed: msg });
  }

  /** Opens the save-to-case dialog, optionally with a case preselected (from a suggestion chip). */
  protected openSaveToCase(preselect: TargetCase | null): void {
    if (!this.message()) { return; }
    this.saveToCasePreselect.set(preselect);
    this.saveToCaseOpen.set(true);
  }

  /** Closes the save-to-case dialog after the email has been stored in the case. */
  protected onSavedToCase(): void {
    this.saveToCaseOpen.set(false);
  }

  /** After a successful send, close the composer and refresh the list if we're viewing Sent. */
  protected onSent(): void {
    this.compose.set(null);
    if (this.selectedFolder()?.wellKnownName === 'sentitems') { this.loadMessages(); }
  }

  protected closeReader(): void {
    this.msgSeq++;
    this.selectedRef.set(null);
    this.message.set(null);
    this.suggestions.set(null);
    this.saveToCaseOpen.set(false);
    this.saveToCasePreselect.set(null);
    this.moveOpen.set(false);
    this.revokeBody();
    this.bodyUrl.set(null);
    this.bodyText.set('');
    this.mobilePane.set('list');
  }

  // ----- helpers -----

  protected rowKey(row: NavRow): string {
    return row.kind === 'mailbox' ? `m:${row.mailbox.id}` : `f:${row.mailboxId}:${row.folder.folderId}`;
  }

  protected folderLabel(f: MailFolder): string {
    const key = wellKnownKey(f.wellKnownName);
    return key ? this.transloco.translate(key) : f.displayName;
  }

  /** Well-known folders (Inbox/Sent/Trash/Drafts) can't be hidden — mirrors the desktop client. */
  protected isWellKnown(f: MailFolder): boolean {
    return !!f.wellKnownName;
  }

  /** Confirms, then hides a folder and refreshes the mailbox's tree. */
  protected hideFolder(mailboxId: string, folder: MailFolder): void {
    if (this.folderBusy()) { return; }
    if (!confirm(this.transloco.translate('email.hidden.confirm', { name: this.folderLabel(folder) }))) { return; }
    this.folderBusy.set(folder.folderId);
    this.api.setFolderHidden(mailboxId, folder.folderId, true).subscribe({
      next: () => {
        this.folderBusy.set(null);
        if (this.selectedFolderId() === folder.folderId) { this.selectedFolderId.set(null); this.messages.set(null); }
        this.api.refreshFolders(mailboxId);
      },
      error: () => this.folderBusy.set(null),
    });
  }

  /** Opens the "manage hidden folders" dialog for a mailbox. */
  protected openHidden(mailboxId: string): void {
    this.hiddenList.set(null);
    this.hiddenDialog.set({ mailboxId });
    this.api.hiddenFolders(mailboxId).subscribe({
      next: (list) => this.hiddenList.set(list),
      error: () => this.hiddenList.set([]),
    });
  }

  /** Unhides a folder from the dialog, then refreshes the tree + the dialog list. */
  protected unhide(mailboxId: string, folder: MailFolder): void {
    if (this.folderBusy()) { return; }
    this.folderBusy.set(folder.folderId);
    this.api.setFolderHidden(mailboxId, folder.folderId, false).subscribe({
      next: () => {
        this.folderBusy.set(null);
        this.api.refreshFolders(mailboxId);
        this.hiddenList.update((list) => (list ?? []).filter((f) => f.folderId !== folder.folderId));
      },
      error: () => this.folderBusy.set(null),
    });
  }

  private currentRange(): TimeRange {
    return this.ranges.find((r) => r.id === this.rangeId()) ?? this.ranges[0];
  }

  private patchMessage(ref: string, patch: Partial<MailMessage>): void {
    this.messages.update((list) => list?.map((m) => (m.messageRef === ref ? { ...m, ...patch } : m)) ?? list);
  }

  private removeFromList(ref: string): void {
    this.messages.update((list) => list?.filter((m) => m.messageRef !== ref) ?? list);
  }

  /** Adjusts the selected folder's unread badge via the service cache (survives view re-open). */
  private bumpUnread(delta: number): void {
    const mid = this.selectedMailboxId(); const fid = this.selectedFolderId();
    if (!mid || !fid) { return; }
    this.api.adjustUnread(mid, fid, delta);
  }

  protected formatDate(iso: string | null): string {
    if (!iso) { return ''; }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) { return ''; }
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    return new Intl.DateTimeFormat(this.transloco.getActiveLang(), sameDay
      ? { hour: '2-digit', minute: '2-digit' }
      : { day: '2-digit', month: '2-digit', year: '2-digit' }).format(d);
  }

  protected formatDateTime(iso: string | null): string {
    if (!iso) { return ''; }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) { return ''; }
    return new Intl.DateTimeFormat(this.transloco.getActiveLang(), {
      dateStyle: 'medium', timeStyle: 'short',
    }).format(d);
  }

  protected formatSize(bytes: number): string {
    if (!bytes) { return ''; }
    if (bytes < 1024) { return `${bytes} B`; }
    if (bytes < 1024 * 1024) { return `${Math.round(bytes / 1024)} KB`; }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private revokeBody(): void {
    if (this.bodyBlobUrl) {
      URL.revokeObjectURL(this.bodyBlobUrl);
      this.bodyBlobUrl = null;
    }
  }
}

// ---------- module-scope helpers ----------

/** Builds a display tree from the flat folder list, ordered well-known-first then alphabetical. */
function buildTree(flat: MailFolder[]): FolderNode[] {
  const byId = new Map<string, FolderNode>();
  for (const f of flat) {
    byId.set(f.folderId, { ...f, children: [], depth: 0 });
  }
  const roots: FolderNode[] = [];
  for (const node of byId.values()) {
    const parent = node.parentFolderId ? byId.get(node.parentFolderId) : undefined;
    if (parent) { parent.children.push(node); } else { roots.push(node); }
  }
  const sortRec = (nodes: FolderNode[], depth: number) => {
    nodes.sort((a, b) => wellKnownOrder(a.wellKnownName) - wellKnownOrder(b.wellKnownName)
      || a.displayName.localeCompare(b.displayName));
    for (const n of nodes) { n.depth = depth; sortRec(n.children, depth + 1); }
  };
  sortRec(roots, 0);
  return roots;
}

function flatten(nodes: FolderNode[], visit: (n: FolderNode) => void): void {
  for (const n of nodes) { visit(n); flatten(n.children, visit); }
}

function findFolder(nodes: FolderNode[], folderId: string): FolderNode | null {
  for (const n of nodes) {
    if (n.folderId === folderId) { return n; }
    const found = findFolder(n.children, folderId);
    if (found) { return found; }
  }
  return null;
}

function firstMatch(nodes: FolderNode[], pred: (n: FolderNode) => boolean): FolderNode | null {
  for (const n of nodes) {
    if (pred(n)) { return n; }
    const found = firstMatch(n.children, pred);
    if (found) { return found; }
  }
  return null;
}

/** i18n key for a well-known folder name, or '' to fall back to the server-provided display name. */
function wellKnownKey(wellKnownName: string): string {
  switch (wellKnownName) {
    case 'inbox': return 'email.folder.inbox';
    case 'sentitems': return 'email.folder.sent';
    case 'deleteditems': return 'email.folder.trash';
    case 'drafts': return 'email.folder.drafts';
    default: return '';
  }
}

/** Rewrites <img src="cid:…"> to data: URIs using inline attachments that carry their bytes. */
function inlineCidImages(html: string, attachments: { contentId: string; contentType: string; contentBase64: string | null }[]): string {
  let out = html;
  for (const a of attachments) {
    if (!a.contentId || !a.contentBase64) { continue; }
    const cid = a.contentId.replace(/^<|>$/g, '');
    const dataUri = `data:${a.contentType};base64,${a.contentBase64}`;
    // Match cid:<id> in src="…" / src='…' regardless of surrounding quotes.
    const re = new RegExp(`cid:${escapeRegExp(cid)}`, 'gi');
    out = out.replace(re, dataUri);
  }
  return out;
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function bytesBlob(base64: string, contentType: string): Blob {
  return new Blob([base64ToBytes(base64.replace(/\s/g, ''))], { type: contentType || 'application/octet-stream' });
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

function safeName(s: string): string {
  return (s ?? '').replace(/[\\/:*?"<>|]+/g, '_').trim().slice(0, 80);
}

/** yyyy-MM-dd for `days` before today (local), for the sinceDate query param. */
function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
