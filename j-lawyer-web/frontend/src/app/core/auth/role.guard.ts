import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Route guard that requires the user to hold at least one of the roles listed in the route's
 * `data.requiredRoles` (a `string[]`). Used by the role-scoped settings screens. When the user
 * lacks every required role they are redirected to the settings landing route, which forwards them
 * to the first screen they can access. Composes with {@link authGuard} (auth is checked first at the
 * shell level). The server still enforces each operation, so this guard is UX/navigation only.
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const required = (route.data?.['requiredRoles'] as string[] | undefined) ?? [];
  if (auth.hasAnyRole(required)) {
    return true;
  }
  return router.createUrlTree(['/settings']);
};
