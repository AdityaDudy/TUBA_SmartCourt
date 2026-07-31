import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { API } from '../api/api-endpoints';
import { LoginRequest, LoginResponse, UserProfileDto } from '../api/api-response.types';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly ACCESS_TOKEN_KEY  = 'courtos_access_token';
  private readonly REFRESH_TOKEN_KEY = 'courtos_refresh_token';
  private readonly USER_KEY          = 'courtos_user';

  // ── Reactive state ────────────────────────────────────────
  private _user    = signal<UserProfileDto | null>(this._loadUser());
  private _loading = signal(false);

  readonly currentUser  = this._user.asReadonly();
  readonly isLoading    = this._loading.asReadonly();
  readonly isLoggedIn   = computed(() => !!this._user() && !!this.getAccessToken());
  readonly userRole     = computed(() => this._user()?.role ?? null);
  readonly userName     = computed(() => this._user()?.name ?? '');
  readonly userInitials = computed(() => this._user()?.initials ?? '?');
  readonly userGradient = computed(() => this._user()?.gradient ?? '');
  readonly userAvatar   = computed(() => this._user()?.avatar ?? null);
  readonly permissions  = computed(() => this._user()?.permissions ?? []);

  // ── Login ─────────────────────────────────────────────────
  login(email: string, password: string): Observable<any> {
    this._loading.set(true);
    const body: LoginRequest = { email, password };

    return this.http.post<any>(API.AUTH.LOGIN, body).pipe(
      tap(res => {
        // Unwrap data envelope from ApiResponse
        const payload = res.success ? res.data : res;
        this._persistSession(payload);
        this._loading.set(false);
        this.router.navigate(['/app/dashboard'], { replaceUrl: true });
      }),
      catchError(err => {
        this._loading.set(false);
        const msg = err?.error?.message ?? 'Invalid credentials. Please try again.';
        return throwError(() => new Error(msg));
      }),
    );
  }

  // ── Verify OTP ────────────────────────────────────────────
  verifyOtp(email: string, otp: string): Observable<any> {
    this._loading.set(true);
    return this.http.post<any>(API.AUTH.VERIFY_OTP, { email, otp }).pipe(
      tap(res => {
        const payload = res.success ? res.data : res;
        this._persistSession(payload);
        this._loading.set(false);
        this.router.navigate(['/app/dashboard'], { replaceUrl: true });
      }),
      catchError(err => {
        this._loading.set(false);
        return throwError(() => new Error(err?.error?.message ?? 'OTP verification failed.'));
      }),
    );
  }

  // ── Forgot Password ───────────────────────────────────────
  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(API.AUTH.FORGOT, { email });
  }

  // ── Refresh Token ─────────────────────────────────────────
  refreshToken(): Observable<{ accessToken: string }> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return throwError(() => new Error('No refresh token'));

    return this.http
      .post<any>(API.AUTH.REFRESH, { refreshToken })
      .pipe(
        map(res => {
          const payload = res.success ? res.data : res;
          return { accessToken: payload.accessToken };
        }),
        tap(({ accessToken }) => localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken)),
        catchError(err => {
          this.clearSession();
          return throwError(() => err);
        }),
      );
  }

  // ── Change Password ───────────────────────────────────────
  changePassword(oldPassword: string, newPassword: string): Observable<any> {
    return this.http.post<any>(`${API.AUTH.LOGIN.replace('/login', '')}/change-password`, { oldPassword, newPassword });
  }

  // ── Update Local & Server User Profile ────────────────────
  updateUserProfile(updatedUser: Partial<UserProfileDto>) {
    const current = this._user();
    if (!current) return;
    const merged = { ...current, ...updatedUser };
    this._user.set(merged);
    localStorage.setItem(this.USER_KEY, JSON.stringify(merged));
  }

  // ── Logout ────────────────────────────────────────────────
  logout() {
    const token = this.getAccessToken();
    if (token) {
      // Fire-and-forget logout API call
      this.http.post(API.AUTH.LOGOUT, {}).subscribe({ error: () => {} });
    }
    this.clearSession();
    this.router.navigate(['/auth']);
  }

  // ── Get Me (re-hydrate) ───────────────────────────────────
  loadCurrentUser(): Observable<any> {
    return this.http.get<any>(API.AUTH.ME).pipe(
      tap(res => {
        const user = res.success ? res.data : res;
        this._user.set(user);
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
      }),
      catchError(err => {
        if (err.status === 401) this.clearSession();
        return throwError(() => err);
      }),
    );
  }

  // ── Token management ─────────────────────────────────────
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  clearSession() {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem('courtos_ai_chat');
    this._user.set(null);
  }

  // ── Permission check ─────────────────────────────────────
  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  hasAnyPermission(...perms: string[]): boolean {
    return perms.some(p => this.permissions().includes(p));
  }

  isRole(...roles: string[]): boolean {
    return roles.includes(this.userRole() ?? '');
  }

  // ── Private helpers ───────────────────────────────────────
  private _persistSession(res: LoginResponse) {
    localStorage.setItem(this.ACCESS_TOKEN_KEY,  res.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, res.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(res.user));
    this._user.set(res.user);
  }

  private _loadUser(): UserProfileDto | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
