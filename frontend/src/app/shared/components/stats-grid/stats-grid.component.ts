import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../stat-card/stat-card.component';

@Component({
  selector: 'app-stats-grid',
  standalone: true,
  imports: [CommonModule, StatCardComponent],
  template: `
    <div class="stats-grid">
      @for (s of stats; track s.label) {
        <app-stat-card [label]="s.label" [value]="s.value" [sub]="s.sub ?? ''" [color]="s.color ?? ''"></app-stat-card>
      }
    </div>
  `
})
export class StatsGridComponent {
  @Input({ required: true }) stats: { label: string; value: string | number; sub?: string; color?: string }[] = [];
}
