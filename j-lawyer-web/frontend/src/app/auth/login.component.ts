import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';

/**
 * Full-screen login page (outside the shell). Exchanges credentials for a session via
 * AuthService, then routes to the returnUrl (or /akten). Reactive form, translated,
 * theme-aware. Demo credentials while on the mock backend: admin / a.
 */
@Component({
  selector: 'jl-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="wrap">
      <form class="card" [formGroup]="form" (ngSubmit)="submit()">
        <div class="brand"><span class="dots" aria-hidden="true"></span><b>j-lawyer</b></div>
        <h1>{{ 'auth.title' | transloco }}</h1>
        <p class="sub">{{ 'auth.subtitle' | transloco }}</p>

        <label>
          <span>{{ 'auth.username' | transloco }}</span>
          <input formControlName="username" autocomplete="username" autofocus />
        </label>
        <label>
          <span>{{ 'auth.password' | transloco }}</span>
          <input type="password" formControlName="password" autocomplete="current-password" />
        </label>

        @if (error()) { <p class="error">{{ 'auth.error' | transloco }}</p> }

        <button type="submit" class="btn" [disabled]="pending() || form.invalid">
          {{ (pending() ? 'auth.signingIn' : 'auth.signIn') | transloco }}
        </button>

        <p class="hint">{{ 'auth.demoHint' | transloco }}</p>
      </form>
    </div>
  `,
  styles: [`
    .wrap { min-height: 100dvh; display: grid; place-items: center; background: var(--jl-ground); padding: 24px; }
    .card {
      width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 12px;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      padding: 28px 28px 22px; box-shadow: 0 1px 2px rgba(11,27,44,.06), 0 10px 30px rgba(11,27,44,.10);
    }
    .brand { display: flex; align-items: center; gap: 8px; font-weight: 800; letter-spacing: -.02em; color: var(--jl-ink); }
    .brand .dots { width: 9px; height: 9px; border-radius: 50%; background: var(--jl-red);
      box-shadow: 14px 0 0 var(--jl-green), 28px 0 0 var(--jl-blue); margin-right: 30px; }
    h1 { margin: 8px 0 0; font-size: 1.4rem; font-weight: 800; letter-spacing: -.02em; }
    .sub { margin: 0 0 6px; color: var(--jl-ink-soft); font-size: .88rem; }
    label { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; font-weight: 600; color: var(--jl-ink-soft); }
    input {
      font: inherit; font-weight: 400; color: var(--jl-ink); background: var(--jl-surface-alt);
      border: 1px solid var(--jl-line-strong); border-radius: 8px; padding: 9px 11px;
    }
    input:focus-visible { outline: 2px solid var(--jl-blue); outline-offset: 1px; border-color: var(--jl-blue); }
    .btn {
      margin-top: 6px; background: var(--jl-blue); color: #fff; border: none; border-radius: 8px;
      padding: 10px 14px; font: inherit; font-weight: 600; cursor: pointer;
    }
    .btn:hover:not(:disabled) { background: var(--jl-blue-strong); }
    .btn:disabled { opacity: .6; cursor: default; }
    .error { margin: 0; color: var(--jl-red); font-size: .82rem; font-weight: 600; }
    .hint { margin: 4px 0 0; color: var(--jl-ink-faint); font-size: .76rem; text-align: center; }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly pending = signal(false);
  protected readonly error = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.pending()) {
      return;
    }
    this.pending.set(true);
    this.error.set(false);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/akten';
        void this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.error.set(true);
        this.pending.set(false);
      },
    });
  }
}
