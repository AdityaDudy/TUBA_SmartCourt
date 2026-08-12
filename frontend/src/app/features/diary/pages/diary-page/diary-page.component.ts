import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DiaryScopeOptions, DiaryScopeMember, DiaryEventDto } from '../../../../core/api/api-response.types';
import { Hearing, Task, Filing } from '../../../../core/models';
import { DiaryEventCardComponent } from '../../components/diary-event-card/diary-event-card.component';

import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-diary-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DiaryEventCardComponent],
  templateUrl: './diary-page.component.html',
  styleUrl: './diary-page.component.scss'
})
export class DiaryPageComponent implements OnInit {
  ds   = inject(DataService);
  auth = inject(AuthService);

  events           = signal<DiaryEventDto[]>([]);
  hearingsList     = signal<Hearing[]>([]);
  year             = signal(new Date().getFullYear());
  month            = signal(new Date().getMonth() + 1);
  viewMode         = signal<'calendar' | 'list'>('calendar');
  showForm         = signal(false);
  form             = signal<Partial<DiaryEventDto>>({ type: 'hearing', urgent: false });

  // Scope state signals
  scope            = signal<'own' | 'team' | 'org'>('own');
  selectedMemberId = signal<number | null>(null);
  scopeOptions     = signal<DiaryScopeOptions | null>(null);

  // Edit state
  showEditForm  = signal(false);
  editingEvent  = signal<DiaryEventDto | null>(null);
  editForm      = signal<Partial<DiaryEventDto>>({});

  // Selected day detail & Event detail modal
  selectedDay = signal<string | null>(null);
  selectedDetailEvent = signal<any | null>(null);

  openDetail(event: any) {
    this.selectedDetailEvent.set(event);
  }

  closeDetail() {
    this.selectedDetailEvent.set(null);
  }

  readonly MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  readonly DAYS   = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  readonly TYPES: Array<'hearing' | 'meeting' | 'task' | 'deadline'> = ['hearing', 'meeting', 'task', 'deadline'];

  get monthLabel() { return `${this.MONTHS[this.month()-1]} ${this.year()}`; }

    // Dynamic integrated timeline signal
  allDiaryEvents = computed(() => {
    const localEvents = this.events();
    const tasks = this.ds.tasks();
    const filings = this.ds.filings();
    const hearings = this.hearingsList();

    const currentScope = this.scope();
    const memberIdFilter = this.selectedMemberId();
    const currentUser = this.auth.currentUser();
    const options = this.scopeOptions();

    // Build set of allowed owner names for task/filing/hearing scoping
    let allowedOwnerNames: Set<string> | null = null;
    if (memberIdFilter != null && options) {
      const allMembers = [...(options.teamMembers || []), ...(options.orgMembers || [])];
      const target = allMembers.find(m => m.id === memberIdFilter);
      if (target) {
        allowedOwnerNames = new Set([target.name.toLowerCase()]);
      }
    } else if (currentScope === 'own' && currentUser) {
      allowedOwnerNames = new Set([currentUser.name.toLowerCase()]);
    } else if (currentScope === 'team' && options && options.teamMembers) {
      allowedOwnerNames = new Set(options.teamMembers.map(m => m.name.toLowerCase()));
    } // 'org' scope allows all

    const isAllowedOwner = (ownerName?: string) => {
      if (!allowedOwnerNames) return true; // org view allows all
      if (!ownerName) return false;
      const lower = ownerName.toLowerCase();
      for (const name of allowedOwnerNames) {
        if (lower.includes(name) || name.includes(lower)) return true;
      }
      return false;
    };

    const list: any[] = [];

    const mattersMap = new Map(this.ds.matters().map(m => [String(m.id), m]));

    // 1. Add local events (already scoped server-side)
    for (const e of localEvents) {
      const eTitleSearch = (e.title || e.matterTitle || '').toLowerCase();
      const linkedMatter = e.matterId
        ? mattersMap.get(String(e.matterId))
        : (eTitleSearch ? this.ds.matters().find(m => m.title && eTitleSearch.includes(m.title.toLowerCase())) : null);

      list.push({
        id: e.id,
        title: e.title,
        date: e.date || (e as any).eventDate,
        time: e.time || (e as any).eventTime || '09:00',
        type: e.type || 'meeting',
        urgent: e.urgent || false,
        court: (linkedMatter && linkedMatter.court) ? linkedMatter.court : (e.court || ''),
        matterId: e.matterId || (linkedMatter ? linkedMatter.id : undefined),
        matterTitle: e.matterTitle || (linkedMatter ? linkedMatter.title : ''),
        clientId: e.clientId || (linkedMatter ? linkedMatter.clientId : undefined),
        clientName: e.clientName || (linkedMatter ? linkedMatter.clientName : ''),
        ownerId: e.ownerId,
        ownerName: e.ownerName || '',
        notes: e.notes || '',
        isLocal: true,
        original: e
      });
    }

    // 2. Add Tasks (scoped client-side to active scope selection)
    for (const t of tasks) {
      if (!t.done && isAllowedOwner(t.assignedTo)) {
        const dateStr = t.dueDate || t.due;
        if (dateStr) {
          const mId = (t as any).matterId;
          const linkedMatter = mId ? mattersMap.get(String(mId)) : null;
          list.push({
            id: 'task_' + t.id,
            title: `Task: ${t.title}`,
            date: dateStr,
            time: '09:00',
            type: 'task',
            urgent: t.priority === 'Urgent' || t.priority === 'High',
            court: (t as any).court || (linkedMatter ? linkedMatter.court : ''),
            matterId: mId,
            matterTitle: t.matterTitle || t.matter || (linkedMatter ? linkedMatter.title : ''),
            clientId: (t as any).clientId || (linkedMatter ? linkedMatter.clientId : undefined),
            clientName: (t as any).clientName || (linkedMatter ? linkedMatter.clientName : ''),
            ownerName: t.assignedTo || '',
            notes: t.notes || '',
            isLocal: false,
            original: t
          });
        }
      }
    }

    // 3. Add Filings (scoped client-side to active scope selection)
    for (const f of filings) {
      if (!isAllowedOwner(f.advocate)) continue;

      const mId = (f as any).matterId;
      const linkedMatter = mId ? mattersMap.get(String(mId)) : null;
      const courtName = f.court || (linkedMatter ? linkedMatter.court : '');

      const dateStr = f.dueDate || f.due;
      if (dateStr) {
        list.push({
          id: 'filing_' + f.id,
          title: `Filing Due: ${f.title}`,
          date: dateStr,
          time: '17:00',
          type: 'deadline',
          urgent: f.status !== 'Filed' && new Date(dateStr) <= new Date(),
          court: courtName,
          matterId: mId,
          matterTitle: f.matterTitle || f.matter || (linkedMatter ? linkedMatter.title : ''),
          clientId: (f as any).clientId || (linkedMatter ? linkedMatter.clientId : undefined),
          clientName: (f as any).clientName || (linkedMatter ? linkedMatter.clientName : ''),
          ownerName: f.advocate || '',
          notes: f.notes || f.description || '',
          isLocal: false,
          original: f
        });
      }
      if (f.filedDate) {
        list.push({
          id: 'filed_' + f.id,
          title: `Filed: ${f.title}`,
          date: f.filedDate,
          time: '12:00',
          type: 'deadline',
          urgent: false,
          court: courtName,
          matterId: mId,
          matterTitle: f.matterTitle || f.matter || (linkedMatter ? linkedMatter.title : ''),
          clientId: (f as any).clientId || (linkedMatter ? linkedMatter.clientId : undefined),
          clientName: (f as any).clientName || (linkedMatter ? linkedMatter.clientName : ''),
          ownerName: f.advocate || '',
          notes: f.notes || f.description || '',
          isLocal: false,
          original: f
        });
      }
    }

    // 4. Add Hearings (scoped client-side to active scope selection)
    for (const h of hearings) {
      if (!isAllowedOwner(h.advocate)) continue;

      const mId = (h as any).matterId;
      const titleSearch = (h.title || h.caseTitle || '').toLowerCase();
      const linkedMatter = mId
        ? mattersMap.get(String(mId))
        : (titleSearch ? this.ds.matters().find(m => m.title && titleSearch.includes(m.title.toLowerCase())) : null);

      const dateStr = h.hearingDate;
      if (dateStr) {
        list.push({
          id: 'hearing_' + h.id,
          title: `Court Hearing: ${h.title || h.caseTitle || h.caseNo}`,
          date: dateStr,
          time: h.hearingTime || h.time || '10:00',
          type: 'hearing',
          urgent: h.status === 'Urgent',
          court: (linkedMatter && linkedMatter.court) ? linkedMatter.court : (h.court || ''),
          matterId: mId || (linkedMatter ? linkedMatter.id : undefined),
          matterTitle: h.caseTitle || (linkedMatter ? linkedMatter.title : ''),
          clientId: (h as any).clientId || (linkedMatter ? linkedMatter.clientId : undefined),
          clientName: (h as any).clientName || (linkedMatter ? linkedMatter.clientName : ''),
          ownerName: h.advocate || '',
          notes: `Bench: ${h.bench || '—'} · Stage: ${h.stage || '—'}`,
          isLocal: false,
          original: h
        });
      }
    }

    return list;
  });

  // Filtered month timeline signal
  thisMonthEvents = computed(() => {
    const prefix = `${this.year()}-${String(this.month()).padStart(2, '0')}`;
    return this.allDiaryEvents().filter(e => e.date && e.date.startsWith(prefix))
      .sort((a, b) => {
        if (a.date !== b.date) return a.date < b.date ? -1 : 1;
        return a.time < b.time ? -1 : 1;
      });
  });

  // Date-grouped events for List View with next upcoming event on top
  groupedMonthEvents = computed(() => {
    const events = [...this.thisMonthEvents()];
    const today = this.todayStr;

    // Partition into upcoming (date >= today) and past (date < today)
    const upcoming = events.filter(e => e.date >= today).sort((a, b) => {
      if (a.date !== b.date) return a.date < b.date ? -1 : 1;
      return (a.time || '') < (b.time || '') ? -1 : 1;
    });

    const past = events.filter(e => e.date < today).sort((a, b) => {
      if (a.date !== b.date) return a.date > b.date ? -1 : 1; // most recent past first
      return (a.time || '') < (b.time || '') ? -1 : 1;
    });

    const sortedList = [...upcoming, ...past];

    // Group sorted items by date preserving section ordering
    const groups: { date: string; formattedDate: string; isToday: boolean; isPast: boolean; events: any[] }[] = [];
    const groupMap = new Map<string, any[]>();

    for (const e of sortedList) {
      if (!groupMap.has(e.date)) {
        groupMap.set(e.date, []);
      }
      groupMap.get(e.date)!.push(e);
    }

    groupMap.forEach((evts, dateStr) => {
      let formattedDate = dateStr;
      try {
        const parts = dateStr.split('-');
        if (parts.length === 3) {
          const d = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
          formattedDate = d.toLocaleDateString('en-US', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        }
      } catch (err) {}

      groups.push({
        date: dateStr,
        formattedDate,
        isToday: dateStr === today,
        isPast: dateStr < today,
        events: evts
      });
    });

    return groups;
  });

  ngOnInit() {
    this.ds.getDiaryScopeOptions().subscribe(opts => this.scopeOptions.set(opts));
    this.loadEvents();
  }

  loadEvents() {
    this.ds.loadMatters().subscribe();
    this.ds.getDiaryEvents(this.year(), this.month(), this.scope(), this.selectedMemberId())
      .subscribe(e => this.events.set(e));
    this.ds.loadTasks().subscribe();
    this.ds.loadFilings().subscribe();
    this.ds.filterHearings('All').subscribe(h => this.hearingsList.set(h));
  }

  setScope(s: 'own' | 'team' | 'org') {
    this.scope.set(s);
    this.selectedMemberId.set(null);
    this.loadEvents();
  }

  selectMember(memberId: number | null) {
    this.selectedMemberId.set(memberId);
    this.loadEvents();
  }

  prev() {
    if (this.month() === 1) { this.month.set(12); this.year.update(y => y-1); }
    else { this.month.update(m => m-1); }
    this.loadEvents();
  }

  next() {
    if (this.month() === 12) { this.month.set(1); this.year.update(y => y+1); }
    else { this.month.update(m => m+1); }
    this.loadEvents();
  }

  // ── Calendar grid ──────────────────────────────────────────────
  calendarDays = computed(() => {
    const y = this.year();
    const m = this.month();
    const firstDay = new Date(y, m - 1, 1).getDay();
    const daysInMonth = new Date(y, m, 0).getDate();
    const cells: (number | null)[] = [];
    for (let i = 0; i < firstDay; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) cells.push(d);
    // pad to multiple of 7
    while (cells.length % 7 !== 0) cells.push(null);
    return cells;
  });

  calendarWeeks = computed(() => {
    const days = this.calendarDays();
    const weeks = [];
    for (let i = 0; i < days.length; i += 7) {
      weeks.push(days.slice(i, i + 7));
    }
    return weeks;
  });

  todayStr = new Date().toISOString().split('T')[0];

  getDateStr(day: number | null): string {
    if (!day) return '';
    const m = String(this.month()).padStart(2, '0');
    const d = String(day).padStart(2, '0');
    return `${this.year()}-${m}-${d}`;
  }

  isToday(day: number | null): boolean {
    return !!day && this.getDateStr(day) === this.todayStr;
  }

  eventsOnDay(day: number | null): any[] {
    if (!day) return [];
    const ds = this.getDateStr(day);
    return this.allDiaryEvents().filter(e => e.date === ds);
  }

  // Conflict detection
  conflicts = computed(() => {
    const seen: Record<string, string[]> = {};
    for (const e of this.allDiaryEvents()) {
      const key = `${e.date}_${e.time}`;
      if (!seen[key]) seen[key] = [];
      seen[key].push(e.id);
    }
    const conflictIds = new Set<string>();
    for (const ids of Object.values(seen)) {
      if (ids.length > 1) ids.forEach(id => conflictIds.add(id));
    }
    return conflictIds;
  });

  isConflict(e: any): boolean {
    return this.conflicts().has(e.id);
  }

  // Today's events panel
  todayEvents = computed(() =>
    this.allDiaryEvents().filter(e => e.date === this.todayStr)
      .sort((a, b) => ((a.time || '') < (b.time || '') ? -1 : 1))
  );

  // Selected day events
  selectedDayEvents = computed(() => {
    const sel = this.selectedDay();
    if (!sel) return [];
    return this.allDiaryEvents().filter(e => e.date === sel)
      .sort((a, b) => ((a.time || '') < (b.time || '') ? -1 : 1));
  });

  selectDay(day: number | null) {
    if (!day) return;
    const ds = this.getDateStr(day);
    this.selectedDay.set(ds === this.selectedDay() ? null : ds);
  }

  // Week view
  currentWeekStart = computed(() => {
    const today = new Date();
    const dow = today.getDay();
    const start = new Date(today);
    start.setDate(today.getDate() - dow);
    return start;
  });

  weekDays = computed(() => {
    const start = this.currentWeekStart();
    const days = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      days.push({
        label: this.DAYS[i],
        dateStr: d.toISOString().split('T')[0],
        dayNum: d.getDate(),
        isToday: d.toISOString().split('T')[0] === this.todayStr,
      });
    }
    return days;
  });

  weekHours = [8,9,10,11,12,13,14,15,16,17,18,19];

  eventsAtSlot(dateStr: string, hour: number): any[] {
    return this.allDiaryEvents().filter(e => {
      const eTime = e.time || '';
      const eHour = parseInt(eTime.split(':')[0], 10);
      return e.date === dateStr && eHour === hour;
    });
  }

  // Type display helpers
  typeIcon(type?: string): string {
    const map: Record<string, string> = {
      hearing: 'fas fa-gavel',
      meeting: 'fas fa-phone',
      task: 'fas fa-pencil',
      deadline: 'fas fa-bell',
    };
    return map[type || ''] || 'fas fa-circle';
  }

  typeLabel(type?: string): string {
    const map: Record<string, string> = {
      hearing: 'Hearing',
      meeting: 'Meeting',
      task: 'Drafting Task',
      deadline: 'Deadline',
    };
    return map[type || ''] || type || '';
  }

  typeDotClass(type?: string): string {
    return 'dot-' + (type || 'hearing');
  }

  typeBadgeClass(type?: string): string {
    const map: Record<string, string> = {
      hearing: 'b-g',
      meeting: 'b-o',
      task: 'b-a',
      deadline: 'b-r',
    };
    return map[type || ''] || 'b-t';
  }

  // Linked matter chip
  getMatterChip(e: any): string | null {
    return e.matterTitle || null;
  }

  onMatterSelect(event: Event, isEdit = false) {
    const target = event.target as HTMLSelectElement;
    const val = target.value;
    const matterId = val ? Number(val) : null;
    const matter = matterId ? this.ds.matters().find(m => String(m.id) === String(matterId)) : null;
    const matterTitle = matter ? matter.title : (val && target.selectedIndex >= 0 ? target.options[target.selectedIndex].text : '');

    if (isEdit) {
      this.editForm.update(f => ({
        ...f,
        matterId,
        matterTitle,
        clientId: matter ? matter.clientId : f.clientId,
        clientName: matter ? matter.clientName : f.clientName,
        court: (matter && matter.court) ? matter.court : (f.court || '')
      }));
    } else {
      this.form.update(f => ({
        ...f,
        matterId,
        matterTitle,
        clientId: matter ? matter.clientId : f.clientId,
        clientName: matter ? matter.clientName : f.clientName,
        court: (matter && matter.court) ? matter.court : (f.court || '')
      }));
    }
  }

  // CRUD
  save() {
    if (!this.form().title) return;
    const f = this.form();
    const payload = {
      ...f,
      eventDate: f.date || f.eventDate || new Date().toISOString().split('T')[0],
      eventTime: f.time || f.eventTime || '09:00',
      matterId: f.matterId ? Number(f.matterId) : null
    };
    this.ds.createDiaryEvent(payload).subscribe(() => {
      this.showForm.set(false);
      this.form.set({ type: 'hearing', urgent: false });
      this.loadEvents();
    });
  }

  openEdit(e: any, stopEvent?: MouseEvent) {
    stopEvent?.stopPropagation();
    this.editingEvent.set(e);
    this.editForm.set({ ...e });
    this.showEditForm.set(true);
  }

  saveEdit() {
    const event = this.editingEvent();
    if (!event) return;
    const f = this.editForm();
    const payload = {
      ...f,
      eventDate: f.date || f.eventDate,
      eventTime: f.time || f.eventTime,
      matterId: f.matterId ? Number(f.matterId) : null
    };
    this.ds.updateDiaryEvent(event.id, payload).subscribe(() => {
      this.showEditForm.set(false);
      this.editingEvent.set(null);
      this.loadEvents();
    });
  }

  deleteEvent(e: any, stopEvent?: MouseEvent) {
    stopEvent?.stopPropagation();
    if (confirm(`Delete "${e.title}"?`)) {
      this.ds.deleteDiaryEvent(e.id).subscribe(() => this.loadEvents());
    }
  }

  openAddForDay(day: number | null) {
    const ds = day ? this.getDateStr(day) : (this.selectedDay() || this.todayStr);
    this.form.set({ type: 'hearing', urgent: false, date: ds } as any);
    this.showForm.set(true);
  }
}
