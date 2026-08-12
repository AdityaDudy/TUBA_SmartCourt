import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skel-stat stat-card">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value" [style.color]="color">{{ value }}</div>
      @if (sub) { <div class="stat-sub">{{ sub }}</div> }
    </div>
  `
})
export class StatCardComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) value!: string | number;
  @Input() sub = '';
  @Input() color = '';
}
