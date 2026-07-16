import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SettingsScreenComponent } from './settings-screen.component';
import { GENERAL_SECTIONS } from './section-registry';

/** "Allgemein" screen — dictionaries/value lists (no particular right to view). */
@Component({
  selector: 'jl-general-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SettingsScreenComponent],
  template: `<jl-settings-screen titleKey="settings.screen.general" [sections]="sections" />`,
  styles: [':host { display: block; height: 100%; }'],
})
export class GeneralSettingsComponent {
  protected readonly sections = GENERAL_SECTIONS;
}
