import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { base64ToBytes } from '../shared/document-preview.models';
import { BeaService } from './bea.service';
import {
  BeaAttachment, BeaFolder, BeaFolderNode, beaFolderOrder, BeaMessage, BeaMessageHeader,
  BeaRestriction, BEA_RESTRICTIONS, Postbox,
} from './bea.models';

/** A postbox header or an (indented) folder row in the left navigation. */
type NavRow =
  | { kind: 'postbox'; postbox: Postbox }
  | { kind: 'folder'; safeId: string; folder: BeaFolderNode };

/** The folder tree of one postbox. */
interface PostboxFolders {
  postbox: Postbox;
  nodes: BeaFolderNode[];
}

/**
 * beA module — the web equivalent of the Swing client's BeaInboxPanel (read & triage scope).
 * Three panes: postbox/folder tree, message list (download-restriction presets + sender search),
 * and a reader that shows the beA-specifics a plain mailbox lacks: the two Aktenzeichen (sender /
 * court), the two attachment groups (documents vs. technical/VHN), the message journal, and the
 * signature-verification status. The HTML body (beA bodies are usually plain text) renders in a
 * sandboxed blob iframe (CSP frame-src 'self' blob:). Triage: mark read (auto on open), delete,
 * move. Compose/send, eEB confirm/reject and save-to-case are a later phase. The beA session is
 * established server-side from the user's stored certificate — the browser never sees it. All data
 * comes from /v8/bea/** (BeaService).
 */
@Component({
  selector: 'jl-bea',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="bea" [class.show-reader]="selectedId()">
      <!-- ---------- Left: postboxes + folders ---------- -->
      <aside class="folders">
        <header class="col-head">
          <h1><jl-icon name="shield" [size]="16" /> {{ 'bea.title' | transloco }}</h1>
          <button type="button" class="icon-btn" [disabled]="loginState() !== 'ok'"
                  (click)="refresh()" [title]="'bea.refresh' | transloco" [attr.aria-label]="'bea.refresh' | transloco">
            <jl-icon name="refresh" [size]="15" />
          </button>
        </header>
        <div class="col-body">
          @switch (loginState()) {
            @case ('loading') { <p class="muted pad">{{ 'bea.connecting' | transloco }}</p> }
            @case ('error') {
              <p class="pad">
                {{ 'bea.loginError' | transloco }}
                <button type="button" class="btn-retry" (click)="connect()">{{ 'bea.retry' | transloco }}</button>
              </p>
            }
            @default {
              @if (!postboxes().length) {
                <p class="muted pad">{{ 'bea.noPostboxes' | transloco }}</p>
              } @else {
                @for (row of navRows(); track rowKey(row)) {
                  @if (row.kind === 'postbox') {
                    <div class="mbx" [title]="row.postbox.userName">
                      <jl-icon name="shield" [size]="13" />
                      <span class="mbx-name">{{ row.postbox.label }}</span>
                    </div>
                  } @else {
                    <button type="button" class="fld"
                            [class.sel]="row.safeId === selectedSafeId() && row.folder.id === selectedFolderId()"
                            [style.padding-left.px]="18 + row.folder.depth * 14"
                            (click)="selectFolder(row.safeId, row.folder)">
                      <span class="fld-name">{{ folderLabel(row.folder) }}</span>
                      @if (row.folder.unreadMessageCount > 0) {
                        <span class="fld-badge">{{ row.folder.unreadMessageCount }}</span>
                      }
                    </button>
                  }
                }
              }
            }
          }
        </div>
      </aside>

      <!-- ---------- Middle: message list ---------- -->
      <section class="list">
        <header class="col-head list-head">
          <div class="list-title">
            <h2>{{ selectedFolder() ? folderLabel(selectedFolder()!) : ('bea.selectFolder' | transloco) }}</h2>
            @if (messages()?.length) { <span class="count">{{ messages()!.length }}</span> }
          </div>
          <div class="list-tools">
            <div class="search">
              <jl-icon name="search" [size]="14" />
              <input type="search" [value]="searchInput()" [placeholder]="'bea.searchPlaceholder' | transloco"
                     [disabled]="!selectedFolderId()"
                     (input)="searchInput.set($any($event.target).value)"
                     (keydown.enter)="applySearch()" />
              @if (search()) { <button type="button" class="clear" (click)="clearSearch()" aria-label="clear">✕</button> }
            </div>
            <select class="restrict" [disabled]="!selectedFolderId()"
                    [value]="restrictionId()" (change)="changeRestriction($any($event.target).value)">
              @for (r of restrictions; track r.id) { <option [value]="r.id">{{ r.labelKey | transloco }}</option> }
            </select>
          </div>
        </header>
        <div class="col-body">
          @if (loginState() !== 'ok') {
            <p class="muted pad">{{ 'bea.selectFolderHint' | transloco }}</p>
          } @else if (!selectedFolderId()) {
            <p class="muted pad">{{ 'bea.selectFolderHint' | transloco }}</p>
          } @else if (messagesLoading()) {
            <p class="muted pad">{{ 'bea.loading' | transloco }}</p>
          } @else if (messagesError()) {
            <p class="pad">
              {{ 'bea.listError' | transloco }}
              <button type="button" class="btn-retry" (click)="loadMessages()">{{ 'bea.retry' | transloco }}</button>
            </p>
          } @else if (!messages()?.length) {
            <p class="muted pad">{{ 'bea.empty' | transloco }}</p>
          } @else {
            @for (m of messages()!; track m.id) {
              <button type="button" class="msg" [class.unread]="!m.read" [class.sel]="m.id === selectedId()"
                      (click)="selectMessage(m)">
                @if (!m.read) { <span class="dot" aria-hidden="true"></span> }
                <div class="msg-main">
                  <div class="msg-top">
                    <span class="msg-from">{{ m.sender || ('bea.noSender' | transloco) }}</span>
                    <span class="msg-date">{{ formatDate(m.receptionTime || m.sentTime) }}</span>
                  </div>
                  <div class="msg-sub">
                    @if (m.confidential) { <jl-icon name="shield" [size]="12" /> }
                    @if (m.signed) { <jl-icon name="check" [size]="12" /> }
                    <span class="msg-subject">{{ m.subject || ('bea.noSubject' | transloco) }}</span>
                  </div>
                  @if (m.referenceNumber || m.referenceJustice) {
                    <div class="msg-refs">
                      @if (m.referenceNumber) { <span>{{ 'bea.refSender' | transloco }}: {{ m.referenceNumber }}</span> }
                      @if (m.referenceJustice) { <span>{{ 'bea.refCourt' | transloco }}: {{ m.referenceJustice }}</span> }
                    </div>
                  }
                </div>
              </button>
            }
          }
        </div>
      </section>

      <!-- ---------- Right: reader ---------- -->
      <section class="reader">
        @if (selectedId()) {
          <header class="col-head reader-head">
            <button type="button" class="back" (click)="closeReader()">‹ {{ 'bea.back' | transloco }}</button>
            <div class="reader-actions">
              <button type="button" class="icon-btn" [disabled]="acting() || verifying()" (click)="verify()"
                      [title]="'bea.verify' | transloco"><jl-icon name="shield" [size]="15" /></button>
              <div class="menu-wrap">
                <button type="button" class="icon-btn" [disabled]="acting() || !moveTargets().length"
                        (click)="moveOpen.set(!moveOpen())" [title]="'bea.move' | transloco">
                  <jl-icon name="cases" [size]="15" />
                </button>
                @if (moveOpen()) {
                  <div class="menu" (mouseleave)="moveOpen.set(false)">
                    @for (t of moveTargets(); track t.id) {
                      <button type="button" class="menu-item" (click)="move(t)">{{ folderLabel(t) }}</button>
                    }
                  </div>
                }
              </div>
              <button type="button" class="icon-btn danger" [disabled]="acting()" (click)="remove()"
                      [title]="'bea.delete' | transloco">✕</button>
            </div>
          </header>
          <div class="col-body reader-body">
            @if (messageLoading()) {
              <p class="muted pad">{{ 'bea.loadingMessage' | transloco }}</p>
            } @else if (messageError()) {
              <p class="pad">
                {{ 'bea.messageError' | transloco }}
                <button type="button" class="btn-retry" (click)="reloadMessage()">{{ 'bea.retry' | transloco }}</button>
              </p>
            } @else if (message()) {
              @let msg = message()!;
              <div class="headers">
                <h3 class="subject">{{ msg.subject || ('bea.noSubject' | transloco) }}</h3>
                <div class="badges">
                  @if (msg.confidential) { <span class="badge warn">{{ 'bea.confidential' | transloco }}</span> }
                  @if (msg.eebRequested) { <span class="badge accent">{{ 'bea.eebRequested' | transloco }}</span> }
                  @if (statusBadge(); as sb) { <span class="badge" [class]="sb.cls">{{ sb.label | transloco }}</span> }
                </div>
                <dl class="hdr-grid">
                  <dt>{{ 'bea.from' | transloco }}</dt><dd>{{ msg.senderName || msg.senderSafeId }}</dd>
                  @if (msg.recipients.length) {
                    <dt>{{ 'bea.to' | transloco }}</dt><dd>{{ recipientNames(msg) }}</dd>
                  }
                  <dt>{{ 'bea.date' | transloco }}</dt><dd>{{ formatDateTime(msg.receptionTime || msg.createdTime) }}</dd>
                  @if (msg.referenceNumber) { <dt>{{ 'bea.refSender' | transloco }}</dt><dd>{{ msg.referenceNumber }}</dd> }
                  @if (msg.referenceJustice) { <dt>{{ 'bea.refCourt' | transloco }}</dt><dd>{{ msg.referenceJustice }}</dd> }
                </dl>
              </div>

              <div class="body">
                @if (bodyUrl()) {
                  <iframe class="body-frame" sandbox="allow-popups allow-popups-to-escape-sandbox"
                          [src]="bodyUrl()" [title]="msg.subject"></iframe>
                } @else if (bodyText()) {
                  <pre class="body-text">{{ bodyText() }}</pre>
                } @else {
                  <p class="muted">{{ 'bea.noBody' | transloco }}</p>
                }
              </div>

              @if (verifyUrl()) {
                <div class="atts">
                  <h4>{{ 'bea.verifyResult' | transloco }}</h4>
                  <iframe class="verify-frame" sandbox="" [src]="verifyUrl()" title="verification"></iframe>
                </div>
              }

              @if (documents().length) {
                <div class="atts">
                  <h4>{{ 'bea.documents' | transloco }} ({{ documents().length }})</h4>
                  <ul>
                    @for (a of documents(); track a.name) {
                      <li>
                        <button type="button" class="att" [disabled]="downloadingAtt() === a.name" (click)="download(a)">
                          <jl-icon name="download" [size]="14" />
                          <span class="att-name">{{ a.alias || a.name }}</span>
                          <span class="att-size">{{ formatSize(a.size) }}</span>
                        </button>
                      </li>
                    }
                  </ul>
                </div>
              }

              @if (technical().length) {
                <div class="atts">
                  <h4>{{ 'bea.technical' | transloco }} ({{ technical().length }})</h4>
                  <ul>
                    @for (a of technical(); track a.name) {
                      <li>
                        <button type="button" class="att tech" [disabled]="downloadingAtt() === a.name" (click)="download(a)">
                          <jl-icon name="download" [size]="14" />
                          <span class="att-name">{{ a.alias || a.name }}</span>
                          <span class="att-size">{{ formatSize(a.size) }}</span>
                        </button>
                      </li>
                    }
                  </ul>
                </div>
              }

              @if (msg.journal.length) {
                <div class="atts">
                  <h4>{{ 'bea.journal' | transloco }} ({{ msg.journal.length }})</h4>
                  <div class="rpt-scroll">
                    <table class="jtable">
                      <thead>
                        <tr>
                          <th>{{ 'bea.jEvent' | transloco }}</th>
                          <th>{{ 'bea.jFrom' | transloco }}</th>
                          <th>{{ 'bea.jWhen' | transloco }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (j of msg.journal; track $index) {
                          <tr>
                            <td>{{ journalEvent(j.eventType) }}</td>
                            <td>{{ j.fromSurnameFirstname || j.fromUsername }}</td>
                            <td>{{ formatDateTime(j.timestamp) }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                </div>
              }
            }
          </div>
        } @else {
          <p class="empty reader-empty">{{ 'bea.selectMessageHint' | transloco }}</p>
        }
      </section>
    </div>
  `,
  styleUrl: './bea.component.css',
})
export class BeaComponent {
  private readonly api = inject(BeaService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly transloco = inject(TranslocoService);

  protected readonly restrictions = BEA_RESTRICTIONS;

  // beA session (login state, postboxes, folders) is cached in the service and survives view
  // open/close; the component derives the folder tree. Only the message list is component-local.
  protected readonly loginState = this.api.sessionState;
  protected readonly postboxes = this.api.postboxes;

  protected readonly selectedSafeId = signal<string | null>(null);
  protected readonly selectedFolderId = signal<number | null>(null);

  protected readonly messages = signal<BeaMessageHeader[] | null>(null);
  protected readonly messagesLoading = signal(false);
  protected readonly messagesError = signal(false);
  protected readonly restrictionId = signal<string>(BEA_RESTRICTIONS[0].id);
  protected readonly searchInput = signal('');
  protected readonly search = signal('');

  protected readonly selectedId = signal<string | null>(null);
  protected readonly message = signal<BeaMessage | null>(null);
  protected readonly messageLoading = signal(false);
  protected readonly messageError = signal(false);
  protected readonly bodyUrl = signal<SafeResourceUrl | null>(null);
  protected readonly bodyText = signal('');
  protected readonly verifyUrl = signal<SafeResourceUrl | null>(null);
  protected readonly verifying = signal(false);
  protected readonly acting = signal(false);
  protected readonly moveOpen = signal(false);
  protected readonly downloadingAtt = signal<string | null>(null);

  private msgSeq = 0;
  private bodyBlobUrl: string | null = null;
  private verifyBlobUrl: string | null = null;

  /** Folder trees derived from the cached (flat) folder lists — one per postbox, in order. */
  private readonly folderTrees = computed<PostboxFolders[]>(() => {
    const raw = this.api.folders();
    return this.postboxes().map((pb) => ({ postbox: pb, nodes: buildTree(raw[pb.safeId] ?? []) }));
  });

  protected readonly navRows = computed<NavRow[]>(() => {
    const rows: NavRow[] = [];
    for (const pf of this.folderTrees()) {
      rows.push({ kind: 'postbox', postbox: pf.postbox });
      flatten(pf.nodes, (f) => rows.push({ kind: 'folder', safeId: pf.postbox.safeId, folder: f }));
    }
    return rows;
  });

  protected readonly selectedFolder = computed<BeaFolderNode | null>(() => {
    const sid = this.selectedSafeId(); const fid = this.selectedFolderId();
    if (!sid || fid == null) { return null; }
    const pf = this.folderTrees().find((x) => x.postbox.safeId === sid);
    return pf ? findFolder(pf.nodes, fid) : null;
  });

  protected readonly moveTargets = computed<BeaFolderNode[]>(() => {
    const sid = this.selectedSafeId(); const fid = this.selectedFolderId();
    const pf = this.folderTrees().find((x) => x.postbox.safeId === sid);
    if (!pf) { return []; }
    const flat: BeaFolderNode[] = [];
    flatten(pf.nodes, (f) => flat.push(f));
    return flat.filter((f) => f.id !== fid);
  });

  /** Real documents (non-technical attachments). */
  protected readonly documents = computed(() => (this.message()?.attachments ?? []).filter((a) => !a.technicalAttachment));
  /** Technical attachments + VHN (signatures, structured data). */
  protected readonly technical = computed(() => [
    ...(this.message()?.attachments ?? []).filter((a) => a.technicalAttachment),
    ...(this.message()?.vhnAttachments ?? []),
  ]);

  /** Signature-verification badge derived from the message's verificationStatus. */
  protected readonly statusBadge = computed<{ label: string; cls: string } | null>(() => {
    const s = (this.message()?.verificationStatus ?? '').toUpperCase();
    if (!s) { return null; }
    if (s.includes('SUCCESS')) { return { label: 'bea.sigSuccess', cls: 'ok' }; }
    if (s.includes('PARTIAL')) { return { label: 'bea.sigPartial', cls: 'warn' }; }
    if (s.includes('FAIL')) { return { label: 'bea.sigFailed', cls: 'bad' }; }
    return null;
  });

  private autoSelected = false;

  constructor() {
    // Establish the session only if not already cached; the effect opens the first inbox as soon
    // as the (cached or freshly fetched) tree is available — once per component instance.
    this.api.ensureSession();
    effect(() => {
      const trees = this.folderTrees();
      if (!this.autoSelected && trees.length && this.selectedFolderId() == null) {
        this.autoSelected = true;
        this.autoSelectInbox();
      }
    });
    inject(DestroyRef).onDestroy(() => { this.revokeBody(); this.revokeVerify(); });
  }

  /** Re-establishes the beA session (used by the error-state retry). */
  protected connect(): void {
    this.api.ensureSession(true);
  }

  private autoSelectInbox(): void {
    if (this.selectedFolderId() != null) { return; }
    for (const pf of this.folderTrees()) {
      const inbox = firstMatch(pf.nodes, (f) => f.type === 'INBOX') ?? pf.nodes[0];
      if (inbox) { this.selectFolder(pf.postbox.safeId, inbox); return; }
    }
  }

  protected selectFolder(safeId: string, folder: BeaFolderNode): void {
    this.selectedSafeId.set(safeId);
    this.selectedFolderId.set(folder.id);
    this.search.set('');
    this.searchInput.set('');
    this.closeReader();
    this.loadMessages();
  }

  protected loadMessages(): void {
    const sid = this.selectedSafeId(); const fid = this.selectedFolderId();
    if (!sid || fid == null) { return; }
    this.messagesLoading.set(true);
    this.messagesError.set(false);
    this.api.searchMessages(sid, fid, this.currentRestriction(), this.search() || undefined).subscribe({
      next: (rows) => { this.messages.set(rows); this.messagesLoading.set(false); },
      error: () => { this.messagesError.set(true); this.messagesLoading.set(false); },
    });
  }

  protected changeRestriction(id: string): void { this.restrictionId.set(id); this.loadMessages(); }
  protected applySearch(): void { this.search.set(this.searchInput().trim()); this.loadMessages(); }
  protected clearSearch(): void { this.search.set(''); this.searchInput.set(''); this.loadMessages(); }

  protected selectMessage(m: BeaMessageHeader): void {
    this.selectedId.set(m.id);
    this.reloadMessage();
    if (!m.read) {
      this.patchMessage(m.id, { read: true });
      this.bumpUnread(-1);
      this.api.markRead(this.selectedSafeId()!, m.id).subscribe({ error: () => {} });
    }
  }

  protected reloadMessage(): void {
    const sid = this.selectedSafeId(); const id = this.selectedId();
    if (!sid || !id) { return; }
    const seq = ++this.msgSeq;
    this.messageLoading.set(true);
    this.messageError.set(false);
    this.message.set(null);
    this.resetBody();
    this.api.getMessage(sid, id, true).subscribe({
      next: (msg) => {
        if (seq !== this.msgSeq) { return; }
        this.message.set(msg);
        this.renderBody(msg);
        this.messageLoading.set(false);
      },
      error: () => {
        if (seq !== this.msgSeq) { return; }
        this.messageError.set(true);
        this.messageLoading.set(false);
      },
    });
  }

  /** Renders the body: HTML in a sandboxed blob iframe (no remote loads), plain text in a <pre>. */
  private renderBody(msg: BeaMessage): void {
    const body = msg.body ?? '';
    if (!/<[a-z!][\s\S]*>/i.test(body)) {
      this.bodyText.set(body);
      return;
    }
    this.bodyBlobUrl = URL.createObjectURL(new Blob([sandboxDoc(body)], { type: 'text/html' }));
    this.bodyUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.bodyBlobUrl));
  }

  protected verify(): void {
    const sid = this.selectedSafeId(); const id = this.selectedId();
    if (!sid || !id) { return; }
    this.verifying.set(true);
    this.api.verify(sid, id).subscribe({
      next: (res) => {
        this.message.update((m) => (m ? { ...m, verificationStatus: res.status } : m));
        this.revokeVerify();
        if (res.html) {
          this.verifyBlobUrl = URL.createObjectURL(new Blob([sandboxDoc(res.html)], { type: 'text/html' }));
          this.verifyUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.verifyBlobUrl));
        }
        this.verifying.set(false);
      },
      error: () => this.verifying.set(false),
    });
  }

  protected move(target: BeaFolderNode): void {
    const sid = this.selectedSafeId(); const id = this.selectedId();
    if (!sid || !id) { return; }
    this.moveOpen.set(false);
    this.acting.set(true);
    this.api.moveMessage(sid, id, target.id).subscribe({
      next: () => { this.acting.set(false); this.removeFromList(id); this.closeReader(); },
      error: () => this.acting.set(false),
    });
  }

  protected remove(): void {
    const sid = this.selectedSafeId(); const id = this.selectedId();
    if (!sid || !id) { return; }
    this.acting.set(true);
    this.api.deleteMessage(sid, id).subscribe({
      next: () => { this.acting.set(false); this.removeFromList(id); this.closeReader(); },
      error: () => this.acting.set(false),
    });
  }

  protected download(a: BeaAttachment): void {
    const sid = this.selectedSafeId(); const id = this.selectedId();
    if (!sid || !id) { return; }
    if (a.content) {
      triggerDownload(bytesBlob(a.content), a.alias || a.name);
      return;
    }
    this.downloadingAtt.set(a.name);
    this.api.getAttachment(sid, id, a.name).subscribe({
      next: (full) => {
        if (full.content) { triggerDownload(bytesBlob(full.content), full.alias || full.name || a.name); }
        this.downloadingAtt.set(null);
      },
      error: () => this.downloadingAtt.set(null),
    });
  }

  protected refresh(): void {
    this.api.ensureSession(true);
    if (this.selectedFolderId() != null) { this.loadMessages(); }
  }

  protected closeReader(): void {
    this.msgSeq++;
    this.selectedId.set(null);
    this.message.set(null);
    this.moveOpen.set(false);
    this.resetBody();
  }

  // ----- helpers -----

  protected rowKey(row: NavRow): string {
    return row.kind === 'postbox' ? `p:${row.postbox.safeId}` : `f:${row.safeId}:${row.folder.id}`;
  }

  protected folderLabel(f: BeaFolder): string {
    const key = folderTypeKey(f.type);
    return key ? this.transloco.translate(key) : f.name;
  }

  protected recipientNames(msg: BeaMessage): string {
    return msg.recipients.map((r) => r.name || r.safeId).filter(Boolean).join(', ');
  }

  /**
   * Human-readable journal event name, mirroring the desktop client's
   * BeaJournalEventTypes.getDisplayName: a localized name for the known event types, else a
   * generic fallback (drop the MESSAGE_ prefix, underscores → spaces, capitalize).
   */
  protected journalEvent(eventType: string): string {
    if (!eventType) { return ''; }
    const key = `bea.event.${eventType.toUpperCase()}`;
    const translated = this.transloco.translate(key);
    if (translated && translated !== key) { return translated; }
    let r = eventType;
    if (r.toUpperCase().startsWith('MESSAGE_')) { r = r.substring(8); }
    r = r.replace(/_/g, ' ').toLowerCase().trim();
    return r ? r.charAt(0).toUpperCase() + r.slice(1) : '';
  }

  private currentRestriction(): BeaRestriction {
    return this.restrictions.find((r) => r.id === this.restrictionId()) ?? this.restrictions[0];
  }

  private patchMessage(id: string, patch: Partial<BeaMessageHeader>): void {
    this.messages.update((list) => list?.map((m) => (m.id === id ? { ...m, ...patch } : m)) ?? list);
  }

  private removeFromList(id: string): void {
    this.messages.update((list) => list?.filter((m) => m.id !== id) ?? list);
  }

  private bumpUnread(delta: number): void {
    const sid = this.selectedSafeId(); const fid = this.selectedFolderId();
    if (!sid || fid == null) { return; }
    this.api.adjustUnread(sid, fid, delta);
  }

  protected formatDate(iso: string | null): string {
    if (!iso) { return ''; }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) { return ''; }
    const sameDay = d.toDateString() === new Date().toDateString();
    return new Intl.DateTimeFormat(this.transloco.getActiveLang(), sameDay
      ? { hour: '2-digit', minute: '2-digit' }
      : { day: '2-digit', month: '2-digit', year: '2-digit' }).format(d);
  }

  protected formatDateTime(iso: string | null): string {
    if (!iso) { return ''; }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) { return ''; }
    return new Intl.DateTimeFormat(this.transloco.getActiveLang(), { dateStyle: 'medium', timeStyle: 'short' }).format(d);
  }

  protected formatSize(bytes: number): string {
    if (!bytes) { return ''; }
    if (bytes < 1024) { return `${bytes} B`; }
    if (bytes < 1024 * 1024) { return `${Math.round(bytes / 1024)} KB`; }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private resetBody(): void {
    this.revokeBody();
    this.revokeVerify();
    this.bodyUrl.set(null);
    this.bodyText.set('');
    this.verifyUrl.set(null);
  }

  private revokeBody(): void {
    if (this.bodyBlobUrl) { URL.revokeObjectURL(this.bodyBlobUrl); this.bodyBlobUrl = null; }
  }

  private revokeVerify(): void {
    if (this.verifyBlobUrl) { URL.revokeObjectURL(this.verifyBlobUrl); this.verifyBlobUrl = null; }
  }
}

// ---------- module-scope helpers ----------

function buildTree(flat: BeaFolder[]): BeaFolderNode[] {
  const byId = new Map<number, BeaFolderNode>();
  for (const f of flat) {
    byId.set(f.id, { ...f, children: [], depth: 0 });
  }
  const roots: BeaFolderNode[] = [];
  for (const node of byId.values()) {
    const parent = node.parentId ? byId.get(node.parentId) : undefined;
    if (parent) { parent.children.push(node); } else { roots.push(node); }
  }
  const sortRec = (nodes: BeaFolderNode[], depth: number) => {
    nodes.sort((a, b) => beaFolderOrder(a.type) - beaFolderOrder(b.type) || a.name.localeCompare(b.name));
    for (const n of nodes) { n.depth = depth; sortRec(n.children, depth + 1); }
  };
  sortRec(roots, 0);
  return roots;
}

function flatten(nodes: BeaFolderNode[], visit: (n: BeaFolderNode) => void): void {
  for (const n of nodes) { visit(n); flatten(n.children, visit); }
}

function findFolder(nodes: BeaFolderNode[], id: number): BeaFolderNode | null {
  for (const n of nodes) {
    if (n.id === id) { return n; }
    const found = findFolder(n.children, id);
    if (found) { return found; }
  }
  return null;
}

function firstMatch(nodes: BeaFolderNode[], pred: (n: BeaFolderNode) => boolean): BeaFolderNode | null {
  for (const n of nodes) {
    if (pred(n)) { return n; }
    const found = firstMatch(n.children, pred);
    if (found) { return found; }
  }
  return null;
}

/** i18n key for a well-known folder type, or '' to fall back to the server-provided name. */
function folderTypeKey(type: string): string {
  switch (type) {
    case 'INBOX': return 'bea.folder.inbox';
    case 'SENT': return 'bea.folder.sent';
    case 'DRAFT': return 'bea.folder.drafts';
    case 'OUTBOX': return 'bea.folder.outbox';
    case 'TRASH': return 'bea.folder.trash';
    default: return '';
  }
}

/** Wraps untrusted HTML in a strict-CSP, script-disabled document for a sandboxed blob iframe. */
function sandboxDoc(html: string): string {
  return '<!doctype html><html><head><meta charset="utf-8">' +
    '<meta http-equiv="Content-Security-Policy" ' +
    `content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:">` +
    '<base target="_blank">' +
    '<style>html,body{margin:0;padding:12px;font:14px/1.5 system-ui,-apple-system,Segoe UI,Roboto,sans-serif;' +
    'color:#16232e;word-break:break-word;} img{max-width:100%;height:auto;} a{color:#0b5cad;} table{border-collapse:collapse;}' +
    'td,th{border:1px solid #ccc;padding:4px 8px;}</style>' +
    `</head><body>${html}</body></html>`;
}

function bytesBlob(base64: string): Blob {
  return new Blob([base64ToBytes(base64.replace(/\s/g, ''))], { type: 'application/octet-stream' });
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'download';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
