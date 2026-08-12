import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports-page.component.html',
  styleUrl: './reports-page.component.scss'
})
export class ReportsPageComponent implements OnInit {
  ds = inject(DataService);
  router = inject(Router);
  toast = inject(ToastService);

  // Filters
  fy = signal<string>('2026');
  practiceArea = signal<string>('');
  court = signal<string>('');
  advocate = signal<string>('');

  // Data Loading
  summaryData = signal<any>(null);
  loading = signal<boolean>(false);

  // Chart Granularity ('monthly' | 'quarterly')
  chartGranularity = signal<'monthly' | 'quarterly'>('monthly');

  // Scheduling Modal States
  showScheduleModal = signal<boolean>(false);
  selectedReportType = signal<string>('');
  scheduleEmail = signal<string>('');
  scheduleFreq = signal<string>('Monthly');

  // Computed Helpers for Charts
  maxFunnelCount = computed(() => {
    const funnel = this.summaryData()?.funnel || [];
    if (!funnel.length) return 1;
    const max = Math.max(...funnel.map((f: any) => f.count || 0));
    return max > 0 ? max : 1;
  });

  // Palette color generator for practice area donut chart & legends
  getAreaColor(idx: number): string {
    const palette = [
      '#3b82f6', // Bright Blue
      '#10b981', // Emerald Green
      '#f59e0b', // Amber
      '#8b5cf6', // Violet/Purple
      '#ec4899', // Pink
      '#06b6d4', // Cyan
      '#6366f1', // Indigo
      '#f97316'  // Orange
    ];
    return palette[idx % palette.length];
  }

  // Skeletons / Trend Mock Sparklines
  sparklineMatters = [10, 15, 8, 12, 22, 35, 42];
  sparklineWinRate = [65, 68, 70, 72, 71, 75, 74];
  sparklineRevenue = [12, 18, 25, 30, 28, 42, 48];
  sparklineClients = [20, 28, 35, 42, 55, 70, 87];

  // Dynamic Options computed directly from loaded Matter records
  availablePracticeAreas = computed(() => {
    const list = this.ds.matters().map(m => m.area).filter(Boolean);
    return Array.from(new Set(list)).sort();
  });

  availableCourts = computed(() => {
    const list = this.ds.matters().map(m => m.court).filter(Boolean);
    return Array.from(new Set(list)).sort();
  });

  availableAdvocates = computed(() => {
    const fromMatters = this.ds.matters().map(m => m.advocate).filter(Boolean);
    const fromUsers = this.ds.users().map(u => u.name).filter(Boolean);
    const set = new Set([...fromMatters, ...fromUsers]);
    return Array.from(set).sort();
  });

  ngOnInit() {
    // Ensure matters, clients, and users are loaded to compute dynamic filters
    this.ds.loadMatters().subscribe();
    this.ds.loadClients().subscribe();
    this.ds.loadUsers().subscribe();
    this.loadData();
  }

  loadData() {
    this.loading.set(true);
    this.ds.getReportsSummary(
      this.fy(),
      this.practiceArea(),
      this.court(),
      this.advocate()
    ).subscribe({
      next: (res) => {
        this.summaryData.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  // Drilldown KPIs -> Live filtered list in respective screens
  drilldown(kpi: string) {
    if (kpi === 'matters') {
      this.router.navigate(['/app/matters'], {
        queryParams: {
          fy: this.fy(),
          practiceArea: this.practiceArea(),
          court: this.court(),
          advocate: this.advocate()
        }
      });
    } else if (kpi === 'winrate') {
      this.router.navigate(['/app/matters'], {
        queryParams: {
          status: 'Disposed',
          practiceArea: this.practiceArea(),
          court: this.court(),
          advocate: this.advocate()
        }
      });
    } else if (kpi === 'revenue') {
      this.router.navigate(['/app/billing'], {
        queryParams: {
          status: 'Paid',
          fy: this.fy()
        }
      });
    } else if (kpi === 'clients') {
      this.router.navigate(['/app/clients'], {
        queryParams: {
          status: 'Active'
        }
      });
    }
  }

  // Bar chart drilldown
  drillToHearings(label: string) {
    // Map Month abbreviation to numbers or search keywords
    this.router.navigate(['/app/cause-list'], {
      queryParams: {
        filter: label,
        fy: this.fy()
      }
    });
    this.toast.info(`Drilling down to hearings in ${label}`);
  }

  // Dynamic exports respecting current filter state
  exportReport(type: string, format: string) {
    const filters = `FY: ${this.fy()} | Area: ${this.practiceArea() || 'All'} | Court: ${this.court() || 'All'} | Advocate: ${this.advocate() || 'All'}`;
    this.toast.info(`Generating ${format.toUpperCase()} for "${type}"...`);
    
    this.ds.exportReportCustom(type, format, filters).subscribe({
      next: (blob) => {
        this.toast.success(`${type} generated successfully!`);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        let downloadExt = format;
        if (format === 'xlsx') {
          downloadExt = 'csv';
        }
        a.download = `${type.toLowerCase().replace(/[^a-z0-9]/g, '_')}.${downloadExt}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.toast.error('Failed to generate report export.');
      }
    });
  }

  // Scheduling Popups
  openSchedule(type: string) {
    this.selectedReportType.set(type);
    this.scheduleEmail.set('');
    this.scheduleFreq.set('Monthly');
    this.showScheduleModal.set(true);
  }

  saveSchedule() {
    if (!this.scheduleEmail() || !this.scheduleEmail().includes('@')) {
      this.toast.error('Please enter a valid email address.');
      return;
    }

    const filters = `FY: ${this.fy()} | Area: ${this.practiceArea() || 'All'} | Court: ${this.court() || 'All'} | Advocate: ${this.advocate() || 'All'}`;
    this.ds.scheduleReport(
      this.selectedReportType(),
      filters,
      this.scheduleFreq(),
      this.scheduleEmail()
    ).subscribe({
      next: () => {
        this.toast.success(`Recurring delivery scheduled for ${this.scheduleEmail()}!`);
        this.showScheduleModal.set(false);
      },
      error: () => {
        this.toast.error('Failed to configure report schedule.');
      }
    });
  }

  // Format Helper
  fmtRevenue(val: number): string {
    if (val >= 100000) {
      return '₹' + (val / 100000).toFixed(1) + 'L';
    } else if (val >= 1000) {
      return '₹' + (val / 1000).toFixed(0) + 'K';
    }
    return '₹' + val.toLocaleString('en-IN');
  }
}
