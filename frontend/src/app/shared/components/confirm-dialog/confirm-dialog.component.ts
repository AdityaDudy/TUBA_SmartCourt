import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalShellComponent } from '../modal-shell/modal-shell.component';
import { AppButtonComponent } from '../button/button.component';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, ModalShellComponent, AppButtonComponent],
  template: `
    <app-modal-shell [open]="open" [title]="title" icon="triangle-exclamation" maxWidth="420px" (close)="cancel.emit()">
      <p style="margin: 0; font-size: 14px; color: var(--txt);">{{ message }}</p>
      <div class="confirm-actions" style="display:flex; gap:10px; justify-flex-end; margin-top:20px;">
        <app-button variant="outline" (onClick)="cancel.emit()">{{ cancelLabel }}</app-button>
        <app-button [variant]="danger ? 'danger' : 'primary'" (onClick)="confirm.emit()">{{ confirmLabel }}</app-button>
      </div>
    </app-modal-shell>
  `
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirm';
  @Input() message = 'Are you sure you want to proceed?';
  @Input() confirmLabel = 'Confirm';
  @Input() cancelLabel = 'Cancel';
  @Input() danger = true;
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
