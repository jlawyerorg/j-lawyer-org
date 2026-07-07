import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** True for calls to the j-lawyer REST API (not static assets like i18n JSON). */
function isApiRequest(url: string): boolean {
  return url.includes('/rest/') || url.includes('/j-lawyer-io/');
}

/**
 * Attaches the Bearer access token to API requests and, on a 401, signs the user out
 * (design.md Decision 5). Asset requests (e.g. i18n/*.json) are left untouched.
 * When real REST + refresh lands, extend the 401 branch to try a silent refresh first.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const api = isApiRequest(req.url);

  const request =
    api && auth.token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${auth.token}` } })
      : req;

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      if (api && err.status === 401) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
