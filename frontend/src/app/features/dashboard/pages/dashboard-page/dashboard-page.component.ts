import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { CommonModule, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Task } from '../../../../core/models';
import { SkeletonComponent } from '../../../../shared/components/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { CountUpDirective } from '../../../../shared/directives/count-up.directive';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, DatePipe, DecimalPipe, RouterLink, SkeletonComponent, EmptyStateComponent, CountUpDirective],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  public ds = inject(DataService);
  private auth = inject(AuthService);
  private router = inject(Router);

  userName = this.auth.userName;

  stats       = this.ds.dashStats;
  matters     = this.ds.matters;
  hearings    = this.ds.hearings;
  tasks       = this.ds.tasks;

  mattersLoading  = this.ds.mattersLoading;
  hearingsLoading = this.ds.hearingsLoading;

  timeline    = signal<any[]>([]);
  teamPerf    = signal<any[]>([]);
  revenue     = signal<any>(null);
  courtDist   = signal<any[]>([]);

  // Widget Configuration / Reordering / Hiding
  activeWidgets = signal<string[]>(['alerts', 'actions', 'kpis', 'causelist', 'timeline', 'tasks', 'courtDist', 'teamPerf', 'revenue']);
  showConfigPanel = signal(false);

  // Role gating computed flags
  isAdmin = computed(() => this.auth.isRole('Admin', 'Firm Owner'));
  canCreateMatter = computed(() => !this.auth.isRole('Clerk', 'Paralegal'));
  canInvoice = computed(() => !this.auth.isRole('Clerk', 'Paralegal'));

  readonly today = new Date();

  ngOnInit() {
    this.ds.loadDashboardStats().subscribe();
    this.ds.loadHearings().subscribe();
    this.ds.loadMatters().subscribe();
    this.ds.loadTasks().subscribe();
    this.ds.getTimeline().subscribe(t  => this.timeline.set(t));
    this.ds.getTeamPerformance().subscribe(t => this.teamPerf.set(t));
    this.ds.getRevenue().subscribe(r => this.revenue.set(r));
    this.ds.getCourtDistribution().subscribe(c => this.courtDist.set(c));
  }

  get urgentHearings() { return this.hearings().filter(h => h.status === 'Urgent'); }
  get todayHearings()  { return this.hearings(); }
  get openTasks()      { return this.tasks().filter(t => !t.done); }

  barWidth(pct: number): string { return `${Math.min(pct, 100)}%`; }

  formatCurrency(val: number): string {
    if (val >= 100000) return '₹' + (val / 100000).toFixed(1) + 'L';
    if (val >= 1000)   return '₹' + (val / 1000).toFixed(0)  + 'K';
    return '₹' + val;
  }

  // Toggle widget visibility
  toggleWidget(w: string) {
    const current = this.activeWidgets();
    if (current.includes(w)) {
      this.activeWidgets.set(current.filter(item => item !== w));
    } else {
      this.activeWidgets.set([...current, w]);
    }
  }

  isWidgetActive(w: string): boolean {
    return this.activeWidgets().includes(w);
  }

  // Inline dynamic drilldowns matching spec
  drillToMatters(status?: string, court?: string) {
    const queryParams: any = {};
    if (status) queryParams.status = status;
    if (court) queryParams.court = court;
    this.router.navigate(['/app/matters'], { queryParams });
  }

  drillToHearings(type?: string) {
    const queryParams: any = {};
    if (type) queryParams.type = type;
    this.router.navigate(['/app/cause-list'], { queryParams });
  }

  drillToTasks(status?: string) {
    const queryParams: any = {};
    if (status) queryParams.status = status;
    this.router.navigate(['/app/tasks'], { queryParams });
  }

  drillToFilings(status?: string) {
    const queryParams: any = {};
    if (status) queryParams.status = status;
    this.router.navigate(['/app/filings'], { queryParams });
  }

  drillToBilling(status?: string) {
    const queryParams: any = {};
    if (status) queryParams.status = status;
    this.router.navigate(['/app/billing'], { queryParams });
  }

  // Inline optimistic task check update
  toggleTask(t: Task) {
    t.done = !t.done;
    this.ds.toggleTaskDone(String(t.id)).subscribe({
      error: () => {
        t.done = !t.done; // Revert locally if request fails
      }
    });
  }

  // ── Redirect Quick Actions to native component forms ──────────────────

  openAddMatter() {
    this.router.navigate(['/app/matters'], { queryParams: { new: '1' } });
  }

  openAddClient() {
    this.router.navigate(['/app/clients'], { queryParams: { new: '1' } });
  }

  openAddTask() {
    this.router.navigate(['/app/tasks'], { queryParams: { new: '1' } });
  }

  openNewFiling() {
    this.router.navigate(['/app/filings'], { queryParams: { new: '1' } });
  }

  openInvoice() {
    this.router.navigate(['/app/billing'], { queryParams: { new: '1' } });
  }

  openUpload() {
    this.router.navigate(['/app/documents'], { queryParams: { new: '1' } });
  }

  openTracker() {
    this.router.navigate(['/app/tracker']);
  }

  openAiAssistant() {
    this.router.navigate(['/app/ai-assistant']);
  }
}
