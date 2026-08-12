import { Component, inject, OnInit, signal, computed, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../../environments/environment';
import type { Client } from '../../../../core/models';
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
  selector: 'app-clients-page',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DocumentPreviewModalComponent, PaginatorComponent,
    AppButtonComponent, StatusBadgeComponent, SearchInputComponent, FilterTabsComponent
  ],
  templateUrl: './clients-page.component.html',
  styleUrl: './clients-page.component.scss'
})
export class ClientsPageComponent implements OnInit, OnDestroy {
  ds = inject(DataService);
  sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);
  clients  = this.ds.clients;
  loading  = this.ds.clientsLoading;
  total    = this.ds.clientTotal;
  search   = signal('');
  filter   = signal('All');
  showForm = signal(false);

  // 0-based page index (Spring convention)
  page = signal(0);
  readonly size = 12;

  private _destroy$ = new Subject<void>();
  private _search$  = new Subject<string>();

  constructor() {
    // Debounced search — fires API call 300 ms after last keystroke
    this._search$.pipe(debounceTime(300), takeUntil(this._destroy$)).subscribe(q => {
      this.page.set(0);
      this._reload();
    });
  }

  // Dynamic types list from Masters
  types = computed(() => {
    return this.ds.masters()?.clientTypes || ['Individual', 'Company', 'Government', 'NGO', 'Trust'];
  });

  // Dynamic filter tabs
  filters = computed(() => {
    return ['All', ...this.types()];
  });

  // Initialize form with dynamic default type if available
  form = signal<Partial<Client>>({ type: 'Individual', status: 'Active' });

  // Track validation status
  submitted = signal(false);

  // View, Tab and Edit states
  selectedClient = signal<Client | null>(null);
  showViewModal = signal(false);
  activeProfileTab = signal<'overview' | 'corporate' | 'documents' | 'compliance' | 'finance'>('overview');
  clientDocuments = signal<any[]>([]);
  clientFinanceRollup = signal<any>(null);
  showEditForm = signal(false);
  editForm = signal<Partial<Client>>({});
  editSubmitted = signal(false);

  ngOnInit() {
    this._reload();
    this.ds.loadMasters().subscribe();
    this.ds.loadMatters({ size: 1000 }).subscribe();

    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openAddForm();
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
    this.ds.loadClients({
      page:   this.page(),
      size:   this.size,
      sort:   'name',
      order:  'asc',
      search: this.search() || undefined,
      filter: f !== 'All' ? { type: f } : undefined,
    }).subscribe();
  }

  getActiveMattersCount(clientId: any): number {
    if (!clientId) return 0;
    const cid = String(clientId);
    return this.ds.matters().filter(m => {
      const matchClient = String(m.clientId) === cid;
      const statusLower = (m.status || 'active').toLowerCase();
      const isActive = statusLower === 'active' || statusLower === 'pending' || statusLower === 'ongoing';
      return matchClient && isActive;
    }).length;
  }

  // Validations for creation form
  get isNameValid(): boolean {
    const name = this.form().name?.trim();
    return !!name && name.length >= 2;
  }

  get isEmailValid(): boolean {
    const email = this.form().email?.trim();
    if (!email) return true; // Optional field
    const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return regex.test(email);
  }

  get isMobileValid(): boolean {
    const mobile = this.form().mobile?.trim();
    if (!mobile) return true; // Optional field
    const regex = /^[+]?[0-9\s\-()]{8,15}$/;
    return regex.test(mobile);
  }

  get isFormValid(): boolean {
    return this.isNameValid && this.isEmailValid && this.isMobileValid;
  }

  // Validations for edit form
  get isEditNameValid(): boolean {
    const name = this.editForm().name?.trim();
    return !!name && name.length >= 2;
  }

  get isEditEmailValid(): boolean {
    const email = this.editForm().email?.trim();
    if (!email) return true; // Optional field
    const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return regex.test(email);
  }

  get isEditMobileValid(): boolean {
    const mobile = this.editForm().mobile?.trim();
    if (!mobile) return true; // Optional field
    const regex = /^[+]?[0-9\s\-()]{8,15}$/;
    return regex.test(mobile);
  }

  get isEditFormValid(): boolean {
    return this.isEditNameValid && this.isEditEmailValid && this.isEditMobileValid;
  }

  openAddForm() {
    const defaultType = this.types()[0] || 'Individual';
    this.form.set({ type: defaultType, status: 'Active' });
    this.submitted.set(false);
    this.showForm.set(true);
  }

  openViewModal(client: Client) {
    this.selectedClient.set(client);
    this.activeProfileTab.set('overview');
    this.showViewModal.set(true);
    this.loadClientDocs(client.id);
    this.ds.getClientRollup(Number(client.id)).subscribe(res => {
      this.clientFinanceRollup.set(res);
    });
  }

  loadClientDocs(clientId: string) {
    this.ds.getClientFolderContents(clientId).subscribe({
      next: (docs) => {
        // filter out documents that have a matterId (only keep client-level documents)
        const clientDocs = docs.filter(d => !d.matterId);
        this.clientDocuments.set(clientDocs);
      }
    });
  }

  onClientFileUpload(clientId: string, event: Event) {
    const target = event.target as HTMLInputElement;
    const file = target?.files?.[0];
    if (!file) return;

    this.ds.storeLocalUrl(file.name, file);
    this.ds.uploadClientDocument(clientId, file, 'Client Document').subscribe({
      next: () => {
        this.loadClientDocs(clientId);
        target.value = ''; // Reset input
      }
    });
  }

  get complianceChecklist() {
    const client = this.selectedClient();
    if (!client) return [];

    const docs = this.clientDocuments();
    const hasDoc = (keyword: string) => docs.some(d =>
      d.name?.toLowerCase().includes(keyword) ||
      d.type?.toLowerCase().includes(keyword)
    );

    const list: { name: string; status: 'Passed' | 'Missing'; details: string }[] = [];

    if (client.type?.toLowerCase() === 'individual') {
      const hasAadhar = !!client.aadhar?.trim() || hasDoc('aadhar');
      list.push({
        name: 'Aadhaar Card (KYC ID Proof)',
        status: hasAadhar ? 'Passed' : 'Missing',
        details: hasAadhar ? `ID Proof present (No: ${client.aadhar || 'Document uploaded'})` : 'Please upload Aadhaar or specify in Overview'
      });
      const hasPan = !!client.pan?.trim() || hasDoc('pan');
      list.push({
        name: 'PAN Card (Tax Registration)',
        status: hasPan ? 'Passed' : 'Missing',
        details: hasPan ? `PAN present (No: ${client.pan || 'Document uploaded'})` : 'Please upload PAN copy or specify in Overview'
      });
    } else {
      const hasCin = !!client.cin?.trim() || hasDoc('cin') || hasDoc('incorporation');
      list.push({
        name: 'Certificate of Incorporation (CIN/LLPIN)',
        status: hasCin ? 'Passed' : 'Missing',
        details: hasCin ? `CIN present (No: ${client.cin || 'Document uploaded'})` : 'Please upload Certificate of Incorporation or specify in Corporate tab'
      });
      const hasGst = !!client.gstin?.trim() || !!client.gst?.trim() || hasDoc('gst');
      list.push({
        name: 'GSTIN Registration',
        status: hasGst ? 'Passed' : 'Missing',
        details: hasGst ? `GSTIN present (No: ${client.gstin || client.gst || 'Document uploaded'})` : 'Please upload GST registration or specify in Corporate tab'
      });
      const hasPan = !!client.pan?.trim() || hasDoc('pan');
      list.push({
        name: 'Company PAN Card',
        status: hasPan ? 'Passed' : 'Missing',
        details: hasPan ? `Company PAN present (No: ${client.pan || 'Document uploaded'})` : 'Please upload PAN card copy or specify in Overview'
      });
    }

    const hasVak = client.vakalatnamaOnFile || hasDoc('vakalatnama');
    list.push({
      name: 'Vakalatnama (Authorisation Form)',
      status: hasVak ? 'Passed' : 'Missing',
      details: hasVak ? 'Vakalatnama is signed and verified on file' : 'Signed Vakalatnama document is missing'
    });

    const hasEng = client.engagementLetterSigned || hasDoc('engagement');
    list.push({
      name: 'Engagement Letter (Agreement Terms)',
      status: hasEng ? 'Passed' : 'Missing',
      details: hasEng ? 'Engagement letter is signed and on record' : 'Signed Engagement letter is missing'
    });

    return list;
  }

  openEditModal(client: Client) {
    this.selectedClient.set(client);
    this.editForm.set({ ...client });
    this.editSubmitted.set(false);
    this.showEditForm.set(true);
    this.showViewModal.set(false);
  }

  save() {
    this.submitted.set(true);
    if (!this.isFormValid) return;

    this.ds.createClient(this.form()).subscribe({
      next: () => {
        this.showForm.set(false);
        const defaultType = this.types()[0] || 'Individual';
        this.form.set({ type: defaultType, status: 'Active' });
        this.submitted.set(false);
      }
    });
  }

  update() {
    const client = this.selectedClient();
    if (!client) return;

    this.editSubmitted.set(true);
    if (!this.isEditFormValid) return;

    this.ds.updateClient(client.id, this.editForm()).subscribe({
      next: (updatedClient) => {
        this.showEditForm.set(false);
        this.selectedClient.set(updatedClient);
        this.editSubmitted.set(false);
        this.showViewModal.set(true); // Return to view profile modal
        this.loadClientDocs(client.id);
      }
    });
  }

  selectedDocForPreview = signal<any>(null);
  showPreviewModal = signal(false);

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

    const client = this.selectedClient();
    const clientName = client?.name || doc.clientName || 'No Client';

    let matterTitle = 'Unlinked Matter';
    if (doc.matterId) {
      const matter = this.ds.matters().find(m => String(m.id) === String(doc.matterId));
      if (matter) {
        matterTitle = matter.title;
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

  openDocPreview(doc: any) {
    this.selectedDocForPreview.set(doc);
    this.showPreviewModal.set(true);
  }

  fmtPrecise(n?: number) {
    if (n === undefined || n === null) return '₹0.00';
    return '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
