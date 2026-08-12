import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import {
  provideRouter,
  withViewTransitions,
  withComponentInputBinding,
  withRouterConfig,
} from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withViewTransitions({ skipInitialTransition: true }),
      withComponentInputBinding(),
      withRouterConfig({ onSameUrlNavigation: 'reload', canceledNavigationResolution: 'computed' }),
    ),
    provideHttpClient(
      withInterceptors([authInterceptor]),
      withFetch(),
    ),
    provideAnimationsAsync(),
  ],
};
