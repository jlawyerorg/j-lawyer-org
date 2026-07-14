import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';

/** One background photo and its location, as published in backgrounds/manifest.json. */
interface Background {
  file: string;
  country: string;
  description: string;
}

/**
 * Full-screen login page (outside the shell). Exchanges credentials for a session via
 * AuthService, then routes to the returnUrl (or /desktop). Reactive form, translated,
 * theme-aware. Demo credentials while on the mock backend: admin / a.
 *
 * Behind the login card a random photo from backgrounds/manifest.json is shown (mirroring
 * the desktop LoginDialog): the image auto-scales to fill the viewport (object-fit: cover),
 * its location is shown in a badge, and a button cycles to the next image. The photos are
 * pre-optimized for the web (max 1920px, progressive JPEG) so a remote client only pulls a
 * few hundred KB for the one image it shows.
 */
@Component({
  selector: 'jl-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="wrap">
      <div class="bg" aria-hidden="true">
        @if (current(); as bg) {
          <img class="photo" [class.shown]="imgLoaded()" [src]="'backgrounds/' + bg.file"
               (load)="imgLoaded.set(true)" alt="" />
        }
        <div class="scrim"></div>
      </div>

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

      @if (current(); as bg) {
        <div class="bg-info">
          @if (bg.description) {
            <span class="loc" [title]="bg.description"><jl-icon name="pin" [size]="14" />{{ bg.description }}</span>
          }
          @if (backgrounds().length > 1) {
            <button type="button" class="bg-next" (click)="nextBackground()"
                    [title]="'auth.nextBackground' | transloco" [attr.aria-label]="'auth.nextBackground' | transloco">
              <jl-icon name="refresh" [size]="15" />
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .wrap { position: relative; min-height: 100dvh; display: grid; place-items: center;
      background: var(--jl-ground); padding: 24px; overflow: hidden; }
    .bg { position: absolute; inset: 0; z-index: 0; }
    .photo { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;
      opacity: 0; transition: opacity .6s ease; }
    .photo.shown { opacity: 1; }
    /* Darken the photo for card + location legibility (works in light and dark themes). */
    .scrim { position: absolute; inset: 0;
      background: linear-gradient(180deg, rgba(11,27,44,.35) 0%, rgba(11,27,44,.20) 40%, rgba(11,27,44,.55) 100%); }
    .card {
      position: relative; z-index: 1;
      width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 12px;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      padding: 28px 28px 22px; box-shadow: 0 1px 2px rgba(11,27,44,.10), 0 18px 50px rgba(11,27,44,.35);
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
    /* Location badge + cycle button, bottom-left, on the photo. */
    .bg-info { position: absolute; z-index: 1; left: 16px; bottom: 14px;
      display: flex; align-items: center; gap: 8px; }
    .loc, .bg-next {
      display: inline-flex; align-items: center; gap: 6px; height: 30px;
      color: #fff; background: rgba(11,27,44,.42); border: 1px solid rgba(255,255,255,.20);
      border-radius: 999px; backdrop-filter: blur(4px); font-size: .76rem; font-weight: 600;
      text-shadow: 0 1px 2px rgba(0,0,0,.4);
    }
    .loc { padding: 0 12px 0 10px; max-width: min(60vw, 460px); }
    .loc :first-child { flex: none; opacity: .9; }
    .bg-next { width: 30px; justify-content: center; padding: 0; cursor: pointer; transition: background .12s; }
    .bg-next:hover { background: rgba(11,27,44,.62); }
    .bg-next:focus-visible { outline: 2px solid #fff; outline-offset: 1px; }
  `],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);

  protected readonly pending = signal(false);
  protected readonly error = signal(false);

  /** Shuffled background photos (empty when the manifest is missing → plain background). */
  protected readonly backgrounds = signal<Background[]>([]);
  private readonly index = signal(0);
  protected readonly imgLoaded = signal(false);
  protected readonly current = computed<Background | null>(() => this.backgrounds()[this.index()] ?? null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor() {
    // Load and shuffle the background manifest; failures just leave the plain ground colour.
    this.http.get<Background[]>('backgrounds/manifest.json').subscribe({
      next: (list) => this.backgrounds.set(shuffle(list ?? [])),
      error: () => this.backgrounds.set([]),
    });
  }

  /** Advances to the next background photo (wraps around), fading the new one in on load. */
  protected nextBackground(): void {
    const list = this.backgrounds();
    if (list.length < 2) {
      return;
    }
    this.imgLoaded.set(false);
    this.index.update((i) => (i + 1) % list.length);
  }

  protected submit(): void {
    if (this.form.invalid || this.pending()) {
      return;
    }
    this.pending.set(true);
    this.error.set(false);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/desktop';
        void this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.error.set(true);
        this.pending.set(false);
      },
    });
  }
}

/** Returns a new array with the elements shuffled (Fisher–Yates). */
function shuffle<T>(items: T[]): T[] {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}
