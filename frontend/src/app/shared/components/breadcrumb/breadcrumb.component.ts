import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

export interface BreadcrumbItem {
  label: string;
  route?: string;
  icon?: string;
}

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <nav class="breadcrumb" aria-label="Breadcrumb">
      @for (item of items; track item.label; let last = $last) {
        @if (!last && item.route) {
          <a class="bc-item" [routerLink]="item.route">
            @if (item.icon) { <i class="fas" [class]="'fa-' + item.icon"></i> }
            {{ item.label }}
          </a>
          <span class="bc-sep">
            <i class="fas fa-chevron-right" style="font-size:9px"></i>
          </span>
        } @else {
          <span class="bc-item active">
            @if (item.icon) { <i class="fas" [class]="'fa-' + item.icon"></i> }
            {{ item.label }}
          </span>
        }
      }
    </nav>
  `
})
export class BreadcrumbComponent {
  @Input() items: BreadcrumbItem[] = [];
}
