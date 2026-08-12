import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../../environments/environment';

interface PathSegment {
  id: string;
  name: string;
  type: 'client' | 'matter';
}

import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { AppButtonComponent } from '../../../../shared/components/button/button.component';
import { DocumentPreviewModalComponent } from '../../../../shared/components/document-preview-modal/document-preview-modal.component';

@Component({
  selector: 'app-documents-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SearchInputComponent,
    AppButtonComponent,
    DocumentPreviewModalComponent
  ],
  templateUrl: './documents-page.component.html',
  styleUrl: './documents-page.component.scss'
})
export class DocumentsPageComponent implements OnInit {
  ds = inject(DataService);
  toast = inject(ToastService);
  auth = inject(AuthService);
  sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);

  // Lists from DMS APIs
  clientFolders = signal<any[]>([]);
  matterFolders = signal<any[]>([]);
  recent = signal<any[]>([]);

  // Navigation states
  activeTab = signal<'explorer' | 'recent'>('explorer');
  groupByMode = signal<'client' | 'matter'>('client');
  activePath = signal<PathSegment[]>([]);

  // Folder contents (when viewing documents inside a matter)
  folderContents = signal<any[]>([]);

  // Filter toolbar signals
  searchQuery = signal<string>('');
  docTypeFilter = signal<string>('');
  hideEmptyFolders = signal<boolean>(false);
  sortMode = signal<'name' | 'count' | 'date'>('name');
  viewMode = signal<'grid' | 'list'>('grid');

  // Upload modal state
  showUploadModal = signal(false);
  uploadForm = signal({
    clientId: '',
    matterId: '',
    docType: '',
    tags: ''
  });
  selectedFile = signal<File | null>(null);
  isDragging = signal<boolean>(false);
  uploadProgress = signal<number>(0);
  isUploading = signal<boolean>(false);

  // Sync master values
  docTypes = computed(() => this.ds.masters()?.docTypes || []);
  clientsList = this.ds.clients;

  // Dynamic matters list filtered by selected client in upload form
  uploadMattersFiltered = computed(() => {
    const selectedClientId = this.uploadForm().clientId;
    if (!selectedClientId) return this.ds.matters();
    return this.ds.matters().filter(m => String(m.clientId) === String(selectedClientId));
  });

  // Folder selector computed
  displayedFolders = computed(() => {
    const mode = this.groupByMode();
    const query = this.searchQuery().toLowerCase().trim();
    const hideEmpty = this.hideEmptyFolders();
    const sort = this.sortMode();
    const path = this.activePath();

    let items: any[] = [];

    if (path.length === 0) {
      // Root Level: Clients or Matters folders
      if (mode === 'client') {
        items = this.clientFolders().map(cf => ({
          id: cf.clientId,
          name: cf.clientName,
          type: 'client' as const,
          count: cf.count || 0,
          lastUpdated: cf.lastUpdated || '—',
          bg: 'var(--primary-light, #eff6ff)',
          tc: 'var(--primary, #3b82f6)'
        }));
      } else {
        items = this.matterFolders().map(mf => ({
          id: mf.matterId,
          name: mf.matterTitle,
          type: 'matter' as const,
          count: mf.count || 0,
          lastUpdated: mf.lastUpdated || '—',
          bg: 'var(--primary-light, #eff6ff)',
          tc: 'var(--primary, #3b82f6)'
        }));
      }
    } else if (path.length === 1 && path[0].type === 'client') {
      // Drilled into Client folder -> show Matters of this Client
      const clientId = path[0].id;
      const clientMatters = this.ds.matters().filter(m => String(m.clientId) === String(clientId));
      items = clientMatters.map(m => {
        const folderMeta = this.matterFolders().find(mf => String(mf.matterId) === String(m.id));
        return {
          id: String(m.id),
          name: m.title,
          type: 'matter' as const,
          count: folderMeta ? folderMeta.count : 0,
          lastUpdated: folderMeta ? folderMeta.lastUpdated : '—',
          bg: 'var(--primary-light, #eff6ff)',
          tc: 'var(--primary, #3b82f6)'
        };
      });
    }

    // Apply search filter
    if (query) {
      items = items.filter(f => f.name.toLowerCase().includes(query));
    }

    // Apply empty folders toggle filter
    if (hideEmpty) {
      items = items.filter(f => f.count > 0);
    }

    // Apply sorting
    if (sort === 'name') {
      items.sort((a, b) => a.name.localeCompare(b.name));
    } else if (sort === 'count') {
      items.sort((a, b) => b.count - a.count);
    } else if (sort === 'date') {
      items.sort((a, b) => b.lastUpdated.localeCompare(a.lastUpdated));
    }

    return items;
  });

  // Computed filtered files inside open matter folder
  filteredFiles = computed(() => {
    let files = this.folderContents();
    const query = this.searchQuery().toLowerCase().trim();
    const docType = this.docTypeFilter();

    // Exclude raw task-uploaded docs, keep only the filing entry
    files = files.filter((f: any) => !(f.source === 'task' && f.itemType !== 'filing'));

    if (query) {
      files = files.filter(f =>
        f.name.toLowerCase().includes(query) ||
        (f.tags && f.tags.some((t: string) => t.toLowerCase().includes(query)))
      );
    }

    if (docType) {
      files = files.filter(f => f.type === docType);
    }

    return files;
  });



  ngOnInit() {
    this.refreshDms();
    this.ds.loadMasters().subscribe();
    this.ds.loadClients().subscribe();
    this.ds.loadMatters().subscribe();

    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openUpload();
      }
    });
  }

  refreshDms() {
    this.ds.getClientFolders().subscribe(f => this.clientFolders.set(f));
    this.ds.getMatterFolders().subscribe(f => this.matterFolders.set(f));
    this.ds.getRecentDocuments().subscribe(d =>
      this.recent.set(d.filter((doc: any) => doc.source !== 'task'))
    );

    const path = this.activePath();
    if (path.length > 0) {
      const leaf = path[path.length - 1];
      this.loadFolderContents(leaf.type, leaf.id);
    }
  }

  openFolder(type: 'client' | 'matter', id: string, name: string) {
    if (type === 'client') {
      this.activePath.set([{ id, name, type }]);
      this.loadFolderContents(type, id);
    } else {
      // Drilling into a matter folder
      const path = this.activePath();
      if (path.length > 0 && path[0].type === 'client') {
        this.activePath.set([path[0], { id, name, type }]);
      } else {
        this.activePath.set([{ id, name, type }]);
      }
      this.loadFolderContents(type, id);
    }
  }

  loadFolderContents(type: 'client' | 'matter', id: string) {
    if (type === 'matter') {
      this.ds.getMatterFolderContents(id).subscribe(c => this.folderContents.set(c));
    } else if (type === 'client') {
      this.ds.getClientFolderContents(id).subscribe(c => this.folderContents.set(c));
    }
  }

  deleteDoc(doc: any, event?: Event) {
    if (event) event.stopPropagation();
    if (!this.auth.hasPermission('delete_matters')) {
      this.toast.error('You do not have permission to delete documents.');
      return;
    }
    if (!confirm(`Are you sure you want to delete "${doc.name}"?`)) return;

    const removeFromLocal = () => {
      this.folderContents.update(list => list.filter(item => item.id !== doc.id));
      this.recent.update(list => list.filter(item => item.id !== doc.id));
      this.refreshDms();
    };

    if (doc.itemType === 'filing') {
      // Item is a filed filing — delete via filings endpoint (which also cascade-deletes linked doc)
      this.ds.deleteFiling(String(doc.id)).subscribe({ next: removeFromLocal });
    } else {
      // Item is a plain document
      this.ds.deleteDocument(doc.id).subscribe({ next: removeFromLocal });
    }
  }

  navigateToBreadcrumb(index: number) {
    const path = this.activePath();
    const newPath = path.slice(0, index + 1);
    this.activePath.set(newPath);
    const leaf = newPath[newPath.length - 1];
    if (leaf.type === 'matter' || leaf.type === 'client') {
      this.loadFolderContents(leaf.type, leaf.id);
    } else {
      this.folderContents.set([]);
    }
  }

  resetPath() {
    this.activePath.set([]);
    this.folderContents.set([]);
  }

  openUpload() {
    let prefilledClientId = '';
    let prefilledMatterId = '';
    const path = this.activePath();

    if (path.length > 0) {
      if (path[0].type === 'client') {
        prefilledClientId = path[0].id;
      }
      const matterSeg = path.find(p => p.type === 'matter');
      if (matterSeg) {
        prefilledMatterId = matterSeg.id;
      }
    }

    const firstDocType = this.docTypes()[0] || '';
    this.uploadForm.set({
      clientId: prefilledClientId,
      matterId: prefilledMatterId,
      docType: firstDocType,
      tags: ''
    });
    this.selectedFile.set(null);
    this.uploadProgress.set(0);
    this.isUploading.set(false);
    this.showUploadModal.set(true);
  }

  onFileChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.selectedFile.set(file);
      this.ds.storeLocalUrl(file.name, file);
    }
  }

  // Drag and Drop helpers
  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.selectedFile.set(file);
      this.ds.storeLocalUrl(file.name, file);
      this.toast.info(`Selected file: ${file.name}`);
    }
  }

  performUpload() {
    const file = this.selectedFile();
    if (!file) {
      this.toast.error('Please select or drag a file to upload!');
      return;
    }

    if (!this.uploadForm().matterId) {
      this.toast.error('Please select a Matter to link this file!');
      return;
    }

    this.isUploading.set(true);
    this.uploadProgress.set(10);

    // Simulate upload progress steps
    const interval = setInterval(() => {
      const current = this.uploadProgress();
      if (current < 90) {
        this.uploadProgress.set(current + 15);
      }
    }, 150);

    const fd = new FormData();
    fd.append('file', file);
    fd.append('matterId', this.uploadForm().matterId);
    if (this.uploadForm().docType) fd.append('docType', this.uploadForm().docType);
    if (this.uploadForm().tags) fd.append('tags', this.uploadForm().tags);

    this.ds.uploadDocument(fd).subscribe({
      next: () => {
        clearInterval(interval);
        this.uploadProgress.set(100);
        setTimeout(() => {
          this.showUploadModal.set(false);
          this.isUploading.set(false);
          this.refreshDms();
          this.toast.success('Document uploaded and OCR metadata indexed successfully.');
        }, 300);
      },
      error: () => {
        clearInterval(interval);
        this.isUploading.set(false);
        this.toast.error('Document upload failed. Please try again.');
      }
    });
  }

  // Helper checking if a folder is empty
  isFolderEmpty(folder: any): boolean {
    return folder.count === 0;
  }

  // Document preview helpers
  showPreviewModal = signal(false);
  selectedDocForPreview = signal<any>(null);

  getPreviewDetails = computed(() => {
    const doc = this.selectedDocForPreview();
    if (!doc) return null;

    let rawUrl: string | null = null;
    if (doc.s3Url) {
      const apiBase = environment.apiBaseUrl.replace('/api', '');
      if (doc.s3Url.startsWith('/uploads/') || doc.s3Url.startsWith('/api/')) {
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

    let clientName = doc.clientName || 'No Client';
    let matterTitle = 'Unlinked Matter';

    if (doc.matterId) {
      const matter = this.ds.matters().find(m => String(m.id) === String(doc.matterId));
      if (matter) {
        matterTitle = matter.title;
        clientName = matter.clientName || clientName;
      }
    } else if (doc.clientId) {
      const client = this.ds.clients().find(c => String(c.id) === String(doc.clientId));
      if (client) {
        clientName = client.name;
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
}
