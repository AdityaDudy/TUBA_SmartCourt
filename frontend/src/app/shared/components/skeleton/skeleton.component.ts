import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- STAT strip skeletons -->
    @if (type === 'stat') {
      <div class="stats-grid">
        @for (i of rowArr; track i) {
          <div class="skel-stat">
            <div class="skel-line short xs"></div>
            <div class="skel-line lg" style="width:50%"></div>
            <div class="skel-line xs short"></div>
          </div>
        }
      </div>
    }

    <!-- Table skeletons -->
    @if (type === 'table') {
      <div class="skeleton-wrapper">
        @for (i of rowArr; track i) {
          <div class="skel-table-row" [style.grid-template-columns]="colTemplate">
            <div style="display:flex;align-items:center;gap:10px">
              <div class="skel-avatar"></div>
              <div style="flex:1"><div class="skel-line med"></div><div class="skel-line xs short"></div></div>
            </div>
            @for (j of colArr; track j) {
              <div class="skel-line" [class.short]="j % 2 === 0"></div>
            }
          </div>
        }
      </div>
    }

    <!-- Card skeleton -->
    @if (type === 'card') {
      @for (i of rowArr; track i) {
        <div class="skel-card" style="margin-bottom:12px">
          <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px">
            <div class="skel-rect" style="width:36px;height:36px;border-radius:9px;flex-shrink:0"></div>
            <div style="flex:1"><div class="skel-line med"></div><div class="skel-line xs short"></div></div>
          </div>
          <div class="skel-line"></div>
          <div class="skel-line med"></div>
          <div class="skel-line short"></div>
        </div>
      }
    }

    <!-- List skeleton (simple rows) -->
    @if (type === 'list') {
      <div class="skeleton-wrapper">
        @for (i of rowArr; track i) {
          <div style="display:flex;align-items:center;gap:10px;padding:10px 14px;border-bottom:1px solid var(--bdr)">
            <div class="skel-avatar"></div>
            <div style="flex:1"><div class="skel-line med"></div><div class="skel-line xs short"></div></div>
            <div class="skel-line" style="width:56px;margin:0"></div>
          </div>
        }
      </div>
    }
  `
})
export class SkeletonComponent {
  @Input() type: 'stat' | 'table' | 'card' | 'list' = 'list';
  @Input() rows = 5;
  @Input() cols = 4;

  get rowArr(): number[] { return Array.from({ length: this.rows }, (_, i) => i); }
  get colArr(): number[] { return Array.from({ length: this.cols - 1 }, (_, i) => i); }

  get colTemplate(): string {
    // First column is wide (avatar + name), rest equal
    return `2fr ${'1fr '.repeat(this.cols - 1).trim()}`;
  }
}
