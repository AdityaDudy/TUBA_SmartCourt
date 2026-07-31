import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface BulkAction {
  label: string;
  icon: string;
  danger?: boolean;
  fn: () => void;
}

@Component({
  selector: 'app-bulk-action-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bulk-bar" [class.visible]="selected > 0" role="toolbar" aria-label="Bulk actions">
      <span class="bulk-count">{{ selected }}</span>
      <span class="bulk-label">selected</span>
      <div class="bulk-sep"></div>
      @for (action of actions; track action.label) {
        <button
          class="bulk-action"
          [class.danger]="action.danger"
          (click)="action.fn()"
          [attr.aria-label]="action.label">
          <i class="fas" [class]="'fa-' + action.icon"></i>
          {{ action.label }}
        </button>
      }
      <div class="bulk-sep"></div>
      <button class="bulk-action" (click)="clear.emit()" aria-label="Clear selection">
        <i class="fas fa-times"></i>
      </button>
    </div>
  `
})
export class BulkActionBarComponent {
  @Input() selected = 0;
  @Input() actions: BulkAction[] = [];
  @Output() clear = new EventEmitter<void>();
}
