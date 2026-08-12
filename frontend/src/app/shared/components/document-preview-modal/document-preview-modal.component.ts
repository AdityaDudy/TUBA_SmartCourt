import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-document-preview-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-preview-modal.component.html',
  styleUrl: './document-preview-modal.component.scss'
})
export class DocumentPreviewModalComponent {
  @Input() doc: any = null;
  @Input() details: any = null;
  @Output() close = new EventEmitter<void>();
  @Output() delete = new EventEmitter<any>();

  public auth = inject(AuthService);

  onClose() {
    this.close.emit();
  }

  onDelete() {
    this.delete.emit(this.doc);
    this.close.emit();
  }
}
