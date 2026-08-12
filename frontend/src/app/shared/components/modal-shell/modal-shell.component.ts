import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal-shell',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (open) {
      <div class="modal-overlay" (click)="close.emit()">
        <div class="modal" [style.maxWidth]="maxWidth" [style.width]="width" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>@if (icon) {<i class="fas" [class]="'fa-' + icon"></i>} {{ title }}</h3>
            <button class="modal-close" (click)="close.emit()"><i class="fas fa-times"></i></button>
          </div>
          <div class="modal-body" [style.maxHeight]="bodyMaxHeight" [style.overflowY]="bodyMaxHeight ? 'auto' : null">
            <ng-content></ng-content>
          </div>
          @if (showFooter) {
            <div class="modal-footer">
              <ng-content select="[modal-footer]"></ng-content>
            </div>
          }
        </div>
      </div>
    }
  `
})
export class ModalShellComponent {
  @Input({ required: true }) open = false;
  @Input() title = '';
  @Input() icon = '';
  @Input() maxWidth = '650px';
  @Input() width = '90%';
  @Input() bodyMaxHeight = '70vh';
  @Input() showFooter = false;
  @Output() close = new EventEmitter<void>();
}
