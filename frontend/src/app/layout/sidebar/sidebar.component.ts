import { Component, Input, Output, EventEmitter, inject, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { CommonModule } from '@angular/common';

interface NavItem {
  label: string; icon: string; route: string;
  badge?: number; exact?: boolean;
  permission?: string;
}
interface NavSection { title: string; items: NavItem[]; }

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  @Input()  collapsed = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  readonly auth = inject(AuthService);
  readonly data = inject(DataService);
  readonly router = inject(Router);

  readonly unreadCount = computed(() => this.data.unreadNotifCount());

  readonly navSections: NavSection[] = [
    {
      title: 'Main',
      items: [
        { label: 'Dashboard',    icon: 'fa-gauge-high',       route: '/app/dashboard', exact: true },
        { label: 'Diary',        icon: 'fa-calendar-days',    route: '/app/diary' },
        { label: 'Cause List',   icon: 'fa-gavel',            route: '/app/cause-list' },
      ],
    },
    {
      title: 'Practice',
      items: [
        { label: 'Clients',      icon: 'fa-users',            route: '/app/clients' },
        { label: 'Matters',      icon: 'fa-folder-open',      route: '/app/matters' },
        { label: 'Court Tracker',icon: 'fa-magnifying-glass', route: '/app/tracker' },
        { label: 'Tasks',        icon: 'fa-list-check',       route: '/app/tasks' },
        { label: 'Filings',      icon: 'fa-file-arrow-up',    route: '/app/filings' },
      ],
    },
    {
      title: 'Knowledge',
      items: [
        { label: 'Documents',    icon: 'fa-folder',           route: '/app/documents' },
        { label: 'Knowledge Base',icon: 'fa-book-open',       route: '/app/knowledge-base' },
        { label: 'AI Assistant', icon: 'fa-robot',            route: '/app/ai-assistant' },
      ],
    },
    {
      title: 'Finance',
      items: [
        { label: 'Billing',      icon: 'fa-receipt',          route: '/app/billing' },
        { label: 'Reports',      icon: 'fa-chart-bar',        route: '/app/reports' },
      ],
    },
    {
      title: 'Admin',
      items: [
        { label: 'Users',        icon: 'fa-user-tie',         route: '/app/user-management', permission: 'manage_users' },
        { label: 'Masters',      icon: 'fa-sliders',          route: '/app/masters', permission: 'manage_users' },
        { label: 'Settings',     icon: 'fa-gear',             route: '/app/settings', permission: 'system_settings' },
      ],
    },
  ];

  logout() {
    this.auth.logout();
  }
}
