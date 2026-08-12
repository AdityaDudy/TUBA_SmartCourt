export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  department: string;
  matters: string;
  lastLogin: string;
  mfa: boolean;
  status: UserStatus;
  av: string;        // initials
  bg: string;        // gradient CSS string
  mobile: string;
  avatar?: string;
  barCouncilNo?: string;
  designation?: string;
  permissions?: string[];
  scope?: DataScopeType;
}

export type UserRole = string;
export type UserStatus = 'online' | 'offline' | 'inactive' | 'active';
export type DataScopeType = 'scope_org' | 'scope_team' | 'scope_own';

export interface DataScopeOption {
  key: DataScopeType;
  label: string;
  description: string;
}

export const DATA_SCOPES: DataScopeOption[] = [
  { key: 'scope_org',  label: 'View Org-wide Data', description: 'See all matters, clients, tasks, and documents across the entire firm.' },
  { key: 'scope_team', label: 'View Team Data',     description: 'See records created by or assigned to users in the same department.' },
  { key: 'scope_own',  label: 'View Own Data Only', description: 'See only records where user is creator or assignee (default fallback).' },
];

export const ROLE_LABELS: Record<string, string> = {
  admin:    'Admin',
  senior:   'Sr. Advocate',
  advocate: 'Advocate',
  clerk:    'Clerk',
  readonly: 'Read-only',
};

export const ROLE_BADGE_CLASS: Record<string, string> = {
  admin:    'b-a',
  senior:   'b-g',
  advocate: 'b-g',
  clerk:    'b-t',
  readonly: 'b-gr',
};

export const STATUS_LABELS: Record<UserStatus, string> = {
  online:   'Online',
  offline:  'Offline',
  inactive: 'Inactive',
  active:   'Active',
};

export const STATUS_DOT_CLASS: Record<UserStatus, string> = {
  online:   's-on',
  offline:  's-off',
  inactive: 's-ina',
  active:   's-on',
};

export interface Permission {
  key: string;
  label: string;
  sub: string;
}

export const ALL_PERMISSIONS: Permission[] = [
  { key: 'view_all',            label: 'View All Matters',       sub: 'Read all matters' },
  { key: 'create_matters',      label: 'Create Matters',         sub: 'Register new matters' },
  { key: 'edit_matters',        label: 'Edit Matters',           sub: 'Modify matter details' },
  { key: 'delete_matters',      label: 'Delete Matters',         sub: 'Remove matters permanently' },
  { key: 'view_docs',           label: 'View Documents',         sub: 'Access DMS files' },
  { key: 'upload_docs',         label: 'Upload Documents',       sub: 'Add files' },
  { key: 'delete_docs',         label: 'Delete Documents',       sub: 'Remove files' },
  { key: 'manage_tasks',        label: 'Manage Tasks',           sub: 'Full task administration' },
  { key: 'manage_tasks_assign', label: 'Assign Tasks',          sub: 'Assign tasks to team members' },
  { key: 'manage_tasks_close',  label: 'Close Own Tasks',        sub: 'Mark own assigned tasks complete' },
  { key: 'view_billing',        label: 'View Org Billing',       sub: 'See firm-wide invoices' },
  { key: 'view_own_billing',    label: 'View Own Billing',       sub: 'See only personal matter invoices' },
  { key: 'create_invoices',     label: 'Create Invoices',        sub: 'Generate invoices' },
  { key: 'export_billing',      label: 'Export Billing Data',    sub: 'Export sensitive financial reports' },
  { key: 'export_data',         label: 'Export General Reports', sub: 'Download general system reports' },
  { key: 'manage_clients',      label: 'Manage Clients',         sub: 'Add & edit clients' },
  { key: 'delete_clients',      label: 'Delete Clients',         sub: 'Remove client profiles' },
  { key: 'manage_users',        label: 'Manage Users',           sub: 'Invite & manage user accounts' },
  { key: 'manage_roles',        label: 'Manage Roles & Perms',   sub: 'Redefine role permissions' },
  { key: 'impersonate_user',    label: 'Impersonate User',       sub: 'Log in as another user for audit support' },
  { key: 'system_settings',     label: 'System Settings',       sub: 'Modify config' },
  { key: 'view_audit',          label: 'View Org Audit Log',     sub: 'Read firm-wide security trail' },
  { key: 'view_own_audit',      label: 'View Own Audit Trail',   sub: 'Read own activity history' },
];

export const ROLE_PERMISSIONS: Record<string, string[]> = {
  admin:    ALL_PERMISSIONS.map((p) => p.key).concat(['scope_org']),
  senior:   ['view_all','create_matters','edit_matters','view_docs','upload_docs','manage_tasks','manage_tasks_assign','manage_tasks_close','view_billing','export_billing','manage_clients','export_data','scope_team'],
  advocate: ['view_all','create_matters','edit_matters','view_docs','upload_docs','manage_tasks_close','view_own_billing','scope_own'],
  clerk:    ['view_all','view_docs','upload_docs','manage_tasks_close','view_own_billing','scope_own'],
  readonly: ['view_all','view_docs','scope_own'],
};

