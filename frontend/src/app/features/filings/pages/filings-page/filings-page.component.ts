import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../../environments/environment';
import type { Filing } from '../../../../core/models';

@Component({
  selector: 'app-filings-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './filings-page.component.html',
  styleUrl: './filings-page.component.scss'
})
export class FilingsPageComponent implements OnInit {
  ds = inject(DataService);
  auth = inject(AuthService);
  toast = inject(ToastService);
  sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);

  filings = this.ds.filings;
  loading = this.ds.filingsLoading;
  search = signal('');
  filter = signal('All');
  taskSubmissionsFilter = signal<'all' | 'filings' | 'tasks'>('all');
  viewMode = signal<'table' | 'kanban'>('table');

  // Create modal
  showForm = signal(false);
  form = signal<Partial<Filing>>({ status: 'Draft', stage: 'Draft' });

  // Edit / drawer
  showDrawer = signal(false);
  drawerFiling = signal<Filing | null>(null);
  showEditModal = signal(false);
  editForm = signal<Partial<Filing>>({});

  // Defect modal
  showDefectModal = signal(false);
  defectFiling = signal<Filing | null>(null);
  defectForm = signal({ description: '', resubmissionDeadline: '' });

  // Preview Document signals
  selectedDocForPreview = signal<any | null>(null);
  showPreviewModal = signal(false);
  drawerDocument = signal<any | null>(null);

  getPreviewDetails = computed(() => {
    const doc = this.selectedDocForPreview();
    if (!doc) return null;

    let rawUrl: string | null = null;
    if (doc.s3Url) {
      const apiBase = environment.apiBaseUrl.replace('/api', '');
      if (doc.s3Url.startsWith('/uploads/')) {
        rawUrl = `${apiBase}${doc.s3Url}`;
      } else if (doc.s3Url.startsWith('http')) {
        rawUrl = doc.s3Url.replace(/http:\/\/localhost:\d+/, apiBase);
      }
    }

    if (!rawUrl) {
      // Strip "Task Submission: " prefix — the blob store key is the raw filename,
      // not the full filing title, so we must clean it before lookup.
      const cleanName = (doc.name || '').replace(/^Task Submission:\s*/i, '').trim();
      const localBlobUrl = this.ds.getLocalUrl(cleanName) || this.ds.getLocalUrl(doc.name);
      if (localBlobUrl) {
        rawUrl = localBlobUrl;
      }
    }

    const localUrl = rawUrl ? this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl) : null;

    let clientName = doc.clientName || 'No Client';
    let matterTitle = 'Unlinked Matter';

    if (doc.matterId) {
      const matter = this.ds.matters().find(m => String(m.id) === String(doc.matterId));
      if (matter) {
        matterTitle = matter.title;
        clientName = matter.clientName || clientName;
      }
    }

    const urlToParse = doc.s3Url || doc.name || '';
    const ext = urlToParse.split('.').pop()?.toLowerCase() || '';
    const isImage = ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext);
    const isPdf = ext === 'pdf';
    const isTemplateInstance = !rawUrl && !isImage && !isPdf;

    return {
      ...doc,
      localUrl,
      rawUrl,
      clientName,
      matterTitle,
      isImage,
      isPdf,
      isTemplateInstance
    };
  });

  // Pagination
  currentPage = signal(1);
  readonly pageSize = 10;

  // Dynamic lists
  docTypes = computed(() => this.ds.masters()?.docTypes || []);
  courtsList = computed(() => this.ds.masters()?.courts || []);
  filingStagesList = computed(() => this.ds.masters()?.filingStages || ['Draft', 'Under Review', 'Approved', 'Signed', 'Filed', 'Defects Raised', 'Defects Cleared', 'Returned']);
  mattersList = this.ds.matters;
  selectedFilingFile = signal<File | null>(null);

  onFileSelected(event: any) {
    const file = event.target.files?.[0];
    if (file) {
      this.selectedFilingFile.set(file);
    }
  }

  filters = computed(() => {
    const stages = this.ds.masters()?.filingStages || ['Draft', 'Under Review', 'Approved', 'Signed', 'Filed', 'Defects Raised', 'Defects Cleared', 'Returned'];
    return ['All', ...stages];
  });

  STAGE_ORDER = computed(() => {
    const all = this.ds.masters()?.filingStages || ['Draft', 'Under Review', 'Approved', 'Signed', 'Filed', 'Defects Raised', 'Defects Cleared', 'Returned'];
    return all.filter(s => {
      const lower = s.toLowerCase();
      return !lower.includes('defect') && lower !== 'returned';
    });
  });

  ngOnInit() {
    this.ds.loadFilings().subscribe();
    this.ds.loadMasters().subscribe();
    this.ds.loadMatters().subscribe();

    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openNewFilingModal();
      }
    });
  }

  /** Newest filings first — falls back to id if createdAt isn't present. */
  private sortByLatest(list: Filing[]): Filing[] {
    return [...list].sort((a: any, b: any) => {
      const aTime = a.createdAt ? new Date(a.createdAt).getTime() : Number(a.id) || 0;
      const bTime = b.createdAt ? new Date(b.createdAt).getTime() : Number(b.id) || 0;
      return bTime - aTime;
    });
  }

  /** Display label for a filing's title — strips redundant prefixes for clean linear column alignment. */
  displayTitle(f: Filing): string {
    if (f.title?.startsWith('Task Submission:')) {
      const fileName = f.title.replace(/^Task Submission:\s*/i, '').trim();
      if (f.matterTitle && fileName.startsWith(f.matterTitle + ' : ')) {
        return fileName.replace(f.matterTitle + ' : ', '');
      }
      return fileName;
    }
    return f.title;
  }

  get filtered() {
    const taskFilter = this.taskSubmissionsFilter();
    const list = this.sortByLatest(this.filings())
      .filter(f => {
        if (taskFilter === 'tasks') return this.isTaskSubmission(f);
        if (taskFilter === 'filings') return !this.isTaskSubmission(f);
        return true;
      })
      .filter(f => {
        const activeFilter = this.filter();
        if (activeFilter === 'All') return true;
        const target = activeFilter.toLowerCase();
        const stageLower = f.stage?.toLowerCase() || '';
        const statusLower = f.status?.toLowerCase() || '';

        if (stageLower === target || statusLower === target) return true;
        if (target.includes('defect') || target === 'defects') {
          return stageLower.includes('defect') || statusLower.includes('defect');
        }
        return false;
      })
      .filter(f => !this.search() || f.title?.toLowerCase().includes(this.search().toLowerCase()));
    return list;
  }

  get totalPages() { return Math.ceil(this.filtered.length / this.pageSize) || 1; }

  get paginatedFilings() {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  // Grouping for table view
  get groupedFilings(): { matter: string; filings: Filing[] }[] {
    const groups: Record<string, Filing[]> = {};
    for (const f of this.paginatedFilings) {
      const key = f.matterTitle || 'Unlinked Filings';
      if (!groups[key]) groups[key] = [];
      groups[key].push(f);
    }
    return Object.entries(groups).map(([matter, filings]) => ({ matter, filings }));
  }

  // Kanban columns
  kanbanStages = computed(() => {
    const all = this.ds.masters()?.filingStages || ['Draft', 'Under Review', 'Approved', 'Signed', 'Filed', 'Defects Raised', 'Defects Cleared', 'Returned'];
    return all.filter(s => s !== 'Returned' && s !== 'Defects Cleared');
  });

  getKanbanFilings(stage: string) {
    return this.filtered.filter(f => {
      if (stage === 'Defects Raised') return f.stage?.toLowerCase().includes('defect');
      return f.stage?.toLowerCase() === stage.toLowerCase();
    });
  }

  // Urgency helpers
  today = new Date().toISOString().split('T')[0];

  isOverdue(dueDate?: string): boolean {
    if (!dueDate) return false;
    const stage = '';
    return dueDate < this.today;
  }

  isOverdueFiling(f: Filing): boolean {
    if (!f.dueDate || f.stage === 'Filed' || f.stage === 'Defects Cleared') return false;
    return f.dueDate < this.today;
  }

  isUrgent(dueDate?: string): boolean {
    if (!dueDate) return false;
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    return dueDate >= this.today && dueDate <= tomorrowStr;
  }

  dueDateClass(f: Filing): string {
    if (f.stage === 'Filed' || f.stage === 'Defects Cleared') return 'due-green';
    if (this.isOverdueFiling(f)) return 'due-red';
    if (this.isUrgent(f.dueDate)) return 'due-amber';
    return 'due-gray';
  }

  dueDateLabel(f: Filing): string {
    if (this.isOverdueFiling(f)) return 'Overdue';
    if (this.isUrgent(f.dueDate)) return 'Due Soon';
    return '';
  }

  // Stage stepper
  getStepperIndex(stage?: string): number {
    if (!stage) return 0;
    const idx = this.STAGE_ORDER().findIndex(s => s.toLowerCase() === stage.toLowerCase());
    return idx >= 0 ? idx : 0;
  }

  isTaskSubmission(f: Filing): boolean {
    return !!f.title?.startsWith('Task Submission:');
  }

  getStatusBadgeClass(status?: string): string {
    if (!status) return 'b-t';
    const s = status.toLowerCase();
    if (s === 'filed' || s === 'defects cleared') return 'b-g';
    if (s === 'draft') return 'b-t';
    if (s.includes('defect') || s === 'returned') return 'b-r';
    if (s === 'approved' || s === 'signed') return 'b-g';
    if (s === 'under review') return 'b-a';
    return 'b-o';
  }

  getKanbanCardClass(stage: string): string {
    const map: Record<string, string> = {
      'Draft': 'kc-draft',
      'Under Review': 'kc-review',
      'Approved': 'kc-approved',
      'Signed': 'kc-signed',
      'Filed': 'kc-filed',
      'Defects Raised': 'kc-defect',
    };
    return map[stage] || '';
  }

  // CRUD operations
  onMatterChange(event: Event, mode: 'create' | 'edit') {
    const id = (event.target as HTMLSelectElement).value;
    const m = this.ds.matters().find(x => String(x.id) === id);
    if (m) {
      if (mode === 'create') {
        this.form.update(f => ({ ...f, matterId: Number(m.id), matterTitle: m.title }));
      } else {
        this.editForm.update(f => ({ ...f, matterId: Number(m.id), matterTitle: m.title }));
      }
    }
  }

  openNewFilingModal() {
    const firstMatter = this.ds.matters()[0];
    const firstDocType = this.docTypes()[0] || '';
    const firstCourt = this.courtsList()[0] || '';
    this.form.set({
      title: '',
      filingType: firstDocType,
      matterId: firstMatter ? Number(firstMatter.id) : undefined,
      matterTitle: firstMatter ? firstMatter.title : '',
      court: firstCourt,
      dueDate: new Date().toISOString().split('T')[0],
      stage: 'Draft',
      status: 'Draft',
      notes: '',
      advocate: this.auth.userName() || 'Adv. Amit Sharma'
    });
    this.showForm.set(true);
  }

  save() {
    if (!this.form().title) return;
    const filingStage = this.form().stage;
    const matterId = this.form().matterId;
    const filingType = this.form().filingType;
    const file = this.selectedFilingFile();

    const createFilingWithPayload = (payload: Partial<Filing>) => {
      this.ds.createFiling(payload).subscribe(() => {
        this.showForm.set(false);
        this.selectedFilingFile.set(null);
        this.ds.loadFilings().subscribe();
      });
    };

    if (file && matterId) {
      // Only attach the file to the filing workflow — do NOT create a matter-linked
      // Document yet. It becomes visible in the matter's Document Vault only once
      // this filing progresses to the "Filed" stage (handled by the backend).
      this.ds.uploadFilingAttachment(file).subscribe({
        next: (uploaded) => {
          this.toast.success(`"${file.name}" attached to filing.`);
          const payload = {
            ...this.form(),
            s3Url: uploaded?.s3Url
          };
          createFilingWithPayload(payload);
        },
        error: () => {
          this.toast.error('File attachment upload failed. Creating filing without attachment.');
          createFilingWithPayload(this.form());
        }
      });
    } else {
      createFilingWithPayload(this.form());
    }
  }

  openDrawer(f: Filing) {
    this.drawerFiling.set(f);
    this.drawerDocument.set(null);
    this.showDrawer.set(true);

    if (f.s3Url) {
      this.drawerDocument.set({
        name: f.title,
        s3Url: f.s3Url,
        matterId: f.matterId,
        type: f.filingType || 'Filing',
        source: 'Filing (' + f.stage + ')'
      });
    } else if (f.matterId) {
      this.ds.getMatterFolderContents(String(f.matterId)).subscribe(contents => {
        const cleanTitle = (f.title || '').replace('Task Submission: ', '').trim().toLowerCase();
        const match = contents.find(d => {
          const dName = (d.name || '').toLowerCase();
          return dName === cleanTitle || cleanTitle.includes(dName) || dName.includes(cleanTitle);
        });
        if (match) {
          this.drawerDocument.set(match);
        }
      });
    }
  }

  openEdit(f: Filing) {
    this.editForm.set({ ...f });
    this.showEditModal.set(true);
    this.showDrawer.set(false);
  }

  saveEdit() {
    const form = this.editForm();
    if (!form.id) return;
    const updates = { ...form };
    if (updates.stage) updates.status = updates.stage;
    if (updates.stage !== 'Filed') updates.filedDate = undefined;

    // If moving to Defects Raised, prompt for details
    if (updates.stage === 'Defects Raised') {
      this.defectFiling.set(this.editForm() as Filing);
      this.showDefectModal.set(true);
      this.showEditModal.set(false);
      return;
    }

    this.ds.updateFiling(form.id, updates).subscribe(() => {
      this.showEditModal.set(false);
      this.ds.loadFilings().subscribe();
    });
  }

  saveDefect() {
    const filing = this.defectFiling();
    if (!filing) return;
    const updates = {
      ...this.editForm(),
      stage: 'Defects Raised',
      status: 'Defects Raised',
      notes: (this.editForm().notes || '') + `\n\n[DEFECT] ${this.defectForm().description}`,
      dueDate: this.defectForm().resubmissionDeadline || this.editForm().dueDate,
    };
    this.ds.updateFiling(filing.id, updates).subscribe(() => {
      this.showDefectModal.set(false);
      this.defectFiling.set(null);
      this.ds.loadFilings().subscribe();
    });
  }

  moveToNextStage(f: Filing, event: MouseEvent) {
    event.stopPropagation();
    const currentIdx = this.getStepperIndex(f.stage);
    if (currentIdx >= this.STAGE_ORDER().length - 1) return;
    const nextStage = this.STAGE_ORDER()[currentIdx + 1];
    const updates: Partial<Filing> = { stage: nextStage, status: nextStage };
    if (nextStage === 'Filed') updates.filedDate = this.today;
    this.ds.updateFiling(f.id, updates).subscribe((res) => {
      this.ds.loadFilings().subscribe();
      if (this.drawerFiling()?.id === f.id) {
        this.drawerFiling.set({ ...f, ...updates });
      }
    });
  }

  setFilingStage(f: Filing, stage: string) {
    const updates: Partial<Filing> = { stage: stage, status: stage };
    if (stage === 'Filed') updates.filedDate = this.today;
    else updates.filedDate = undefined;
    this.ds.updateFiling(f.id, updates).subscribe(() => {
      this.ds.loadFilings().subscribe();
      if (this.drawerFiling()?.id === f.id) {
        this.drawerFiling.set({ ...f, ...updates });
      }
    });
  }

  getNextStageName(f: Filing): string | null {
    const currentIdx = this.getStepperIndex(f.stage);
    if (currentIdx >= 0 && currentIdx < this.STAGE_ORDER().length - 1) {
      return this.STAGE_ORDER()[currentIdx + 1];
    }
    return null;
  }

  markReturned(f: Filing, event: MouseEvent) {
    event.stopPropagation();
    if (confirm(`Mark "${f.title}" as Returned from Registry? This will create a new filing pre-filled from this one.`)) {
      this.ds.updateFiling(f.id, { stage: 'Returned', status: 'Returned' }).subscribe(() => {
        this.ds.loadFilings().subscribe();
        // Pre-fill new filing form from old one
        this.form.set({
          title: f.title + ' (Re-filed)',
          filingType: f.filingType,
          matterId: f.matterId,
          matterTitle: f.matterTitle,
          court: f.court,
          dueDate: new Date().toISOString().split('T')[0],
          stage: 'Draft',
          status: 'Draft',
          notes: `Re-filing of "${f.title}" — previous copy returned by registry.`,
          advocate: f.advocate
        });
        this.showForm.set(true);
      });
    }
  }

  deleteFiling(id: string, event: MouseEvent) {
    event.stopPropagation();
    if (confirm('Delete this filing permanently?')) {
      this.ds.deleteFiling(id).subscribe(() => {
        this.showDrawer.set(false);
      });
    }
  }
}