import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast-container.component.html',
  styleUrl: './toast-container.component.scss',
})
export class ToastContainerComponent {
  readonly toast = inject(ToastService);

  readonly cssClass: Record<string, string> = {
    success: 'toast-g',
    error:   'toast-r',
    warning: 'toast-a',
    info:    'toast-b',
  };
}
