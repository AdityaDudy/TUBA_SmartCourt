import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../../../core/services/data.service';
import { RecentSearchDto, ScrapeJobStatusResponse, CaseSearchResultDto } from '../../../../core/api/api-response.types';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

/** CNR pattern: 4 uppercase letters + 10–14 digits, e.g. DLST010012342024 */
const CNR_REGEX = /^[A-Z]{4}\d{10,14}$/;

/** Recent case-number search entry stored in localStorage */
export interface RecentCaseNumberSearch {
  value:      string;
  searchedAt: string;
}

const LS_KEY = 'tuba_recent_case_searches';
const LS_MAX = 8;

function loadCaseSearchHistory(): RecentCaseNumberSearch[] {
  try { return JSON.parse(localStorage.getItem(LS_KEY) ?? '[]'); }
  catch { return []; }
}

function saveCaseSearchHistory(list: RecentCaseNumberSearch[]): void {
  localStorage.setItem(LS_KEY, JSON.stringify(list.slice(0, LS_MAX)));
}

/** Parses string like "CS(OS) 403/2026", "CS OS 403 2026", or "403/2026" */
function parseCaseNumberInput(raw: string): { caseType: string; number: string; year: string } {
  const trimmed = raw.trim();

  // Pattern matching type + number + / + year, e.g. "CS(OS) 403/2026" or "WP 1247/2024"
  const matchSlash = trimmed.match(/^(.*?)\s*(\d+)\s*\/\s*(\d{4})$/);
  if (matchSlash) {
    return {
      caseType: matchSlash[1].trim(),
      number: matchSlash[2].trim(),
      year: matchSlash[3].trim()
    };
  }

  // Pattern matching type + number + space + year, e.g. "CS(OS) 403 2026"
  const matchSpace = trimmed.match(/^(.*?)\s*(\d+)\s+(\d{4})$/);
  if (matchSpace) {
    return {
      caseType: matchSpace[1].trim(),
      number: matchSpace[2].trim(),
      year: matchSpace[3].trim()
    };
  }

  return { caseType: '', number: '', year: '' };
}

@Component({
  selector: 'app-tracker-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tracker-page.component.html',
  styleUrl: './tracker-page.component.scss'
})
export class TrackerPageComponent implements OnInit, OnDestroy {
  private ds     = inject(DataService);
  private router = inject(Router);

  cnr        = signal('');
  searchType = signal('CNR Number');
  loading    = signal(false);
  error      = signal<string | null>(null);

  // Async job state (CNR path only)
  jobStatus = signal<ScrapeJobStatusResponse | null>(null);
  private pollSub?: Subscription;

  // Case-number candidate picker
  searchResults = signal<CaseSearchResultDto[]>([]);
  showPicker    = signal(false);

  // CNR recent searches — from backend
  recentSearches = signal<RecentSearchDto[]>([]);

  // Case Number recent searches — from localStorage
  recentCaseSearches = signal<RecentCaseNumberSearch[]>(loadCaseSearchHistory());

  readonly searchTypes = ['CNR Number', 'Case Number'];

  courts = computed(() =>
    this.ds.masters()?.courts ||
    ['Supreme Court of India', 'Delhi High Court', 'Bombay High Court', 'Madras High Court', 'NCLT Mumbai', 'ITAT Delhi']
  );

  get cnrValid(): boolean {
    return this.searchType() !== 'CNR Number' || CNR_REGEX.test(this.cnr().trim().toUpperCase());
  }

  get cnrFormatError(): string | null {
    const v = this.cnr().trim();
    if (!v || this.searchType() !== 'CNR Number') return null;
    if (!CNR_REGEX.test(v.toUpperCase())) return 'CNR format: 4 letters + 10–14 digits (e.g. DLST010012342024)';
    return null;
  }

  get inputPlaceholder(): string {
    return this.searchType() === 'CNR Number' ? 'e.g. DLST010012342024' : 'e.g. CS(OS) 403/2026';
  }

  get hasRecents(): boolean {
    return this.recentSearches().length > 0 || this.recentCaseSearches().length > 0;
  }

  ngOnInit() {
    this.ds.loadMasters().subscribe();
    this.ds.getRecentSearches().subscribe({
      next: r => this.recentSearches.set(r),
      error: () => {}
    });
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  onCnrInput(value: string) {
    this.cnr.set(value);
    if (CNR_REGEX.test(value.trim().toUpperCase()) && this.searchType() !== 'CNR Number') {
      this.searchType.set('CNR Number');
    }
    if (this.showPicker()) { this.closePicker(); }
  }

  onSearchTypeChange(type: string) {
    this.searchType.set(type);
    this.cnr.set('');
    this.closePicker();
    this.error.set(null);
  }

  search() {
    const rawValue = this.cnr().trim();
    if (!rawValue) return;

    this.error.set(null);
    this.closePicker();
    this.pollSub?.unsubscribe();

    if (this.searchType() === 'CNR Number') {
      // ── CNR path (existing, unchanged) ──────────────────────────
      const rawCnr = rawValue.toUpperCase();
      if (!this.cnrValid) return;
      this.loading.set(true);
      this.jobStatus.set(null);
      this.ds.searchByCnr(rawCnr).subscribe({
        next: resp => this.handleSearchResponse(resp, rawCnr),
        error: err => {
          this.loading.set(false);
          this.error.set(err?.message || 'Search failed. Please try again.');
        }
      });

    } else {
      // ── Case Number path — Resolver Pipeline ──────────────────────
      const parsed = parseCaseNumberInput(rawValue);
      if (!parsed.number || !parsed.year) {
        this.error.set('Please enter case number in format: TYPE NUMBER/YEAR (e.g. CS(OS) 403/2026 or 403/2026)');
        return;
      }

      this.loading.set(true);
      this.jobStatus.set(null);
      this.pushCaseSearchHistory(rawValue);

      this.ds.searchByCaseNumber(parsed.caseType, parsed.number, parsed.year).subscribe({
        next: resp => {
          const targetCnr = resp.cnr || resp.result?.cnr || rawValue;
          this.handleSearchResponse(resp, targetCnr);
        },
        error: err => {
          this.loading.set(false);
          this.error.set(err?.message || 'Case resolution failed. Case may not be indexed yet.');
        }
      });
    }
  }


  // ── Recent chips handlers ──────────────────────────────────────

  /** Re-run a saved CNR search (backend history chip) */
  runRecentSearch(s: RecentSearchDto) {
    this.searchType.set('CNR Number');
    this.cnr.set(s.cnr);
    this.closePicker();
    this.search();
  }

  /** Re-run a saved Case Number search (localStorage chip) */
  runRecentCaseSearch(entry: RecentCaseNumberSearch) {
    this.searchType.set('Case Number');
    this.cnr.set(entry.value);
    this.error.set(null);
    this.closePicker();
    this.search();
  }

  // ── Picker ────────────────────────────────────────────────────

  /** User selects a candidate → feed its CNR into the existing CNR pipeline */
  selectResult(result: CaseSearchResultDto) {
    this.closePicker();
    this.cnr.set(result.cnr);
    this.searchType.set('CNR Number');
    this.error.set(null);
    this.loading.set(true);
    this.jobStatus.set(null);
    this.ds.searchByCnr(result.cnr).subscribe({
      next: resp => this.handleSearchResponse(resp, result.cnr),
      error: err => {
        this.loading.set(false);
        this.error.set(err?.message || 'Search failed. Please try again.');
      }
    });
  }

  closePicker() {
    this.showPicker.set(false);
    this.searchResults.set([]);
  }

  // ── Private helpers ───────────────────────────────────────────

  private pushCaseSearchHistory(value: string) {
    // Deduplicate on value, newest first
    const existing = loadCaseSearchHistory().filter(e => e.value !== value);
    const updated: RecentCaseNumberSearch[] = [
      { value, searchedAt: new Date().toISOString() },
      ...existing
    ];
    saveCaseSearchHistory(updated);
    this.recentCaseSearches.set(updated.slice(0, LS_MAX));
  }

  private handleSearchResponse(resp: ScrapeJobStatusResponse, cnr: string) {
    this.jobStatus.set(resp);
    if (resp.status === 'DONE' && resp.result) {
      this.loading.set(false);
      this.router.navigate(['/app/tracker', cnr]);
      return;
    }
    this.startPolling(resp.jobId!, cnr);
  }

  private startPolling(jobId: number, cnr: string) {
    this.pollSub = interval(3000).pipe(
      switchMap(() => this.ds.pollJobStatus(jobId)),
      takeWhile(r => r.status === 'PENDING' || r.status === 'RUNNING', true)
    ).subscribe({
      next: resp => {
        this.jobStatus.set(resp);
        if (resp.status === 'DONE') {
          this.loading.set(false);
          this.router.navigate(['/app/tracker', cnr]);
        } else if (resp.status === 'FAILED') {
          this.loading.set(false);
          this.error.set(resp.errorMessage || 'Scrape failed. Please try again.');
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Connection lost. Please refresh and try again.');
      }
    });
  }
}
