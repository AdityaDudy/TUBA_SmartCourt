import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, of } from 'rxjs';
import { tap, catchError, map, finalize } from 'rxjs/operators';
import { API } from '../api/api-endpoints';
import {
  ApiResponse, PagedResponse, ListParams,
  DashboardStatsDto, MastersDto, TrackerResultDto,
  DiaryEventDto, DiaryScopeOptions, DocumentFolderDto, RevenueDto, TeamPerformanceDto,
  CaseDetailResponse, ScrapeJobStatusResponse, RecentSearchDto, MatterSuggestion, CaseSearchResultDto,
} from '../api/api-response.types';
import {
  Matter, Client, Hearing, Task, Filing,
  Invoice, User, Notification, AuditEntry, Session, Masters, Expense, Payment, InvoiceLineItem, PendingBillable,
} from '../models';
import { ToastService } from './toast.service';

// ── Entity State Shape ─────────────────────────────────────────
interface EntityState<T> {
  items: T[];
  loading: boolean;
  loaded: boolean;
  error: string | null;
  total: number;
}

function emptyState<T>(): EntityState<T> {
  return { items: [], loading: false, loaded: false, error: null, total: 0 };
}

// ── Global App State ───────────────────────────────────────────
interface AppState {
  matters: EntityState<Matter>;
  clients: EntityState<Client>;
  hearings: EntityState<Hearing>;
  tasks: EntityState<Task>;
  filings: EntityState<Filing>;
  billing: EntityState<Invoice>;
  users: EntityState<User>;
  notifications: EntityState<Notification>;
  auditLog: EntityState<AuditEntry>;
  masters: Masters | null;
  dashStats: DashboardStatsDto | null;
  expenses: EntityState<Expense>;
}

// ── Service ────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class DataService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  // ── Reactive State (Signal) ──────────────────────────────
  private _state = signal<AppState>({
    matters: emptyState(),
    clients: emptyState(),
    hearings: emptyState(),
    tasks: emptyState(),
    filings: emptyState(),
    billing: emptyState(),
    users: emptyState(),
    notifications: emptyState(),
    auditLog: emptyState(),
    masters: null,
    dashStats: null,
    expenses: emptyState(),
  });

  // ── Public Selectors ─────────────────────────────────────
  readonly matters = computed(() => this._state().matters.items);
  readonly clients = computed(() => this._state().clients.items);
  readonly hearings = computed(() => this._state().hearings.items);
  readonly tasks = computed(() => this._state().tasks.items);
  readonly filings = computed(() => this._state().filings.items);
  readonly billing = computed(() => this._state().billing.items);
  readonly users = computed(() => this._state().users.items);
  readonly notifications = computed(() => this._state().notifications.items);
  readonly auditLog = computed(() => {
    const logs = this._state().auditLog.items;
    console.log('DataService: auditLog computed evaluated, items =', logs ? logs.length : 0);
    return logs;
  });
  readonly masters = computed(() => this._state().masters);
  readonly dashStats = computed(() => this._state().dashStats);
  readonly expenses = computed(() => this._state().expenses.items);
  readonly auditLogLoading = computed(() => this._state().auditLog.loading);

  readonly mattersLoading = computed(() => this._state().matters.loading);
  readonly clientsLoading = computed(() => this._state().clients.loading);
  readonly hearingsLoading = computed(() => this._state().hearings.loading);
  readonly tasksLoading = computed(() => this._state().tasks.loading);
  readonly filingsLoading = computed(() => this._state().filings.loading);
  readonly billingLoading = computed(() => this._state().billing.loading);
  readonly usersLoading = computed(() => this._state().users.loading);
  readonly expensesLoading = computed(() => this._state().expenses.loading);

  // Derived stats
  readonly activeMattersCount = computed(() => this.matters().filter(m => m.status === 'Active').length);
  readonly urgentHearingsCount = computed(() => this.hearings().filter(h => h.status === 'Urgent').length);
  readonly todayHearingsCount = computed(() => this.hearings().length);
  readonly openTasksCount = computed(() => this.tasks().filter(t => !t.done).length);
  readonly overdueTasksCount = computed(() => this.tasks().filter(t => t.status === 'Overdue').length);
  readonly pendingFilingsCount = computed(() => this.filings().filter(f => f.status !== 'Filed').length);
  readonly unreadNotifCount = computed(() => this.notifications().filter(n => !n.read).length);
  readonly totalOutstanding = computed(() =>
    this.billing().filter(b => b.status !== 'Paid').reduce((s, b) => s + b.amount, 0)
  );

  constructor() {
    this.initSseStreams();
  }

  private initSseStreams() {
    // 1. Audit Log stream
    const auditSource = new EventSource(`${API.USERS.AUDIT_LOG}/stream`);
    auditSource.addEventListener('audit', (e: any) => {
      try {
        const entry = JSON.parse(e.data);
        this.patch('auditLog', {
          items: [entry, ...this.auditLog()]
        });
      } catch (err) {
        console.error('Failed to parse audit event:', err);
      }
    });
    auditSource.onerror = () => {
      // Close connection on error to avoid rapid 1-second reconnect spam
      auditSource.close();
    };

    // 2. Notification stream
    const notifSource = new EventSource(`${API.NOTIFICATIONS.LIST}/stream`);
    notifSource.addEventListener('notification', (e: any) => {
      try {
        const notif = JSON.parse(e.data);
        this.patch('notifications', {
          items: [notif, ...this.notifications()]
        });
        this.toast.info(notif.title + ': ' + (notif.message || ''));
      } catch (err) {
        console.error('Failed to parse notification event:', err);
      }
    });
    notifSource.onerror = () => {
      // Close connection on error to avoid rapid 1-second reconnect spam
      notifSource.close();
    };
  }

  // ── Private state updater ─────────────────────────────────
  private patch<K extends keyof AppState>(key: K, partial: any) {
    this._state.update(s => ({
      ...s,
      [key]: { ...(s[key] as object), ...partial },
    }));
  }

  private setLoading<K extends keyof AppState>(key: K, loading: boolean) {
    this.patch(key, { loading });
  }

  // ── HTTP helper with error handling ───────────────────────
  private get<T>(url: string, params?: HttpParams): Observable<T> {
    return this.http.get<ApiResponse<T>>(url, { params }).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  private post<T>(url: string, body: unknown): Observable<T> {
    return this.http.post<ApiResponse<T>>(url, body).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  private put<T>(url: string, body: unknown): Observable<T> {
    return this.http.put<ApiResponse<T>>(url, body).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  private del<T>(url: string): Observable<T> {
    return this.http.delete<ApiResponse<T>>(url).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  private _handleError(err: any) {
    const msg = err?.error?.message ?? err?.message ?? 'An error occurred';
    this.toast.error(msg);
    return throwError(() => new Error(msg));
  }

  // Getters for internal array storage
  getClients() { return this.clients(); }
  getHearings() { return this.hearings(); }
  getMatters() { return this.matters(); }

  // ============================================================
  //  DASHBOARD
  // ============================================================
  loadDashboardStats(): Observable<DashboardStatsDto> {
    return this.get<DashboardStatsDto>(API.DASHBOARD.STATS).pipe(
      tap(stats => this._state.update(s => ({ ...s, dashStats: stats }))),
    );
  }

  getRevenue(): Observable<RevenueDto> {
    return this.get<RevenueDto>(API.DASHBOARD.REVENUE);
  }

  getTeamPerformance(): Observable<TeamPerformanceDto[]> {
    return this.get<TeamPerformanceDto[]>(API.DASHBOARD.TEAM_PERF);
  }

  getCourtDistribution(): Observable<{ court: string; count: number; pct: number }[]> {
    return this.get(API.DASHBOARD.COURT_DIST);
  }

  getTimeline(): Observable<{ date: string; title: string; sub: string; urgent: boolean }[]> {
    return this.get(API.DASHBOARD.TIMELINE);
  }

  // ============================================================
  //  MATTERS
  // ============================================================
  loadMatters(params?: ListParams): Observable<Matter[]> {
    this.patch('matters', { loading: true, error: null });
    const qp = this._toParams(params);
    return this.get<Matter[]>(API.MATTERS.LIST, qp).pipe(
      tap(items => this.patch('matters', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('matters', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('matters', { loading: false })),
    );
  }

  getMatterById(id: string): Observable<Matter> {
    return this.get<Matter>(API.MATTERS.GET(id));
  }

  searchMatters(query: string): Observable<Matter[]> {
    return this.get<Matter[]>(API.MATTERS.SEARCH, new HttpParams().set('q', query));
  }

  createMatter(data: Partial<Matter>): Observable<Matter> {
    return this.post<Matter>(API.MATTERS.CREATE, data).pipe(
      tap(m => this.patch('matters', { items: [m, ...this.matters()] })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Matter registered successfully!')),
    );
  }

  updateMatter(id: string, data: Partial<Matter>): Observable<Matter> {
    return this.put<Matter>(API.MATTERS.UPDATE(id), data).pipe(
      tap(updated => this.patch('matters', {
        items: this.matters().map(m => m.id === id ? updated : m),
      })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Matter updated.')),
    );
  }

  deleteMatter(id: string): Observable<void> {
    return this.del<void>(API.MATTERS.DELETE(id)).pipe(
      tap(() => this.patch('matters', { items: this.matters().filter(m => m.id !== id) })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Matter deleted.')),
    );
  }

  getMatterTimeline(id: string) {
    return this.get<{ date: string; event: string; sub: string }[]>(API.MATTERS.TIMELINE(id));
  }

  // ============================================================
  //  CLIENTS
  // ============================================================
  loadClients(params?: ListParams): Observable<Client[]> {
    this.patch('clients', { loading: true, error: null });
    return this.get<Client[]>(API.CLIENTS.LIST, this._toParams(params)).pipe(
      tap(items => this.patch('clients', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('clients', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('clients', { loading: false })),
    );
  }

  getClientById(id: string): Observable<Client> {
    return this.get<Client>(API.CLIENTS.GET(id));
  }

  searchClients(query: string): Observable<Client[]> {
    return this.get<Client[]>(API.CLIENTS.SEARCH, new HttpParams().set('q', query));
  }

  createClient(data: Partial<Client>): Observable<Client> {
    return this.post<Client>(API.CLIENTS.CREATE, data).pipe(
      tap(c => this.patch('clients', { items: [c, ...this.clients()] })),
      tap(() => this.toast.success('Client added successfully!')),
    );
  }

  updateClient(id: string, data: Partial<Client>): Observable<Client> {
    return this.put<Client>(API.CLIENTS.UPDATE(id), data).pipe(
      tap(updated => this.patch('clients', {
        items: this.clients().map(c => c.id === id ? updated : c),
      })),
      tap(() => this.toast.success('Client updated.')),
    );
  }

  // ============================================================
  //  HEARINGS / CAUSE LIST
  // ============================================================
  loadHearings(date?: string): Observable<Hearing[]> {
    this.patch('hearings', { loading: true, error: null });
    const url = date ? API.HEARINGS.BY_DATE(date) : API.HEARINGS.TODAY;
    return this.get<Hearing[]>(url).pipe(
      tap(items => this.patch('hearings', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('hearings', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('hearings', { loading: false })),
    );
  }

  filterHearings(type: string): Observable<Hearing[]> {
    const params = new HttpParams().set('type', type);
    return this.get<Hearing[]>(API.HEARINGS.FILTER, params);
  }

  /** Admin: re-derive hearing rows from already-stored TrackedCase data (no new eCourts calls) */
  syncHearings(): Observable<{ synced: number }> {
    return this.http.post<any>(API.HEARINGS.SYNC, {}).pipe(
      map(r => r.data),
      tap((res: { synced: number }) => this.toast.success(`Sync complete — ${res.synced} hearing(s) updated.`)),
      catchError(err => this._handleError(err)),
    );
  }

  // ============================================================
  //  TASKS
  // ============================================================
  loadTasks(params?: ListParams): Observable<Task[]> {
    this.patch('tasks', { loading: true, error: null });
    return this.get<Task[]>(API.TASKS.LIST, this._toParams(params)).pipe(
      tap(items => this.patch('tasks', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('tasks', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('tasks', { loading: false })),
    );
  }

  createTask(data: Partial<Task>): Observable<Task> {
    return this.post<Task>(API.TASKS.CREATE, data).pipe(
      tap(t => this.patch('tasks', { items: [t, ...this.tasks()] })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Task created!')),
    );
  }

  updateTask(id: string, data: Partial<Task>): Observable<Task> {
    return this.put<Task>(API.TASKS.UPDATE(id), data).pipe(
      tap(updated => this.patch('tasks', {
        items: this.tasks().map(t => t.id === id ? updated : t),
      })),
      tap(() => this.loadDashboardStats().subscribe()),
    );
  }

  toggleTaskDone(id: string): Observable<Task> {
    return this.put<Task>(API.TASKS.TOGGLE_DONE(id), {}).pipe(
      tap(updated => this.patch('tasks', {
        items: this.tasks().map(t => t.id === id ? updated : t),
      })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(t => this.toast.success(t.done ? 'Task complete!' : 'Task reopened.')),
    );
  }

  deleteTask(id: string): Observable<void> {
    return this.del<void>(API.TASKS.DELETE(id)).pipe(
      tap(() => this.patch('tasks', { items: this.tasks().filter(t => t.id !== id) })),
      tap(() => this.loadDashboardStats().subscribe()),
    );
  }

  // ============================================================
  //  FILINGS
  // ============================================================
  loadFilings(params?: ListParams): Observable<Filing[]> {
    this.patch('filings', { loading: true, error: null });
    return this.get<Filing[]>(API.FILINGS.LIST, this._toParams(params)).pipe(
      tap(items => this.patch('filings', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('filings', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('filings', { loading: false })),
    );
  }

  createFiling(data: Partial<Filing>): Observable<Filing> {
    return this.post<Filing>(API.FILINGS.CREATE, data).pipe(
      tap(f => this.patch('filings', { items: [f, ...this.filings()] })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Filing created!')),
    );
  }

  updateFiling(id: string, data: Partial<Filing>): Observable<Filing> {
    return this.put<Filing>(API.FILINGS.UPDATE(id), data).pipe(
      tap(updated => this.patch('filings', {
        items: this.filings().map(f => f.id === id ? updated : f),
      })),
      tap(() => this.loadDashboardStats().subscribe()),
    );
  }

  deleteFiling(id: string): Observable<void> {
    return this.del<void>(API.FILINGS.DELETE(id)).pipe(
      tap(() => {
        this.patch('filings', { items: this.filings().filter(f => f.id !== id) });
        this.loadDashboardStats().subscribe();
        this.toast.success('Filing deleted.');
      }),
    );
  }

  // ============================================================
  //  BILLING & EXPENSES
  // ============================================================
  loadBilling(params?: ListParams): Observable<Invoice[]> {
    this.patch('billing', { loading: true, error: null });
    return this.get<Invoice[]>(API.BILLING.INVOICES, this._toParams(params)).pipe(
      tap(items => this.patch('billing', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('billing', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('billing', { loading: false })),
    );
  }

  getBillingSummary(): Observable<{ total: number; collected: number; outstanding: number; overdue: number; expenses?: number }> {
    return this.get(API.BILLING.SUMMARY);
  }

  createInvoice(data: Partial<Invoice>): Observable<Invoice> {
    return this.post<Invoice>(API.BILLING.CREATE, data).pipe(
      tap(i => this.patch('billing', { items: [i, ...this.billing()] })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Invoice generated!')),
    );
  }

  addBillingEntry(data: unknown): Observable<unknown> {
    return this.post(API.BILLING.ADD_ENTRY, data).pipe(
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Billing entry added.')),
    );
  }

  getPendingBillables(): Observable<PendingBillable[]> {
    return this.get<PendingBillable[]>(API.BILLING.PENDING_BILLABLES);
  }

  recordPayment(invoiceId: number, payment: Partial<Payment>): Observable<Payment> {
    return this.post<Payment>(API.BILLING.PAYMENTS(String(invoiceId)), payment).pipe(
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Payment recorded successfully!')),
    );
  }

  getPayments(invoiceId: number): Observable<Payment[]> {
    return this.get<Payment[]>(API.BILLING.PAYMENTS(String(invoiceId)));
  }

  loadExpenses(): Observable<Expense[]> {
    this.patch('expenses', { loading: true, error: null });
    return this.get<Expense[]>(API.BILLING.EXPENSES).pipe(
      tap(items => this.patch('expenses', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('expenses', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('expenses', { loading: false })),
    );
  }

  createExpense(data: Partial<Expense>): Observable<Expense> {
    return this.post<Expense>(API.BILLING.EXPENSES, data).pipe(
      tap(e => this.patch('expenses', { items: [e, ...this.expenses()] })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Expense logged successfully.')),
    );
  }

  updateExpense(id: number, data: Partial<Expense>): Observable<Expense> {
    return this.put<Expense>(API.BILLING.EXPENSE_DETAIL(String(id)), data).pipe(
      tap(updated => this.patch('expenses', {
        items: this.expenses().map(e => e.id === id ? updated : e)
      })),
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Expense updated.')),
    );
  }

  deleteExpense(id: number): Observable<void> {
    return this.del<void>(API.BILLING.EXPENSE_DETAIL(String(id))).pipe(
      tap(() => {
        this.patch('expenses', { items: this.expenses().filter(e => e.id !== id) });
        this.loadDashboardStats().subscribe();
        this.toast.success('Expense deleted.');
      }),
    );
  }

  getMatterRollup(matterId: number): Observable<any> {
    return this.get(API.BILLING.ROLLUP_MATTER(String(matterId)));
  }

  getClientRollup(clientId: number): Observable<any> {
    return this.get(API.BILLING.ROLLUP_CLIENT(String(clientId)));
  }

  bulkRemind(invoiceIds: number[]): Observable<any> {
    return this.post(API.BILLING.BULK_REMIND, invoiceIds).pipe(
      tap(() => this.toast.success('Payment reminders sent to clients.')),
    );
  }

  // ============================================================
  //  DOCUMENTS
  // ============================================================
  getDocumentFolders(): Observable<any[]> {
    return this.get<any[]>(API.DOCUMENTS.FOLDERS);
  }

  getClientFolders(): Observable<any[]> {
    return this.get<any[]>(API.DOCUMENTS.CLIENT_FOLDERS);
  }

  getMatterFolders(): Observable<any[]> {
    return this.get<any[]>(API.DOCUMENTS.MATTER_FOLDERS);
  }

  getClientFolderContents(id: string): Observable<any[]> {
    return this.get<any[]>(API.DOCUMENTS.CLIENT_CONTENTS(id));
  }

  getMatterFolderContents(id: string): Observable<any[]> {
    return this.get<any[]>(API.DOCUMENTS.MATTER_CONTENTS(id));
  }

  getRecentDocuments(): Observable<{ name: string; meta: string; icon: string; bg: string; tc: string; tag: string; label: string }[]> {
    return this.get(API.DOCUMENTS.RECENT);
  }

  uploadDocument(formData: FormData): Observable<any> {
    return this.http.post<ApiResponse<any>>(
      API.DOCUMENTS.UPLOAD, formData
    ).pipe(
      map(r => r.data),
      tap(() => this.toast.success('Document uploaded!')),
      catchError(err => this._handleError(err)),
    );
  }

  uploadClientDocument(clientId: string, file: File, docType?: string, tags?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('clientId', clientId);
    if (docType) formData.append('docType', docType);
    if (tags) formData.append('tags', tags);
    return this.uploadDocument(formData);
  }

  uploadMatterDocument(matterId: string, file: File, docType?: string, tags?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('matterId', matterId);
    if (docType) formData.append('docType', docType);
    return this.uploadDocument(formData);
  }

  /**
   * Stores a file as a filing attachment ONLY — does not create a matter-linked
   * Document record, so it will not appear in the matter's Document Vault yet.
   * Use this for Task Submission uploads and other filing attachments. The file
   * is automatically linked into the matter's Document Vault by the backend once
   * the owning Filing's stage is set to "Filed".
   */
  uploadFilingAttachment(file: File): Observable<{ name: string; s3Url: string; fileSize: number; mimeType: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<any>>(API.DOCUMENTS.UPLOAD_FOR_FILING, formData).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  createMockDocument(name: string, matterId: any, docType: string): Observable<any> {
    return this.post<any>(API.DOCUMENTS.CREATE_MOCK, { name, matterId, docType }).pipe(
      tap(() => this.toast.success('Attached document saved to Matter explorer.'))
    );
  }

  deleteDocument(id: number): Observable<void> {
    return this.del<void>(API.DOCUMENTS.DELETE(String(id))).pipe(
      tap(() => this.toast.success('Document deleted.'))
    );
  }

  // ============================================================
  //  KNOWLEDGE BASE
  // ============================================================
  getJudgments() {
    return this.get<{ title: string; court: string; tag: string; label: string }[]>(API.KNOWLEDGE.JUDGMENTS);
  }

  getTemplates() {
    return this.get<{ name: string; type: string; updatedAt: string }[]>(API.KNOWLEDGE.TEMPLATES);
  }

  getArticles() {
    return this.get<{ title: string; author: string; date: string; summary: string }[]>(API.KNOWLEDGE.ARTICLES);
  }

  // ============================================================
  //  COURT TRACKER
  // ============================================================

  /** Initiates a CNR search. Returns 202+jobId (async) or 200+result (cached). */
  searchByCnr(cnr: string): Observable<ScrapeJobStatusResponse> {
    return this.http.get<ApiResponse<ScrapeJobStatusResponse>>(
      API.TRACKER.SEARCH, { params: new HttpParams().set('cnr', cnr) }
    ).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  /** Resolves Case Number -> CNR and enqueues/returns scrape job */
  searchByCaseNumber(caseType: string, number: string, year: string): Observable<ScrapeJobStatusResponse> {
    let params = new HttpParams()
      .set('number', number)
      .set('year', year);
    if (caseType) params = params.set('caseType', caseType);

    return this.http.get<ApiResponse<ScrapeJobStatusResponse>>(
      API.TRACKER.SEARCH_BY_CASE_NUMBER, { params }
    ).pipe(
      map(r => r.data),
      catchError(err => this._handleError(err)),
    );
  }

  /**
   * Case-number search — returns a lightweight candidate list.
   * The user picks one row; the chosen cnr is then passed to searchByCnr().
   */
  searchAdvanced(value: string): Observable<CaseSearchResultDto[]> {
    let params = new HttpParams()
      .set('type', 'CASE_NUMBER')
      .set('value', value);
    return this.http.get<ApiResponse<CaseSearchResultDto[]>>(
      API.TRACKER.SEARCH_ADVANCED, { params }
    ).pipe(
      map(r => r.data ?? []),
      catchError(err => this._handleError(err)),
    );
  }

  /** Poll a scrape job for status updates (PENDING → RUNNING → DONE/FAILED/CAPTCHA_REQUIRED) */
  pollJobStatus(jobId: number): Observable<ScrapeJobStatusResponse> {
    return this.get<ScrapeJobStatusResponse>(API.TRACKER.JOB_STATUS(String(jobId)));
  }

  /** Get the persisted case detail from DB (requires a previous successful search) */
  getCaseDetail(cnr: string): Observable<CaseDetailResponse> {
    return this.get<CaseDetailResponse>(API.TRACKER.DETAIL(cnr));
  }

  /** Force re-fetch from eCourts, bypassing cache */
  refreshCase(cnr: string): Observable<ScrapeJobStatusResponse> {
    return this.post<ScrapeJobStatusResponse>(API.TRACKER.REFRESH(cnr), {}).pipe(
      tap(() => this.toast.info('Refresh queued — updating from court servers...')),
    );
  }

  /** Export a formatted PDF case summary (not the raw court document) */
  exportCase(cnr: string, format: 'pdf' | 'docx' = 'pdf'): Observable<Blob> {
    return this.http.get(API.TRACKER.EXPORT(cnr, format), { responseType: 'blob' }).pipe(
      catchError(err => this._handleError(err)),
    );
  }

  /** Download a single order PDF via the proxied endpoint */
  downloadOrder(cnr: string, orderId: number): Observable<Blob> {
    return this.http.get(
      API.TRACKER.ORDER_DOWNLOAD(cnr, String(orderId)), { responseType: 'blob' }
    ).pipe(catchError(err => this._handleError(err)));
  }

  /** Download all orders for a case as a ZIP archive */
  downloadAllOrders(cnr: string): Observable<Blob> {
    return this.http.get(API.TRACKER.DOWNLOAD_ALL(cnr), { responseType: 'blob' }).pipe(
      catchError(err => this._handleError(err)),
    );
  }

  /** Toggle case alert subscription for the current user */
  toggleAlert(cnr: string, enabled: boolean): Observable<void> {
    return this.post<void>(API.TRACKER.ALERT(cnr), { enabled }).pipe(
      tap(() => this.toast.success(enabled ? 'Case alerts enabled.' : 'Case alerts disabled.')),
    );
  }

  /** Get recent CNR searches for the current user (drives the chips on the search page) */
  getRecentSearches(): Observable<RecentSearchDto[]> {
    return this.get<RecentSearchDto[]>(API.TRACKER.HISTORY);
  }

  /** Get a matter link suggestion for a tracked case (non-binding — user confirms) */
  suggestMatterLink(cnr: string): Observable<MatterSuggestion | null> {
    return this.get<MatterSuggestion | null>(API.TRACKER.SUGGEST_MATTER(cnr));
  }

  /** Confirm and apply a matter link to a tracked case */
  linkCaseToMatter(cnr: string, matterId: number): Observable<void> {
    return this.post<void>(API.TRACKER.LINK_MATTER(cnr), { matterId }).pipe(
      tap(() => this.toast.success('Case linked to matter.')),
    );
  }

  /** Save an order document into the linked Matter's Document Vault folder */
  saveOrderToMatterVault(cnr: string, orderId: number): Observable<{ saved: boolean; docId?: number }> {
    return this.http.post<any>(API.TRACKER.SAVE_TO_MATTER(cnr, String(orderId)), {}).pipe(
      map(r => r.data),
      tap((res) => this.toast.success(res.alreadyExisted ? 'Document is already in the Matter folder.' : 'Order saved to Matter Document Vault!')),
      catchError(err => this._handleError(err)),
    );
  }


  /** @deprecated — use searchByCnr() */
  trackCase(caseNo: string, court?: string, searchType?: string): Observable<TrackerResultDto> {
    return this.searchByCnr(caseNo) as any;
  }

  // ============================================================
  //  USERS
  // ============================================================
  loadUsers(params?: ListParams): Observable<User[]> {
    this.patch('users', { loading: true, error: null });
    return this.get<User[]>(API.USERS.LIST, this._toParams(params)).pipe(
      tap(items => this.patch('users', { items, loaded: true, total: items.length })),
      catchError(err => { this.patch('users', { error: err.message }); return throwError(() => err); }),
      finalize(() => this.patch('users', { loading: false })),
    );
  }

  getUserById(id: number): Observable<User> {
    return this.get<User>(API.USERS.GET(id));
  }

  inviteUser(data: { name: string; email: string; mobile: string; role: string; dept: string; designation: string; barCouncilNo: string }): Observable<User> {
    return this.post<User>(API.USERS.INVITE, data).pipe(
      tap(u => this.patch('users', { items: [...this.users(), u] })),
      tap(() => this.toast.success(`Invite sent to ${data.email}`)),
    );
  }

  updateUser(id: number, data: Partial<User>): Observable<User> {
    return this.put<User>(API.USERS.UPDATE(id), data).pipe(
      tap(updated => this.patch('users', {
        items: this.users().map(u => u.id === id ? { ...u, ...updated, ...(data.avatar ? { avatar: data.avatar } : {}) } : u),
      })),
      tap(() => this.toast.success('User updated.')),
    );
  }

  suspendUser(id: number): Observable<void> {
    return this.put<void>(API.USERS.SUSPEND(id), {}).pipe(
      tap(() => {
        this.patch('users', {
          items: this.users().map(u => u.id === id ? { ...u, status: 'inactive' as const } : u),
        });
        this.toast.warning('User suspended.');
      }),
    );
  }

  updateUserPermissions(id: number, permissions: string[]): Observable<string[]> {
    return this.put<string[]>(API.USERS.PERMISSIONS(id), { permissions }).pipe(
      tap(perms => {
        this.patch('users', {
          items: this.users().map(u => u.id === id ? { ...u, permissions: perms } : u)
        });
        this.toast.success('Permissions updated successfully.');
      })
    );
  }

  getUserSessions(id: number): Observable<Session[]> {
    return this.get<Session[]>(API.USERS.SESSIONS(id));
  }

  getAllSessions(): Observable<Session[]> {
    return this.get<Session[]>(API.USERS.ALL_SESSIONS);
  }

  killSession(sessionId: string): Observable<void> {
    return this.del<void>(API.USERS.KILL_SESSION(sessionId)).pipe(
      tap(() => this.toast.success('Session terminated.')),
    );
  }

  loadAuditLog(): Observable<AuditEntry[]> {
    this.patch('auditLog', { loading: true, error: null });
    console.log('loadAuditLog: Triggered GET /api/audit-log');
    return this.get<AuditEntry[]>(API.USERS.AUDIT_LOG).pipe(
      tap(items => {
        console.log('loadAuditLog: Successfully fetched', items ? items.length : 0, 'items');
        this.patch('auditLog', { items, loaded: true, total: items.length });
      }),
      catchError(err => {
        console.error('loadAuditLog: Failed to fetch items', err);
        this.patch('auditLog', { error: err.message });
        return throwError(() => err);
      }),
      finalize(() => this.patch('auditLog', { loading: false })),
    );
  }

  getIpWhitelist(): Observable<{ ip: string; label: string; blocked: boolean }[]> {
    return this.get(API.USERS.IP_WHITELIST);
  }

  addIpWhitelist(ip: string, label: string, blocked: boolean): Observable<any> {
    return this.post(API.USERS.IP_WHITELIST, { ip, label, blocked }).pipe(
      tap(() => this.toast.success('IP rule added successfully!'))
    );
  }

  deleteIpWhitelist(ip: string): Observable<any> {
    return this.http.delete<ApiResponse<any>>(`${API.USERS.IP_WHITELIST}?ip=${encodeURIComponent(ip)}`).pipe(
      map(r => r.data),
      tap(() => this.toast.success('IP rule removed.'))
    );
  }

  // ============================================================
  //  MASTERS
  // ============================================================
  loadMasters(): Observable<Masters> {
    return this.get<Masters>(API.MASTERS.ALL).pipe(
      tap(m => this._state.update(s => ({ ...s, masters: m }))),
    );
  }

  updateMasterCategory(key: keyof Masters, values: string[]): Observable<string[]> {
    return this.put<string[]>(API.MASTERS.UPDATE(key), { items: values }).pipe(
      tap(updated => this._state.update(s => ({
        ...s,
        masters: s.masters ? { ...s.masters, [key]: updated } : null,
      }))),
      tap(() => this.toast.success('Master updated.')),
    );
  }

  addMasterItem(key: keyof Masters, item: string): Observable<string[]> {
    return this.post<string[]>(API.MASTERS.ADD_ITEM(key), { item }).pipe(
      tap(updated => this._state.update(s => ({
        ...s,
        masters: s.masters ? { ...s.masters, [key]: updated } : null,
      }))),
    );
  }

  deleteMasterItem(key: keyof Masters, item: string): Observable<string[]> {
    return this.del<string[]>(API.MASTERS.DELETE_ITEM(key, item)).pipe(
      tap(updated => this._state.update(s => ({
        ...s,
        masters: s.masters ? { ...s.masters, [key]: updated } : null,
      }))),
    );
  }

  reorderMasterItems(key: keyof Masters, items: string[]): Observable<string[]> {
    return this.put<string[]>(API.MASTERS.REORDER(key), { items });
  }

  // ============================================================
  //  NOTIFICATIONS
  // ============================================================
  loadNotifications(): Observable<Notification[]> {
    return this.get<Notification[]>(API.NOTIFICATIONS.LIST).pipe(
      tap(items => this.patch('notifications', { items, loaded: true })),
    );
  }

  markNotificationRead(id: string): Observable<void> {
    return this.put<void>(API.NOTIFICATIONS.MARK_READ(id), {}).pipe(
      tap(() => this.patch('notifications', {
        items: this.notifications().map(n => n.id === id ? { ...n, read: true } : n),
      })),
    );
  }

  markAllNotificationsRead(): Observable<void> {
    return this.put<void>(API.NOTIFICATIONS.MARK_ALL_READ, {}).pipe(
      tap(() => this.patch('notifications', {
        items: this.notifications().map(n => ({ ...n, read: true })),
      })),
    );
  }

  // ============================================================
  //  DIARY
  // ============================================================
  getDiaryEvents(year: number, month: number, scope: 'own' | 'team' | 'org' = 'own', memberId?: number | null): Observable<DiaryEventDto[]> {
    return this.get<DiaryEventDto[]>(API.DIARY.BY_MONTH(year, month, scope, memberId));
  }

  getDiaryScopeOptions(): Observable<DiaryScopeOptions> {
    return this.get<DiaryScopeOptions>(API.DIARY.SCOPE_OPTIONS);
  }

  createDiaryEvent(data: Partial<DiaryEventDto>): Observable<DiaryEventDto> {
    return this.post<DiaryEventDto>(API.DIARY.CREATE, data).pipe(
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Event added to diary.')),
    );
  }

  updateDiaryEvent(id: string, data: Partial<DiaryEventDto>): Observable<DiaryEventDto> {
    return this.put<DiaryEventDto>(API.DIARY.UPDATE(id), data).pipe(
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Diary event updated.')),
    );
  }

  deleteDiaryEvent(id: string): Observable<void> {
    return this.del<void>(API.DIARY.DELETE(id)).pipe(
      tap(() => this.loadDashboardStats().subscribe()),
      tap(() => this.toast.success('Diary event removed.')),
    );
  }

  // ============================================================
  //  REPORTS
  // ============================================================
  getReportsSummary(fy?: string, practiceArea?: string, court?: string, advocate?: string): Observable<any> {
    let params = new HttpParams();
    if (fy) params = params.set('fy', fy);
    if (practiceArea) params = params.set('practiceArea', practiceArea);
    if (court) params = params.set('court', court);
    if (advocate) params = params.set('advocate', advocate);
    return this.get<any>(API.REPORTS.SUMMARY, params);
  }

  exportReportCustom(type: string, format: string, filters: string): Observable<Blob> {
    return this.http.post(
      API.REPORTS.EXPORT,
      { type, format, filters },
      { responseType: 'blob' }
    ).pipe(catchError(err => this._handleError(err)));
  }

  scheduleReport(type: string, filters: string, frequency: string, email: string): Observable<any> {
    return this.post<any>(API.REPORTS.SCHEDULE, { type, filters, frequency, email });
  }

  // ============================================================
  //  SETTINGS
  // ============================================================
  getFirmSettings(): Observable<Record<string, unknown>> {
    return this.get(API.SETTINGS.FIRM);
  }

  updateFirmSettings(data: Record<string, unknown>): Observable<void> {
    return this.put<void>(API.SETTINGS.FIRM, data).pipe(
      tap(() => this.toast.success('Firm settings saved.')),
    );
  }

  getNotificationSettings(): Observable<Record<string, boolean>> {
    return this.get(API.SETTINGS.NOTIFICATIONS);
  }

  updateNotificationSettings(data: Record<string, boolean>): Observable<void> {
    return this.put<void>(API.SETTINGS.NOTIFICATIONS, data).pipe(
      tap(() => this.toast.success('Notification preferences saved.')),
    );
  }

  // ============================================================
  //  PRIVATE HELPERS
  // ============================================================
  private _toParams(params?: ListParams): HttpParams | undefined {
    if (!params) return undefined;
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', params.page.toString());
    if (params.size !== undefined) p = p.set('size', params.size.toString());
    if (params.sort) p = p.set('sort', params.sort);
    if (params.order) p = p.set('order', params.order);
    if (params.search) p = p.set('search', params.search);
    return p;
  }

  // Local Blob URL Cache for file preview
  private localUrls: Record<string, string> = {};

  storeLocalUrl(filename: string, file: File) {
    const url = URL.createObjectURL(file);
    this.localUrls[filename] = url;
  }

  getLocalUrl(filename: string): string | null {
    return this.localUrls[filename] || null;
  }

  // Expose raw state for debugging
  getState() { return this._state(); }
}
