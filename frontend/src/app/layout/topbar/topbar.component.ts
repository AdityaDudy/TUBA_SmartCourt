import { Component, inject, signal, Output, EventEmitter, computed, OnInit, HostListener, ElementRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { ThemeService } from '../../core/services/theme.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, FormsModule, CommonModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent {
  @Output() toggleSidebar = new EventEmitter<void>();

  readonly auth   = inject(AuthService);
  readonly data   = inject(DataService);
  readonly theme  = inject(ThemeService);
  readonly router = inject(Router);
  private  elementRef = inject(ElementRef);

  searchQuery     = signal('');
  notifPanelOpen  = signal(false);
  userMenuOpen    = signal(false);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.notifPanelOpen.set(false);
      this.userMenuOpen.set(false);
    }
  }

  get isDarkMode(): boolean {
    const mode = this.theme.mode();
    if (mode === 'system') {
      return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    }
    return mode === 'dark';
  }

  toggleTheme() {
    this.theme.setMode(this.isDarkMode ? 'light' : 'dark');
  }

  readonly notifications    = this.data.notifications;
  readonly unreadCount      = computed(() => this.data.unreadNotifCount());

  toggleNotif() {
    this.notifPanelOpen.update(v => !v);
    this.userMenuOpen.set(false);
    if (this.notifPanelOpen()) {
      this.data.loadNotifications().subscribe();
    }
  }

  toggleUserMenu() {
    this.userMenuOpen.update(v => !v);
    this.notifPanelOpen.set(false);
  }

  markAllRead() {
    this.data.markAllNotificationsRead().subscribe();
  }

  doSearch() {
    if (this.searchQuery().trim()) {
      this.router.navigate(['/app/matters'], { queryParams: { q: this.searchQuery() } });
    }
  }

  newMatter() {
    this.router.navigate(['/app/matters'], { queryParams: { new: '1' } });
  }

  logout() {
    this.auth.logout();
  }

  getIcon(type: string): string {
    const map: Record<string, string> = {
      hearing: 'fa-gavel', task: 'fa-list-check', billing: 'fa-receipt',
      warning: 'fa-triangle-exclamation', info: 'fa-circle-info'
    };
    return map[type] ?? 'fa-bell';
  }
}
