import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../../environments/environment';
import type { Matter, Task, TaskPriority } from '../../../../core/models';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';

import { DocumentPreviewModalComponent } from '../../../../shared/components/document-preview-modal/document-preview-modal.component';
import { PaginatorComponent } from '../../../../shared/components/paginator/paginator.component';
import { AppButtonComponent } from '../../../../shared/components/button/button.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { FilterTabsComponent } from '../../../../shared/components/filter-tabs/filter-tabs.component';
import { ModalShellComponent } from '../../../../shared/components/modal-shell/modal-shell.component';

@Component({
  selector: 'app-matters-page',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DocumentPreviewModalComponent, PaginatorComponent,
    AppButtonComponent, SearchInputComponent, FilterTabsComponent
  ],
  templateUrl: './matters-page.component.html',
  styleUrl: './matters-page.component.scss',
})
export class MattersPageComponent implements OnInit, OnDestroy {
  ds = inject(DataService);
  auth = inject(AuthService);
  toast = inject(ToastService);
  sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);

  matters  = this.ds.matters;
  loading  = this.ds.mattersLoading;
  total    = this.ds.matterTotal;
  search   = signal('');
  filter   = signal('All');
  showForm = signal(false);

  // Pagination (0-based)
  page = signal(0);
  readonly size = 8;

  private _destroy$ = new Subject<void>();
  private _search$  = new Subject<string>();

  // Edit states
  showEditForm = signal(false);
  selectedMatter = signal<Matter | null>(null);
  editForm = signal<Partial<Matter>>({});
  editSubmitted = signal(false);

  // View modal state
  showViewModal = signal(false);
  activeDetailTab = signal<'summary' | 'timeline' | 'documents' | 'tasks' | 'finance'>('summary');

  // Creation form state
  form = signal<Partial<Matter>>({ status: 'Active', type: 'Litigation', priority: 'Medium' });
  submitted = signal(false);

  // Expanded fields state
  matterDocuments = signal<any[]>([]);
  showAddTaskForm = signal(false);
  taskForm = signal({
    title: '',
    description: '',
    dueDate: '',
    priority: 'Medium',
    assignedTo: ''
  });

  showUploadModal = signal(false);
  uploadingDoc = signal(false);
  uploadForm = signal({
    docType: 'Petition',
    tags: ''
  });

  selectedDocForPreview = signal<any>(null);
  showPreviewModal = signal(false);
  matterFinanceRollup = signal<any>(null);

  get isTaskFormValid(): boolean {
    return !!this.taskForm().title?.trim() && !!this.taskForm().dueDate;
  }

  // Dynamic dropdowns & filters from Masters
  types = computed(() => this.ds.masters()?.matterTypes || ['Litigation', 'Arbitration', 'Advisory', 'IBC/NCLT', 'Compliance', 'Tax Matter', 'Consumer', 'Real Estate', 'Criminal']);
  courts = computed(() => this.ds.masters()?.courts || ['Supreme Court of India', 'Delhi High Court', 'Bombay High Court', 'NCLT Mumbai', 'NCLT Delhi', 'ITAT Delhi', 'District Court']);
  practiceAreas = computed(() => this.ds.masters()?.practiceAreas || ['Corporate Law', 'Litigation', 'Arbitration', 'Insolvency & Bankruptcy', 'Taxation', 'Real Estate', 'Labour & Employment']);
  prios = computed(() => this.ds.masters()?.priorities || ['Urgent', 'High', 'Medium', 'Low']);
  
  // Sync filters tab with dynamic Priorities set in master
  filters = computed(() => ['All', ...this.prios()]);

  constructor() {
    this._search$.pipe(debounceTime(300), takeUntil(this._destroy$)).subscribe(() => {
      this.page.set(0);
      this._reload();
    });
  }

  ngOnInit() { 
    this._reload(); 
    this.ds.loadMasters().subscribe();
    this.ds.loadClients().subscribe();
    this.ds.loadUsers().subscribe();
    this.ds.loadHearings().subscribe();
    this.ds.loadFilings().subscribe();
    this.ds.loadTasks().subscribe();

    this.route.paramMap.subscribe((params: any) => {
      const matId = params.get('matId');
      if (matId) {
        this.ds.loadMatters().subscribe(page => {
          const match = (page.content || this.ds.matters()).find(m => String(m.id) === String(matId));
          if (match) {
            this.openViewModal(match);
          }
        });
      }
    });

    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openAddForm();
      }
      if (params['court']) {
        this.courtFilter.set(params['court']);
      } else {
        this.courtFilter.set(null);
      }
      this._reload();
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

  onFilterChange(f: string) {
    this.filter.set(f);
    this.page.set(0);
    this._reload();
  }

  onPageChange(p: number) {
    this.page.set(p);
    this._reload();
  }

  private _reload() {
    const f = this.filter();
    const court = this.courtFilter();
    this.ds.loadMatters({
      page:   court ? 0 : this.page(),
      size:   court ? 1000 : this.size,
      sort:   'id',
      order:  'desc',
      search: this.search() || undefined,
      filter: f !== 'All' ? { priority: f } : undefined,
    }).subscribe();
  }

  get matterHearings() {
    const matter = this.selectedMatter();
    if (!matter) return [];
    return this.ds.hearings().filter(h => String(h.matterId) === String(matter.id));
  }

  get matterFilings() {
    const matter = this.selectedMatter();
    if (!matter) return [];
    return this.ds.filings().filter(f => String(f.matterId) === String(matter.id));
  }

  get matterTasks() {
    const matter = this.selectedMatter();
    if (!matter) return [];
    return this.ds.tasks().filter(t => String(t.matterId) === String(matter.id));
  }

  get matterTimelineEvents() {
    const matter = this.selectedMatter();
    if (!matter) return [];
    
    const events: { date: string; type: string; title: string; desc: string }[] = [];
    
    // Matter registration event
    if (matter.filingDate) {
      events.push({
        date: matter.filingDate,
        type: 'Registration',
        title: 'Matter Registered',
        desc: 'Case registered with title: "' + matter.title + '" in ' + matter.court
      });
    }

    this.matterHearings.forEach(h => {
      events.push({
        date: h.hearingDate || '',
        type: 'Hearing',
        title: 'Court Hearing: ' + (h.stage || 'Hearing'),
        desc: 'Scheduled in ' + h.court + ' with ' + h.advocate + '. Time: ' + (h.hearingTime || '—')
      });
    });

    this.matterFilings.forEach(f => {
      events.push({
        date: f.filedDate || f.dueDate || '',
        type: 'Filing',
        title: 'Document Filing: ' + f.title,
        desc: 'Filing status: ' + f.status + ' (' + (f.filingType || 'Pleading') + '). Filed by ' + f.advocate
      });
    });

    // Document upload events
    this.matterDocuments().forEach(d => {
      events.push({
        date: d.date || '',
        type: 'Document',
        title: 'Document Uploaded: ' + d.name,
        desc: 'Document of type ' + d.type + ' uploaded by system.'
      });
    });

    // Task events
    this.matterTasks.forEach(t => {
      events.push({
        date: t.dueDate || '',
        type: 'Task',
        title: (t.done ? 'Task Completed: ' : 'Task Due: ') + t.title,
        desc: 'Assigned to: ' + t.assignedTo + '. Priority: ' + t.priority + '. Description: ' + (t.notes || '—')
      });
    });

    // Sort descending
    return events.sort((a, b) => b.date.localeCompare(a.date));
  }

  courtFilter = signal<string | null>(null);

  filteredMatters = computed(() => {
    let list = this.ds.matters();
    const court = this.courtFilter();
    if (court) {
      const courtLower = court.trim().toLowerCase();
      list = list.filter(m => {
        const mCourt = (m.court || '').trim().toLowerCase();
        return mCourt.includes(courtLower) || courtLower.includes(mCourt);
      });
    }
    return list;
  });

  clearCourtFilter() {
    this.courtFilter.set(null);
    this.page.set(0);
    this._reload();
  }



  // Register Form Validations
  get isTitleValid(): boolean {
    const title = this.form().title?.trim();
    return !!title && title.length >= 3 && title.length <= 200;
  }

  get isClientValid(): boolean {
    return !!this.form().clientId;
  }

  get isTypeValid(): boolean {
    return !!this.form().type;
  }

  get isCourtValid(): boolean {
    return !!this.form().court;
  }

  get isAdvocateValid(): boolean {
    return !!this.form().advocate;
  }

  get isCaseNoValid(): boolean {
    const caseNo = this.form().caseNumber || this.form().caseNo;
    if (!caseNo) return true; // Optional field
    return caseNo.trim().length <= 50;
  }

  get isCnrNoValid(): boolean {
    const cnr = this.form().cnrNumber;
    if (!cnr) return true; // Optional field
    return cnr.trim().length <= 50;
  }

  get isOppositePartyValid(): boolean {
    const op = this.form().oppositeParty;
    if (!op) return true; // Optional field
    return op.trim().length <= 100;
  }

  get isBgValid(): boolean {
    const bg = this.form().bg;
    if (!bg) return true; // Optional field
    return bg.trim().length <= 1000;
  }

  get isFormValid(): boolean {
    return this.isTitleValid && this.isClientValid && this.isTypeValid && this.isCourtValid && this.isAdvocateValid && this.isCaseNoValid && this.isCnrNoValid && this.isOppositePartyValid && this.isBgValid;
  }

  // Edit Form Validations
  get isEditTitleValid(): boolean {
    const title = this.editForm().title?.trim();
    return !!title && title.length >= 3 && title.length <= 200;
  }

  get isEditClientValid(): boolean {
    return !!this.editForm().clientId || !!this.editForm().clientName;
  }

  get isEditTypeValid(): boolean {
    return !!this.editForm().type;
  }

  get isEditCourtValid(): boolean {
    return !!this.editForm().court;
  }

  get isEditAdvocateValid(): boolean {
    return !!this.editForm().advocate || !!this.editForm().adv;
  }

  get isEditCaseNoValid(): boolean {
    const caseNo = this.editForm().caseNumber || this.editForm().caseNo;
    if (!caseNo) return true; // Optional field
    return caseNo.trim().length <= 50;
  }

  get isEditCnrNoValid(): boolean {
    const cnr = this.editForm().cnrNumber;
    if (!cnr) return true; // Optional field
    return cnr.trim().length <= 50;
  }

  get isEditOppositePartyValid(): boolean {
    const op = this.editForm().oppositeParty;
    if (!op) return true; // Optional field
    return op.trim().length <= 100;
  }

  get isEditBgValid(): boolean {
    const bg = this.editForm().bg;
    if (!bg) return true; // Optional field
    return bg.trim().length <= 1000;
  }

  get isEditFormValid(): boolean {
    return this.isEditTitleValid && this.isEditClientValid && this.isEditTypeValid && this.isEditCourtValid && this.isEditAdvocateValid && this.isEditCaseNoValid && this.isEditCnrNoValid && this.isEditOppositePartyValid && this.isEditBgValid;
  }

  loadMatterDocs(matterId: string) {
    this.ds.getMatterFolderContents(matterId).subscribe({
      next: (docs) => {
        this.matterDocuments.set(docs);
      }
    });
  }

  openAddForm() {
    const defaultType = this.types()[0] || 'Litigation';
    const defaultCourt = this.courts()[0] || 'Supreme Court of India';
    const defaultPrio = this.prios()[0] || 'Medium';
    const firstClient = this.ds.clients()[0];
    const firstUser = this.ds.users()[0];

    this.form.set({ 
      title: '',
      caseNumber: '',
      cnrNumber: '',
      clientId: firstClient ? Number(firstClient.id) : undefined,
      clientName: firstClient ? firstClient.name : '',
      type: defaultType, 
      court: defaultCourt,
      priority: defaultPrio,
      advocate: firstUser ? firstUser.name : '',
      oppositeParty: '',
      bg: '',
      status: 'Active',
      coCounsel: '',
      opposingCounsel: '',
      limitationDeadline: '',
      relatedMatterId: undefined
    });
    this.submitted.set(false);
    this.showForm.set(true);
  }

  onClientChange(clientIdStr: string, isEdit: boolean) {
    const clientId = Number(clientIdStr);
    const clientObj = this.ds.clients().find(c => Number(c.id) === clientId);
    if (!clientObj) return;

    if (isEdit) {
      this.editForm.update(f => ({ ...f, clientId, clientName: clientObj.name }));
    } else {
      this.form.update(f => ({ ...f, clientId, clientName: clientObj.name }));
    }
  }

  save() {
    this.submitted.set(true);
    if (!this.isFormValid) return;

    // Map frontend caseNumber to caseNo for backend mapping compatibility
    const payload: Partial<Matter> = { 
      ...this.form(), 
      caseNo: this.form().caseNumber,
      cnrNumber: this.form().cnrNumber || undefined,
      relatedMatterId: (this.form().relatedMatterId && this.form().relatedMatterId !== '') ? String(this.form().relatedMatterId) : undefined
    };

    this.ds.createMatter(payload).subscribe(() => {
      this.showForm.set(false);
      this.submitted.set(false);
    });
  }

  openViewModal(matter: Matter) {
    this.selectedMatter.set(matter);
    this.activeDetailTab.set('summary');
    this.showViewModal.set(true);
    this.loadMatterDocs(matter.id);
    this.ds.getMatterRollup(Number(matter.id)).subscribe(res => {
      this.matterFinanceRollup.set(res);
    });
  }

  openEditModal(matter: Matter) {
    this.selectedMatter.set(matter);

    let clientId = matter.clientId ? Number(matter.clientId) : undefined;
    let clientName = matter.clientName || matter.client || '';
    if (!clientId && clientName) {
      const matched = this.ds.clients().find(c => c.name === clientName);
      if (matched) {
        clientId = Number(matched.id);
      } else if (this.ds.clients().length > 0) {
        clientId = Number(this.ds.clients()[0].id);
        clientName = this.ds.clients()[0].name;
      }
    } else if (!clientId && this.ds.clients().length > 0) {
      clientId = Number(this.ds.clients()[0].id);
      clientName = this.ds.clients()[0].name;
    }

    const advocate = matter.advocate || matter.adv || (this.ds.users()[0]?.name ?? '');

    this.editForm.set({ 
      ...matter,
      title: matter.title || '',
      caseNumber: matter.caseNumber || matter.caseNo || '',
      caseNo: matter.caseNo || matter.caseNumber || '',
      cnrNumber: matter.cnrNumber || '',
      clientId,
      clientName,
      advocate,
      adv: advocate,
      court: matter.court || this.courts()[0] || 'Supreme Court of India',
      type: matter.type || this.types()[0] || 'Litigation',
      relatedMatterId: matter.relatedMatterId ? String(matter.relatedMatterId) : undefined
    });
    this.editSubmitted.set(false);
    this.showEditForm.set(true);
    this.showViewModal.set(false);
  }

  update() {
    const matter = this.selectedMatter();
    if (!matter) return;

    this.editSubmitted.set(true);
    if (!this.isEditFormValid) {
      this.toast.error('Please fix the errors in the form before saving.');
      return;
    }

    const formVal = this.editForm();
    const payload: Partial<Matter> = { 
      ...formVal, 
      caseNo: formVal.caseNumber || formVal.caseNo || undefined,
      cnrNumber: formVal.cnrNumber?.trim() ? formVal.cnrNumber.trim() : undefined,
      limitationDeadline: formVal.limitationDeadline?.trim() ? formVal.limitationDeadline.trim() : undefined,
      nextHearing: formVal.nextHearing?.trim() ? formVal.nextHearing.trim() : undefined,
      filingDate: formVal.filingDate?.trim() ? formVal.filingDate.trim() : undefined,
      relatedMatterId: (formVal.relatedMatterId && formVal.relatedMatterId !== '') ? String(formVal.relatedMatterId) : undefined
    };

    this.ds.updateMatter(matter.id, payload).subscribe({
      next: () => {
        this.showEditForm.set(false);
        this.editSubmitted.set(false);
        this.selectedMatter.set(null);
      },
      error: (err) => {
        console.error('Error updating matter:', err);
      }
    });
  }

  deleteMatter(id: string) {
    if (confirm('Are you sure you want to delete this matter?')) {
      this.ds.deleteMatter(id).subscribe();
    }
  }

  onMatterFileUpload(event: Event) {
    const matter = this.selectedMatter();
    if (!matter) return;

    const target = event.target as HTMLInputElement;
    const file = target?.files?.[0];
    if (!file) return;

    this.ds.storeLocalUrl(file.name, file);
    this.uploadingDoc.set(true);
    this.ds.uploadMatterDocument(
      matter.id,
      file,
      this.uploadForm().docType,
      this.uploadForm().tags
    ).subscribe({
      next: () => {
        this.uploadingDoc.set(false);
        this.showUploadModal.set(false);
        this.loadMatterDocs(matter.id);
      },
      error: () => {
        this.uploadingDoc.set(false);
      }
    });
  }

  saveTask() {
    const matter = this.selectedMatter();
    if (!matter) return;

    if (!this.isTaskFormValid) return;

    const payload: Partial<Task> = {
      title: this.taskForm().title,
      notes: this.taskForm().description,
      dueDate: this.taskForm().dueDate,
      due: this.taskForm().dueDate,
      priority: this.taskForm().priority as TaskPriority,
      assignedTo: this.taskForm().assignedTo,
      assign: this.taskForm().assignedTo,
      matterId: Number(matter.id),
      matterTitle: matter.title,
      done: false,
      status: 'To Do'
    };

    this.ds.createTask(payload).subscribe({
      next: () => {
        this.showAddTaskForm.set(false);
        this.taskForm.set({
          title: '',
          description: '',
          dueDate: '',
          priority: 'Medium',
          assignedTo: this.ds.users()[0]?.name || ''
        });
        this.toast.success('Task created successfully!');
        this.ds.loadTasks().subscribe();
      }
    });
  }

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
      const localBlobUrl = this.ds.getLocalUrl(doc.name);
      if (localBlobUrl) {
        rawUrl = localBlobUrl;
      }
    }

    const localUrl = rawUrl ? this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl) : null;
    
    const matter = this.selectedMatter();
    const clientName = matter?.clientName || doc.clientName || 'No Client';
    const matterTitle = matter?.title || 'Unlinked Matter';
    
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

  openDocPreview(doc: any) {
    this.selectedDocForPreview.set(doc);
    this.showPreviewModal.set(true);
  }

  deleteDoc(doc: any) {
    if (!this.auth.hasPermission('delete_matters')) {
      this.toast.error('You do not have permission to delete documents.');
      return;
    }
    if (!confirm(`Are you sure you want to delete "${doc.name}"?`)) return;

    this.ds.deleteDocument(doc.id).subscribe({
      next: () => {
        this.matterDocuments.update(list => list.filter(item => item.id !== doc.id));
        this.toast.success('Document deleted.');
      }
    });
  }

  getRelatedMatter(id: any) {
    if (!id) return null;
    return this.ds.matters().find(m => String(m.id) === String(id));
  }

  fmtPrecise(n?: number) {
    if (n === undefined || n === null) return '₹0.00';
    return '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
