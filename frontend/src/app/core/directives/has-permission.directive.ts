import { Directive, Input, TemplateRef, ViewContainerRef, inject, EffectRef, effect } from '@angular/core';
import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[hasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private readonly auth = inject(AuthService);
  private readonly templateRef = inject(TemplateRef<any>);
  private readonly viewContainer = inject(ViewContainerRef);

  private requiredPermissions: string[] = [];
  private hasView = false;

  @Input() set hasPermission(val: string | string[]) {
    if (typeof val === 'string') {
      this.requiredPermissions = [val];
    } else if (Array.isArray(val)) {
      this.requiredPermissions = val;
    } else {
      this.requiredPermissions = [];
    }
    this.updateView();
  }

  constructor() {
    effect(() => {
      // Re-evaluate whenever user permissions signal changes
      this.auth.permissions();
      this.updateView();
    });
  }

  private updateView() {
    const hasPerm = this.requiredPermissions.length === 0 || 
                    this.auth.hasAnyPermission(...this.requiredPermissions);

    if (hasPerm && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!hasPerm && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
