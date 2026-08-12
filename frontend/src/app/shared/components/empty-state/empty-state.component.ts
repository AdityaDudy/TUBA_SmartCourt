import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="empty-state">
      <div class="es-icon-wrap">
        <i class="fas" [class]="'fa-' + icon"></i>
      </div>
      <div class="es-title">{{ title }}</div>
      @if (subtitle) {
        <div class="es-sub">{{ subtitle }}</div>
      }
      @if (actionLabel && actionRoute) {
        <a class="btn-primary btn-sm" [routerLink]="actionRoute" style="margin-top:8px">
          <i class="fas fa-plus"></i> {{ actionLabel }}
        </a>
      }
      @if (actionLabel && !actionRoute) {
        <button class="btn-primary btn-sm" (click)="action.emit()" style="margin-top:8px">
          <i class="fas fa-plus"></i> {{ actionLabel }}
        </button>
      }
    </div>
  `
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Nothing here yet';
  @Input() subtitle = '';
  @Input() actionLabel = '';
  @Input() actionRoute = '';
  @Output() action = new EventEmitter<void>();
}
