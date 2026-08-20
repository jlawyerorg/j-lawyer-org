import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SettingsScreenComponent } from './settings-screen.component';
import { SYSTEM_SECTIONS } from './section-registry';

/**
 * "System" screen — needs sysAdminRole. No editable sections yet (every desktop System dialog needs
 * a new server endpoint); the screen shows an empty state until those land.
 */
@Component({
  selector: 'jl-system-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SettingsScreenComponent],
  template: `<jl-settings-screen titleKey="settings.screen.system" [sections]="sections" />`,
  styles: [':host { display: block; height: 100%; }'],
})
export class SystemSettingsComponent {
  protected readonly sections = SYSTEM_SECTIONS;
}
