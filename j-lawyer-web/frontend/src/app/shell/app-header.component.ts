import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { ThemeService } from '../core/theme.service';

/**
 * Navy top bar: wordmark (logo colors), global search, notifications, theme toggle,
 * user avatar. Chrome stays navy in both themes (design-mockup.html).
 */
@Component({
  selector: 'jl-app-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <span class="wordmark"><span class="dots" aria-hidden="true"></span><b>j-lawyer</b><span class="sub">Kanzlei München</span></span>

    <label class="search">
      <jl-icon name="search" [size]="15" />
      <input type="search" placeholder="Akten, Kontakte, Dokumente durchsuchen …" aria-label="Suche" />
    </label>

    <span class="spacer"></span>

    <button type="button" class="icon-btn" aria-label="Nachrichten">
      <jl-icon name="mail" /><span class="badge">3</span>
    </button>
    <button type="button" class="icon-btn" aria-label="Benachrichtigungen">
      <jl-icon name="bell" /><span class="badge">5</span>
    </button>
    <button type="button" class="icon-btn" [attr.aria-label]="themeLabel()" (click)="theme.toggle()">
      <jl-icon [name]="theme.theme() === 'dark' ? 'sun' : 'moon'" />
    </button>
    <span class="avatar" title="Dr. Kunze">DK</span>
  `,
  styles: [`
    :host {
      display: flex; align-items: center; gap: 14px; height: 100%; padding: 0 14px;
      background: var(--jl-navy); color: #eaf1f7; border-bottom: 1px solid var(--jl-navy-line);
    }
    .wordmark { display: flex; align-items: center; gap: 8px; font-weight: 800; letter-spacing: -.02em; }
    .wordmark .dots {
      width: 9px; height: 9px; border-radius: 50%; background: var(--jl-red);
      box-shadow: 14px 0 0 var(--jl-green), 28px 0 0 var(--jl-blue); margin-right: 30px;
    }
    .wordmark b { color: #fff; }
    .wordmark .sub { color: #7f97ab; font-weight: 600; font-size: .82rem; }
    .search {
      flex: 1; max-width: 440px; display: flex; align-items: center; gap: 8px;
      background: #ffffff14; border: 1px solid #ffffff1f; color: #cddae6;
      padding: 7px 12px; border-radius: 8px;
    }
    .search input { all: unset; flex: 1; color: #eaf1f7; min-width: 0; font: inherit; }
    .search input::placeholder { color: #8ba0b2; }
    .spacer { flex: 1; }
    .icon-btn {
      position: relative; width: 34px; height: 34px; display: grid; place-items: center;
      border-radius: 8px; background: transparent; border: 1px solid transparent;
      color: #b9c9d6; cursor: pointer; transition: background .13s ease, color .13s ease;
    }
    .icon-btn:hover { background: #ffffff14; color: #fff; }
    .badge {
      position: absolute; top: 1px; right: 1px; min-width: 15px; height: 15px; padding: 0 3px;
      background: var(--jl-red); color: #fff; font-size: .62rem; font-weight: 700;
      border-radius: 8px; display: grid; place-items: center; border: 2px solid var(--jl-navy);
    }
    .avatar {
      width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center;
      background: linear-gradient(135deg, var(--jl-blue), #0a5b90); color: #fff;
      font-size: .78rem; font-weight: 700;
    }
    @media (max-width: 680px) { .search { display: none; } .wordmark .sub { display: none; } }
  `],
})
export class AppHeaderComponent {
  protected readonly theme = inject(ThemeService);
  protected readonly themeLabel = computed(() =>
    this.theme.theme() === 'dark' ? 'Zu hellem Design wechseln' : 'Zu dunklem Design wechseln',
  );
}
