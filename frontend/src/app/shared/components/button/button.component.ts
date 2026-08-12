import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [class]="classes"
      [disabled]="disabled || loading"
      [type]="type"
      (click)="onClick.emit($event)">
      @if (loading) { <i class="fas fa-spinner fa-spin"></i> }
      @if (!loading && icon) { <i class="fas" [class]="'fa-' + icon"></i> }
      <ng-content></ng-content>
    </button>
  `
})
export class AppButtonComponent {
  @Input() variant: 'primary' | 'outline' | 'ghost' | 'danger' | 'icon' = 'primary';
  @Input() size: 'xs' | 'sm' | 'md' = 'md';
  @Input() icon = '';
  @Input() disabled = false;
  @Input() loading = false;
  @Input() type: 'button' | 'submit' = 'button';
  @Output() onClick = new EventEmitter<MouseEvent>();

  get classes(): string {
    const base = `btn-${this.variant}`;
    return this.size === 'md' ? base : `btn-${this.size} ${base}`;
  }
}
