import {
  ChangeDetectionStrategy, Component, computed, inject, input, signal,
} from '@angular/core';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { SettingsSection } from './section-registry';
import { OptionListEditorComponent } from './option-list-editor.component';
import { SettingsUsersComponent } from './settings-users.component';
import { SettingsGroupsComponent } from './settings-groups.component';
import { FirmProfileComponent } from './firm-profile.component';
import { FinanceTypesComponent } from './finance-types.component';
import { FinancePoolsComponent } from './finance-pools.component';
import { FinancePositionsComponent } from './finance-positions.component';
import { FinanceSettingsComponent } from './finance-settings.component';
import { PartyTypesComponent } from './party-types.component';
import { CaseNumberingComponent } from './case-numbering.component';
import { FolderTemplatesComponent } from './folder-templates.component';
import { ScanSettingsComponent } from './scan-settings.component';
import { BackupSettingsComponent } from './backup-settings.component';
import { StirlingSettingsComponent } from './stirling-settings.component';
import { BeaSettingsComponent } from './bea-settings.component';
import { MultiValueTagsComponent } from './multi-value-tags.component';
import { MvEntityType } from './multi-value-tag.service';
import { SearchIndexComponent } from './search-index.component';
import { CustomFieldsComponent } from './custom-fields.component';
import { CardDavSyncComponent } from './carddav-sync.component';
import { NameTemplatesComponent } from './name-templates.component';
import { CalendarSetupsComponent } from './calendar-setups.component';
import { CalendarEntryTemplatesComponent } from './calendar-entry-templates.component';
import { BankStatementConfigsComponent } from './bank-statement-configs.component';
import { TimesheetTemplatesComponent } from './timesheet-templates.component';
import { TimesheetSettingsComponent } from './timesheet-settings.component';
import { AssistantServersComponent } from './assistant-servers.component';
import { AssistantPromptsComponent } from './assistant-prompts.component';
import { AssistantReplacementsComponent } from './assistant-replacements.component';
import { SystemMailboxComponent } from './system-mailbox.component';
import { SecuritySettingsComponent } from './security-settings.component';
import { ServerMonitoringComponent } from './server-monitoring.component';
import { SystemReportComponent } from './system-report.component';
import { WebhooksComponent } from './webhooks.component';

interface SectionGroup {
  groupKey: string;
  items: SettingsSection[];
}

/**
 * Shared shell for a role-scoped settings screen: a searchable, grouped section list (master) and
 * the selected section's editor (detail), reusing the app's master-detail layout. Each screen
 * (general / administration / system) passes its own `sections`; the detail switches on the
 * section `kind` to render the matching editor. Search matches the translated title + keywords, so
 * the desktop's dozens of menu items become one findable list.
 */
@Component({
  selector: 'jl-settings-screen',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, OptionListEditorComponent, SettingsUsersComponent, SettingsGroupsComponent, FirmProfileComponent, FinanceTypesComponent, FinancePoolsComponent, FinancePositionsComponent, FinanceSettingsComponent, PartyTypesComponent, CaseNumberingComponent, FolderTemplatesComponent, ScanSettingsComponent, BackupSettingsComponent, StirlingSettingsComponent, BeaSettingsComponent, MultiValueTagsComponent, SearchIndexComponent, CustomFieldsComponent, CardDavSyncComponent, NameTemplatesComponent, CalendarSetupsComponent, CalendarEntryTemplatesComponent, BankStatementConfigsComponent, TimesheetTemplatesComponent, TimesheetSettingsComponent, AssistantServersComponent, AssistantPromptsComponent, AssistantReplacementsComponent, SystemMailboxComponent, SecuritySettingsComponent, ServerMonitoringComponent, SystemReportComponent, WebhooksComponent],
  template: `
    <div class="st" [class.show-detail]="selected()">
      <section class="st-list">
        <header class="st-list-head">
          <h1>{{ titleKey() | transloco }}</h1>
          <div class="st-search">
            <jl-icon name="search" [size]="15" />
            <input type="search" [value]="search()" (input)="search.set($any($event.target).value)"
                   [placeholder]="'settings.search' | transloco" />
          </div>
        </header>

        @if (groups().length === 0) {
          <p class="st-empty">{{ (sections().length === 0 ? 'settings.noSections' : 'settings.noMatch') | transloco }}</p>
        } @else {
          @for (g of groups(); track g.groupKey) {
            <div class="st-group">
              <h2>{{ g.groupKey | transloco }}</h2>
              @for (s of g.items; track s.id) {
                <button type="button" class="st-item" [class.sel]="selected()?.id === s.id" (click)="select(s)">
                  {{ s.titleKey | transloco }}
                </button>
              }
            </div>
          }
        }
      </section>

      <section class="st-detail">
        @if (selected(); as s) {
          <header class="st-detail-head">
            <button type="button" class="st-back" (click)="selected.set(null)" [attr.aria-label]="'settings.back' | transloco">
              <jl-icon name="chevron-left" [size]="18" />
            </button>
            <h2>{{ s.titleKey | transloco }}</h2>
          </header>
          <div class="st-detail-body">
            @switch (s.kind) {
              @case ('optionGroup') { <jl-option-list-editor [group]="s.optionGroup!" /> }
              @case ('users') { <jl-settings-users /> }
              @case ('groups') { <jl-settings-groups /> }
              @case ('firmProfile') { <jl-firm-profile /> }
              @case ('invoiceTypes') { <jl-finance-types /> }
              @case ('invoicePools') { <jl-finance-pools /> }
              @case ('invoicePositions') { <jl-finance-positions /> }
              @case ('financeSettings') { <jl-finance-settings /> }
              @case ('partyTypes') { <jl-party-types /> }
              @case ('caseNumbering') { <jl-case-numbering /> }
              @case ('folderTemplates') { <jl-folder-templates /> }
              @case ('scanSettings') { <jl-scan-settings /> }
              @case ('backupSettings') { <jl-backup-settings /> }
              @case ('stirlingSettings') { <jl-stirling-settings /> }
              @case ('beaSettings') { <jl-bea-settings /> }
              @case ('multiValueTags') { <jl-multi-value-tags [entityType]="$any(s.entityType)" /> }
              @case ('searchIndex') { <jl-search-index /> }
              @case ('customFields') { <jl-custom-fields /> }
              @case ('cardDavSync') { <jl-carddav-sync /> }
              @case ('nameTemplates') { <jl-name-templates /> }
              @case ('calendarSetups') { <jl-calendar-setups /> }
              @case ('calendarEntryTemplates') { <jl-calendar-entry-templates /> }
              @case ('bankStatementConfigs') { <jl-bank-statement-configs /> }
              @case ('timesheetTemplates') { <jl-timesheet-templates /> }
              @case ('timesheetSettings') { <jl-timesheet-settings /> }
              @case ('assistantServers') { <jl-assistant-servers /> }
              @case ('assistantPrompts') { <jl-assistant-prompts /> }
              @case ('assistantReplacements') { <jl-assistant-replacements /> }
              @case ('systemMailbox') { <jl-system-mailbox /> }
              @case ('security') { <jl-security-settings /> }
              @case ('serverMonitoring') { <jl-server-monitoring /> }
              @case ('systemReport') { <jl-system-report /> }
              @case ('webhooks') { <jl-webhooks /> }
            }
          </div>
        } @else {
          <p class="st-placeholder">{{ 'settings.selectSection' | transloco }}</p>
        }
      </section>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    .st { display: grid; grid-template-columns: 300px 1fr; gap: 0; height: 100%; min-height: 0; }
    .st-list { border-right: 1px solid var(--jl-line); overflow-y: auto; padding: 16px 14px; display: flex; flex-direction: column; gap: 6px; }
    .st-list-head h1 { margin: 0 0 12px; font-size: 1.1rem; font-weight: 700; color: var(--jl-ink); }
    .st-search { display: flex; align-items: center; gap: 7px; padding: 7px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 9px; background: var(--jl-surface); color: var(--jl-ink-soft); margin-bottom: 8px; }
    .st-search input { flex: 1 1 auto; border: 0; outline: none; background: transparent; font: inherit; font-size: .88rem; color: var(--jl-ink); }
    .st-group { display: flex; flex-direction: column; gap: 2px; margin-bottom: 8px; }
    .st-group h2 { margin: 8px 4px 4px; font-size: .7rem; font-weight: 700; text-transform: uppercase;
      letter-spacing: .05em; color: var(--jl-ink-faint); }
    .st-item { text-align: left; font: inherit; font-size: .88rem; color: var(--jl-ink); background: transparent;
      border: 0; border-radius: 7px; padding: 7px 10px; cursor: pointer; }
    .st-item:hover { background: var(--jl-surface-alt); }
    .st-item.sel { background: var(--jl-blue); color: #fff; font-weight: 600; }
    .st-empty { color: var(--jl-ink-soft); font-size: .86rem; padding: 8px 4px; }
    .st-detail { overflow-y: auto; padding: 20px 22px; min-width: 0; }
    .st-detail-head { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
    .st-detail-head h2 { margin: 0; font-size: 1.05rem; font-weight: 700; color: var(--jl-ink); }
    .st-back { display: none; align-items: center; justify-content: center; width: 32px; height: 32px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .st-placeholder { color: var(--jl-ink-soft); font-size: .9rem; }
    @media (max-width: 760px) {
      .st { grid-template-columns: 1fr; }
      .st-detail { display: none; }
      .st.show-detail .st-list { display: none; }
      .st.show-detail .st-detail { display: block; }
      .st-back { display: inline-flex; }
    }
  `],
})
export class SettingsScreenComponent {
  private readonly transloco = inject(TranslocoService);

  readonly titleKey = input.required<string>();
  readonly sections = input.required<SettingsSection[]>();

  protected readonly search = signal('');
  protected readonly selected = signal<SettingsSection | null>(null);

  protected readonly groups = computed<SectionGroup[]>(() => {
    const q = this.search().trim().toLowerCase();
    const order: string[] = [];
    const map = new Map<string, SettingsSection[]>();
    for (const s of this.sections()) {
      if (q && !this.haystack(s).includes(q)) { continue; }
      if (!map.has(s.groupKey)) { map.set(s.groupKey, []); order.push(s.groupKey); }
      map.get(s.groupKey)!.push(s);
    }
    return order.map((g) => ({ groupKey: g, items: map.get(g)! }));
  });

  private haystack(s: SettingsSection): string {
    return `${this.transloco.translate(s.titleKey)} ${s.keywords ?? ''}`.toLowerCase();
  }

  protected select(s: SettingsSection): void {
    this.selected.set(s);
  }
}
