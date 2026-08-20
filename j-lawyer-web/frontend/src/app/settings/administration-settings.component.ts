import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SettingsScreenComponent } from './settings-screen.component';
import { ADMINISTRATION_SECTIONS } from './section-registry';

/** "Administration" screen — users, roles, groups (needs adminRole). */
@Component({
  selector: 'jl-administration-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SettingsScreenComponent],
  template: `<jl-settings-screen titleKey="settings.screen.administration" [sections]="sections" />`,
  styles: [':host { display: block; height: 100%; }'],
})
export class AdministrationSettingsComponent {
  protected readonly sections = ADMINISTRATION_SECTIONS;
}
