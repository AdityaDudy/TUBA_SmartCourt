import { environment } from '../../../environments/environment';

const BASE = environment.apiBaseUrl;

export const API = {
  // ── Auth ──────────────────────────────────────────────────
  AUTH: {
    LOGIN: `${BASE}/auth/login`,
    LOGOUT: `${BASE}/auth/logout`,
    ME: `${BASE}/auth/me`,
    REFRESH: `${BASE}/auth/refresh`,
    FORGOT: `${BASE}/auth/forgot-password`,
    RESET: `${BASE}/auth/reset-password`,
    VERIFY_OTP: `${BASE}/auth/verify-otp`,
  },

  // ── Dashboard ─────────────────────────────────────────────
  DASHBOARD: {
    STATS: `${BASE}/dashboard/stats`,
    TIMELINE: `${BASE}/dashboard/timeline`,
    TEAM_PERF: `${BASE}/dashboard/team-performance`,
    COURT_DIST: `${BASE}/dashboard/court-distribution`,
    REVENUE: `${BASE}/dashboard/revenue`,
  },

  // ── Matters ───────────────────────────────────────────────
  MATTERS: {
    LIST: `${BASE}/matters`,
    CREATE: `${BASE}/matters`,
    GET: (id: string) => `${BASE}/matters/${id}`,
    UPDATE: (id: string) => `${BASE}/matters/${id}`,
    DELETE: (id: string) => `${BASE}/matters/${id}`,
    TIMELINE: (id: string) => `${BASE}/matters/${id}/timeline`,
    DOCUMENTS: (id: string) => `${BASE}/matters/${id}/documents`,
    TASKS: (id: string) => `${BASE}/matters/${id}/tasks`,
    SEARCH: `${BASE}/matters/search`,
  },

  // ── Clients ───────────────────────────────────────────────
  CLIENTS: {
    LIST: `${BASE}/clients`,
    CREATE: `${BASE}/clients`,
    GET: (id: string) => `${BASE}/clients/${id}`,
    UPDATE: (id: string) => `${BASE}/clients/${id}`,
    DELETE: (id: string) => `${BASE}/clients/${id}`,
    MATTERS: (id: string) => `${BASE}/clients/${id}/matters`,
    SEARCH: `${BASE}/clients/search`,
  },

  // ── Hearings / Cause List ─────────────────────────────────
  HEARINGS: {
    TODAY: `${BASE}/hearings/today`,
    BY_DATE: (date: string) => `${BASE}/hearings?date=${date}`,
    LIST: `${BASE}/hearings`,
    GET: (id: string) => `${BASE}/hearings/${id}`,
    FILTER: `${BASE}/hearings/filter`,
    SYNC: `${BASE}/hearings/sync`,
  },

  // ── Tasks ─────────────────────────────────────────────────
  TASKS: {
    LIST: `${BASE}/tasks`,
    CREATE: `${BASE}/tasks`,
    GET: (id: string) => `${BASE}/tasks/${id}`,
    UPDATE: (id: string) => `${BASE}/tasks/${id}`,
    DELETE: (id: string) => `${BASE}/tasks/${id}`,
    TOGGLE_DONE: (id: string) => `${BASE}/tasks/${id}/toggle`,
    MY_TASKS: `${BASE}/tasks/my`,
    OVERDUE: `${BASE}/tasks/overdue`,
  },

  // ── Filings ───────────────────────────────────────────────
  FILINGS: {
    LIST: `${BASE}/filings`,
    CREATE: `${BASE}/filings`,
    GET: (id: string) => `${BASE}/filings/${id}`,
    UPDATE: (id: string) => `${BASE}/filings/${id}`,
    DELETE: (id: string) => `${BASE}/filings/${id}`,
    UPLOAD: `${BASE}/filings/upload`,
  },

  // ── Billing ───────────────────────────────────────────────
  BILLING: {
    INVOICES: `${BASE}/billing/invoices`,
    CREATE: `${BASE}/billing/invoices`,
    GET: (id: string) => `${BASE}/billing/invoices/${id}`,
    UPDATE: (id: string) => `${BASE}/billing/invoices/${id}`,
    ENTRIES: `${BASE}/billing/entries`,
    ADD_ENTRY: `${BASE}/billing/entries`,
    SUMMARY: `${BASE}/billing/summary`,
    OVERDUE: `${BASE}/billing/overdue`,
    PENDING_BILLABLES: `${BASE}/billing/pending-billables`,
    PAYMENTS: (id: string) => `${BASE}/billing/invoices/${id}/payments`,
    EXPENSES: `${BASE}/billing/expenses`,
    EXPENSE_DETAIL: (id: string) => `${BASE}/billing/expenses/${id}`,
    BULK_REMIND: `${BASE}/billing/invoices/bulk-remind`,
    ROLLUP_MATTER: (id: string) => `${BASE}/billing/rollup/matter/${id}`,
    ROLLUP_CLIENT: (id: string) => `${BASE}/billing/rollup/client/${id}`,
  },

  // ── Documents ─────────────────────────────────────────────
  DOCUMENTS: {
    LIST: `${BASE}/documents`,
    UPLOAD: `${BASE}/documents/upload`,
    GET: (id: string) => `${BASE}/documents/${id}`,
    DELETE: (id: string) => `${BASE}/documents/${id}`,
    CREATE_MOCK: `${BASE}/documents/create-mock`,
    FOLDERS: `${BASE}/documents/folders`,
    CLIENT_FOLDERS: `${BASE}/documents/folders/client`,
    MATTER_FOLDERS: `${BASE}/documents/folders/matter`,
    CLIENT_CONTENTS: (id: string) => `${BASE}/documents/folders/client/${id}/contents`,
    MATTER_CONTENTS: (id: string) => `${BASE}/documents/folders/matter/${id}/contents`,
    RECENT: `${BASE}/documents/recent`,
    BY_MATTER: (matterId: string) => `${BASE}/documents?matterId=${matterId}`,
    UPLOAD_FOR_FILING: `${BASE}/documents/upload-for-filing`,
  },

  // ── Knowledge Base ────────────────────────────────────────
  KNOWLEDGE: {
    JUDGMENTS: `${BASE}/knowledge/judgments`,
    TEMPLATES: `${BASE}/knowledge/templates`,
    ARTICLES: `${BASE}/knowledge/articles`,
    SEARCH: `${BASE}/knowledge/search`,
  },
  // ── Tracker ────────────────────────────────────────────
  TRACKER: {
    SEARCH:                `${BASE}/tracker/search`,
    SEARCH_BY_CASE_NUMBER: `${BASE}/tracker/search-by-case-number`,
    SEARCH_ADVANCED:       `${BASE}/tracker/search-advanced`,
    JOB_STATUS:     (jobId: string)                    => `${BASE}/tracker/job/${jobId}`,
    DETAIL:         (cnr: string)                      => `${BASE}/tracker/${cnr}`,
    REFRESH:        (cnr: string)                      => `${BASE}/tracker/${cnr}/refresh`,
    ALERT:          (cnr: string)                      => `${BASE}/tracker/${cnr}/alert`,
    ORDER_DOWNLOAD: (cnr: string, orderId: string)     => `${BASE}/tracker/${cnr}/orders/${orderId}/download`,
    DOWNLOAD_ALL:   (cnr: string)                      => `${BASE}/tracker/${cnr}/download-all`,
    EXPORT:         (cnr: string, fmt: string)         => `${BASE}/tracker/${cnr}/export?format=${fmt}`,
    HISTORY:        `${BASE}/tracker/history`,
    SUGGEST_MATTER: (cnr: string)                      => `${BASE}/tracker/${cnr}/suggest-matter`,
    LINK_MATTER:    (cnr: string)                      => `${BASE}/tracker/${cnr}/link-matter`,
    SAVE_TO_MATTER: (cnr: string, orderId: string)    => `${BASE}/tracker/${cnr}/orders/${orderId}/save-to-matter`,
  },

  // ── Users ─────────────────────────────────────────────────
  USERS: {
    LIST: `${BASE}/users`,
    INVITE: `${BASE}/users/invite`,
    GET: (id: number) => `${BASE}/users/${id}`,
    UPDATE: (id: number) => `${BASE}/users/${id}`,
    SUSPEND: (id: number) => `${BASE}/users/${id}/suspend`,
    ACTIVATE: (id: number) => `${BASE}/users/${id}/activate`,
    PERMISSIONS: (id: number) => `${BASE}/users/${id}/permissions`,
    SESSIONS: (id: number) => `${BASE}/users/${id}/sessions`,
    ACTIVITY: (id: number) => `${BASE}/users/${id}/activity`,
    KILL_SESSION: (sessionId: string) => `${BASE}/sessions/${sessionId}`,
    ALL_SESSIONS: `${BASE}/sessions`,
    IP_WHITELIST: `${BASE}/security/ip-whitelist`,
    AUDIT_LOG: `${BASE}/audit-log`,
  },

  // ── Masters ───────────────────────────────────────────────
  MASTERS: {
    ALL: `${BASE}/masters`,
    GET: (key: string) => `${BASE}/masters/${key}`,
    UPDATE: (key: string) => `${BASE}/masters/${key}`,
    ADD_ITEM: (key: string) => `${BASE}/masters/${key}/items`,
    DELETE_ITEM: (key: string, item: string) => `${BASE}/masters/${key}/items/${encodeURIComponent(item)}`,
    REORDER: (key: string) => `${BASE}/masters/${key}/reorder`,
  },

  // ── Notifications ─────────────────────────────────────────
  NOTIFICATIONS: {
    LIST: `${BASE}/notifications`,
    MARK_READ: (id: string) => `${BASE}/notifications/${id}/read`,
    MARK_ALL_READ: `${BASE}/notifications/read-all`,
    UNREAD_COUNT: `${BASE}/notifications/unread-count`,
  },

  // ── Settings ──────────────────────────────────────────────
  SETTINGS: {
    FIRM: `${BASE}/settings/firm`,
    NOTIFICATIONS: `${BASE}/settings/notifications`,
    SECURITY: `${BASE}/settings/security`,
    THEME: `${BASE}/settings/theme`,
  },

  // ── Reports ───────────────────────────────────────────────
  REPORTS: {
    REVENUE: `${BASE}/reports/revenue`,
    MATTERS: `${BASE}/reports/matters`,
    TEAM: `${BASE}/reports/team`,
    EXPORT: `${BASE}/reports/export`,
    SUMMARY: `${BASE}/reports/summary`,
    SCHEDULE: `${BASE}/reports/schedule`,
  },

  // ── Diary ─────────────────────────────────────────────────
  DIARY: {
    EVENTS: `${BASE}/diary/events`,
    BY_MONTH: (year: number, month: number, scope: string = 'own', memberId?: number | null) =>
      `${BASE}/diary/events?year=${year}&month=${month}&scope=${scope}${memberId ? '&memberId=' + memberId : ''}`,
    SCOPE_OPTIONS: `${BASE}/diary/scope-options`,
    CREATE: `${BASE}/diary/events`,
    UPDATE: (id: string) => `${BASE}/diary/events/${id}`,
    DELETE: (id: string) => `${BASE}/diary/events/${id}`,
  },
} as const;
