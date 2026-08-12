import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="error-state">
      <div class="es-icon-wrap">
        <i class="fas fa-triangle-exclamation"></i>
      </div>
      <div class="es-title">{{ title }}</div>
      <div class="es-sub">{{ message }}</div>
      @if (showRetry) {
        <button class="btn-outline btn-sm" (click)="retry.emit()" style="margin-top:8px">
          <i class="fas fa-rotate-right"></i> Try Again
        </button>
      }
    </div>
  `
})
export class ErrorStateComponent {
  @Input() title = 'Something went wrong';
  @Input() message = 'An error occurred while loading data.';
  @Input() showRetry = true;
  @Output() retry = new EventEmitter<void>();
}
