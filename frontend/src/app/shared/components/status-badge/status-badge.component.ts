import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="colorClass">{{ label ?? status }}</span>`
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;
  @Input() label?: string;
  @Input() colorMap?: Record<string, string>;

  get colorClass(): string {
    const map = this.colorMap ?? StatusBadgeComponent.DEFAULT_MAP;
    return map[this.status] ?? 'b-t';
  }

  static DEFAULT_MAP: Record<string, string> = {
    'Active': 'b-g',
    'Closed': 'b-t',
    'Urgent': 'b-r',
    'High': 'b-a',
    'Medium': 'b-g',
    'Paid': 'b-g',
    'Unpaid': 'b-a',
    'Overdue': 'b-r',
    'Partially Paid': 'b-a-outline',
    'Draft': 'b-t',
    'Filed': 'b-g',
    'Pending': 'b-a',
  };
}
