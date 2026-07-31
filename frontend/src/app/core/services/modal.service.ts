import { Injectable, signal } from '@angular/core';

export type ModalId =
  | 'add-matter'
  | 'add-client'
  | 'add-task'
  | 'add-filing'
  | 'add-invoice'
  | 'add-billing-entry'
  | 'upload-doc'
  | 'matter-detail'
  | 'invite-user'
  | 'edit-user'
  | 'manage-master'
  | 'notifications'
  | 'tracker-result';

export interface ModalState<T = unknown> {
  id: ModalId;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class ModalService {
  /** Currently open modal stack */
  private _stack = signal<ModalState[]>([]);
  stack = this._stack.asReadonly();

  open<T = unknown>(id: ModalId, data?: T) {
    this._stack.update((s) => [...s, { id, data }]);
    document.body.style.overflow = 'hidden';
  }

  close(id?: ModalId) {
    if (id) {
      this._stack.update((s) => s.filter((m) => m.id !== id));
    } else {
      // Close top-most
      this._stack.update((s) => s.slice(0, -1));
    }
    if (this._stack().length === 0) {
      document.body.style.overflow = '';
    }
  }

  closeAll() {
    this._stack.set([]);
    document.body.style.overflow = '';
  }

  isOpen(id: ModalId): boolean {
    return this._stack().some((m) => m.id === id);
  }

  getData<T = unknown>(id: ModalId): T | undefined {
    return this._stack().find((m) => m.id === id)?.data as T;
  }
}
