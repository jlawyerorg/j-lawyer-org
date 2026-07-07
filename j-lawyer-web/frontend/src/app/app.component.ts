import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ShellComponent } from './shell/shell.component';

/** Application root — hosts the responsive shell. */
@Component({
  selector: 'jl-root',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShellComponent],
  template: `<jl-shell />`,
})
export class AppComponent {}
