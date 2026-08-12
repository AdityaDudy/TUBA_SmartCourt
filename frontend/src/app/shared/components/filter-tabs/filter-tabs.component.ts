import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-filter-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="filter-tabs">
      @for (opt of options; track opt) {
        <button class="ftab" [class.active]="active === opt" (click)="select(opt)">{{ opt }}</button>
      }
    </div>
  `
})
export class FilterTabsComponent {
  @Input({ required: true }) options: string[] = [];
  @Input() active = '';
  @Output() activeChange = new EventEmitter<string>();

  select(opt: string) {
    this.activeChange.emit(opt);
  }
}
