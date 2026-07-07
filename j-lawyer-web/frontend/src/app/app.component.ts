import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Application root — a bare router outlet. The router decides between the login page
 * (public) and the shell layout route (guarded), so login renders outside the shell.
 */
@Component({
  selector: 'jl-root',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent {}
