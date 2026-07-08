import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { API_ROOT, AUTH_BASE } from '../api';
import { AuthService } from './auth.service';

/** A protected REST call: hits the API but is not one of the public auth endpoints. */
function isProtectedApiRequest(url: string): boolean {
  return url.includes(API_ROOT) && !url.includes(AUTH_BASE);
}

/**
 * Attaches the Bearer access token to protected API requests and, on a 401, signs the user
 * out (design.md Decision 5). The public auth endpoints (login/refresh/logout) are left
 * untouched — they carry no bearer and manage their own errors via AuthService — as are
 * static assets (e.g. i18n/*.json).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const protectedApi = isProtectedApiRequest(req.url);

  const request =
    protectedApi && auth.token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${auth.token}` } })
      : req;

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      if (protectedApi && err.status === 401) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
