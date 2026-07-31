import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

/** Decode a JWT payload without verifying signature (client-side only). */
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // exp is in seconds; Date.now() is in ms
    return payload.exp ? payload.exp * 1000 < Date.now() : false;
  } catch {
    return true; // malformed token → treat as expired
  }
}

export const authGuard: CanActivateFn = (_route, _state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  const redirectToLogin = () =>
    router.createUrlTree(['/auth'], { queryParams: { returnUrl: _state.url } });

  const accessToken  = auth.getAccessToken();
  const refreshToken = auth.getRefreshToken();

  // No tokens at all → go to login immediately
  if (!accessToken || !refreshToken) {
    auth.clearSession();
    return redirectToLogin();
  }

  // Access token still valid → allow through
  if (!isTokenExpired(accessToken)) {
    return true;
  }

  // Access token expired but refresh token exists → try to refresh proactively.
  // This prevents the dashboard flash: we wait for the refresh result BEFORE
  // rendering any protected route.
  return auth.refreshToken().pipe(
    map(() => true),
    catchError(() => {
      auth.clearSession();
      return of(redirectToLogin());
    }),
  );
};
