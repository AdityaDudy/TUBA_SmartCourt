import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="search-wrap">
      <i class="fas fa-search"></i>
      <input type="text" [placeholder]="placeholder" [value]="value"
             (input)="onInput($event)">
    </div>
  `
})
export class SearchInputComponent {
  @Input() placeholder = 'Search…';
  @Input() value = '';
  @Input() debounceMs = 0;
  @Output() valueChange = new EventEmitter<string>();

  private timer: any;

  onInput(e: Event) {
    const v = (e.target as HTMLInputElement).value;
    if (!this.debounceMs) {
      this.valueChange.emit(v);
      return;
    }
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.valueChange.emit(v), this.debounceMs);
  }
}
