import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

let isRefreshing  = false;
const refreshDone = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  // Attach Bearer token to every request
  const token   = auth.getAccessToken();
  const authed  = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authed).pipe(
    catchError((err: HttpErrorResponse) => {

      // ── 401: attempt token refresh ──────────────────────
      if (err.status === 401 && !req.url.includes('/auth/')) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshDone.next(null);

          return auth.refreshToken().pipe(
            switchMap(({ accessToken }) => {
              isRefreshing = false;
              refreshDone.next(accessToken);
              return next(req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }));
            }),
            catchError(refreshErr => {
              isRefreshing = false;
              auth.clearSession();
              router.navigate(['/auth']);
              return throwError(() => refreshErr);
            }),
          );
        }

        // Queue pending requests until refresh completes
        return refreshDone.pipe(
          filter(t => t !== null),
          take(1),
          switchMap(t => next(req.clone({ setHeaders: { Authorization: `Bearer ${t}` } }))),
        );
      }

      // ── 403: forbidden ──────────────────────────────────
      if (err.status === 403) {
        router.navigate(['/app/dashboard']);
      }

      return throwError(() => err);
    }),
  );
};
