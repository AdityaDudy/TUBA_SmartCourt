import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Hearing } from '../../../../core/models';

import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-cause-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cause-list-page.component.html',
  styleUrl: './cause-list-page.component.scss',
})
export class CauseListPageComponent implements OnInit {
  private ds     = inject(DataService);
  private auth   = inject(AuthService);
  private router = inject(Router);

  hearings    = this.ds.hearings;
  loading     = this.ds.hearingsLoading;

  // ── Filters ────────────────────────────────────────────────
  courtFilter  = signal<string>('All');
  statusFilter = signal<string>('All');
  searchQuery  = signal<string>('');
  viewDate     = signal<string>(this.todayIso());
  dateMode     = signal<'date' | 'all' | 'completed'>('all');

  readonly courtTabs  = ['All', 'SC', 'HC', 'NCLT', 'ITAT', 'District'];
  readonly statusTabs = ['All', 'Scheduled', 'Urgent', 'Completed'];

  // ── Detail Modal ────────────────────────────────────────────
  selectedHearing = signal<Hearing | null>(null);

  openDetail(h: Hearing): void {
    this.selectedHearing.set(h);
  }

  closeDetail(): void {
    this.selectedHearing.set(null);
  }

  // ── Admin ──────────────────────────────────────────────────
  syncing     = signal(false);
  isAdmin     = computed(() => {
    const u = this.auth.currentUser();
    return u?.role === 'admin' || u?.role === 'ROLE_ADMIN';
  });

  // ── Pagination ──────────────────────────────────────────────
  readonly Math = Math;
  currentPage = signal<number>(1);
  pageSize    = signal<number>(10);
  readonly pageSizeOptions = [10, 25, 50, 100];

  // ── Derived ────────────────────────────────────────────────
  filtered = computed<Hearing[]>(() => {
    const q        = this.searchQuery().toLowerCase().trim();
    const cf       = this.courtFilter();
    const sf       = this.statusFilter();
    const mode     = this.dateMode();
    const todayStr = new Date().toISOString().split('T')[0];

    return this.hearings()
      .filter(h => {
        // Mode filtering
        if (mode === 'all') {
          // All Upcoming Hearings: omit past dates (< todayStr)
          const hDate = h.hearingDate || '';
          if (hDate < todayStr) {
            return false;
          }
        } else if (mode === 'completed') {
          // Completed Hearings section: only include completed hearings or past hearings
          if (h.status !== 'Completed' && (h.hearingDate || '') >= todayStr) {
            return false;
          }
        }

        const courtOk  = cf === 'All' || (h.court && h.court.toLowerCase().includes(cf.toLowerCase()));
        const statusOk = sf === 'All' || h.status === sf;
        const searchOk = !q ||
          h.caseTitle?.toLowerCase().includes(q) ||
          (h.caseNo ?? h.caseNumber ?? '').toLowerCase().includes(q) ||
          h.advocate?.toLowerCase().includes(q) ||
          h.court?.toLowerCase().includes(q) ||
          h.bench?.toLowerCase().includes(q);
        return courtOk && statusOk && searchOk;
      })
      .sort((a, b) => {
        const dateA = a.hearingDate || '';
        const dateB = b.hearingDate || '';

        if (mode === 'completed') {
          // Sort completed hearings by most recent date first
          return dateB.localeCompare(dateA);
        }

        const isUpcomingA = dateA >= todayStr;
        const isUpcomingB = dateB >= todayStr;

        if (isUpcomingA && isUpcomingB) {
          return dateA.localeCompare(dateB);
        }
        if (isUpcomingA && !isUpcomingB) return -1;
        if (!isUpcomingA && isUpcomingB) return 1;

        return dateB.localeCompare(dateA);
      });
  });

  totalPages = computed(() => {
    const total = this.filtered().length;
    return Math.ceil(total / this.pageSize()) || 1;
  });

  paginatedHearings = computed(() => {
    const page = this.currentPage();
    const size = this.pageSize();
    const start = (page - 1) * size;
    return this.filtered().slice(start, start + size);
  });

  urgentCount    = computed(() => this.hearings().filter(h => h.status === 'Urgent').length);
  scheduledCount = computed(() => this.hearings().filter(h => h.status === 'Scheduled').length);
  completedCount = computed(() => this.hearings().filter(h => h.status === 'Completed').length);

  // ── Lifecycle ──────────────────────────────────────────────
  ngOnInit(): void {
    this.reloadHearings();
  }

  setDateMode(mode: 'date' | 'all' | 'completed'): void {
    this.dateMode.set(mode);
    this.reloadHearings();
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  private reloadHearings(): void {
    this.currentPage.set(1);
    if (this.dateMode() === 'all' || this.dateMode() === 'completed') {
      this.ds.loadHearings('all').subscribe();
    } else {
      this.ds.loadHearings(this.viewDate()).subscribe();
    }
  }

  // ── Date navigation ────────────────────────────────────────
  onDatePicked(dateStr: string): void {
    if (!dateStr) return;
    this.viewDate.set(dateStr);
    this.dateMode.set('date');
    this.reloadHearings();
  }

  goToDate(offset: number): void {
    const d = new Date(this.viewDate());
    d.setDate(d.getDate() + offset);
    this.viewDate.set(this.isoDate(d));
    this.dateMode.set('date');
    this.reloadHearings();
  }

  goToToday(): void {
    this.viewDate.set(this.todayIso());
    this.dateMode.set('date');
    this.reloadHearings();
  }

  isToday(): boolean {
    return this.viewDate() === this.todayIso();
  }

  displayDate(): string {
    if (this.dateMode() === 'all') return 'Upcoming Hearings Overview';
    if (this.dateMode() === 'completed') return 'Completed Hearings Archive';
    const d = new Date(this.viewDate() + 'T00:00:00');
    if (this.isToday()) return 'Today — ' + d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    return d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  // ── Admin sync ─────────────────────────────────────────────
  syncNow(): void {
    if (this.syncing()) return;
    this.syncing.set(true);
    this.ds.syncHearings().subscribe({
      next: () => {
        this.syncing.set(false);
        this.reloadHearings();
      },
      error: () => this.syncing.set(false),
    });
  }

  // ── Helpers ────────────────────────────────────────────────
  statusClass(status: string | undefined): string {
    switch (status) {
      case 'Urgent':    return 'badge-urgent';
      case 'Completed': return 'badge-done';
      default:          return 'badge-sched';
    }
  }

  courtIcon(court: string | undefined): string {
    if (!court) return 'fa-landmark';
    const c = court.toLowerCase();
    if (c.includes('supreme')) return 'fa-building-columns';
    if (c.includes('high'))    return 'fa-scale-balanced';
    if (c.includes('nclt') || c.includes('company')) return 'fa-briefcase';
    if (c.includes('itat') || c.includes('income')) return 'fa-file-invoice-dollar';
    return 'fa-landmark';
  }

  formatHearingDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr + 'T00:00:00');
      return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  caseTitle(h: Hearing): string {
    return h.caseTitle ?? h.title ?? 'Untitled Case';
  }

  caseNo(h: Hearing): string {
    return h.caseNo ?? h.caseNumber ?? '—';
  }

  hearingTime(h: Hearing): string {
    return h.hearingTime ?? h.time ?? '—';
  }

  private todayIso(): string {
    return this.isoDate(new Date());
  }

  private isoDate(d: Date): string {
    return d.toISOString().split('T')[0];
  }
}
