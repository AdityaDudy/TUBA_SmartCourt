import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
  icon: string;
  duration: number;
}

const ICONS: Record<ToastType, string> = {
  success: 'fa-check-circle',
  error:   'fa-times-circle',
  warning: 'fa-exclamation-triangle',
  info:    'fa-info-circle',
};

@Injectable({ providedIn: 'root' })
export class ToastService {
  toasts = signal<ToastMessage[]>([]);

  show(message: string, type: ToastType = 'success', duration = 3500) {
    const id = `toast_${Date.now()}_${Math.random()}`;
    const toast: ToastMessage = { id, message, type, icon: ICONS[type], duration };

    this.toasts.update((list) => [...list, toast]);

    setTimeout(() => this.dismiss(id), duration);
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string)   { this.show(message, 'error'); }
  warning(message: string) { this.show(message, 'warning'); }
  info(message: string)    { this.show(message, 'info'); }

  dismiss(id: string) {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
