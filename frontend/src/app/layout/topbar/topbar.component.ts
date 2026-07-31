import { Component, inject, signal, Output, EventEmitter, computed, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, FormsModule, CommonModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent implements OnInit {
  @Output() toggleSidebar = new EventEmitter<void>();

  readonly auth  = inject(AuthService);
  readonly data  = inject(DataService);
  readonly router = inject(Router);

  searchQuery     = signal('');
  notifPanelOpen  = signal(false);
  userMenuOpen    = signal(false);
  isDarkMode      = signal(false);

  ngOnInit() {
    const savedTheme = localStorage.getItem('user-theme') || 'green';
    this.isDarkMode.set(savedTheme === 'dark');
    const root = document.documentElement;
    if (savedTheme === 'dark') {
      root.setAttribute('data-theme', 'dark');
      root.removeAttribute('data-brand-theme');
    } else {
      root.setAttribute('data-theme', 'light');
      root.setAttribute('data-brand-theme', savedTheme);
    }
  }

  toggleTheme() {
    const root = document.documentElement;
    const isDark = root.getAttribute('data-theme') === 'dark';
    if (isDark) {
      root.setAttribute('data-theme', 'light');
      root.setAttribute('data-brand-theme', 'green');
      localStorage.setItem('user-theme', 'green');
      this.isDarkMode.set(false);
    } else {
      root.setAttribute('data-theme', 'dark');
      root.removeAttribute('data-brand-theme');
      localStorage.setItem('user-theme', 'dark');
      this.isDarkMode.set(true);
    }
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
