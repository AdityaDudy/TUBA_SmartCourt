import { Injectable, signal, effect, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type BrandTheme = 'green' | 'navy' | 'grey';
export type ThemeMode = 'light' | 'dark' | 'system';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private http = inject(HttpClient);
  private readonly BRAND_KEY = 'courtos_brand_theme';
  private readonly MODE_KEY = 'courtos_theme_mode';

  brand = signal<BrandTheme>(this._loadBrand());
  mode = signal<ThemeMode>(this._loadMode());

  constructor() {
    // Media query listener for system mode changes
    window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (this.mode() === 'system') {
        this._applyMode('system');
      }
    });

    effect(() => {
      this._applyBrand(this.brand());
      this._applyMode(this.mode());
    });
  }

  setBrand(b: BrandTheme) {
    this.brand.set(b);
    localStorage.setItem(this.BRAND_KEY, b);
    this.saveUserPreference(b, this.mode());
  }

  setMode(m: ThemeMode) {
    this.mode.set(m);
    localStorage.setItem(this.MODE_KEY, m);
    this.saveUserPreference(this.brand(), m);
  }

  private saveUserPreference(brand: BrandTheme, mode: ThemeMode) {
    const rawUser = localStorage.getItem('courtos_user');
    if (rawUser) {
      try {
        const u = JSON.parse(rawUser);
        if (u.id) {
          const themeStr = `${brand}:${mode}`;
          this.http.put(`${environment.apiBaseUrl}/users/${u.id}`, { theme: themeStr }).subscribe({
            error: () => {} // Silent catch for offline or unauthenticated mode
          });
        }
      } catch (ignored) {}
    }
  }

  private _loadBrand(): BrandTheme {
    const b = localStorage.getItem(this.BRAND_KEY) || localStorage.getItem('user-theme');
    if (b === 'navy' || b === 'grey' || b === 'green') return b;
    return 'green';
  }

  private _loadMode(): ThemeMode {
    const m = localStorage.getItem(this.MODE_KEY) as ThemeMode;
    if (m === 'light' || m === 'dark' || m === 'system') return m;
    const old = localStorage.getItem('courtos_theme');
    if (old === 'dark') return 'dark';
    return 'light';
  }

  private _applyBrand(b: BrandTheme) {
    const root = document.documentElement;
    if (b === 'green') {
      root.removeAttribute('data-brand-theme');
    } else {
      root.setAttribute('data-brand-theme', b);
    }
  }

  private _applyMode(m: ThemeMode) {
    const root = document.documentElement;
    let isDark = false;
    if (m === 'system') {
      isDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    } else {
      isDark = m === 'dark';
    }
    root.setAttribute('data-theme', isDark ? 'dark' : 'light');
  }
}
