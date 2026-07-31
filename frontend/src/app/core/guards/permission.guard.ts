import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const requiredPermission = route.data?.['permission'] as string | undefined;
  const requiredPermissions = route.data?.['permissions'] as string[] | undefined;

  if (requiredPermission && !auth.hasPermission(requiredPermission)) {
    router.navigate(['/app/dashboard']);
    return false;
  }

  if (requiredPermissions && !auth.hasAnyPermission(...requiredPermissions)) {
    router.navigate(['/app/dashboard']);
    return false;
  }

  return true;
};
