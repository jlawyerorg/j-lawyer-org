import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentPreviewComponent } from '../shared/document-preview.component';
import { DocumentContentService } from '../shared/document-content.service';
import { fileKind, kindGlyph, PreviewDoc, previewKindOf } from '../shared/document-preview.models';
import { firstValueFrom, forkJoin, map } from 'rxjs';
import { CaseFilter, CasesService } from './cases.service';
import { PinsService } from '../shell/pins.service';
import { CaseEditorComponent } from './case-editor.component';
import { PartyAddComponent } from './party-add.component';
import { PartyEditorComponent } from './party-editor.component';
import { EventEditorComponent } from '../calendar/event-editor.component';
import { CalendarService } from '../calendar/calendar.service';
import { CalendarEvent, CalendarEventType, CaseRef, EventDraft } from '../calendar/calendar.models';
import { AuthService } from '../core/auth/auth.service';
import {
  AccountEntry, CaseDetail, CaseDocument, CaseGroup, CaseHistoryEntry, CaseInvoice, CaseMessage, CasePayment,
  CaseTag, CaseTimesheet, CaseWrite, DocFolder, DocSortKey, DueDate, MultiValueTagDef, Party, PartyUpdate, TimesheetPosition,
} from './case.models';

/** A row in the document folder tree, flattened for display with indentation + a doc count. */
interface DocFolderRow {
  id: string;
  name: string;
  depth: number;
  count: number;
  isRoot: boolean;
}

type CaseTab = 'overview' | 'documents' | 'parties' | 'deadlines' | 'finance' | 'zeiten' | 'history';
/** Sub-view within the finance tab (invoices, payments and the case account are too much for one screen). */
type FinanceView = 'invoices' | 'payments' | 'account';
/** Status filter for the Zeiten tab (show all timesheets, only open, or only closed ones). */
type ZeitenFilter = 'all' | 'open' | 'closed';
/** A timesheet with its loaded positions (the "Zeiten" tab lists positions per timesheet). */
interface TimesheetView extends CaseTimesheet {
  positions: TimesheetPosition[];
}

/**
 * Akten (cases) module — responsive master-detail (design-mockup.html): searchable case
 * list + case detail (overview with parties, deadlines, recent documents, note). Data comes
 * from the real REST API via CasesService. The list is filtered/searched and paginated
 * server-side (infinite scroll via {@link CasesService.loadMore}); the detail is fetched
 * lazily per selection.
 */
@Component({
  selector: 'jl-akten',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe, DecimalPipe, DocumentPreviewComponent,
    CaseEditorComponent, PartyAddComponent, PartyEditorComponent, EventEditorComponent],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <!-- Aktenliste -->
      <section class="list">
        <header class="list-head">
          <h1>{{ 'akten.title' | transloco }}</h1>
          <span class="count">{{ cases.total() }}</span>
          <button type="button" class="btn-primary">
            <jl-icon name="plus" [size]="14" />{{ 'akten.new' | transloco }}
          </button>
        </header>

        <div class="filters" role="tablist">
          @for (f of filters; track f) {
            <button type="button" class="chip" [class.on]="filter() === f" (click)="setFilter(f)">
              {{ 'akten.filter.' + f | transloco }}
            </button>
          }
        </div>

        <label class="search">
          <jl-icon name="search" [size]="14" />
          <input type="search" [placeholder]="'akten.searchCases' | transloco"
                 [value]="search()" (input)="onSearch($any($event.target).value)" />
        </label>

        <div class="rows" (scroll)="onScroll($event)">
          @if (cases.listLoading() && cases.overviews().length === 0) {
            <p class="muted loading">{{ 'akten.loading' | transloco }}</p>
          } @else if (cases.listError() && cases.overviews().length === 0) {
            <p class="empty">
              {{ 'akten.error' | transloco }}
              <button type="button" class="btn-retry" (click)="cases.reload()">{{ 'akten.retry' | transloco }}</button>
            </p>
          } @else {
            @for (c of cases.overviews(); track c.id) {
              <button type="button" class="row" [class.sel]="c.id === selectedId()" (click)="open(c.id)">
                <span class="az">{{ c.fileNumber }}</span>
                <span class="name">{{ c.name }}</span>
                <span class="sub">{{ c.subjectField || c.reason || '—' }}{{ c.lawyer ? ' · ' + c.lawyer : '' }}</span>
                <span class="r-right">
                  <span class="pill" [class]="c.status">{{ 'akten.status.' + c.status | transloco }}</span>
                  <span class="date">{{ c.lastChanged | date: 'dd.MM.yy' }}</span>
                </span>
              </button>
            } @empty {
              <p class="empty">{{ 'akten.empty' | transloco }}</p>
            }
            @if (cases.listLoading() && cases.overviews().length > 0) {
              <p class="muted loading more">{{ 'akten.loadingMore' | transloco }}</p>
            }
          }
        </div>
      </section>

      <!-- Aktendetail -->
      <section class="detail">
        @if (detailLoading()) {
          <p class="muted detail-empty">{{ 'akten.loading' | transloco }}</p>
        } @else {
          @if (selected(); as c) {
          <div class="detail-head">
            <button type="button" class="back" (click)="clearSelection()">‹ {{ 'akten.back' | transloco }}</button>
            <div class="crumbs">{{ 'akten.title' | transloco }} › <b>{{ c.fileNumber }}</b></div>
            <div class="title-row">
              <h2>{{ c.name }}</h2>
              <span class="pill" [class]="c.status">{{ 'akten.status.' + c.status | transloco }}</span>
              <button type="button" class="pin-toggle" [class.on]="pins.isPinned('case', c.id)"
                      (click)="togglePin(c)"
                      [title]="(pins.isPinned('case', c.id) ? 'pins.unpinItem' : 'pins.pinItem') | transloco">
                <jl-icon name="pushpin" [size]="16" />
              </button>
              <span class="head-actions">
                <button type="button" class="hbtn" (click)="openEditCase()" [title]="'akten.editCase' | transloco">
                  <jl-icon name="edit" [size]="15" /> {{ 'akten.edit' | transloco }}
                </button>
              </span>
            </div>
            <div class="meta">
              <span><span class="k">{{ 'akten.meta.fileNumber' | transloco }}</span> <b>{{ c.fileNumber }}</b></span>
              <span><span class="k">{{ 'akten.meta.subjectField' | transloco }}</span> <b>{{ c.subjectField || '—' }}</b></span>
              <span><span class="k">{{ 'akten.meta.lawyer' | transloco }}</span> <b>{{ c.lawyer || '—' }}</b></span>
              <span><span class="k">{{ 'akten.meta.claimValue' | transloco }}</span> <b>{{ c.claimValue | number: '1.2-2' }} €</b></span>
            </div>
            <div class="tabs" role="tablist">
              @for (t of tabs; track t) {
                <button type="button" class="tab" [class.on]="activeTab() === t" (click)="selectTab(t)">
                  {{ 'akten.tabs.' + t | transloco }}
                </button>
              }
            </div>
          </div>

          <div class="detail-body">
            @if (activeTab() === 'overview') {
              <div class="grid">
                <!-- Notiz (weiter oben, meist wichtig) -->
                @if (c.notice) {
                  <div class="card full">
                    <div class="card-h"><h3>{{ 'akten.note' | transloco }}</h3></div>
                    <div class="card-b"><p class="note">{{ c.notice }}</p></div>
                  </div>
                }

                <!-- Grund & Verantwortliche -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.about' | transloco }}</h3></div>
                  <div class="card-b">
                    <dl class="kv">
                      <dt>{{ 'akten.meta.reason' | transloco }}</dt><dd>{{ c.reason || '—' }}</dd>
                      <dt>{{ 'akten.meta.lawyer' | transloco }}</dt><dd>{{ c.lawyer || '—' }}</dd>
                      <dt>{{ 'akten.meta.assistant' | transloco }}</dt><dd>{{ c.assistant || '—' }}</dd>
                    </dl>
                  </div>
                </div>

                <!-- Etiketten -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.tags' | transloco }}</h3></div>
                  <div class="card-b">
                    @if (caseTags() === null) {
                      <p class="muted">{{ 'akten.loading' | transloco }}</p>
                    } @else {
                      <div class="chips">
                        @for (def of multiTagDefs(); track def.tagName) {
                          <span class="chip list-chip">
                            <span class="lc-name">{{ def.tagName }}</span>
                            <select #mtSel class="lc-select" (change)="setMultiTag(def, mtSel.value)">
                              <option value="" [selected]="!multiTagValue(def)">—</option>
                              @for (v of def.values; track v) {
                                <option [value]="v" [selected]="multiTagValue(def) === v">{{ v }}</option>
                              }
                            </select>
                          </span>
                        }
                        @for (t of singleTags(); track t.id) {
                          <span class="chip removable">{{ t.name }}
                            <button type="button" class="chip-x" (click)="removeTag(t)" [title]="'akten.tag.remove' | transloco">✕</button>
                          </span>
                        }
                        @if (!singleTags().length && !multiTagDefs().length) {
                          <span class="muted">{{ 'akten.noTags' | transloco }}</span>
                        }
                      </div>
                      @if (availableTags().length) {
                        <select class="add-select" #tagSel (change)="addTag(tagSel.value); tagSel.value=''">
                          <option value="">+ {{ 'akten.tag.add' | transloco }}</option>
                          @for (t of availableTags(); track t) { <option [value]="t">{{ t }}</option> }
                        </select>
                      }
                    }
                  </div>
                </div>

                <!-- Beteiligte -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.parties' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (p of c.parties; track p.id) {
                      <div class="party">
                        <span class="pa" [style.background]="p.color || null" [style.color]="p.color ? contrastOn(p.color) : null">{{ initials(p.contact || p.involvementType) }}</span>
                        <span>
                          <span class="role">{{ p.involvementType || '—' }}</span>
                          <span class="nm">{{ p.contact || '—' }}</span>
                          @if (p.reference) { <span class="ref">{{ 'akten.party.reference' | transloco }}: {{ p.reference }}</span> }
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noParties' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Berechtigungen -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.permissions' | transloco }}</h3></div>
                  <div class="card-b">
                    @if (caseGroups() === null) {
                      <p class="muted">{{ 'akten.loading' | transloco }}</p>
                    } @else {
                      <div class="chips">
                        @for (g of caseGroups(); track g.id) {
                          <span class="chip removable">{{ g.name }}
                            <button type="button" class="chip-x" (click)="removeAllowedGroup(g)" [title]="'akten.perm.remove' | transloco">✕</button>
                          </span>
                        } @empty {
                          <span class="muted">{{ 'akten.noPermissions' | transloco }}</span>
                        }
                      </div>
                      @if (availableGroups().length) {
                        <select class="add-select" #grpSel (change)="addAllowedGroup(grpSel.value); grpSel.value=''">
                          <option value="">+ {{ 'akten.perm.add' | transloco }}</option>
                          @for (g of availableGroups(); track g.id) { <option [value]="g.id">{{ g.name }}</option> }
                        </select>
                      }
                    }
                  </div>
                </div>

                <!-- Fristen & Wiedervorlagen (überfällig + demnächst, max. 10) -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.deadlines' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (d of overviewDueDates(); track d.id) {
                      <div class="frist" [class.overdue]="isOverdue(d)">
                        <span class="bar" [class.deadline]="d.type === 'deadline'"></span>
                        <span class="fdate">{{ d.dueDate | date: 'dd.MM.' }}</span>
                        <span class="fx">
                          <span class="ft">{{ d.reason }}</span>
                          <span class="fs">{{ d.assignee }} · {{ 'akten.dueType.' + d.type | transloco }}</span>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noDeadlines' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Instant-Nachrichten -->
                <div class="card">
                  <div class="card-h">
                    <h3>{{ 'akten.messages' | transloco }}</h3>
                    @if (caseMessages()?.length) { <span class="card-count">{{ caseMessages()?.length }}</span> }
                  </div>
                  <div class="card-b">
                    @if (caseMessages() === null) {
                      <p class="muted">{{ 'akten.loading' | transloco }}</p>
                    } @else {
                      <form class="composer" (submit)="$event.preventDefault(); sendMessage()">
                        <input type="text" [value]="messageDraft()" (input)="messageDraft.set($any($event.target).value)"
                               [placeholder]="'akten.msg.placeholder' | transloco" />
                        <button type="submit" class="send-btn" [disabled]="!messageDraft().trim()">{{ 'akten.msg.send' | transloco }}</button>
                      </form>
                      <div class="chat">
                        @for (m of chatMessages(); track m.id) {
                          <div class="bubble" [class.mine]="m.sender === currentUser()">
                            <span class="bubble-meta">{{ m.sender }} · {{ m.sent | date: 'dd.MM.yyyy HH:mm' }}</span>
                            <span class="bubble-text">{{ m.content }}</span>
                          </div>
                        } @empty {
                          <p class="muted">{{ 'akten.noMessages' | transloco }}</p>
                        }
                      </div>
                    }
                  </div>
                </div>

                <!-- Letzte Dokumente (max. 20) -->
                <div class="card full">
                  <div class="card-h"><h3>{{ 'akten.recentDocs' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (doc of overviewDocuments(); track doc.id) {
                      <div class="doc">
                        <span class="ftype" [class]="'kind-' + docKind(doc)" [title]="doc.ext || ''">
                          <jl-icon [name]="kindIcon(docKind(doc))" [size]="16" />
                          <span class="ext-lbl">{{ doc.ext || '—' }}</span>
                        </span>
                        <span>
                          <span class="dn">{{ doc.name }}</span>
                          <span class="dmeta">{{ doc.date | date: 'dd.MM.yyyy' }}</span>
                        </span>
                        <span class="dsz">{{ doc.size }}</span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noDocs' | transloco }}</p>
                    }
                  </div>
                </div>
              </div>
            } @else if (activeTab() === 'documents') {
              <div class="docs" [class.m-folders]="docMobilePane() === 'folders'" [class.m-list]="docMobilePane() === 'list'">
                <!-- Folder tree (multi-select; default all selected, like the Swing client) -->
                <aside class="doc-folders">
                  <header class="col-head">
                    <h3>{{ 'akten.docs.folders' | transloco }}</h3>
                    <button type="button" class="all-toggle" (click)="toggleAllFolders()">
                      {{ (allFoldersSelected() ? 'akten.docs.selectNone' : 'akten.docs.selectAll') | transloco }}
                    </button>
                    <button type="button" class="to-list" (click)="docMobilePane.set('list')">
                      {{ 'akten.docs.show' | transloco }} ({{ visibleDocs().length }}) ›
                    </button>
                  </header>
                  <div class="col-body">
                    @for (f of docFolderRows(); track f.id) {
                      <button type="button" class="fld" [class.sel]="isFolderSelected(f.id)"
                              [style.padding-left.px]="10 + f.depth * 14" (click)="toggleFolder(f.id)">
                        <span class="fld-box" [class.on]="isFolderSelected(f.id)">
                          @if (isFolderSelected(f.id)) { <jl-icon name="check" [size]="11" /> }
                        </span>
                        <jl-icon name="folder" [size]="14" />
                        <span class="fld-name">{{ f.isRoot ? ('akten.docs.root' | transloco) : f.name }}</span>
                        @if (f.count > 0) { <span class="fld-badge">{{ f.count }}</span> }
                      </button>
                    }
                  </div>
                </aside>

                <!-- Document list -->
                <section class="doc-list">
                  <header class="col-head doc-list-head">
                    <div class="dl-title">
                      <button type="button" class="to-folders" (click)="docMobilePane.set('folders')">‹ {{ 'akten.docs.folders' | transloco }}</button>
                      @if (singleSelectedFolder(); as sf) {
                        <h3>{{ sf.isRoot ? ('akten.docs.root' | transloco) : sf.name }}</h3>
                      } @else if (allFoldersSelected()) {
                        <h3>{{ 'akten.docs.allFolders' | transloco }}</h3>
                      } @else {
                        <h3>{{ 'akten.docs.nFolders' | transloco: { n: docFolderSel().size } }}</h3>
                      }
                      <span class="count">{{ visibleDocs().length }}</span>
                    </div>
                    <div class="dl-search">
                      <jl-icon name="search" [size]="14" />
                      <input type="search" [value]="docSearch()" [placeholder]="'akten.docs.searchPlaceholder' | transloco"
                             (input)="docSearch.set($any($event.target).value)" />
                      @if (docSearch()) { <button type="button" class="clear" (click)="clearDocSearch()" aria-label="clear">✕</button> }
                    </div>
                    <div class="dl-tools">
                      <span class="sort-label">{{ 'akten.docs.sortBy' | transloco }}</span>
                      @for (s of sortKeys; track s) {
                        <button type="button" class="sort-btn" [class.on]="cases.docSort().key === s" (click)="toggleSort(s)">
                          {{ 'akten.docs.sort.' + s | transloco }}{{ sortArrow(s) }}
                        </button>
                      }
                      <button type="button" class="date-toggle" (click)="toggleDateMode()" [title]="'akten.docs.dateModeHint' | transloco">
                        {{ (cases.docDateMode() === 'change' ? 'akten.docs.dateChange' : 'akten.docs.dateCreation') | transloco }}
                      </button>
                    </div>
                  </header>
                  <div class="col-body">
                    <div class="dropzone" [class.over]="dragOver()" [class.busy]="docUploading()"
                         (dragover)="onDragOver($event)" (dragleave)="onDragLeave()" (drop)="onDrop($event)"
                         (click)="fileInput.click()">
                      <jl-icon name="download" [size]="18" />
                      <span>{{ (docUploading() ? 'akten.docs.uploading' : uploadHint()) | transloco: { folder: uploadTargetName() } }}</span>
                    </div>
                    <input #fileInput type="file" multiple hidden (change)="onUpload($event)" />
                    @if (docUploadError()) { <p class="up-error">{{ 'akten.docs.uploadError' | transloco }}</p> }
                    @for (doc of visibleDocs(); track doc.id) {
                      <div class="doc doc-row" [class.previewable]="canPreview(doc)"
                           (click)="canPreview(doc) ? preview(doc) : download(doc)">
                        <span class="hl-stripe" [class.single]="!(doc.highlight1 && doc.highlight2)">
                          @if (doc.highlight1) { <span [style.background]="doc.highlight1"></span> }
                          @if (doc.highlight2) { <span [style.background]="doc.highlight2"></span> }
                        </span>
                        <span class="ftype" [class]="'kind-' + docKind(doc)" [title]="doc.ext || ''">
                          <jl-icon [name]="kindIcon(docKind(doc))" [size]="16" />
                          <span class="ext-lbl">{{ doc.ext || '—' }}</span>
                        </span>
                        <span class="doc-main">
                          <span class="dn">
                            @if (doc.favorite) { <jl-icon class="fav" name="star" [size]="13" /> }
                            <span class="dn-name">{{ doc.name }}</span>
                          </span>
                          <span class="dmeta">
                            {{ (cases.docDateMode() === 'change' ? doc.changeDate : doc.date) | date: 'dd.MM.yyyy' }} · {{ doc.size }}@if (doc.version > 1) { · v{{ doc.version }} }@if (showFolderColumn() && docFolderLabel(doc)) { · <span class="dfolder"><jl-icon name="folder" [size]="11" /> {{ docFolderLabel(doc) }}</span> }
                          </span>
                          @if (doc.tags.length) {
                            <span class="dtags">
                              @for (t of doc.tags; track t) { <span class="dtag">{{ t }}</span> }
                            </span>
                          }
                        </span>
                        <span class="doc-actions">
                          @if (canPreview(doc)) {
                            <button type="button" class="doc-btn" (click)="$event.stopPropagation(); preview(doc)">
                              {{ 'akten.docPreview' | transloco }}
                            </button>
                          }
                          <button type="button" class="doc-btn primary" (click)="$event.stopPropagation(); download(doc)">
                            <jl-icon name="download" [size]="14" />{{ 'akten.docDownload' | transloco }}
                          </button>
                          <button type="button" class="doc-btn danger" [disabled]="docDeleting() === doc.id"
                                  (click)="$event.stopPropagation(); confirmDeleteDoc(doc)" [title]="'akten.docs.delete' | transloco">
                            <jl-icon name="trash" [size]="14" />
                          </button>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted pad">{{ (docSearch() ? 'akten.docs.noResults' : 'akten.noDocs') | transloco }}</p>
                    }
                  </div>
                </section>
              </div>
            } @else if (activeTab() === 'parties') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.parties' | transloco }}</h3>
                  <span class="card-count">{{ c.parties.length }}</span>
                  <span class="ch-actions">
                    <button type="button" class="add-btn" (click)="partyAdding.set(true)">
                      <jl-icon name="plus" [size]="14" /> {{ 'akten.party.add' | transloco }}
                    </button>
                  </span>
                </div>
                <div class="card-b">
                  @for (p of c.parties; track p.id) {
                    <div class="party">
                      <span class="pa" [style.background]="p.color || null" [style.color]="p.color ? contrastOn(p.color) : null">{{ initials(p.contact || p.involvementType) }}</span>
                      <span class="party-x">
                        <span class="role">{{ p.involvementType || '—' }}</span>
                        <span class="nm">{{ p.contact || '—' }}</span>
                        @if (p.contactPerson) { <span class="ref">{{ 'akten.party.contactPerson' | transloco }}: {{ p.contactPerson }}</span> }
                        @if (p.reference) { <span class="ref">{{ 'akten.party.reference' | transloco }}: {{ p.reference }}</span> }
                      </span>
                      <span class="row-actions">
                        @if (p.addressId) {
                          <button type="button" class="row-edit" (click)="openContact(p.addressId)" [title]="'akten.party.toContact' | transloco">
                            <jl-icon name="contacts" [size]="15" />
                          </button>
                        }
                        <button type="button" class="row-edit" (click)="editingParty.set(p)" [title]="'akten.party.edit' | transloco">
                          <jl-icon name="edit" [size]="15" />
                        </button>
                        <button type="button" class="row-del" [disabled]="partyRemoving() === p.id"
                                (click)="confirmRemoveParty(p)" [title]="'akten.party.remove' | transloco">
                          <jl-icon name="trash" [size]="15" />
                        </button>
                      </span>
                    </div>
                  } @empty {
                    <p class="muted">{{ 'akten.noParties' | transloco }}</p>
                  }
                </div>
              </div>
            } @else if (activeTab() === 'deadlines') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.deadlines' | transloco }}</h3>
                  <span class="card-count">{{ c.dueDates.length }}</span>
                  <span class="ch-actions">
                    <button type="button" class="add-btn" (click)="openNewEvent()">
                      <jl-icon name="plus" [size]="14" /> {{ 'akten.due.add' | transloco }}
                    </button>
                  </span>
                </div>
                <div class="card-b">
                  @for (d of c.dueDates; track d.id) {
                    <div class="frist" [class.done]="d.done">
                      <span class="bar" [class.deadline]="d.type === 'deadline'"
                            [style.background]="d.calendarColor || null"></span>
                      <span class="fdate">{{ d.dueDate | date: 'dd.MM.yyyy' }}</span>
                      <span class="fx">
                        <span class="ft">{{ d.reason }}</span>
                        <span class="fs">{{ d.assignee }} · {{ 'akten.dueType.' + d.type | transloco }}</span>
                      </span>
                      @if (d.done) { <jl-icon name="check" [size]="14" /> }
                      <span class="row-actions">
                        <button type="button" class="row-edit" (click)="openEditDue(d)" [title]="'akten.due.edit' | transloco">
                          <jl-icon name="edit" [size]="15" />
                        </button>
                        <button type="button" class="row-del" [disabled]="dueDeleting() === d.id"
                                (click)="confirmDeleteDue(d)" [title]="'akten.due.delete' | transloco">
                          <jl-icon name="trash" [size]="15" />
                        </button>
                      </span>
                    </div>
                  } @empty {
                    <p class="muted">{{ 'akten.noDeadlines' | transloco }}</p>
                  }
                </div>
              </div>
            } @else if (activeTab() === 'finance') {
              @if (financeLoading()) {
                <p class="muted tab-todo">{{ 'akten.loading' | transloco }}</p>
              } @else if (financeError()) {
                <p class="tab-todo">
                  {{ 'akten.error' | transloco }}
                  <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                </p>
              } @else {
                <div class="grid">
                  <div class="card full finance-summary">
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.claimValue' | transloco }}</span>
                      <span class="fk-value">{{ c.claimValue | number: '1.2-2' }} €</span>
                    </div>
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.invoiced' | transloco }}</span>
                      <span class="fk-value">{{ sumGross(invoices()) | number: '1.2-2' }} €</span>
                    </div>
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.paid' | transloco }}</span>
                      <span class="fk-value">{{ sumPayments(payments()) | number: '1.2-2' }} €</span>
                    </div>
                  </div>

                  <div class="fin-nav" role="tablist">
                    @for (v of financeViews; track v) {
                      <button type="button" class="fin-nav-btn" [class.on]="financeView() === v" (click)="financeView.set(v)">
                        {{ 'akten.finance.view.' + v | transloco }}
                        <span class="fin-nav-count">{{ financeCount(v) }}</span>
                      </button>
                    }
                  </div>

                  @switch (financeView()) {
                    @case ('invoices') {
                      <div class="card full">
                        <div class="card-b">
                          @for (inv of invoices(); track inv.id) {
                            <div class="fin-row">
                              <span class="fin-main">
                                <span class="dn">{{ inv.invoiceNumber || inv.name || '—' }}</span>
                                <span class="dmeta">
                                  {{ inv.status }}
                                  @if (inv.dueDate) { · {{ 'akten.finance.due' | transloco }} {{ inv.dueDate | date: 'dd.MM.yyyy' }} }
                                </span>
                              </span>
                              <span class="fin-amount">{{ inv.totalGross | number: '1.2-2' }} {{ inv.currency }}</span>
                            </div>
                          } @empty {
                            <p class="muted">{{ 'akten.finance.noInvoices' | transloco }}</p>
                          }
                        </div>
                      </div>
                    }
                    @case ('payments') {
                      <div class="card full">
                        <div class="card-b">
                          @for (pay of payments(); track pay.id) {
                            <div class="fin-row">
                              <span class="fin-main">
                                <span class="dn">{{ pay.name || pay.paymentNumber || pay.reason || '—' }}</span>
                                <span class="dmeta">
                                  {{ pay.status }}
                                  @if (pay.targetDate) { · {{ pay.targetDate | date: 'dd.MM.yyyy' }} }
                                </span>
                              </span>
                              <span class="fin-amount">{{ pay.total | number: '1.2-2' }} {{ pay.currency }}</span>
                            </div>
                          } @empty {
                            <p class="muted">{{ 'akten.finance.noPayments' | transloco }}</p>
                          }
                        </div>
                      </div>
                    }
                    @case ('account') {
                      @if (accountEntries()?.length) {
                        @let sum = accountSummary();
                        <!-- Kategorie-Salden (Einnahmen/Ausgaben, Fremdgeld, Auslagen), analog ArchiveFilePanel -->
                        <div class="acct-sums">
                          @for (g of sum; track g.key) {
                            <div class="acct-sum">
                              <span class="acct-sum-title">{{ 'akten.finance.acct.cat.' + g.key | transloco }}</span>
                              <div class="acct-sum-row">
                                <span>{{ 'akten.finance.acct.in' | transloco }}</span>
                                <span class="num">{{ g.in | number: '1.2-2' }} €</span>
                              </div>
                              <div class="acct-sum-row">
                                <span>{{ 'akten.finance.acct.out' | transloco }}</span>
                                <span class="num">{{ g.out | number: '1.2-2' }} €</span>
                              </div>
                              <div class="acct-sum-row net">
                                <span>{{ 'akten.finance.acct.balance' | transloco }}</span>
                                <span class="num" [class.neg]="g.net < 0">{{ g.net | number: '1.2-2' }} €</span>
                              </div>
                            </div>
                          }
                        </div>

                        <div class="card full">
                          <div class="card-b">
                            <div class="acct-scroll">
                              <table class="acct-table">
                                <thead>
                                  <tr>
                                    <th class="c-date">{{ 'akten.finance.acct.date' | transloco }}</th>
                                    <th class="c-contact">{{ 'akten.finance.acct.contact' | transloco }}</th>
                                    <th class="c-desc">{{ 'akten.finance.acct.description' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.earnings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.spendings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.escrowIn' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.escrowOut' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.expendituresIn' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.expendituresOut' | transloco }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  @for (e of accountEntries(); track e.id) {
                                    <tr>
                                      <td class="c-date">{{ e.date | date: 'dd.MM.yyyy' }}</td>
                                      <td class="c-contact">{{ e.contact || '—' }}</td>
                                      <td class="c-desc">{{ e.description || '—' }}</td>
                                      <td class="num">{{ e.earnings ? (e.earnings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.spendings ? (e.spendings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.escrowIn ? (e.escrowIn | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.escrowOut ? (e.escrowOut | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.expendituresIn ? (e.expendituresIn | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.expendituresOut ? (e.expendituresOut | number: '1.2-2') : '—' }}</td>
                                    </tr>
                                  }
                                </tbody>
                                <tfoot>
                                  <tr>
                                    <td class="c-date"></td>
                                    <td class="c-contact"></td>
                                    <td class="c-desc">{{ 'akten.finance.acct.totals' | transloco }}</td>
                                    <td class="num">{{ sum[0].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[0].out | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[1].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[1].out | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[2].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[2].out | number: '1.2-2' }}</td>
                                  </tr>
                                </tfoot>
                              </table>
                            </div>
                          </div>
                        </div>
                      } @else {
                        <div class="card full"><div class="card-b">
                          <p class="muted">{{ 'akten.finance.noAccountEntries' | transloco }}</p>
                        </div></div>
                      }
                    }
                  }
                </div>
              }
            } @else if (activeTab() === 'zeiten') {
              @if (timesheetsLoading()) {
                <p class="muted tab-todo">{{ 'akten.loading' | transloco }}</p>
              } @else if (timesheetsError()) {
                <p class="tab-todo">
                  {{ 'akten.error' | transloco }}
                  <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                </p>
              } @else {
                <div class="grid">
                  @if (timesheets()?.length) {
                    <div class="fin-nav" role="tablist">
                      @for (f of zeitenFilters; track f) {
                        <button type="button" class="fin-nav-btn" [class.on]="zeitenFilter() === f" (click)="zeitenFilter.set(f)">
                          {{ 'akten.zeiten.filter.' + f | transloco }}
                          <span class="fin-nav-count">{{ zeitenCount(f) }}</span>
                        </button>
                      }
                    </div>
                  }
                  @for (ts of visibleTimesheets(); track ts.id) {
                    <div class="card full">
                      <div class="card-h">
                        <h3>{{ ts.name || '—' }}</h3>
                        <span class="ts-status" [class.closed]="ts.status === 20">
                          {{ (ts.status === 20 ? 'akten.zeiten.closed' : 'akten.zeiten.open') | transloco }}
                        </span>
                        <span class="card-count">{{ ts.positions.length }}</span>
                      </div>
                      <div class="card-b">
                        @if (ts.description) { <p class="ts-desc">{{ ts.description }}</p> }
                        <div class="ts-kpis">
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.duration' | transloco }}</span>
                            <span class="fk-value">{{ tsDuration(ts.positions) }} h</span>
                          </div>
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.net' | transloco }}</span>
                            <span class="fk-value">{{ tsNet(ts.positions) | number: '1.2-2' }} €</span>
                          </div>
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.invoiceable' | transloco }}</span>
                            <span class="fk-value">{{ tsInvoiceable(ts.positions) | number: '1.2-2' }} €</span>
                          </div>
                          @if (ts.limited) {
                            <div class="fin-kpi">
                              <span class="fk-label">{{ 'akten.zeiten.limit' | transloco }}</span>
                              <span class="fk-value">{{ ts.limit | number: '1.2-2' }} € · {{ ts.percentageDone | number: '1.0-0' }} %</span>
                            </div>
                          }
                        </div>

                        @if (ts.positions.length) {
                          <div class="ts-scroll">
                            <table class="ts-table">
                              <thead>
                                <tr>
                                  <th>{{ 'akten.zeiten.col.description' | transloco }}</th>
                                  <th>{{ 'akten.zeiten.col.user' | transloco }}</th>
                                  <th>{{ 'akten.zeiten.col.started' | transloco }}</th>
                                  <th class="num">{{ 'akten.zeiten.col.duration' | transloco }}</th>
                                  <th class="num">{{ 'akten.zeiten.col.rate' | transloco }}</th>
                                  <th class="num">{{ 'akten.zeiten.col.total' | transloco }}</th>
                                  <th>{{ 'akten.zeiten.col.billing' | transloco }}</th>
                                </tr>
                              </thead>
                              <tbody>
                                @for (p of ts.positions; track p.id) {
                                  <tr>
                                    <td>{{ p.name || p.description || '—' }}</td>
                                    <td>{{ p.principal || '—' }}</td>
                                    <td>{{ p.started ? (p.started | date: 'dd.MM.yyyy HH:mm') : '—' }}</td>
                                    <td class="num">
                                      @if (p.running) {
                                        <span class="ts-run">{{ 'akten.zeiten.running' | transloco }}</span>
                                      } @else { {{ durationOf(p) }} }
                                    </td>
                                    <td class="num">{{ p.unitPrice | number: '1.2-2' }} €</td>
                                    <td class="num">{{ p.total | number: '1.2-2' }} €</td>
                                    <td>
                                      @if (p.invoiceId) {
                                        <span class="ts-badge billed">{{ 'akten.zeiten.billed' | transloco }}</span>
                                      } @else {
                                        <span class="ts-badge open">{{ 'akten.zeiten.unbilled' | transloco }}</span>
                                      }
                                    </td>
                                  </tr>
                                }
                              </tbody>
                            </table>
                          </div>
                        } @else {
                          <p class="muted">{{ 'akten.zeiten.noPositions' | transloco }}</p>
                        }
                      </div>
                    </div>
                  } @empty {
                    <div class="card full"><div class="card-b">
                      <p class="muted">{{ 'akten.zeiten.noTimesheets' | transloco }}</p>
                    </div></div>
                  }
                </div>
              }
            } @else if (activeTab() === 'history') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.tabs.history' | transloco }}</h3>
                  @if (history()) { <span class="card-count">{{ history()?.length ?? 0 }}</span> }
                </div>
                <div class="card-b">
                  @if (historyLoading()) {
                    <p class="muted">{{ 'akten.loading' | transloco }}</p>
                  } @else if (historyError()) {
                    <p>
                      {{ 'akten.error' | transloco }}
                      <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                    </p>
                  } @else {
                    @for (h of history(); track h.id) {
                      <div class="hist">
                        <span class="hist-dot"></span>
                        <span class="hist-body">
                          <span class="hist-desc">{{ h.changeDescription }}</span>
                          <span class="hist-meta">{{ h.changeDate | date: 'dd.MM.yyyy HH:mm' }} · {{ h.principal }}</span>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noHistory' | transloco }}</p>
                    }
                  }
                </div>
              </div>
            }
          </div>
          } @else {
            <p class="empty detail-empty">{{ 'akten.selectHint' | transloco }}</p>
          }
        }
      </section>
    </div>

    <jl-document-preview [doc]="previewDoc()" (closed)="previewDoc.set(null)" />

    @if (editingCase(); as ed) {
      <jl-case-editor [caseData]="ed.data" (save)="onSaveCase($event)" (close)="editingCase.set(null)" />
    }
    @if (partyAdding()) {
      <jl-party-add (save)="onAddParty($event)" (close)="partyAdding.set(false)" />
    }
    @if (editingParty(); as p) {
      <jl-party-editor [party]="p" [caseId]="selectedId() ?? ''"
                       (save)="onSaveParty($event)" (close)="editingParty.set(null)" />
    }
    @if (eventEditor(); as ee) {
      <jl-event-editor [event]="ee.event" [presetCase]="ee.event ? null : presetCaseRef()"
                       (save)="onSaveEvent($event)" (remove)="onDeleteEvent($event)"
                       (close)="eventEditor.set(null)" (openCase)="eventEditor.set(null)" />
    }
  `,
  styleUrl: './akten.component.css',
})
export class AktenComponent {
  protected readonly cases = inject(CasesService);
  protected readonly pins = inject(PinsService);
  private readonly documents = inject(DocumentContentService);
  private readonly calendar = inject(CalendarService);
  private readonly transloco = inject(TranslocoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly tabs: CaseTab[] = ['overview', 'documents', 'parties', 'deadlines', 'finance', 'zeiten', 'history'];
  protected readonly filters: CaseFilter[] = ['all', 'open', 'closed'];

  protected readonly filter = signal<CaseFilter>('all');
  protected readonly search = signal('');
  protected readonly activeTab = signal<CaseTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<CaseDetail | null>(null);
  protected readonly detailLoading = signal(false);

  // Document preview — handed to the shared <jl-document-preview> overlay.
  protected readonly previewDoc = signal<PreviewDoc | null>(null);

  // Documents tab: folder navigation. Multiple folders can be viewed at once (like the Swing
  // client) — docFolderSel holds the selected folder ids (default: all). docMobilePane drives the
  // phone drill-down. The 'folder' sort is only offered because several folders can be shown.
  protected readonly docFolderSel = signal<Set<string>>(new Set());
  protected readonly docMobilePane = signal<'folders' | 'list'>('folders');
  protected readonly sortKeys: DocSortKey[] = ['name', 'date', 'size', 'type', 'favorite', 'folder'];
  /** Free-text filter over the case's documents (by name/tag). When set it searches case-wide. */
  protected readonly docSearch = signal('');

  // Overview extras (labels, permissions, messages) — loaded eagerly with the case (overview is
  // the default tab). null while loading, [] when loaded/empty.
  protected readonly caseTags = signal<CaseTag[] | null>(null);
  protected readonly caseGroups = signal<CaseGroup[] | null>(null);
  protected readonly caseMessages = signal<CaseMessage[] | null>(null);

  // History tab state (lazy-loaded per case)
  protected readonly history = signal<CaseHistoryEntry[] | null>(null);
  protected readonly historyLoading = signal(false);
  protected readonly historyError = signal(false);

  // Finance tab state (invoices + payments + case account, lazy-loaded per case)
  protected readonly invoices = signal<CaseInvoice[] | null>(null);
  protected readonly payments = signal<CasePayment[] | null>(null);
  protected readonly accountEntries = signal<AccountEntry[] | null>(null);
  protected readonly financeLoading = signal(false);
  protected readonly financeError = signal(false);
  /** Active sub-view of the finance tab. */
  protected readonly financeView = signal<FinanceView>('invoices');
  protected readonly financeViews: FinanceView[] = ['invoices', 'payments', 'account'];

  // Zeiten (time tracking) tab state — the case's timesheets (open + closed) with their positions.
  protected readonly timesheets = signal<TimesheetView[] | null>(null);
  protected readonly timesheetsLoading = signal(false);
  protected readonly timesheetsError = signal(false);
  /** Show all timesheets, only open or only closed. */
  protected readonly zeitenFilter = signal<ZeitenFilter>('all');
  protected readonly zeitenFilters: ZeitenFilter[] = ['all', 'open', 'closed'];

  // ---- Editing state (Stammdaten / Beteiligte / Dokumente / Kalender) ----
  /** Stammdaten editor: holds the raw case DTO to edit, or null when closed. */
  protected readonly editingCase = signal<{ data: CaseWrite } | null>(null);
  /** Add-party dialog open. */
  protected readonly partyAdding = signal(false);
  /** Party being edited (detail fields), or null when closed. */
  protected readonly editingParty = signal<Party | null>(null);
  /** Id of the party currently being removed (disables its button), or null. */
  protected readonly partyRemoving = signal<string | null>(null);
  /** Calendar-entry editor: holds the entry to edit (null = create against this case), or null when closed. */
  protected readonly eventEditor = signal<{ event: CalendarEvent | null } | null>(null);
  protected readonly docUploading = signal(false);
  protected readonly docUploadError = signal(false);
  protected readonly dragOver = signal(false);
  /** Id of the document currently being deleted, or null. */
  protected readonly docDeleting = signal<string | null>(null);
  /** Id of the due date currently being deleted, or null. */
  protected readonly dueDeleting = signal<string | null>(null);

  // Authorized groups + tags editing state (loaded lazily; all-groups/tag-templates cached in the service).
  protected readonly allGroups = signal<CaseGroup[]>([]);
  protected readonly tagTemplates = signal<string[]>([]);
  /** Configured list-tag ("multi-value") definitions, shown as label + dropdown. */
  protected readonly multiTagDefs = signal<MultiValueTagDef[]>([]);
  /** New instant-message composer text. */
  protected readonly messageDraft = signal('');
  /** The current user's login name (message sender / own-bubble detection). */
  protected readonly currentUser = computed(() => this.auth.user()?.username ?? '');

  /** The selected case as a {@link CaseRef} for the event editor's preset (locked) case. */
  protected readonly presetCaseRef = computed<CaseRef | null>(() => {
    const c = this.selected();
    return c ? { id: c.id, fileNumber: c.fileNumber, name: c.name } : null;
  });

  private autoSelected = false;
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.cases.reload();
    // Editing lookups (cached app-wide in the service): all groups + tag templates + list-tags.
    this.cases.allGroups().subscribe((g) => this.allGroups.set(g));
    this.cases.tagTemplates().subscribe((t) => this.tagTemplates.set(t));
    this.cases.multiValueTags().subscribe((m) => this.multiTagDefs.set(m));
    // The route param (/cases/:id) is the source of truth for the selected case, so
    // deep links, browser back/forward and in-app navigation all stay in sync.
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.autoSelected = true;
        if (id !== this.selectedId()) {
          this.select(id);
        }
      } else if (this.selectedId() !== null) {
        this.selectedId.set(null);
        this.selected.set(null);
      }
    });
    // On wide screens with no deep link, open the first case once the list arrives (once).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.router.navigate(['/cases', rows[0].id], { replaceUrl: true });
      }
    });
  }

  // ----- Documents tab: folders + sorting -----

  /** The case's root folder id ('' when the case has no folder tree). */
  private docRootId(): string {
    return this.selected()?.rootFolder?.id ?? '';
  }

  /** True when a folder id denotes the root ('' sentinel or the actual root id). */
  private isRootFolder(folderId: string): boolean {
    return folderId === '' || folderId === this.docRootId();
  }

  /**
   * The folder a document effectively lives in: root documents may carry no folder id or the root
   * folder's own id, so both collapse to the root id used in the folder rows / selection set.
   */
  private effectiveFolderId(doc: CaseDocument): string {
    return this.isRootFolder(doc.folderId) ? this.docRootId() : doc.folderId;
  }

  /** The folder tree flattened to indented rows (children alphabetical), each with a doc count. */
  protected docFolderRows(): DocFolderRow[] {
    const detail = this.selected();
    if (!detail) { return []; }
    const rows: DocFolderRow[] = [];
    const visit = (folder: DocFolder, depth: number, isRoot: boolean) => {
      rows.push({
        id: folder.id,
        name: folder.name,
        depth,
        count: detail.documents.filter((d) => this.effectiveFolderId(d) === folder.id).length,
        isRoot,
      });
      [...folder.children]
        .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
        .forEach((c) => visit(c, depth + 1, false));
    };
    if (detail.rootFolder) {
      visit(detail.rootFolder, 0, true);
    } else {
      rows.push({ id: '', name: '', depth: 0, count: detail.documents.length, isRoot: true });
    }
    return rows;
  }

  protected isFolderSelected(id: string): boolean {
    return this.docFolderSel().has(id);
  }

  /** Toggles one folder in/out of the current selection. */
  protected toggleFolder(id: string): void {
    const next = new Set(this.docFolderSel());
    if (next.has(id)) { next.delete(id); } else { next.add(id); }
    this.docFolderSel.set(next);
  }

  protected allFolderIds(): string[] {
    return this.docFolderRows().map((f) => f.id);
  }

  protected allFoldersSelected(): boolean {
    const ids = this.allFolderIds();
    const sel = this.docFolderSel();
    return ids.length > 0 && ids.every((id) => sel.has(id));
  }

  /** Selects every folder, or clears the selection when everything is already selected. */
  protected toggleAllFolders(): void {
    this.docFolderSel.set(this.allFoldersSelected() ? new Set() : new Set(this.allFolderIds()));
  }

  /** id -> folder name (leaf), for the per-document folder label when several folders are shown. */
  private folderNameMap(): Map<string, string> {
    const m = new Map<string, string>();
    const walk = (f: DocFolder) => { m.set(f.id, f.name); f.children.forEach(walk); };
    const root = this.selected()?.rootFolder;
    if (root) { walk(root); } else { m.set('', ''); }
    return m;
  }

  /** id -> full folder path ("Dokumente / Gericht"), used for the "Folder" sort. */
  private folderPathMap(): Map<string, string> {
    const m = new Map<string, string>();
    const walk = (f: DocFolder, prefix: string) => {
      const path = prefix ? `${prefix} / ${f.name}` : f.name;
      m.set(f.id, path);
      f.children.forEach((c) => walk(c, path));
    };
    const root = this.selected()?.rootFolder;
    if (root) { walk(root, ''); } else { m.set('', ''); }
    return m;
  }

  /** Leaf folder name of a document (shown on rows when the selection spans several folders). */
  protected docFolderLabel(doc: CaseDocument): string {
    return this.folderNameMap().get(this.effectiveFolderId(doc)) ?? '';
  }

  /** Matches a document against a lower-cased search term (file name or any tag). */
  private matchesSearch(doc: CaseDocument, term: string): boolean {
    return doc.name.toLowerCase().includes(term) || doc.tags.some((t) => t.toLowerCase().includes(term));
  }

  protected clearDocSearch(): void {
    this.docSearch.set('');
  }

  /** File-type category derived from the extension (drives the row icon + its colour). */
  protected docKind(doc: CaseDocument): string {
    return fileKind(doc.ext);
  }

  /** The glyph name for a file-type category (generic 'doc' as the fallback). */
  protected kindIcon(kind: string): string {
    return kindGlyph(kind as ReturnType<typeof fileKind>);
  }

  /** When exactly one folder is selected, its display row (else null) — drives the list heading. */
  protected singleSelectedFolder(): DocFolderRow | null {
    const sel = this.docFolderSel();
    if (sel.size !== 1) { return null; }
    return this.docFolderRows().find((f) => sel.has(f.id)) ?? null;
  }

  /** True when documents should show their folder (search results or a multi-folder selection). */
  protected showFolderColumn(): boolean {
    return this.docSearch().trim().length > 0 || this.docFolderSel().size > 1;
  }

  /**
   * The documents to list: while a search term is present it filters the whole case (by name and
   * tags) ignoring the folder selection; otherwise the documents of the selected folders. Sorted
   * by the active criterion/direction.
   */
  protected visibleDocs(): CaseDocument[] {
    const detail = this.selected();
    if (!detail) { return []; }
    const term = this.docSearch().trim().toLowerCase();
    const sel = this.docFolderSel();
    const docs = term
      ? detail.documents.filter((d) => this.matchesSearch(d, term))
      : detail.documents.filter((d) => sel.has(this.effectiveFolderId(d)));
    const { key, dir } = this.cases.docSort();
    const mode = this.cases.docDateMode();
    const mul = dir === 'asc' ? 1 : -1;
    const paths = key === 'folder' ? this.folderPathMap() : null;
    return docs.sort((a, b) => {
      let p: number;
      if (paths) {
        const pa = paths.get(this.effectiveFolderId(a)) ?? '';
        const pb = paths.get(this.effectiveFolderId(b)) ?? '';
        p = pa.localeCompare(pb, undefined, { sensitivity: 'base' });
      } else {
        p = comparePrimary(a, b, key, mode);
      }
      p *= mul;
      return p !== 0 ? p : a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
    });
  }

  /** Toggles the sort direction when re-clicking the active criterion, else switches criterion. */
  protected toggleSort(key: DocSortKey): void {
    const cur = this.cases.docSort();
    if (cur.key === key) {
      this.cases.docSort.set({ key, dir: cur.dir === 'asc' ? 'desc' : 'asc' });
    } else {
      // Sensible default direction: newest/biggest/favourites first, names/types A→Z.
      const dir = key === 'date' || key === 'size' ? 'desc' : 'asc';
      this.cases.docSort.set({ key, dir });
    }
  }

  /** The ▲/▼ marker for the active sort criterion (empty for the others). */
  protected sortArrow(key: DocSortKey): string {
    const s = this.cases.docSort();
    return s.key === key ? (s.dir === 'asc' ? ' ▲' : ' ▼') : '';
  }

  protected toggleDateMode(): void {
    this.cases.docDateMode.set(this.cases.docDateMode() === 'change' ? 'creation' : 'change');
  }

  /** True when the document can be previewed inline / in a new tab (vs. download-only). */
  protected canPreview(doc: CaseDocument): boolean {
    return previewKindOf(doc.ext) !== 'none';
  }

  /** Opens the shared preview overlay for a document (download-only kinds just download). */
  protected preview(doc: CaseDocument): void {
    if (previewKindOf(doc.ext) === 'none') {
      this.download(doc);
      return;
    }
    this.previewDoc.set({ id: doc.id, name: doc.name, ext: doc.ext });
  }

  /** Fetches a document's bytes and triggers a browser download with its file name. */
  protected download(doc: CaseDocument): void {
    this.documents.download({ id: doc.id, name: doc.name, ext: doc.ext });
  }

  /** Switches the server-side filter and reloads the first page. */
  protected setFilter(f: CaseFilter): void {
    if (this.filter() === f) {
      return;
    }
    this.filter.set(f);
    this.cases.setFilter(f);
  }

  /** Debounces keystrokes into a server-side search (250ms). */
  protected onSearch(value: string): void {
    this.search.set(value);
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
    this.searchDebounce = setTimeout(() => this.cases.setSearch(value), 250);
  }

  /** Loads the next page when the list is scrolled near the bottom. */
  protected onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 240) {
      this.cases.loadMore();
    }
  }

  /** Navigates to a case's deep link; the route subscription performs the actual select. */
  protected open(id: string): void {
    this.router.navigate(['/cases', id]);
  }

  private select(id: string): void {
    this.selectedId.set(id);
    this.activeTab.set('overview');
    this.detailLoading.set(true);
    this.selected.set(null);
    // Reset the lazily-loaded tab data for the new case.
    this.history.set(null);
    this.historyError.set(false);
    this.invoices.set(null);
    this.payments.set(null);
    this.accountEntries.set(null);
    this.financeError.set(false);
    this.financeView.set('invoices');
    this.timesheets.set(null);
    this.timesheetsError.set(false);
    this.zeitenFilter.set('all');
    this.caseTags.set(null);
    this.caseGroups.set(null);
    this.caseMessages.set(null);
    this.docFolderSel.set(new Set());
    this.docMobilePane.set('folders');
    this.docSearch.set('');
    this.cases.loadDetail(id).subscribe((detail) => {
      // ignore a stale response if the user already picked another case
      if (this.selectedId() === id) {
        this.selected.set(detail);
        // Default: every folder selected (mirrors the Swing client's initial state).
        this.docFolderSel.set(new Set(collectFolderIds(detail)));
        this.detailLoading.set(false);
      }
    });
    // Overview extras (labels, permissions, messages) load eagerly alongside the detail.
    this.cases.tags(id).subscribe((t) => { if (this.selectedId() === id) { this.caseTags.set(t); } });
    this.cases.allowedGroups(id).subscribe((g) => { if (this.selectedId() === id) { this.caseGroups.set(g); } });
    this.cases.messages(id).subscribe((m) => { if (this.selectedId() === id) { this.caseMessages.set(m); } });
  }

  /**
   * Re-fetches the current case detail after a write (parties/documents/due dates/master data),
   * preserving the open tab and folder selection. When `refreshList` is set (master-data change)
   * the list is also reloaded so a changed name/status shows there too.
   */
  private reloadDetail(refreshList = false): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.cases.loadDetail(id).subscribe((detail) => {
      if (this.selectedId() === id && detail) {
        this.selected.set(detail);
      }
    });
    if (refreshList) {
      this.cases.reload();
    }
  }

  // ----- Stammdaten (master data) -----

  /** Opens the master-data editor for the current case using its raw (writable) DTO. */
  protected openEditCase(): void {
    const raw = this.cases.rawSelected();
    if (raw) {
      this.editingCase.set({ data: raw });
    }
  }

  /** Persists the edited master data, then closes and reloads the detail + list. */
  protected onSaveCase(data: CaseWrite): void {
    this.cases.saveCase(data).subscribe({
      next: () => { this.editingCase.set(null); this.reloadDetail(true); },
      error: () => undefined, // keep the dialog open on failure
    });
  }

  // ----- Beteiligte (parties) -----

  /** Links the picked contact to the case as a party, then closes and reloads. */
  protected onAddParty(e: { addressId: string; involvementType: string }): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.cases.addParty({ caseId: id, addressId: e.addressId, involvementType: e.involvementType }).subscribe({
      next: () => { this.partyAdding.set(false); this.reloadDetail(); },
      error: () => undefined,
    });
  }

  /** Navigates to the linked contact's address-book entry. */
  protected openContact(addressId: string): void {
    this.router.navigate(['/contacts', addressId]);
  }

  /** Persists edited party detail fields, then closes and reloads. */
  protected onSaveParty(update: PartyUpdate): void {
    this.cases.updateParty(update).subscribe({
      next: () => { this.editingParty.set(null); this.reloadDetail(); },
      error: () => undefined,
    });
  }

  /** Confirms, then removes a party from the case and reloads. */
  protected confirmRemoveParty(p: Party): void {
    if (this.partyRemoving()) {
      return;
    }
    const name = p.contact || p.involvementType || '';
    if (!confirm(this.transloco.translate('akten.party.removeConfirm', { name }))) {
      return;
    }
    const id = this.selectedId();
    this.partyRemoving.set(p.id);
    this.cases.removeParty(p.id).subscribe({
      next: () => { this.partyRemoving.set(null); if (this.selectedId() === id) { this.reloadDetail(); } },
      error: () => this.partyRemoving.set(null),
    });
  }

  // ----- Dokumente (upload / delete) -----

  /** The folder new uploads go into: the selected folder when exactly one (non-root) is selected, else the root. */
  private uploadTargetFolder(): string {
    const sel = this.docFolderSel();
    if (sel.size === 1) {
      const only = [...sel][0];
      return this.isRootFolder(only) ? '' : only;
    }
    return '';
  }

  /** Display name of the current upload target folder (for the dropzone hint); '' means root. */
  protected uploadTargetName(): string {
    const target = this.uploadTargetFolder();
    if (!target) {
      return this.transloco.translate('akten.docs.root');
    }
    return this.findFolderName(this.selected()?.rootFolder ?? null, target) || this.transloco.translate('akten.docs.root');
  }

  /** Which dropzone hint to show (into a named sub-folder vs the root). */
  protected uploadHint(): string {
    return this.uploadTargetFolder() ? 'akten.docs.dropHintFolder' : 'akten.docs.dropHint';
  }

  private findFolderName(node: DocFolder | null, id: string): string {
    if (!node) {
      return '';
    }
    if (node.id === id) {
      return node.name;
    }
    for (const child of node.children) {
      const hit = this.findFolderName(child, id);
      if (hit) {
        return hit;
      }
    }
    return '';
  }

  protected onUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    void this.uploadFiles(files);
  }

  protected onDragOver(event: DragEvent): void {
    if (event.dataTransfer?.types.includes('Files')) {
      event.preventDefault();
      this.dragOver.set(true);
    }
  }

  protected onDragLeave(): void {
    this.dragOver.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    void this.uploadFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  /** Uploads the given file(s) into the current target folder, then reloads the detail. */
  private async uploadFiles(files: File[]): Promise<void> {
    const id = this.selectedId();
    if (!files.length || !id) {
      return;
    }
    const folderId = this.uploadTargetFolder();
    this.docUploading.set(true);
    this.docUploadError.set(false);
    let failed = false;
    for (const file of files) {
      try {
        const base64 = await readAsBase64(file);
        await firstValueFrom(this.cases.uploadDocument(id, file.name, base64, folderId));
      } catch {
        failed = true;
      }
    }
    this.docUploading.set(false);
    this.docUploadError.set(failed);
    if (this.selectedId() === id) {
      this.reloadDetail();
    }
  }

  /** Confirms, then deletes a case document and reloads. */
  protected confirmDeleteDoc(doc: CaseDocument): void {
    if (this.docDeleting()) {
      return;
    }
    if (!confirm(this.transloco.translate('akten.docs.deleteConfirm', { name: doc.name }))) {
      return;
    }
    const id = this.selectedId();
    this.docDeleting.set(doc.id);
    this.cases.deleteDocument(doc.id).subscribe({
      next: () => { this.docDeleting.set(null); if (this.selectedId() === id) { this.reloadDetail(); } },
      error: () => this.docDeleting.set(null),
    });
  }

  // ----- Kalender / Fristen (create / edit / delete) -----

  /** Opens the calendar editor to create a new entry filed against this case. */
  protected openNewEvent(): void {
    this.eventEditor.set({ event: null });
  }

  /** Opens the calendar editor to edit an existing due date (built into a full CalendarEvent). */
  protected openEditDue(d: DueDate): void {
    this.eventEditor.set({ event: this.toCalendarEvent(d) });
  }

  /** Builds the calendar editor's CalendarEvent from a case due date + the current case. */
  private toCalendarEvent(d: DueDate): CalendarEvent {
    const c = this.selected();
    const begin = d.dueDate ? new Date(d.dueDate) : new Date();
    const end = d.endDate ? new Date(d.endDate) : begin;
    return {
      id: d.id,
      type: (d.restType.toLowerCase() as CalendarEventType) || 'followup',
      summary: d.reason,
      description: d.description,
      location: d.location,
      begin,
      end,
      done: d.done,
      assignee: d.assignee,
      reminderMinutes: d.reminderMinutes,
      timed: d.restType === 'EVENT',
      caseId: c?.id ?? '',
      caseFileNumber: c?.fileNumber ?? '',
      caseName: c?.name ?? '',
      color: d.calendarColor,
      calendarName: '',
      calendarId: d.calendarId,
    };
  }

  /** Saves the calendar entry (create or update), then closes and reloads. */
  protected onSaveEvent(draft: EventDraft): void {
    this.calendar.save(draft).subscribe({
      next: () => { this.eventEditor.set(null); this.reloadDetail(); },
      error: () => undefined,
    });
  }

  /** Deletes the entry from the editor's delete button, then closes and reloads. */
  protected onDeleteEvent(id: string): void {
    const caseId = this.selectedId();
    this.calendar.remove(id).subscribe({
      next: () => { this.eventEditor.set(null); if (this.selectedId() === caseId) { this.reloadDetail(); } },
      error: () => undefined,
    });
  }

  /** Confirms, then deletes a due date / calendar entry and reloads. */
  protected confirmDeleteDue(d: DueDate): void {
    if (this.dueDeleting()) {
      return;
    }
    if (!confirm(this.transloco.translate('akten.due.deleteConfirm', { name: d.reason || '' }))) {
      return;
    }
    const id = this.selectedId();
    this.dueDeleting.set(d.id);
    this.calendar.remove(d.id).subscribe({
      next: () => { this.dueDeleting.set(null); if (this.selectedId() === id) { this.reloadDetail(); } },
      error: () => this.dueDeleting.set(null),
    });
  }

  // ----- Berechtigungen (authorized groups) -----

  /** Groups not yet authorized on the case (for the add dropdown). */
  protected availableGroups(): CaseGroup[] {
    const current = new Set((this.caseGroups() ?? []).map((g) => g.id));
    return this.allGroups().filter((g) => !current.has(g.id));
  }

  /** Adds an authorized group (PUT the full new set), then refreshes the list. */
  protected addAllowedGroup(groupId: string): void {
    const g = this.allGroups().find((x) => x.id === groupId);
    const id = this.selectedId();
    if (!g || !id) {
      return;
    }
    const next = [...(this.caseGroups() ?? []), g];
    this.cases.setAllowedGroups(id, next).subscribe({
      next: () => { if (this.selectedId() === id) { this.caseGroups.set(next); } },
      error: () => undefined,
    });
  }

  /** Removes an authorized group (PUT the reduced set), then refreshes the list. */
  protected removeAllowedGroup(g: CaseGroup): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    const next = (this.caseGroups() ?? []).filter((x) => x.id !== g.id);
    this.cases.setAllowedGroups(id, next).subscribe({
      next: () => { if (this.selectedId() === id) { this.caseGroups.set(next); } },
      error: () => undefined,
    });
  }

  // ----- Etiketten (case tags) -----

  /** Names that are list (multi-value) tags — handled by the dropdowns, not the plain chips. */
  private multiTagNames(): Set<string> {
    return new Set(this.multiTagDefs().map((d) => d.tagName));
  }

  /** Plain single-value labels on the case (excludes list-tag values, which get dropdowns). */
  protected singleTags(): CaseTag[] {
    const multi = this.multiTagNames();
    return (this.caseTags() ?? []).filter((t) => !multi.has(t.name));
  }

  /** Single-value tag templates not yet applied (for the add dropdown; excludes list-tag names). */
  protected availableTags(): string[] {
    const current = new Set((this.caseTags() ?? []).map((t) => t.name));
    const multi = this.multiTagNames();
    return this.tagTemplates().filter((t) => !current.has(t) && !multi.has(t));
  }

  /** The currently selected value of a list tag on this case ('' when unset). */
  protected multiTagValue(def: MultiValueTagDef): string {
    return (this.caseTags() ?? []).find((t) => t.name === def.tagName)?.value ?? '';
  }

  /**
   * Sets (or clears) a list tag's value: a value updates/creates it (setTag by name), an empty
   * choice removes the existing tag. The local tag list is updated optimistically first so the
   * dropdown keeps the chosen value (otherwise change detection would snap it back before the
   * server round-trip), then reconciled with the server's tags on completion.
   */
  protected setMultiTag(def: MultiValueTagDef, value: string): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    const existing = (this.caseTags() ?? []).find((t) => t.name === def.tagName);
    // Optimistic local update: drop any current tag of this name, then add the chosen value.
    this.caseTags.update((tags) => {
      const rest = (tags ?? []).filter((t) => t.name !== def.tagName);
      return value ? [...rest, { id: existing?.id ?? 'pending', name: def.tagName, value }] : rest;
    });
    const reconcile = () => this.reloadTags(id);
    if (value) {
      this.cases.addTag(id, def.tagName, value).subscribe({ next: reconcile, error: reconcile });
    } else if (existing) {
      this.cases.removeTag(existing.id).subscribe({ next: reconcile, error: reconcile });
    }
  }

  /** Adds a label/tag to the case, then reloads the tag list. */
  protected addTag(name: string): void {
    const id = this.selectedId();
    if (!name || !id) {
      return;
    }
    this.cases.addTag(id, name).subscribe({
      next: () => this.reloadTags(id),
      error: () => undefined,
    });
  }

  /** Removes a label/tag from the case, then reloads the tag list. */
  protected removeTag(t: CaseTag): void {
    const id = this.selectedId();
    this.cases.removeTag(t.id).subscribe({
      next: () => { if (id) { this.reloadTags(id); } },
      error: () => undefined,
    });
  }

  private reloadTags(id: string): void {
    this.cases.tags(id).subscribe((t) => { if (this.selectedId() === id) { this.caseTags.set(t); } });
  }

  // ----- Instant-Nachrichten (compose) -----

  /** Posts the composed message to the case, then clears the box and reloads the thread. */
  protected sendMessage(): void {
    const id = this.selectedId();
    const content = this.messageDraft().trim();
    const sender = this.currentUser();
    if (!id || !content || !sender) {
      return;
    }
    this.cases.sendMessage(id, sender, content).subscribe({
      next: () => {
        this.messageDraft.set('');
        this.cases.messages(id).subscribe((m) => { if (this.selectedId() === id) { this.caseMessages.set(m); } });
      },
      error: () => undefined,
    });
  }

  /** Case messages newest-first for the chat view (the service already returns newest-first). */
  protected chatMessages(): CaseMessage[] {
    return this.caseMessages() ?? [];
  }

  /** Switches the active detail tab and lazily loads its data on first open. */
  protected selectTab(tab: CaseTab): void {
    this.activeTab.set(tab);
    if (tab === 'history' && this.history() === null && !this.historyLoading()) {
      this.loadHistory();
    } else if (tab === 'finance' && this.invoices() === null && !this.financeLoading()) {
      this.loadFinance();
    } else if (tab === 'zeiten' && this.timesheets() === null && !this.timesheetsLoading()) {
      this.loadTimesheets();
    }
  }

  private loadHistory(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.historyLoading.set(true);
    this.historyError.set(false);
    this.cases.history(id).subscribe({
      next: (rows) => {
        if (this.selectedId() !== id) {
          return;
        }
        this.history.set(rows);
        this.historyLoading.set(false);
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.historyError.set(true);
        this.historyLoading.set(false);
      },
    });
  }

  private loadFinance(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.financeLoading.set(true);
    this.financeError.set(false);
    forkJoin({
      invoices: this.cases.invoices(id),
      payments: this.cases.payments(id),
      accountEntries: this.cases.accountEntries(id),
    }).subscribe({
      next: ({ invoices, payments, accountEntries }) => {
        if (this.selectedId() !== id) {
          return;
        }
        this.invoices.set(invoices);
        this.payments.set(payments);
        this.accountEntries.set(accountEntries);
        this.financeLoading.set(false);
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.financeError.set(true);
        this.financeLoading.set(false);
      },
    });
  }

  /**
   * Loads the case's (open) timesheets, then their positions in parallel. Mirrors the desktop
   * "Zeiten" tab: a list of timesheet projects, each with its time entries. Read-only.
   */
  private loadTimesheets(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.timesheetsLoading.set(true);
    this.timesheetsError.set(false);
    this.cases.timesheets(id).subscribe({
      next: (sheets) => {
        if (this.selectedId() !== id) {
          return;
        }
        if (!sheets.length) {
          this.timesheets.set([]);
          this.timesheetsLoading.set(false);
          return;
        }
        forkJoin(
          sheets.map((s) => this.cases.timesheetPositions(s.id).pipe(
            map((positions) => ({ ...s, positions })),
          )),
        ).subscribe({
          next: (views) => {
            if (this.selectedId() !== id) {
              return;
            }
            this.timesheets.set(views);
            this.timesheetsLoading.set(false);
          },
          error: () => {
            if (this.selectedId() !== id) {
              return;
            }
            this.timesheetsError.set(true);
            this.timesheetsLoading.set(false);
          },
        });
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.timesheetsError.set(true);
        this.timesheetsLoading.set(false);
      },
    });
  }

  /** Retries the currently-active tab's lazy load (history/finance/zeiten). */
  protected retryTab(): void {
    if (this.activeTab() === 'history') {
      this.loadHistory();
    } else if (this.activeTab() === 'finance') {
      this.loadFinance();
    } else if (this.activeTab() === 'zeiten') {
      this.loadTimesheets();
    }
  }

  /** Timesheets matching the active status filter (status 20 = closed). */
  protected visibleTimesheets(): TimesheetView[] {
    const all = this.timesheets() ?? [];
    const f = this.zeitenFilter();
    if (f === 'open') {
      return all.filter((t) => t.status !== 20);
    }
    if (f === 'closed') {
      return all.filter((t) => t.status === 20);
    }
    return all;
  }

  /** Count of timesheets for a given filter (shown as a badge on the filter buttons). */
  protected zeitenCount(f: ZeitenFilter): number {
    const all = this.timesheets() ?? [];
    if (f === 'open') {
      return all.filter((t) => t.status !== 20).length;
    }
    if (f === 'closed') {
      return all.filter((t) => t.status === 20).length;
    }
    return all.length;
  }

  /** Net sum of a timesheet's positions. */
  protected tsNet(positions: TimesheetPosition[]): number {
    return positions.reduce((acc, p) => acc + (p.total || 0), 0);
  }

  /** Sum of the still-unbilled (no invoice) positions — the invoiceable amount. */
  protected tsInvoiceable(positions: TimesheetPosition[]): number {
    return positions.reduce((acc, p) => acc + (p.invoiceId ? 0 : (p.total || 0)), 0);
  }

  /** Total tracked duration of a timesheet's completed positions, formatted "h:mm". */
  protected tsDuration(positions: TimesheetPosition[]): string {
    const ms = positions.reduce((acc, p) => acc + positionMillis(p), 0);
    return formatDurationMs(ms);
  }

  /** A single position's duration, formatted "h:mm" (empty while running or unstarted). */
  protected durationOf(p: TimesheetPosition): string {
    const ms = positionMillis(p);
    return ms > 0 ? formatDurationMs(ms) : '—';
  }

  /** Sum of an invoice list's gross totals (for the finance summary). */
  protected sumGross(list: CaseInvoice[] | null): number {
    return (list ?? []).reduce((acc, i) => acc + (i.totalGross || 0), 0);
  }

  /** Sum of a payment list's totals (for the finance summary). */
  protected sumPayments(list: CasePayment[] | null): number {
    return (list ?? []).reduce((acc, p) => acc + (p.total || 0), 0);
  }

  /** Item count shown as a badge on a finance sub-nav button. */
  protected financeCount(view: FinanceView): number {
    const list = view === 'invoices' ? this.invoices()
      : view === 'payments' ? this.payments()
        : this.accountEntries();
    return list?.length ?? 0;
  }

  /**
   * Per-category account totals (mirrors ArchiveFilePanel.updateAccountTotals): for each of
   * the three categories — earnings/spendings, escrow, expenditures — the sum of credits
   * (`in`), the sum of debits (`out`) and the net (`in - out`). Returned in display order.
   */
  protected accountSummary(): { key: 'earnings' | 'escrow' | 'expenditures'; in: number; out: number; net: number }[] {
    const list = this.accountEntries() ?? [];
    const s = (pick: (e: AccountEntry) => number) => list.reduce((acc, e) => acc + (pick(e) || 0), 0);
    const earningsIn = s((e) => e.earnings);
    const earningsOut = s((e) => e.spendings);
    const escrowIn = s((e) => e.escrowIn);
    const escrowOut = s((e) => e.escrowOut);
    const expIn = s((e) => e.expendituresIn);
    const expOut = s((e) => e.expendituresOut);
    return [
      { key: 'earnings', in: earningsIn, out: earningsOut, net: earningsIn - earningsOut },
      { key: 'escrow', in: escrowIn, out: escrowOut, net: escrowIn - escrowOut },
      { key: 'expenditures', in: expIn, out: expOut, net: expIn - expOut },
    ];
  }

  /** Mobile "back" from the detail: return to the plain list URL. */
  protected clearSelection(): void {
    this.router.navigate(['/cases']);
  }

  /** Toggles the current case as a pinned shortcut in the header pin bar. */
  protected togglePin(c: CaseDetail): void {
    this.pins.toggle({ kind: 'case', id: c.id, label: c.fileNumber, title: c.name });
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .map((w) => w[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  /**
   * Readable foreground (black or white) for a given "#rrggbb" background, chosen by the
   * background's perceived luminance so the party-type colour never renders unreadable initials.
   */
  protected contrastOn(hex: string): string {
    const m = /^#?([0-9a-f]{6})$/i.exec(hex);
    if (!m) { return '#fff'; }
    const n = parseInt(m[1], 16);
    const r = (n >> 16) & 0xff, g = (n >> 8) & 0xff, b = n & 0xff;
    // Rec. 601 luma; > 150 counts as a light background → dark text.
    return 0.299 * r + 0.587 * g + 0.114 * b > 150 ? '#16232e' : '#fff';
  }

  /**
   * Deadlines/follow-ups for the overview: only open (not done) ones with a date — overdue and
   * upcoming — soonest first, capped at 10. Overdue entries naturally sort to the top.
   */
  protected overviewDueDates(): DueDate[] {
    return (this.selected()?.dueDates ?? [])
      .filter((d) => !d.done && d.dueDate)
      .sort((a, b) => (a.dueDate < b.dueDate ? -1 : a.dueDate > b.dueDate ? 1 : 0))
      .slice(0, 10);
  }

  /** Recent documents for the overview: most recent first, capped at 20. */
  protected overviewDocuments(): CaseDocument[] {
    return [...(this.selected()?.documents ?? [])]
      .sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0))
      .slice(0, 20);
  }

  /** True when a deadline is open and its date is today or in the past. */
  protected isOverdue(d: DueDate): boolean {
    if (!d.dueDate) {
      return false;
    }
    const end = new Date();
    end.setHours(23, 59, 59, 999);
    return new Date(d.dueDate) <= end;
  }
}

/** Elapsed milliseconds of a completed position (0 while running or unstarted). */
function positionMillis(p: TimesheetPosition): number {
  if (!p.started || !p.stopped) {
    return 0;
  }
  const start = new Date(p.started).getTime();
  const stop = new Date(p.stopped).getTime();
  return stop > start ? stop - start : 0;
}

/** Formats a duration in milliseconds as "h:mm" (e.g. 125 min -> "2:05"). */
function formatDurationMs(ms: number): string {
  const totalMinutes = Math.round(ms / 60000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}:${String(m).padStart(2, '0')}`;
}

/** Every folder id of a case (root + descendants); ['' ] when the case has no folder tree. */
function collectFolderIds(detail: CaseDetail | null): string[] {
  if (!detail?.rootFolder) { return ['']; }
  const ids: string[] = [];
  const walk = (f: DocFolder) => { ids.push(f.id); f.children.forEach(walk); };
  walk(detail.rootFolder);
  return ids;
}

/**
 * Compares two documents by the given sort criterion in ascending order (the caller applies the
 * direction and a name tiebreaker). Favourites sort ascending = favourites first. The 'folder'
 * criterion is handled by the caller (it needs the folder-path map), so it falls through to 0.
 */
function comparePrimary(a: CaseDocument, b: CaseDocument, key: DocSortKey, mode: 'change' | 'creation'): number {
  switch (key) {
    case 'name':
      return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
    case 'date': {
      const av = mode === 'change' ? a.changeDate : a.date;
      const bv = mode === 'change' ? b.changeDate : b.date;
      return av < bv ? -1 : av > bv ? 1 : 0;
    }
    case 'size':
      return a.sizeBytes - b.sizeBytes;
    case 'type':
      return a.ext.localeCompare(b.ext, undefined, { sensitivity: 'base' });
    case 'favorite':
      return a.favorite === b.favorite ? 0 : a.favorite ? -1 : 1;
    default:
      return 0;
  }
}

/** Reads a File as a base64 string (without the data-URL prefix), for the upload payload. */
function readAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result);
      const comma = result.indexOf(',');
      resolve(comma >= 0 ? result.slice(comma + 1) : result);
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}
