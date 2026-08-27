import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, input, signal, viewChild } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { OfficeConfigService } from '../settings/office-config.service';

/**
 * Embeds the external Office editor (Collabora / OnlyOffice) for one document, filling its host
 * element (OpenSpec add-web-client, Decision 6). It fetches the editor config (which mints the WOPI
 * access token server-side after an ACL check) and embeds the editor the WOPI way: a form-POST of
 * `WOPISrc` + `access_token` into an iframe — no inline script, CSP-safe. Chrome-less: the surrounding
 * overlay / standalone page provides the title bar and close/pop-out controls.
 */
@Component({
  selector: 'jl-office-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    @if (loading()) { <p class="oe-msg">{{ 'akten.docs.office.loading' | transloco }}</p> }
    @else if (error()) { <p class="oe-msg err">{{ 'akten.docs.office.error' | transloco }}</p> }
    <!-- WOPI form-POST: submits WOPISrc + access_token into the editor iframe. -->
    <form #form [action]="action()" method="post" target="jl-office-frame" class="oe-form">
      <input type="hidden" name="access_token" [value]="token()" />
      <input type="hidden" name="access_token_ttl" [value]="tokenTtl()" />
    </form>
    <iframe name="jl-office-frame" #frame title="Office editor" class="oe-frame" [class.hidden]="loading() || error()"></iframe>
  `,
  styles: [`
    :host { display: block; position: relative; width: 100%; height: 100%; }
    .oe-msg { position: absolute; inset: 0; display: grid; place-items: center; margin: 0; color: var(--jl-ink-soft); font-size: .9rem; }
    .oe-msg.err { color: var(--jl-red); }
    .oe-form { display: none; }
    .oe-frame { width: 100%; height: 100%; border: 0; display: block; }
    .oe-frame.hidden { visibility: hidden; }
  `],
})
export class OfficeEditorComponent {
  readonly documentId = input.required<string>();

  private readonly office = inject(OfficeConfigService);
  private readonly form = viewChild<ElementRef<HTMLFormElement>>('form');

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly action = signal('');
  protected readonly token = signal('');
  protected readonly tokenTtl = signal('');
  private submitted = false;

  constructor() {
    // Fetch the editor config when the document id is available.
    effect((onCleanup) => {
      const id = this.documentId();
      if (!id) { return; }
      this.loading.set(true);
      this.error.set(false);
      this.submitted = false;
      const sub = this.office.editorConfig(id).subscribe({
        next: (cfg) => {
          this.token.set(cfg.accessToken);
          // access_token_ttl is the expiry as epoch millis (Collabora/WOPI convention).
          this.tokenTtl.set(String(Date.now() + cfg.accessTokenTtl * 1000));
          // The server returns the complete action URL (WOPISrc + provider placeholders resolved).
          this.action.set(cfg.urlsrc);
          this.loading.set(false);
        },
        error: () => { this.error.set(true); this.loading.set(false); },
      });
      onCleanup(() => sub.unsubscribe());
    });

    // Once the action is set and the form exists, submit it into the iframe (deferred so the
    // hidden-input [value] bindings are flushed to the DOM first).
    effect(() => {
      const f = this.form();
      if (!this.submitted && this.action() && this.token() && f) {
        this.submitted = true;
        setTimeout(() => f.nativeElement.submit(), 0);
      }
    });
  }
}
