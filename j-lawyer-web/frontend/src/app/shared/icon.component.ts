import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Inline SVG icon set. Renders stroke icons via @switch (no innerHTML / no sanitizer
 * bypass — CSP-safe, design.md Decision 2c). Add new glyphs as @case blocks.
 */
@Component({
  selector: 'jl-icon',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @switch (name()) {
        @case ('desktop') {
          <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" />
        }
        @case ('cases') {
          <path d="M3 7a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        }
        @case ('contacts') {
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
        }
        @case ('calendar') {
          <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
        }
        @case ('mail') {
          <path d="M4 4h16v12H5.2L4 17.2z" />
        }
        @case ('doc') {
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" />
        }
        @case ('euro') {
          <path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
        }
        @case ('chart') {
          <path d="M3 3v18h18" /><path d="M7 15l4-4 3 3 5-6" />
        }
        @case ('gear') {
          <circle cx="12" cy="12" r="3" />
          <path
            d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-2.82 1.17V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 8 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15H4.5a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 6 9.4l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 12 4.6a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 2.82 1.18l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 12v.6z"
          />
        }
        @case ('search') {
          <circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" />
        }
        @case ('bell') {
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" />
        }
        @case ('more') {
          <circle cx="12" cy="12" r="9" /><path d="M12 8v8M8 12h8" />
        }
        @case ('grip') {
          <circle cx="9" cy="6" r="1" /><circle cx="15" cy="6" r="1" />
          <circle cx="9" cy="12" r="1" /><circle cx="15" cy="12" r="1" />
          <circle cx="9" cy="18" r="1" /><circle cx="15" cy="18" r="1" />
        }
        @case ('sun') {
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        }
        @case ('moon') {
          <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        }
        @case ('logout') {
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5M21 12H9" />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" />
        }
        @case ('phone') {
          <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2 4.2 2 2 0 0 1 4 2h3a2 2 0 0 1 2 1.7c.1.9.3 1.8.6 2.6a2 2 0 0 1-.5 2.1L7.6 9.8a16 16 0 0 0 6 6l1.4-1.1a2 2 0 0 1 2.1-.5c.8.3 1.7.5 2.6.6a2 2 0 0 1 1.7 2z" />
        }
        @case ('globe') {
          <circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a14 14 0 0 1 0 18 14 14 0 0 1 0-18z" />
        }
        @case ('pin') {
          <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z" /><circle cx="12" cy="10" r="3" />
        }
        @case ('pushpin') {
          <path d="M12 17v5" />
          <path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z" />
        }
        @case ('check') {
          <path d="M20 6 9 17l-5-5" />
        }
        @case ('download') {
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" />
        }
        @case ('refresh') {
          <path d="M21 12a9 9 0 1 1-2.64-6.36M21 3v6h-6" />
        }
        @case ('shield') {
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        }
        @case ('folder') {
          <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        }
        @case ('star') {
          <path d="M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9 6.8 19.2l1-5.8L3.5 9.2l5.9-.9z" />
        }
        @case ('eye') {
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" /><circle cx="12" cy="12" r="3" />
        }
        @case ('eye-off') {
          <path d="M17.9 17.9A10.5 10.5 0 0 1 12 19C5.5 19 2 12 2 12a18 18 0 0 1 5.1-5.9M9.9 4.2A10.5 10.5 0 0 1 12 4c6.5 0 10 7 10 7a18 18 0 0 1-2.2 3.2M9.5 9.5a3 3 0 0 0 4.2 4.2" />
          <path d="M2 2l20 20" />
        }
        @case ('tag') {
          <path d="M20.6 13.4 12 22l-9-9V4a1 1 0 0 1 1-1h8z" /><circle cx="7.5" cy="7.5" r="1.2" />
        }
        @case ('palette') {
          <path d="M12 3a9 9 0 0 0 0 18 2 2 0 0 0 2-2c0-1.1.9-2 2-2h1.5a3.5 3.5 0 0 0 3.5-3.5C21 6.9 17 3 12 3z" />
          <circle cx="7.5" cy="10.5" r="1" /><circle cx="12" cy="7.5" r="1" /><circle cx="16.5" cy="10.5" r="1" />
        }
        @case ('file-text') {
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" />
          <path d="M9 13h6M9 17h6" />
        }
        @case ('image') {
          <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="9" cy="9" r="2" /><path d="m21 15-5-5L5 21" />
        }
        @case ('archive') {
          <path d="M21 8v13H3V8" /><path d="M1 3h22v5H1z" /><path d="M10 12h4" />
        }
        @case ('sheet') {
          <rect x="3" y="3" width="18" height="18" rx="2" /><path d="M3 9h18M9 21V9" />
        }
        @case ('presentation') {
          <path d="M2 3h20" /><path d="M3 3v11a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V3" /><path d="M12 15v6M9 21h6" />
        }
        @case ('edit') {
          <path d="M12 20h9" /><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z" />
        }
        @case ('trash') {
          <path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
          <path d="M10 11v6M14 11v6" />
        }
        @case ('close') {
          <path d="M18 6 6 18M6 6l12 12" />
        }
        @case ('chevron-left') {
          <path d="m15 18-6-6 6-6" />
        }
        @case ('clock') {
          <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />
        }
        @case ('stop') {
          <rect x="6" y="6" width="12" height="12" rx="2" />
        }
        @case ('send') {
          <path d="M22 2 11 13" /><path d="M22 2 15 22l-4-9-9-4z" />
        }
        @case ('reply') {
          <polyline points="9 14 4 9 9 4" /><path d="M20 20v-7a4 4 0 0 0-4-4H4" />
        }
        @case ('reply-all') {
          <polyline points="7 14 2 9 7 4" /><polyline points="12 14 7 9 12 4" />
          <path d="M22 18v-2a4 4 0 0 0-4-4H8" />
        }
        @case ('forward') {
          <polyline points="15 14 20 9 15 4" /><path d="M4 20v-7a4 4 0 0 1 4-4h12" />
        }
        @case ('paperclip') {
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
        }
        @case ('inbox') {
          <path d="M22 12h-6l-2 3h-4l-2-3H2" />
          <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
        }
      }
    </svg>
  `,
  styles: [':host { display: inline-flex; line-height: 0; }'],
})
export class IconComponent {
  readonly name = input.required<string>();
  readonly size = input(18);
}
