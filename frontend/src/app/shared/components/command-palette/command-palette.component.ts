import {
  Component, HostListener, OnInit, signal, computed, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

export interface CommandItem {
  label: string;
  sub?: string;
  icon: string;
  shortcut?: string;
  group: string;
  route?: string;
  action?: () => void;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (open()) {
      <div class="cmd-backdrop" (click)="close()" (keydown.escape)="close()">
        <div class="cmd-palette" (click)="$event.stopPropagation()" role="dialog" aria-label="Command Palette" aria-modal="true">
          <!-- Input -->
          <div class="cmd-input-wrap">
            <i class="fas fa-magnifying-glass"></i>
            <input
              #cmdInput
              class="cmd-input"
              [(ngModel)]="query"
              (ngModelChange)="onQueryChange()"
              placeholder="Search pages, actions…"
              (keydown)="onKeyDown($event)"
              autocomplete="off"
              autofocus
            />
            <span class="cmd-kbd">ESC</span>
          </div>

          <!-- Results -->
          <div class="cmd-results" id="cmd-results">
            @if (filteredGroups().length === 0) {
              <div class="cmd-empty">
                <i class="fas fa-search" style="font-size:20px;margin-bottom:8px;display:block;color:var(--bdr2)"></i>
                No results for "<strong>{{ query }}</strong>"
              </div>
            }
            @for (group of filteredGroups(); track group.label) {
              <div class="cmd-group-label">{{ group.label }}</div>
              @for (item of group.items; track item.label; let gi = $index) {
                <div
                  class="cmd-item"
                  [class.focused]="isFocused(item)"
                  (click)="execute(item)"
                  (mouseenter)="setFocus(item)"
                  role="option">
                  <div class="cmd-item-icon"><i class="fas" [class]="'fa-' + item.icon"></i></div>
                  <div style="flex:1;min-width:0">
                    <div class="cmd-item-label">{{ item.label }}</div>
                    @if (item.sub) { <div class="cmd-item-sub">{{ item.sub }}</div> }
                  </div>
                  @if (item.shortcut) {
                    <span class="cmd-item-shortcut">{{ item.shortcut }}</span>
                  }
                </div>
              }
            }
          </div>

          <!-- Footer -->
          <div class="cmd-footer">
            <span class="cmd-hint"><span class="cmd-kbd">↑↓</span> navigate</span>
            <span class="cmd-hint"><span class="cmd-kbd">↵</span> open</span>
            <span class="cmd-hint"><span class="cmd-kbd">ESC</span> close</span>
          </div>
        </div>
      </div>
    }
  `
})
export class CommandPaletteComponent implements OnInit {
  readonly router = inject(Router);

  open     = signal(false);
  query    = '';
  focused  = signal<CommandItem | null>(null);

  readonly allItems: CommandItem[] = [
    // Navigation
    { label: 'Dashboard',       icon: 'gauge-high',      route: '/app/dashboard',       group: 'Navigate', sub: 'Main overview', shortcut: 'G D' },
    { label: 'Diary',           icon: 'calendar-days',   route: '/app/diary',            group: 'Navigate' },
    { label: 'Cause List',      icon: 'gavel',           route: '/app/cause-list',       group: 'Navigate' },
    { label: 'Clients',         icon: 'users',           route: '/app/clients',          group: 'Navigate' },
    { label: 'Matters',         icon: 'folder-open',     route: '/app/matters',          group: 'Navigate' },
    { label: 'Court Tracker',   icon: 'magnifying-glass',route: '/app/tracker',          group: 'Navigate' },
    { label: 'Tasks',           icon: 'list-check',      route: '/app/tasks',            group: 'Navigate' },
    { label: 'Filings',         icon: 'file-arrow-up',   route: '/app/filings',          group: 'Navigate' },
    { label: 'Documents',       icon: 'folder',          route: '/app/documents',        group: 'Navigate' },
    { label: 'Knowledge Base',  icon: 'book-open',       route: '/app/knowledge-base',   group: 'Navigate' },
    { label: 'AI Assistant',    icon: 'robot',           route: '/app/ai-assistant',     group: 'Navigate' },
    { label: 'Billing',         icon: 'receipt',         route: '/app/billing',          group: 'Navigate' },
    { label: 'Reports',         icon: 'chart-bar',       route: '/app/reports',          group: 'Navigate' },
    { label: 'User Management', icon: 'user-tie',        route: '/app/user-management',  group: 'Admin' },
    { label: 'Masters',         icon: 'sliders',         route: '/app/masters',          group: 'Admin' },
    { label: 'Settings',        icon: 'gear',            route: '/app/settings',         group: 'Admin' },
    { label: 'Audit Log',       icon: 'shield-halved',   route: '/app/settings',         group: 'Admin', sub: 'View all audit events' },
    // Actions (can add router-navigate-based quick actions)
    { label: 'New Matter',      icon: 'briefcase',       route: '/app/matters',          group: 'Quick Actions', sub: 'Open matters page' },
    { label: 'New Invoice',     icon: 'file-invoice',    route: '/app/billing',          group: 'Quick Actions', sub: 'Open billing page' },
    { label: 'Add Client',      icon: 'user-plus',       route: '/app/clients',          group: 'Quick Actions', sub: 'Open clients page' },
    { label: 'Add Task',        icon: 'plus',            route: '/app/tasks',            group: 'Quick Actions', sub: 'Open tasks page' },
  ];

  flatItems: CommandItem[] = [];

  ngOnInit() {
    this.flatItems = this.allItems;
  }

  filteredGroups = computed(() => {
    const q = this.query.toLowerCase().trim();
    const filtered = q
      ? this.allItems.filter(i =>
          i.label.toLowerCase().includes(q) ||
          (i.sub ?? '').toLowerCase().includes(q) ||
          i.group.toLowerCase().includes(q)
        )
      : this.allItems;

    // Group them
    const groups = new Map<string, CommandItem[]>();
    for (const item of filtered) {
      if (!groups.has(item.group)) groups.set(item.group, []);
      groups.get(item.group)!.push(item);
    }
    return Array.from(groups.entries()).map(([label, items]) => ({ label, items }));
  });

  get flatFiltered(): CommandItem[] {
    return this.filteredGroups().flatMap(g => g.items);
  }

  onQueryChange() {
    this.focused.set(this.flatFiltered[0] ?? null);
  }

  isFocused(item: CommandItem): boolean {
    return this.focused() === item;
  }

  setFocus(item: CommandItem) {
    this.focused.set(item);
  }

  onKeyDown(event: KeyboardEvent) {
    const flat = this.flatFiltered;
    const currentIdx = flat.indexOf(this.focused()!);

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      const next = flat[(currentIdx + 1) % flat.length];
      this.focused.set(next ?? null);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      const prev = flat[(currentIdx - 1 + flat.length) % flat.length];
      this.focused.set(prev ?? null);
    } else if (event.key === 'Enter') {
      const f = this.focused();
      if (f) this.execute(f);
    } else if (event.key === 'Escape') {
      this.close();
    }
  }

  execute(item: CommandItem) {
    if (item.route) {
      this.router.navigate([item.route]);
    } else if (item.action) {
      item.action();
    }
    this.close();
  }

  show() {
    this.open.set(true);
    this.query = '';
    this.focused.set(this.allItems[0] ?? null);
  }

  close() {
    this.open.set(false);
  }

  @HostListener('window:keydown', ['$event'])
  onGlobalKeydown(event: KeyboardEvent) {
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.open() ? this.close() : this.show();
    }
  }
}
