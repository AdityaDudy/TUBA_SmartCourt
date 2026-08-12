import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Reusable paginator component.
 * Emits 0-based page indices (Spring Page convention).
 *
 * Usage:
 *   <app-paginator [page]="page()" [size]="20" [total]="ds.matterTotal()"
 *                  (change)="onPageChange($event)" />
 */
@Component({
  selector: 'app-paginator',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pag-root" *ngIf="total > 0">
      <span class="pag-info">{{ from }}–{{ to }} of {{ total }}</span>
      <div class="pag-controls">
        <button class="pag-btn" [disabled]="page === 0"    (click)="go(0)"        title="First">«</button>
        <button class="pag-btn" [disabled]="page === 0"    (click)="go(page - 1)" title="Previous">‹</button>
        <span class="pag-page">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="pag-btn" [disabled]="page >= last"  (click)="go(page + 1)" title="Next">›</button>
        <button class="pag-btn" [disabled]="page >= last"  (click)="go(last)"     title="Last">»</button>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .pag-root {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
    }
    .pag-info {
      font-size: 13px;
      color: var(--text-muted, #64748b);
    }
    .pag-controls {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .pag-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border: 1px solid var(--border, #e2e8f0);
      border-radius: 8px;
      background: var(--surface, #fff);
      color: var(--text, #1e293b);
      font-size: 14px;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
    }
    .pag-btn:hover:not(:disabled) {
      background: var(--primary, #6366f1);
      color: #fff;
      border-color: var(--primary, #6366f1);
    }
    .pag-btn:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }
    .pag-page {
      font-size: 13px;
      min-width: 52px;
      text-align: center;
      color: var(--text-muted, #64748b);
    }
  `]
})
export class PaginatorComponent {
  @Input() page  = 0;
  @Input() size  = 20;
  @Input() total = 0;
  @Output() change = new EventEmitter<number>();

  get last()       { return Math.max(0, Math.ceil(this.total / this.size) - 1); }
  get totalPages() { return Math.max(1, Math.ceil(this.total / this.size)); }
  get from()       { return this.total === 0 ? 0 : this.page * this.size + 1; }
  get to()         { return Math.min((this.page + 1) * this.size, this.total); }

  go(p: number) {
    if (p >= 0 && p <= this.last && p !== this.page) {
      this.change.emit(p);
    }
  }
}
