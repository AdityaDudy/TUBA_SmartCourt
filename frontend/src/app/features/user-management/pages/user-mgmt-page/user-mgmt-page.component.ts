import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { User, UserRole, UserStatus, ALL_PERMISSIONS, ROLE_LABELS, DATA_SCOPES, DataScopeType } from '../../../../core/models';
import { forkJoin, of } from 'rxjs';
import { SkeletonComponent } from '../../../../shared/components/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { BulkActionBarComponent, BulkAction } from '../../../../shared/components/bulk-action-bar/bulk-action-bar.component';
import { CountUpDirective } from '../../../../shared/directives/count-up.directive';

import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { AppButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-user-mgmt-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    EmptyStateComponent,
    BulkActionBarComponent,
    CountUpDirective,
    SearchInputComponent,
    AppButtonComponent,
    SkeletonComponent
  ],
  templateUrl: './user-mgmt-page.component.html',
  styleUrl: './user-mgmt-page.component.scss'
})
export class UserMgmtPageComponent implements OnInit {
  private ds = inject(DataService);
  private toast = inject(ToastService);
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private router = inject(Router);

  // Expose String for template
  readonly String = String;

  // Lists & data sources
  users = this.ds.users;
  matters = this.ds.matters;
  masters = this.ds.masters;
  loading = this.ds.usersLoading;

  getUserMattersCount(user: User): string {
    if (!user) return '0 matters';
    if (user.role === 'admin') {
      return `All (${this.matters().length})`;
    }
    if (user.role === 'clerk') {
      return 'Filing only';
    }
    const count = this.matters().filter(m => m.advocate === user.name || m.adv === user.name).length;
    return `${count} ${count === 1 ? 'matter' : 'matters'}`;
  }

  sessions = signal<any[]>([]);
  auditLog = this.ds.auditLog;
  ipWhitelist = signal<any[]>([]);

  // Pagination
  auditPage = signal(1);
  auditPageSize = 10;
  paginatedAuditLog = computed(() => {
    const start = (this.auditPage() - 1) * this.auditPageSize;
    const logs = this.auditLog();
    return logs.slice(start, start + this.auditPageSize);
  });
  auditTotalPages = computed(() => {
    const logs = this.auditLog();
    return Math.ceil(logs.length / this.auditPageSize) || 1;
  });

  sessionPage = signal(1);
  sessionPageSize = 3;
  paginatedSessions = computed(() => {
    const start = (this.sessionPage() - 1) * this.sessionPageSize;
    return this.sessions().slice(start, start + this.sessionPageSize);
  });
  sessionTotalPages = computed(() => {
    return Math.ceil(this.sessions().length / this.sessionPageSize) || 1;
  });

  // Filtering & Search
  activeRoleTab = signal<string>('all');
  searchQuery = signal<string>('');

  // Bulk select
  selectedUsers = signal<Set<string>>(new Set());
  selectedCount = computed(() => this.selectedUsers().size);

  toggleUserSelect(userId: string) {
    const current = new Set(this.selectedUsers());
    if (current.has(userId)) { current.delete(userId); } else { current.add(userId); }
    this.selectedUsers.set(current);
  }

  isUserSelected(userId: string): boolean {
    return this.selectedUsers().has(userId);
  }

  toggleSelectAll() {
    const filtered = this.filteredUsers();
    if (this.selectedUsers().size === filtered.length) {
      this.selectedUsers.set(new Set());
    } else {
      this.selectedUsers.set(new Set(filtered.map(u => String(u.id))));
    }
  }

  usersLoading = this.ds.usersLoading;

  clearSelection() {
    this.selectedUsers.set(new Set());
  }

  isAllSelected(): boolean {
    const filtered = this.filteredUsers();
    return filtered.length > 0 && this.selectedUsers().size === filtered.length;
  }

  openInviteModal() {
    this.inviteForm.set({
      name: '',
      email: '',
      password: '',
      mobile: '',
      role: 'advocate',
      dept: 'Litigation',
      designation: 'Associate Advocate',
      barCouncilNo: ''
    });
    this.showInviteForm.set(true);
  }

  bulkActions: BulkAction[] = [
    {
      label: 'Deactivate',
      icon: 'user-slash',
      danger: true,
      fn: () => {
        const ids = Array.from(this.selectedUsers());
        if (ids.length === 0) return;
        this.toast.show(`Deactivating ${ids.length} users…`, 'info');
        this.clearSelection();
      }
    },
    {
      label: 'Export CSV',
      icon: 'file-export',
      fn: () => {
        const ids = Array.from(this.selectedUsers());
        const selectedList = this.users().filter(u => ids.includes(String(u.id)));
        if (selectedList.length === 0) return;

        const headers = ['ID', 'Name', 'Email', 'Role', 'Department', 'Designation', 'Mobile', 'Bar Council No', 'Status'];
        const rows = selectedList.map(u => [
          u.id,
          `"${u.name || ''}"`,
          `"${u.email || ''}"`,
          `"${u.role || ''}"`,
          `"${u.department || ''}"`,
          `"${u.designation || ''}"`,
          `"${u.mobile || ''}"`,
          `"${u.barCouncilNo || ''}"`,
          `"${u.status || ''}"`
        ]);

        const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', `system_users_export_${new Date().toISOString().split('T')[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        this.toast.success(`Exported ${selectedList.length} users to CSV`);
        this.clearSelection();
      }
    }
  ];

  // Dynamically retrieved roles from masters
  availableRoles = computed(() => {
    return this.masters()?.roles || ['admin', 'senior', 'advocate', 'clerk', 'readonly'];
  });

  // Selected elements for detail/editing
  selectedUser = signal<User | null>(null);
  selectedUserSessions = signal<any[]>([]);
  selectedUserAudit = signal<any[]>([]);
  editTab = signal<'profile' | 'permissions' | 'sessions' | 'activity'>('profile');

  // Matrix Configuration
  matrixRole = signal<string>('admin');
  rolePermissions = signal<Record<string, string[]>>({
    admin: ALL_PERMISSIONS.map(p => p.key).concat(['scope_org']),
    senior: ['view_all', 'create_matters', 'edit_matters', 'view_docs', 'upload_docs', 'manage_tasks', 'manage_tasks_assign', 'manage_tasks_close', 'view_billing', 'export_billing', 'manage_clients', 'export_data', 'scope_team'],
    advocate: ['view_all', 'create_matters', 'edit_matters', 'view_docs', 'upload_docs', 'manage_tasks_close', 'view_own_billing', 'scope_own'],
    clerk: ['view_all', 'view_docs', 'upload_docs', 'manage_tasks_close', 'view_own_billing', 'scope_own'],
    readonly: ['view_all', 'view_docs', 'scope_own']
  });
  allPermissions = ALL_PERMISSIONS;
  roleLabels = ROLE_LABELS;
  dataScopes = DATA_SCOPES;

  getRoleScope(role: string): DataScopeType {
    const perms = this.rolePermissions()[role] || [];
    if (perms.includes('scope_org') || role === 'admin') return 'scope_org';
    if (perms.includes('scope_team')) return 'scope_team';
    return 'scope_own';
  }

  setRoleScope(role: string, targetScope: DataScopeType) {
    let currentPerms = [...(this.rolePermissions()[role] || [])];
    // Remove all existing scope tokens
    currentPerms = currentPerms.filter(p => !['scope_org', 'scope_team', 'scope_own'].includes(p));
    // Add selected scope token
    currentPerms.push(targetScope);

    this.http.put(`${environment.apiBaseUrl}/users/roles/${role}/permissions`, { permissions: currentPerms }).subscribe({
      next: () => {
        this.toast.success(`Scope for role '${role}' updated to ${targetScope}.`);
        this.rolePermissions.update(map => ({
          ...map,
          [role]: currentPerms
        }));
        this.ds.loadUsers().subscribe();
      },
      error: (err) => {
        this.toast.error(`Failed to update role scope.`);
        console.error(err);
      }
    });
  }

  // Modals visibility
  showInviteForm = signal(false);
  showEditForm = signal(false);
  showSuspendConfirm = signal(false);
  userToSuspend = signal<User | null>(null);

  // Security config inputs
  forceMfa = signal<boolean>(true);
  maxAttempts = signal<number>(5);
  sessionTimeout = signal<string>('1 hr');

  // Whitelist IP form
  newIpAddress = signal('');
  newIpLabel = signal('');
  newIpBlocked = signal(false);

  // Forms
  inviteForm = signal({
    name: '',
    email: '',
    password: '',
    mobile: '',
    role: 'advocate',
    dept: 'Litigation',
    designation: 'Associate Advocate',
    barCouncilNo: ''
  });

  // Edit user profile form
  editUserForm = signal({
    name: '',
    email: '',
    password: '',
    mobile: '',
    role: 'advocate',
    department: '',
    designation: '',
    status: 'active' as UserStatus,
    avatar: ''
  });

  getRoleLabel(role: string): string {
    if (!role) return '';
    const r = role.toLowerCase();
    if (this.roleLabels[r]) return this.roleLabels[r];
    return role.charAt(0).toUpperCase() + role.slice(1).replace(/([A-Z])/g, ' $1').trim();
  }

  getRoleBadgeClass(role: string): string {
    if (!role) return 'badge-clerk';
    const r = role.toLowerCase();
    if (r === 'admin') return 'badge-admin';
    if (r === 'senior') return 'badge-senior';
    if (r === 'advocate') return 'badge-adv';
    return 'badge-clerk';
  }

  // Helper to check if a user has a live active session
  isUserOnline(user: User): boolean {
    if (!user) return false;
    const userSessions = this.sessions().filter(s => 
      (s.user && (s.user === user.name || s.user === user.email)) || s.userId === user.id
    );
    return userSessions.length > 0;
  }

  // Derived user statistics
  totalUsersCount = computed(() => this.users().length);
  onlineUsersCount = computed(() => this.users().filter(u => this.isUserOnline(u)).length);
  pendingInvitesCount = computed(() => this.users().filter(u => u.email.endsWith('@invited.com') || u.status === ('invited' as any)).length);
  inactiveUsersCount = computed(() => this.users().filter(u => u.status === 'inactive').length);
  mfaEnabledCount = computed(() => this.users().filter(u => u.mfa).length);
  mfaEnabledPercentage = computed(() => {
    const total = this.totalUsersCount();
    if (!total) return 0;
    return Math.round((this.mfaEnabledCount() / total) * 100);
  });

  // Filtered Users List
  filteredUsers = computed(() => {
    let list = this.users();

    // 1. Role Filter
    const tab = this.activeRoleTab();
    if (tab !== 'all') {
      list = list.filter(u => u.role.toLowerCase() === tab.toLowerCase());
    }

    // 2. Search query filter
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      list = list.filter(u =>
        u.name.toLowerCase().includes(query) ||
        u.email.toLowerCase().includes(query) ||
        (u.department && u.department.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit() {
    if (!this.auth.hasPermission('manage_users')) {
      this.router.navigate(['/app/dashboard']);
      return;
    }
    this.refreshAll();
    this.ds.loadMasters().subscribe();
    this.ds.loadMatters().subscribe();
  }

  refreshAll() {
    this.ds.loadUsers().subscribe();
    this.loadGeneralData();
  }

  getInitials(name: string): string {
    if (!name) return 'US';
    // Strip honorifics
    const clean = name.replace(/^(Adv\.|Dr\.|Mr\.|Ms\.|Mrs\.)\s+/i, '').trim();
    const parts = clean.split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }

  loadGeneralData() {
    this.ds.getAllSessions().subscribe(s => {
      this.sessions.set(s);
      this.sessionPage.set(1);
    });
    this.ds.loadAuditLog().subscribe(() => {
      this.auditPage.set(1);
    });
    this.ds.getIpWhitelist().subscribe(ips => this.ipWhitelist.set(ips));
    this.http.get<any>(`${environment.apiBaseUrl}/settings/security`).subscribe({
      next: (res) => {
        if (res && res.data) {
          this.forceMfa.set(res.data.mfaEnabled);
          this.maxAttempts.set(res.data.maxAttempts || 5);
          this.sessionTimeout.set(res.data.sessionTimeout || '1 hr');
        }
      }
    });
    this.http.get<any>(`${environment.apiBaseUrl}/users/roles/permissions`).subscribe({
      next: (res) => {
        if (res && res.data) {
          this.rolePermissions.set(res.data);
        }
      }
    });
  }

  // Invite action
  invite() {
    const data = {
      ...this.inviteForm(),
      // Backend mapping compatibility
      dept: this.inviteForm().dept
    };
    this.ds.inviteUser(data).subscribe({
      next: () => {
        this.showInviteForm.set(false);
        this.inviteForm.set({
          name: '',
          email: '',
          password: '',
          mobile: '',
          role: 'advocate',
          dept: 'Litigation',
          designation: 'Associate Advocate',
          barCouncilNo: ''
        });
        this.refreshAll();
      }
    });
  }

  onUserAvatarChange(event: Event, user: User) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        const result = e.target?.result as string;
        if (!result) return;
        this.selectedUser.set({ ...user, avatar: result });
        this.editUserForm.update(f => ({ ...f, avatar: result }));
        this.ds.updateUser(user.id, { avatar: result } as any).subscribe({
          next: (updated) => {
            if (user.id === this.auth.currentUser()?.id) {
              this.auth.updateUserProfile({ avatar: result });
            }
            this.refreshAll();
            this.toast.success(`Profile picture updated for ${user.name}`);
          },
          error: () => {
            this.toast.error('Failed to save profile picture.');
          }
        });
      };
      reader.readAsDataURL(file);
    }
  }

  // Open edit modal
  openEditModal(user: User) {
    this.selectedUser.set(user);
    this.editTab.set('profile');
    this.editUserForm.set({
      name: user.name,
      email: user.email,
      password: '',
      mobile: user.mobile || '',
      role: user.role,
      department: user.department || '',
      designation: user.designation || '',
      status: user.status,
      avatar: user.avatar || ''
    });

    // Fetch sessions and audit logs for this user
    this.ds.getUserSessions(user.id).subscribe(s => this.selectedUserSessions.set(s));
    // Filter audit logs for this specific user
    this.selectedUserAudit.set(this.auditLog().filter(l => l.userId === user.id || l.userEmail === user.email));

    this.showEditForm.set(true);
  }

  saveProfile() {
    const user = this.selectedUser();
    if (!user) return;

    this.ds.updateUser(user.id, this.editUserForm()).subscribe({
      next: () => {
        this.showEditForm.set(false);
        this.refreshAll();
      }
    });
  }

  confirmSuspend(user: User, event: Event) {
    event.stopPropagation();
    this.userToSuspend.set(user);
    this.showSuspendConfirm.set(true);
  }

  suspendConfirmed() {
    const user = this.userToSuspend();
    if (!user) return;
    this.ds.suspendUser(user.id).subscribe({
      next: () => {
        this.showSuspendConfirm.set(false);
        this.refreshAll();
      }
    });
  }

  activateUser(user: User, event: Event) {
    event.stopPropagation();
    // Use put directly since DataService doesn't have activateUser helper
    this.ds.updateUser(user.id, { status: 'active' }).subscribe(() => {
      this.refreshAll();
    });
  }

  // Permissions overrides
  hasPermission(permKey: string): boolean {
    const user = this.selectedUser();
    if (!user) return false;
    return user.permissions?.includes(permKey) || false;
  }

  getUserScope(): DataScopeType {
    const user = this.selectedUser();
    if (!user) return 'scope_own';
    const perms = user.permissions || [];
    if (perms.includes('scope_org') || user.role === 'admin') return 'scope_org';
    if (perms.includes('scope_team')) return 'scope_team';
    return 'scope_own';
  }

  setUserScope(targetScope: DataScopeType) {
    const user = this.selectedUser();
    if (!user) return;

    let currentPerms = [...(user.permissions || [])];
    // Remove all existing scope tokens
    currentPerms = currentPerms.filter(p => !['scope_org', 'scope_team', 'scope_own'].includes(p));
    // Add selected scope token
    currentPerms.push(targetScope);

    // Optimistically update
    this.selectedUser.set({ ...user, permissions: currentPerms });

    this.ds.updateUserPermissions(user.id, currentPerms).subscribe({
      next: (updatedPerms) => {
        this.selectedUser.set({ ...user, permissions: updatedPerms });
        this.toast.success(`Custom scope for ${user.name} updated to ${targetScope}.`);
      }
    });
  }

  togglePermission(permKey: string) {
    const user = this.selectedUser();
    if (!user) return;

    let currentPerms = [...(user.permissions || [])];
    if (currentPerms.includes(permKey)) {
      currentPerms = currentPerms.filter(p => p !== permKey);
    } else {
      currentPerms.push(permKey);
    }

    // Optimistically update
    this.selectedUser.set({ ...user, permissions: currentPerms });

    this.ds.updateUserPermissions(user.id, currentPerms).subscribe({
      next: (updatedPerms) => {
        this.selectedUser.set({ ...user, permissions: updatedPerms });
      }
    });
  }

  // Active Session Controls
  killSession(sessionId: string) {
    this.ds.killSession(sessionId).subscribe({
      next: () => {
        // Refresh local lists
        this.sessions.update(list => list.filter(s => s.id !== sessionId));
        this.selectedUserSessions.update(list => list.filter(s => s.id !== sessionId));
      }
    });
  }

  killAllSessions() {
    // Calling the endpoint directly via HTTP post/delete or custom service method
    // Since kill-all is added to SessionController, we call it via standard Http or just kill sessions one by one
    const list = [...this.sessions()];
    const current = list.find(s => s.current);
    list.forEach(s => {
      if (!s.current) {
        this.killSession(s.id);
      }
    });
  }

  // Whitelist mutations
  addIpRule() {
    const ip = this.newIpAddress().trim();
    const label = this.newIpLabel().trim();
    const blocked = this.newIpBlocked();
    if (!ip) return;

    this.ds.addIpWhitelist(ip, label, blocked).subscribe(() => {
      this.newIpAddress.set('');
      this.newIpLabel.set('');
      this.newIpBlocked.set(false);
      this.ds.getIpWhitelist().subscribe(ips => this.ipWhitelist.set(ips));
    });
  }

  deleteIpRule(ip: string) {
    this.ds.deleteIpWhitelist(ip).subscribe(() => {
      this.ds.getIpWhitelist().subscribe(ips => this.ipWhitelist.set(ips));
    });
  }

  getPermissionMatrixCount(role: string): number {
    // Quick helper to count how many permissions a role has by default
    const defaults: Record<string, string[]> = {
      admin: this.allPermissions.map(p => p.key),
      senior: ['view_all', 'create_matters', 'edit_matters', 'view_docs', 'upload_docs', 'manage_tasks', 'view_billing', 'manage_clients', 'export_data'],
      advocate: ['view_all', 'create_matters', 'edit_matters', 'view_docs', 'upload_docs', 'manage_tasks'],
      clerk: ['view_all', 'view_docs', 'upload_docs', 'manage_tasks', 'view_billing'],
      readonly: ['view_all', 'view_docs']
    };
    return this.rolePermissions()[role]?.length || 0;
  }

  isPermissionEnabledForRole(role: string, permKey: string): boolean {
    if (role === 'admin') return true;
    return this.rolePermissions()[role]?.includes(permKey) || false;
  }

  toggleRolePermission(role: string, permKey: string) {
    if (role === 'admin') return; // Admin always gets all permissions

    let currentPerms = [...(this.rolePermissions()[role] || [])];
    if (currentPerms.includes(permKey)) {
      currentPerms = currentPerms.filter(p => p !== permKey);
    } else {
      currentPerms.push(permKey);
    }

    this.http.put(`${environment.apiBaseUrl}/users/roles/${role}/permissions`, { permissions: currentPerms }).subscribe({
      next: () => {
        this.toast.success(`Role '${role}' updated successfully in database.`);
        this.rolePermissions.update(map => ({
          ...map,
          [role]: currentPerms
        }));
        this.ds.loadUsers().subscribe();
      },
      error: (err) => {
        this.toast.error(`Failed to update role permissions.`);
        console.error(err);
      }
    });
  }

  // Format Helper for timestamps
  formatTime(isoString: string): string {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      const now = new Date();

      const isToday = date.toDateString() === now.toDateString();
      const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      if (isToday) {
        return `Today ${timeStr}`;
      } else {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return `${months[date.getMonth()]} ${date.getDate()} ${timeStr}`;
      }
    } catch (e) {
      return isoString;
    }
  }

  getAuditLabelClass(action: string): string {
    const act = action.toLowerCase();
    if (act.includes('login success') || act.includes('logged in')) return 'badge-login';
    if (act.includes('invite')) return 'badge-invite';
    if (act.includes('export')) return 'badge-export';
    if (act.includes('role') || act.includes('permission')) return 'badge-perm';
    if (act.includes('delete')) return 'badge-delete';
    if (act.includes('suspend')) return 'badge-suspend';
    return 'badge-matter';
  }

  updateMfaEnabled(val: boolean) {
    this.forceMfa.set(val);
    this.saveSecuritySettings();
  }

  updateMaxAttempts(val: number) {
    this.maxAttempts.set(val);
    this.saveSecuritySettings();
  }

  updateSessionTimeout(val: string) {
    this.sessionTimeout.set(val);
    this.saveSecuritySettings();
  }

  private saveSecuritySettings() {
    const payload = {
      mfaEnabled: this.forceMfa(),
      maxAttempts: this.maxAttempts(),
      sessionTimeout: this.sessionTimeout()
    };
    this.http.put(`${environment.apiBaseUrl}/settings/security`, payload).subscribe({
      next: () => {
        this.toast.success('Security settings saved successfully.');
      },
      error: (err) => {
        this.toast.error('Failed to save security settings.');
        console.error(err);
      }
    });
  }
}
