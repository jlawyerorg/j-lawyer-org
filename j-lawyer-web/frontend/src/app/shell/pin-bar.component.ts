import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { PinnedItem, PinsService } from './pins.service';

/**
 * Header pin bar: a persistent, horizontal strip of pinned cases/contacts (browser-tab style),
 * shown across every module. One click opens the item's detail; the "×" unpins it. Renders
 * nothing (and takes no space) while nothing is pinned. Overflow scrolls horizontally.
 */
@Component({
  selector: 'jl-pin-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent, TranslocoModule],
  template: `
    @if (pins.pins().length) {
      <div class="pinbar" [attr.aria-label]="'pins.label' | transloco">
        @for (p of pins.pins(); track p.kind + ':' + p.id) {
          <a class="pin" routerLinkActive="active" [routerLink]="link(p)" [title]="p.title || p.label">
            <jl-icon [name]="p.kind === 'case' ? 'cases' : 'contacts'" [size]="14" />
            <span class="pin-label">{{ p.label }}</span>
            <button type="button" class="unpin" [attr.aria-label]="'pins.unpin' | transloco"
                    (click)="unpin($event, p)">✕</button>
          </a>
        }
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .pinbar {
      display: flex; align-items: center; gap: 8px; overflow-x: auto; white-space: nowrap;
      padding: 7px 16px; background: var(--jl-surface); border-bottom: 1px solid var(--jl-line);
    }
    .pin {
      flex: none; display: inline-flex; align-items: center; gap: 7px; max-width: 240px;
      padding: 5px 6px 5px 11px; border: 1px solid var(--jl-line-strong); border-radius: 8px;
      background: var(--jl-surface); color: var(--jl-ink); text-decoration: none; font-size: .82rem; font-weight: 600;
    }
    .pin:hover { border-color: var(--jl-blue); }
    .pin.active { border-color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); color: var(--jl-blue); }
    .pin jl-icon { flex: none; color: var(--jl-ink-faint); }
    .pin.active jl-icon { color: var(--jl-blue); }
    .pin-label { overflow: hidden; text-overflow: ellipsis; }
    .unpin {
      flex: none; display: inline-grid; place-items: center; width: 18px; height: 18px; border-radius: 5px;
      border: 0; background: transparent; color: var(--jl-ink-faint); cursor: pointer; font-size: .72rem;
    }
    .unpin:hover { background: var(--jl-surface-alt); color: var(--jl-red); }
  `],
})
export class PinBarComponent {
  protected readonly pins = inject(PinsService);

  protected link(p: PinnedItem): unknown[] {
    return p.kind === 'case' ? ['/cases', p.id] : ['/contacts', p.id];
  }

  protected unpin(event: Event, p: PinnedItem): void {
    event.preventDefault();
    event.stopPropagation();
    this.pins.remove(p.kind, p.id);
  }
}
