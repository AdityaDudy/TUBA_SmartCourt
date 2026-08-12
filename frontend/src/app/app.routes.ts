import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  // Default redirect
  { path: '', redirectTo: 'auth', pathMatch: 'full' },

  // ── Auth (public) ─────────────────────────────────────────
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  // ── App Shell (protected) ─────────────────────────────────
  {
    path: 'app',
    loadComponent: () =>
      import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      // Main
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-page/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
        title: 'Dashboard — CourtOS',
      },
      {
        path: 'diary',
        loadComponent: () =>
          import('./features/diary/pages/diary-page/diary-page.component').then(
            (m) => m.DiaryPageComponent,
          ),
        title: 'Advocate Diary — CourtOS',
      },
      {
        path: 'cause-list',
        loadComponent: () =>
          import('./features/cause-list/pages/cause-list-page/cause-list-page.component').then(
            (m) => m.CauseListPageComponent,
          ),
        title: 'Cause List — CourtOS',
      },

      // Practice
      {
        path: 'clients',
        loadComponent: () =>
          import('./features/clients/pages/clients-page/clients-page.component').then(
            (m) => m.ClientsPageComponent,
          ),
        title: 'Clients — CourtOS',
      },
      {
        path: 'matters',
        loadComponent: () =>
          import('./features/matters/pages/matters-page/matters-page.component').then(
            (m) => m.MattersPageComponent,
          ),
        title: 'Matters — CourtOS',
      },
      {
        path: 'matters/:matId',
        loadComponent: () =>
          import('./features/matters/pages/matters-page/matters-page.component').then(
            (m) => m.MattersPageComponent,
          ),
        title: 'Matter Detail — CourtOS',
      },
      {
        path: 'tracker',
        loadComponent: () =>
          import('./features/tracker/pages/tracker-page/tracker-page.component').then(
            (m) => m.TrackerPageComponent,
          ),
        title: 'Court Tracker — CourtOS',
      },
      {
        path: 'tracker/:cnr',
        loadComponent: () =>
          import('./features/tracker/pages/case-detail-page/case-detail-page.component').then(
            (m) => m.CaseDetailPageComponent,
          ),
        title: 'Case Detail — CourtOS',
      },
      {
        path: 'tasks',
        loadComponent: () =>
          import('./features/tasks/pages/tasks-page/tasks-page.component').then(
            (m) => m.TasksPageComponent,
          ),
        title: 'Tasks & Workflow — CourtOS',
      },
      {
        path: 'filings',
        loadComponent: () =>
          import('./features/filings/pages/filings-page/filings-page.component').then(
            (m) => m.FilingsPageComponent,
          ),
        title: 'Filings — CourtOS',
      },

      // Knowledge
      {
        path: 'documents',
        loadComponent: () =>
          import('./features/documents/pages/documents-page/documents-page.component').then(
            (m) => m.DocumentsPageComponent,
          ),
        title: 'Documents — CourtOS',
      },
      {
        path: 'knowledge-base',
        loadComponent: () =>
          import('./features/knowledge-base/pages/kb-page/kb-page.component').then(
            (m) => m.KbPageComponent,
          ),
        title: 'Knowledge Base — CourtOS',
      },
      {
        path: 'ai-assistant',
        loadComponent: () =>
          import('./features/ai-assistant/pages/ai-page/ai-page.component').then(
            (m) => m.AiPageComponent,
          ),
        title: 'AI Assistant — CourtOS',
      },

      // Finance
      {
        path: 'billing',
        loadComponent: () =>
          import('./features/billing/pages/billing-page/billing-page.component').then(
            (m) => m.BillingPageComponent,
          ),
        canActivate: [permissionGuard],
        data: { permissions: ['view_billing', 'view_own_billing'] },
        title: 'Billing & Expenses — CourtOS',
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/pages/reports-page/reports-page.component').then(
            (m) => m.ReportsPageComponent,
          ),
        canActivate: [permissionGuard],
        data: { permissions: ['export_data', 'export_billing'] },
        title: 'Reports — CourtOS',
      },

      // Admin
      {
        path: 'user-management',
        loadComponent: () =>
          import('./features/user-management/pages/user-mgmt-page/user-mgmt-page.component').then(
            (m) => m.UserMgmtPageComponent,
          ),
        canActivate: [permissionGuard],
        data: { permission: 'manage_users' },
        title: 'User Management — CourtOS',
      },
      {
        path: 'masters',
        loadComponent: () =>
          import('./features/masters/pages/masters-page/masters-page.component').then(
            (m) => m.MastersPageComponent,
          ),
        title: 'Masters — CourtOS',
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/pages/settings-page/settings-page.component').then(
            (m) => m.SettingsPageComponent,
          ),
        canActivate: [permissionGuard],
        data: { permission: 'system_settings' },
        title: 'Settings — CourtOS',
      },
    ],
  },

  // Wildcard fallback
  { path: '**', redirectTo: 'auth' },
];
