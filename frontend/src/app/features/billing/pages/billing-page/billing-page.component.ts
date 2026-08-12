import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Invoice, InvoiceLineItem, Expense, Payment, PendingBillable } from '../../../../core/models';
import { ToastService } from '../../../../core/services/toast.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { PaginatorComponent } from '../../../../shared/components/paginator/paginator.component';
import { AppButtonComponent } from '../../../../shared/components/button/button.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { FilterTabsComponent } from '../../../../shared/components/filter-tabs/filter-tabs.component';
import { ModalShellComponent } from '../../../../shared/components/modal-shell/modal-shell.component';

@Component({
  selector: 'app-billing-page',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, PaginatorComponent,
    AppButtonComponent, SearchInputComponent
  ],
  templateUrl: './billing-page.component.html',
  styleUrl: './billing-page.component.scss'
})
export class BillingPageComponent implements OnInit, OnDestroy {
  ds = inject(DataService);
  toast = inject(ToastService);
  private route = inject(ActivatedRoute);
  private sanitizer = inject(DomSanitizer);

  // Core Billing & Expenses
  billing  = this.ds.billing;
  loading  = this.ds.billingLoading;
  total    = this.ds.billingTotal;
  expenses = this.ds.expenses;
  expensesLoading = this.ds.expensesLoading;

  // Search, Tab selection
  search     = signal('');
  filter     = signal('All');
  activeTab  = signal<'invoices' | 'expenses'>('invoices');
  isBillablesCollapsed = signal(true);

  // Pagination
  page = signal(0);
  readonly size = 20;

  private _destroy$ = new Subject<void>();
  private _search$  = new Subject<string>();

  // Stats / Summaries
  summary = signal<any>(null);

  // Modals & Drawers
  showForm = signal(false); // Invoice create modal
  showExpenseForm = signal(false); // Expense create modal
  showPaymentModal = signal(false); // Payment record modal
  showDrawer = signal(false); // Detail drawer

  // Selection states
  selectedInvoice = signal<Invoice | null>(null);
  selectedInvoicePayments = signal<Payment[]>([]);
  bulkSelectedInvoices = signal<number[]>([]);

  // Forms
  form = signal<any>({
    clientId: null,
    clientName: '',
    matterId: null,
    matterTitle: '',
    dueDate: '',
    description: '',
    lineItems: []
  });

  expenseForm = signal<any>({
    matterId: null,
    matterTitle: '',
    clientId: null,
    clientName: '',
    category: 'Court Fees',
    amount: 0,
    date: new Date().toISOString().split('T')[0],
    billable: true,
    receiptPath: ''
  });

  // Expense Pagination State
  expensesPage = signal(1);
  expensesPageSize = 10;

  // Receipt Modal State
  selectedReceipt = signal<{ name: string; url: string; safeUrl: SafeResourceUrl } | null>(null);
  showReceiptModal = signal(false);

  constructor() {
    // Debounced search
    this._search$.pipe(debounceTime(300), takeUntil(this._destroy$)).subscribe(() => {
      this.page.set(0);
      this._reloadBilling();
    });
  }

  // Expense Details Modal State
  selectedExpense = signal<Expense | null>(null);
  showExpenseDetailsModal = signal(false);

  openExpenseDetails(exp: Expense) {
    this.selectedExpense.set(exp);
    this.showExpenseDetailsModal.set(true);
  }

  paymentForm = signal<any>({
    amount: 0,
    paymentDate: new Date().toISOString().split('T')[0],
    mode: 'UPI',
    referenceNo: ''
  });

  // Dynamic Pending Billables Queue
  pendingBillables = signal<PendingBillable[]>([]);
  selectedPendingIds = signal<string[]>([]);

  selectedPendingTotal = computed(() => {
    const selectedIds = this.selectedPendingIds();
    return this.pendingBillables()
      .filter(b => selectedIds.includes(b.id))
      .reduce((sum, b) => sum + (b.suggestedAmount || 0), 0);
  });

  isAllPendingSelected = computed(() => {
    const items = this.pendingBillables();
    return items.length > 0 && this.selectedPendingIds().length === items.length;
  });

  toggleSelectAllPending() {
    if (this.isAllPendingSelected()) {
      this.selectedPendingIds.set([]);
    } else {
      this.selectedPendingIds.set(this.pendingBillables().map(b => b.id));
    }
  }

  ngOnInit() {
    this._reloadBilling();
    this.ds.loadExpenses().subscribe();
    this.ds.loadMatters().subscribe();
    this.ds.getBillingSummary().subscribe(s => this.summary.set(s));
    this.ds.getPendingBillables().subscribe(pb => this.pendingBillables.set(pb));
    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openNewInvoice();
      }
    });
  }

  ngOnDestroy() {
    this._destroy$.next();
    this._destroy$.complete();
  }

  onSearch(value: string) {
    this.search.set(value);
    this._search$.next(value);
  }

  onFilterChange(status: string) {
    this.filter.set(status);
    this.page.set(0);
    this._reloadBilling();
  }

  onPageChange(p: number) {
    this.page.set(p);
    this._reloadBilling();
  }

  private _reloadBilling() {
    const status = this.filter();
    this.ds.loadBilling({
      page:   this.page(),
      size:   this.size,
      sort:   'id',
      order:  'desc',
      search: this.search() || undefined,
      filter: status !== 'All' ? { status } : undefined,
    }).subscribe();
  }

  loadAllData() {
    this._reloadBilling();
    this.ds.loadExpenses().subscribe();
    this.ds.loadMatters().subscribe();
    this.ds.getBillingSummary().subscribe(s => this.summary.set(s));
    this.ds.getPendingBillables().subscribe(pb => this.pendingBillables.set(pb));
  }

  // Count Badges for Status Filters
  // These are computed from the currently loaded page; for accurate totals use getBillingSummary()
  get statusCounts() {
    const list = this.billing();
    return {
      all:           this.total(),
      unpaid:        list.filter(i => i.status === 'Unpaid').length,
      paid:          list.filter(i => i.status === 'Paid').length,
      overdue:       list.filter(i => i.status === 'Overdue').length,
      partiallyPaid: list.filter(i => i.status === 'Partially Paid').length,
      draft:         list.filter(i => i.status === 'Draft').length
    };
  }

  // Dashboard Card Deep-links
  clickStatCard(status: string) {
    this.filter.set(status);
    this.activeTab.set('invoices');
    this.page.set(0);
    this._reloadBilling();
  }


  onReceiptSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.ds.uploadFilingAttachment(file).subscribe({
        next: (res) => {
          if (res?.s3Url) {
            this.expenseForm.update(f => ({ ...f, receiptPath: res.s3Url }));
            this.toast.success('Receipt uploaded to uploads directory!');
          }
        },
        error: (err) => {
          this.toast.error('Failed to upload receipt file: ' + err.message);
        }
      });
    }
  }

  viewReceipt(exp: Expense) {
    if (!exp.receiptPath) return;
    const fullUrl = exp.receiptPath.startsWith('/')
      ? `http://localhost:8084${exp.receiptPath}`
      : exp.receiptPath;
    this.selectedReceipt.set({
      name: `${exp.category} Receipt (${exp.date})`,
      url: fullUrl,
      safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(fullUrl)
    });
    this.showReceiptModal.set(true);
  }

  // Format Helper
  fmt(n: number) {
    return n >= 100000
      ? '₹' + (n / 100000).toFixed(1) + 'L'
      : n >= 1000
        ? '₹' + (n / 1000).toFixed(0) + 'K'
        : '₹' + n;
  }

  // Formatting precision
  fmtPrecise(n?: number) {
    if (n === undefined) return '₹0.00';
    return '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  // Invoice creation form functions
  openNewInvoice() {
    this.form.set({
      clientId: null,
      clientName: '',
      matterId: null,
      matterTitle: '',
      dueDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      description: '',
      lineItems: [{ feeType: 'Retainer', description: 'Legal consultancy charges', amount: 25000 }]
    });
    this.showForm.set(true);
  }

  addLineItem() {
    this.form.update(f => ({
      ...f,
      lineItems: [...f.lineItems, { feeType: 'Consultation', description: '', amount: 5000 }]
    }));
  }

  removeLineItem(idx: number) {
    this.form.update(f => ({
      ...f,
      lineItems: f.lineItems.filter((_: any, i: number) => i !== idx)
    }));
  }

  get invoiceFormTotal() {
    return this.form().lineItems.reduce((acc: number, curr: any) => acc + (curr.amount || 0), 0);
  }

  onInvoiceMatterSelect(event: Event) {
    const mId = +(event.target as HTMLSelectElement).value;
    const matter = this.ds.matters().find(m => +m.id === mId);
    if (matter) {
      this.form.update(f => ({
        ...f,
        matterId: mId,
        matterTitle: matter.title,
        clientId: matter.clientId,
        clientName: matter.clientName || 'No Client'
      }));
    }
  }

  onExpenseMatterSelect(event: Event) {
    const mId = +(event.target as HTMLSelectElement).value;
    const matter = this.ds.matters().find(m => +m.id === mId);
    if (matter) {
      this.expenseForm.update(f => ({
        ...f,
        matterId: mId,
        matterTitle: matter.title,
        clientId: matter.clientId,
        clientName: matter.clientName || 'No Client'
      }));
    }
  }

  saveInvoice() {
    const f = this.form();
    if (!f.clientName || f.lineItems.length === 0) {
      this.toast.error('Please specify client and at least one line item.');
      return;
    }
    const payload = {
      ...f,
      amount: this.invoiceFormTotal,
      status: 'Unpaid'
    };
    this.ds.createInvoice(payload).subscribe(() => {
      this.showForm.set(false);
      this.loadAllData();
    });
  }

  selectedPendingClientCount = computed(() => {
    const selectedIds = this.selectedPendingIds();
    const items = this.pendingBillables().filter(b => selectedIds.includes(b.id));
    const clients = new Set(items.map(i => i.clientName || (i.clientId ? String(i.clientId) : 'No Client')));
    return clients.size;
  });

  // Prepopulate from Pending Billables widget
  generateInvoiceFromPending() {
    const selectedIds = this.selectedPendingIds();
    if (selectedIds.length === 0) return;

    const items = this.pendingBillables().filter(b => selectedIds.includes(b.id));
    if (items.length === 0) return;

    // Strict Client Validation: Ensure all selected items belong to the EXACT same client
    const uniqueClientNames = Array.from(new Set(items.map(i => i.clientName || 'No Client')));
    const uniqueClientIds = Array.from(new Set(items.map(i => i.clientId).filter(id => id != null)));

    if (uniqueClientNames.length > 1 || uniqueClientIds.length > 1) {
      this.toast.error(
        `Cannot bundle items for different clients (${uniqueClientNames.join(', ')}) into a single invoice. Please select billable events for a single client.`
      );
      return;
    }

    const first = items[0];
    const uniqueMatters = Array.from(new Set(items.map(i => i.matterTitle).filter(Boolean)));
    const isMultipleMatters = uniqueMatters.length > 1;

    // Build line items — if multiple matters exist for the same client, prepend matter title to line items for transparency
    const lineItems: InvoiceLineItem[] = items.map(pb => {
      const prefix = isMultipleMatters && pb.matterTitle ? `[${pb.matterTitle}] ` : '';
      return {
        feeType: pb.type === 'Filing' ? 'Filing Charges' : pb.type === 'Hearing' ? 'Appearance Fees' : pb.type === 'Task' ? 'Drafting Fees' : 'Reimbursement',
        description: prefix + pb.description,
        amount: pb.suggestedAmount,
        sourceReference: pb.id
      };
    });

    this.form.set({
      clientId: first.clientId,
      clientName: first.clientName || 'No Client',
      matterId: isMultipleMatters ? undefined : first.matterId,
      matterTitle: isMultipleMatters ? `Multiple Matters (${uniqueMatters.length} cases)` : (first.matterTitle || 'Unlinked'),
      dueDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      description: `Invoice bundled from ${items.length} billable events.`,
      lineItems: lineItems
    });

    this.selectedPendingIds.set([]);
    this.showForm.set(true);
  }

  togglePendingSelection(id: string) {
    this.selectedPendingIds.update(ids => {
      if (ids.includes(id)) {
        return ids.filter(x => x !== id);
      } else {
        return [...ids, id];
      }
    });
  }

  // Invoice Detail Drawer and Payments
  openInvoiceDetails(inv: Invoice) {
    this.selectedInvoice.set(inv);
    this.ds.getPayments(inv.id).subscribe(payments => {
      this.selectedInvoicePayments.set(payments);
    });
    this.showDrawer.set(true);
  }

  openRecordPayment(inv: Invoice) {
    this.selectedInvoice.set(inv);
    this.paymentForm.set({
      amount: (inv.amount - (inv.paidAmount || 0)),
      paymentDate: new Date().toISOString().split('T')[0],
      mode: 'UPI',
      referenceNo: ''
    });
    this.showPaymentModal.set(true);
    this.showDrawer.set(false);
  }

  savePayment() {
    const inv = this.selectedInvoice();
    if (!inv) return;
    const payload = this.paymentForm();
    this.ds.recordPayment(inv.id, payload).subscribe(() => {
      this.showPaymentModal.set(false);
      this.loadAllData();
      this.toast.success('Payment recorded and receipt generated.');
    });
  }

  triggerReminder(inv: Invoice) {
    this.ds.bulkRemind([inv.id]).subscribe(() => {
      this.toast.success(`Reminder sent to ${inv.clientName}`);
    });
  }

  // Bulk Actions
  toggleInvoiceSelection(id: number) {
    this.bulkSelectedInvoices.update(list => {
      if (list.includes(id)) {
        return list.filter(x => x !== id);
      } else {
        return [...list, id];
      }
    });
  }

  bulkSendReminders() {
    const list = this.bulkSelectedInvoices();
    if (list.length === 0) return;
    this.ds.bulkRemind(list).subscribe(() => {
      this.bulkSelectedInvoices.set([]);
    });
  }

  bulkExport() {
    const list = this.bulkSelectedInvoices();
    const count = list.length || this.total();
    this.toast.success(`Exported ${count} invoices to Excel/PDF successfully.`);
  }

  bulkDeleteInvoices() {
    const list = this.bulkSelectedInvoices();
    if (list.length === 0) return;
    if (confirm(`Are you sure you want to delete ${list.length} selected invoice(s)?`)) {
      this.ds.bulkDeleteInvoices(list).subscribe(() => {
        this.bulkSelectedInvoices.set([]);
        this.loadAllData();
      });
    }
  }

  deleteInvoice(inv: Invoice) {
    if (!inv.id || !confirm(`Delete invoice ${inv.invoiceNo} permanently?`)) return;
    this.ds.deleteInvoice(inv.id).subscribe(() => {
      this.showDrawer.set(false);
      this.loadAllData();
    });
  }

  // Expenses Tab Functions
  openNewExpense() {
    const firstMatter = this.ds.matters()[0];
    this.expenseForm.set({
      matterId: firstMatter ? +firstMatter.id : null,
      matterTitle: firstMatter ? firstMatter.title : '',
      clientId: firstMatter ? firstMatter.clientId : null,
      clientName: firstMatter ? firstMatter.clientName || 'No Client' : '',
      category: 'Court Fees',
      amount: 0,
      date: new Date().toISOString().split('T')[0],
      billable: true,
      receiptPath: ''
    });
    this.showExpenseForm.set(true);
  }

  saveExpense() {
    const f = this.expenseForm();
    if (!f.category || f.amount <= 0) {
      this.toast.error('Please enter category and a valid amount.');
      return;
    }
    this.ds.createExpense(f).subscribe(() => {
      this.showExpenseForm.set(false);
      this.loadAllData();
    });
  }

  toggleExpenseBillable(exp: Expense) {
    if (!exp.id) return;
    this.ds.updateExpense(exp.id, { billable: !exp.billable }).subscribe(() => {
      this.loadAllData();
    });
  }

  deleteExpense(exp: Expense) {
    if (!exp.id || !confirm('Are you sure you want to delete this expense?')) return;
    this.ds.deleteExpense(exp.id).subscribe(() => {
      this.loadAllData();
    });
  }

  // Aging column calc
  getAgingLabel(inv: Invoice): string {
    if (inv.status !== 'Overdue' || !inv.dueDate) return '';
    const due = new Date(inv.dueDate);
    const today = new Date();
    const diffTime = Math.abs(today.getTime() - due.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return `${diffDays} days overdue`;
  }

  printInvoice(inv: Invoice) {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      this.toast.error('Pop-up blocker prevented printing. Please allow popups.');
      return;
    }

    const outstanding = inv.amount - (inv.paidAmount || 0);
    const lineItemsHtml = (inv.lineItems && inv.lineItems.length > 0)
      ? inv.lineItems.map(item => `
        <tr>
          <td><span class="fee-type">${item.feeType}</span></td>
          <td>${item.description || '—'}</td>
          <td class="num">₹${item.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
        </tr>
      `).join('')
      : `
        <tr>
          <td><span class="fee-type">Retainer</span></td>
          <td>${inv.description || 'Standard retainer fee invoice'}</td>
          <td class="num">₹${inv.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
        </tr>
      `;

    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Invoice — ${inv.invoiceNo}</title>
        <style>
          body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #1e293b;
            margin: 0;
            padding: 40px;
            font-size: 14px;
            line-height: 1.5;
          }
          .invoice-box {
            max-width: 800px;
            margin: auto;
          }
          .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            border-bottom: 2px solid #e2e8f0;
            padding-bottom: 24px;
            margin-bottom: 30px;
          }
          .logo-area h1 {
            margin: 0;
            font-size: 26px;
            color: #0f172a;
            font-weight: 800;
            letter-spacing: -0.5px;
          }
          .logo-area p {
            margin: 4px 0 0 0;
            color: #64748b;
            font-size: 12px;
          }
          .meta-area {
            text-align: right;
          }
          .meta-area h2 {
            margin: 0;
            font-size: 20px;
            color: #0ea5e9;
            font-weight: 700;
          }
          .meta-area p {
            margin: 4px 0 0 0;
            color: #475569;
          }
          .details-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 40px;
            margin-bottom: 40px;
          }
          .details-block h3 {
            margin: 0 0 10px 0;
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 1px;
            color: #64748b;
          }
          .details-block p {
            margin: 0 0 6px 0;
            font-size: 14px;
            color: #0f172a;
          }
          .details-block .name {
            font-weight: 700;
            font-size: 16px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
          }
          th {
            background: #f8fafc;
            border-bottom: 2px solid #cbd5e1;
            color: #475569;
            font-weight: 700;
            text-transform: uppercase;
            font-size: 11px;
            letter-spacing: 0.5px;
            padding: 12px;
            text-align: left;
          }
          td {
            padding: 12px;
            border-bottom: 1px solid #e2e8f0;
            vertical-align: top;
          }
          .num {
            text-align: right;
            font-family: monospace;
            font-size: 13px;
            font-weight: 600;
          }
          .fee-type {
            display: inline-block;
            background: #f0f9ff;
            color: #0369a1;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
          }
          .totals-wrap {
            display: flex;
            justify-content: flex-end;
            margin-top: 20px;
          }
          .totals-table {
            width: 300px;
            margin-bottom: 0;
          }
          .totals-table td {
            padding: 8px 12px;
            border-bottom: none;
          }
          .totals-table tr.grand-total {
            border-top: 2px solid #0f172a;
            font-size: 16px;
            font-weight: 800;
          }
          .totals-table tr.grand-total td {
            padding-top: 12px;
          }
          .status-stamp {
            display: inline-block;
            border: 3px solid;
            padding: 6px 15px;
            font-size: 16px;
            font-weight: 900;
            text-transform: uppercase;
            border-radius: 6px;
            transform: rotate(-8deg);
            margin-top: 15px;
          }
          .stamp-paid { border-color: #22c55e; color: #22c55e; }
          .stamp-unpaid { border-color: #f59e0b; color: #f59e0b; }
          .stamp-overdue { border-color: #ef4444; color: #ef4444; }
          .stamp-partially-paid { border-color: #d97706; color: #d97706; }
          .footer {
            margin-top: 80px;
            border-top: 1px solid #e2e8f0;
            padding-top: 20px;
            text-align: center;
            font-size: 11px;
            color: #94a3b8;
          }
          @media print {
            body { padding: 0; }
            .no-print { display: none; }
          }
        </style>
      </head>
      <body>
        <div class="invoice-box">
          <div class="header">
            <div class="logo-area">
              <h1>SmartCourt</h1>
              <p>PREMIUM COURTOS PRACTICE MANAGEMENT</p>
            </div>
            <div class="meta-area">
              <h2>INVOICE</h2>
              <p><strong>Invoice No:</strong> ${inv.invoiceNo}</p>
            </div>
          </div>

          <div class="details-grid">
            <div class="details-block">
              <h3>Billed From</h3>
              <p class="name">Adv. Amit Sharma</p>
              <p>SmartCourt Chambers</p>
              <p>Delhi, India</p>
              <p>Email: contact@smartcourt.law</p>
            </div>
            <div class="details-block">
              <h3>Billed To</h3>
              <p class="name">${inv.clientName}</p>
              <p><strong>Matter:</strong> ${inv.matterTitle || 'General Legal Representation'}</p>
              <p><strong>Due Date:</strong> ${inv.dueDate || '—'}</p>
              <p><strong>Date Issued:</strong> ${new Date().toISOString().split('T')[0]}</p>
            </div>
          </div>

          <table>
            <thead>
              <tr>
                <th width="150">Item Category</th>
                <th>Description</th>
                <th width="120" style="text-align: right;">Amount</th>
              </tr>
            </thead>
            <tbody>
              ${lineItemsHtml}
            </tbody>
          </table>

          <div style="display: flex; justify-content: space-between; align-items: flex-start;">
            <div>
              <div class="status-stamp stamp-${inv.status.toLowerCase().replace(' ', '-')}">${inv.status}</div>
            </div>
            <div class="totals-wrap">
              <table class="totals-table">
                <tr>
                  <td>Total Billed:</td>
                  <td class="num">₹${inv.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                </tr>
                <tr>
                  <td style="color: #22c55e;">Amount Paid:</td>
                  <td class="num" style="color: #22c55e;">₹${(inv.paidAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                </tr>
                <tr class="grand-total">
                  <td style="color: #ef4444;">Balance Due:</td>
                  <td class="num" style="color: #ef4444;">₹${outstanding.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                </tr>
              </table>
            </div>
          </div>

          <div class="footer">
            <p>Thank you for your business. Please make payments by the due date.</p>
            <p>© 2026 SmartCourt. All rights reserved.</p>
          </div>
        </div>
        <script>
          window.onload = function() {
            window.print();
          }
        </script>
      </body>
      </html>
    `;

    printWindow.document.write(htmlContent);
    printWindow.document.close();
  }
}
