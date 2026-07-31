import {
  Component, inject, signal, computed, OnInit, OnDestroy
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DataService } from '../../../../core/services/data.service';
import {
  CaseDetailResponse, MatterSuggestion, OrderDto
} from '../../../../core/api/api-response.types';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

type Tab = 'overview' | 'parties' | 'orders' | 'history';

@Component({
  selector: 'app-case-detail-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-detail-page.component.html',
  styleUrl: './case-detail-page.component.scss'
})
export class CaseDetailPageComponent implements OnInit, OnDestroy {
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private ds     = inject(DataService);

  cnr         = signal<string>('');
  detail      = signal<CaseDetailResponse | null>(null);
  loading     = signal(true);
  error       = signal<string | null>(null);
  readonly tabs: { id: Tab; label: string; icon: string }[] = [
    { id: 'history',    label: 'History & Timeline', icon: 'fa-timeline' },
    { id: 'overview',   label: 'Overview',           icon: 'fa-circle-info' },
    { id: 'parties',    label: 'Parties',             icon: 'fa-users' },
    { id: 'orders',     label: 'Orders',              icon: 'fa-file-pdf' },
  ];

  activeTab = signal<Tab>('history');

  // Refresh state
  refreshing  = signal(false);
  private refreshPollSub?: Subscription;

  // Alert
  alertActive  = signal(false);
  alertLoading = signal(false);

  // Matter suggestion
  matterSuggestion = signal<MatterSuggestion | null>(null);
  linkingMatter    = signal(false);
  matterLinked     = signal(false);

  // Download state
  downloadingAll   = signal(false);
  exportingPdf     = signal(false);
  downloadingOrder = signal<number | null>(null);
  savingToMatter   = signal<number | null>(null);

  // Copy tooltip
  copiedField = signal<string | null>(null);

  get caseTitle(): string {
    const d = this.detail();
    if (!d) return '';
    const pet  = d.petitioners?.[0]?.name  || '—';
    const resp = d.respondents?.[0]?.name  || '—';
    return `${pet} vs ${resp}`;
  }

  get statusClass(): string {
    switch (this.detail()?.caseStatus) {
      case 'DISPOSED':    return 'status--disposed';
      case 'STAYED':      return 'status--stayed';
      case 'DISMISSED':   return 'status--dismissed';
      case 'TRANSFERRED': return 'status--transferred';
      default:            return 'status--pending';
    }
  }

  get isStale(): boolean {
    return this.detail()?.cacheSource === 'CACHE';
  }

  ngOnInit() {
    const cnr = this.route.snapshot.paramMap.get('cnr') || '';
    this.cnr.set(cnr.toUpperCase());
    this.loadDetail();
  }

  ngOnDestroy() {
    this.refreshPollSub?.unsubscribe();
  }

  private loadDetail() {
    this.loading.set(true);
    this.error.set(null);

    this.ds.getCaseDetail(this.cnr()).subscribe({
      next: detail => {
        this.detail.set(detail);
        this.alertActive.set(detail.alertActive ?? false);
        this.loading.set(false);
        this.loadMatterSuggestion();
      },
      error: err => {
        this.loading.set(false);
        if (err?.status === 404) {
          // Not in DB yet — trigger a fresh search
          this.ds.searchByCnr(this.cnr()).subscribe({
            next: resp => {
              if (resp.status === 'DONE' && resp.result) {
                this.detail.set(resp.result);
                this.alertActive.set(resp.result.alertActive ?? false);
                this.loadMatterSuggestion();
              } else if (resp.jobId) {
                this.startRefreshPoll(resp.jobId);
              }
            },
            error: () => this.error.set('Case not found. Please check the CNR and try again.')
          });
        } else {
          this.error.set(err?.message || 'Failed to load case details.');
        }
      }
    });
  }

  private loadMatterSuggestion() {
    if (this.detail()?.matterId) {
      this.matterLinked.set(true);
      return;
    }
    this.ds.suggestMatterLink(this.cnr()).subscribe({
      next: s => this.matterSuggestion.set(s),
      error: () => {}
    });
  }

  // ── Tab switching ────────────────────────────────────────────
  setTab(tab: Tab) { this.activeTab.set(tab); }

  // ── Refresh ──────────────────────────────────────────────────
  refresh() {
    if (this.refreshing()) return;
    this.refreshing.set(true);
    this.ds.refreshCase(this.cnr()).subscribe({
      next: resp => {
        if (resp.jobId) this.startRefreshPoll(resp.jobId);
      },
      error: () => this.refreshing.set(false)
    });
  }

  private startRefreshPoll(jobId: number) {
    this.refreshPollSub?.unsubscribe();
    this.refreshPollSub = interval(1000).pipe(
      switchMap(() => this.ds.pollJobStatus(jobId)),
      takeWhile(r => r.status === 'PENDING' || r.status === 'RUNNING', true)
    ).subscribe({
      next: resp => {
        if (resp.status === 'DONE') {
          this.refreshing.set(false);
          this.loadDetail();
        } else if (resp.status === 'FAILED') {
          this.refreshing.set(false);
          this.loading.set(false);
          this.error.set(resp.errorMessage || 'Refresh failed.');
        }
      },
      error: () => {
        this.refreshing.set(false);
        this.loading.set(false);
      }
    });
  }

  // ── Alert toggle ─────────────────────────────────────────────
  toggleAlert() {
    if (this.alertLoading()) return;
    const enable = !this.alertActive();
    this.alertLoading.set(true);
    this.ds.toggleAlert(this.cnr(), enable).subscribe({
      next: () => {
        this.alertActive.set(enable);
        this.alertLoading.set(false);
      },
      error: () => this.alertLoading.set(false)
    });
  }

  // ── Matter link ──────────────────────────────────────────────
  confirmMatterLink() {
    const s = this.matterSuggestion();
    if (!s || this.linkingMatter()) return;
    this.linkingMatter.set(true);
    this.ds.linkCaseToMatter(this.cnr(), s.matterId).subscribe({
      next: () => {
        this.matterLinked.set(true);
        this.matterSuggestion.set(null);
        this.linkingMatter.set(false);
        if (this.detail()) {
          this.detail.update(d => d ? ({ ...d, matterId: s.matterId, matterTitle: s.matterTitle }) : d);
        }
      },
      error: () => this.linkingMatter.set(false)
    });
  }

  dismissMatterSuggestion() { this.matterSuggestion.set(null); }

  // ── Downloads ────────────────────────────────────────────────
  downloadOrder(order: OrderDto) {
    if (!order.id) return;
    this.downloadingOrder.set(order.id);
    this.ds.downloadOrder(this.cnr(), order.id).subscribe({
      next: blob => {
        this.triggerDownload(blob, `Order-${order.orderDate || order.id}.pdf`);
        this.downloadingOrder.set(null);
      },
      error: () => this.downloadingOrder.set(null)
    });
  }

  saveToMatterVault(order: OrderDto) {
    if (!order.id) return;
    this.savingToMatter.set(order.id);
    this.ds.saveOrderToMatterVault(this.cnr(), order.id).subscribe({
      next: () => {
        this.savingToMatter.set(null);
        // Refresh case detail so matter link state is updated
        this.loadDetail();
      },
      error: () => this.savingToMatter.set(null)
    });
  }

  downloadAll() {
    if (this.downloadingAll()) return;
    this.downloadingAll.set(true);
    this.ds.downloadAllOrders(this.cnr()).subscribe({
      next: blob => {
        this.triggerDownload(blob, `Orders-${this.cnr()}.zip`);
        this.downloadingAll.set(false);
      },
      error: () => this.downloadingAll.set(false)
    });
  }

  exportPdf() {
    if (this.exportingPdf()) return;
    this.exportingPdf.set(true);
    this.ds.exportCase(this.cnr(), 'pdf').subscribe({
      next: blob => {
        this.triggerDownload(blob, `CaseSummary-${this.cnr()}.pdf`);
        this.exportingPdf.set(false);
      },
      error: () => this.exportingPdf.set(false)
    });
  }

  private triggerDownload(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  // ── Copy to clipboard ────────────────────────────────────────
  copyToClipboard(text: string, field: string) {
    navigator.clipboard.writeText(text).then(() => {
      this.copiedField.set(field);
      setTimeout(() => this.copiedField.set(null), 1800);
    });
  }

  // ── Navigation ───────────────────────────────────────────────
  backToSearch() { this.router.navigate(['/app/tracker']); }

  // ── Helpers ──────────────────────────────────────────────────
  nvl(v: string | null | undefined): string {
    return v && v.trim() ? v.trim() : '—';
  }

  formatDate(d: string | null | undefined): string {
    if (!d || d === '—') return '—';
    const trimmed = d.trim();
    // Handle dd-MM-yyyy or dd/MM/yyyy explicitly before passing to new Date()
    if (/^\d{2}[-/]\d{2}[-/]\d{4}$/.test(trimmed)) {
      const parts = trimmed.split(/[-/]/);
      const iso = `${parts[2]}-${parts[1]}-${parts[0]}`;
      const dt = new Date(iso);
      if (!isNaN(dt.getTime())) {
        return dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
      }
    }
    const dt = new Date(trimmed);
    if (!isNaN(dt.getTime())) {
      return dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    }
    return trimmed;
  }

  formatFileSize(bytes: number | null | undefined): string {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }

  timeSince(iso: string | null | undefined): string {
    if (!iso) return 'Never';
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 2) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  }
}
