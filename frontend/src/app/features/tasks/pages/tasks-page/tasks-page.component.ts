import { Component, inject, OnInit, signal, computed, effect, untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DataService } from '../../../../core/services/data.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Filing, Task } from '../../../../core/models';
import { ToastService } from '../../../../core/services/toast.service';
import { DomSanitizer } from '@angular/platform-browser';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tasks-page.component.html',
  styleUrl: './tasks-page.component.scss'
})
export class TasksPageComponent implements OnInit {
  ds = inject(DataService);
  auth = inject(AuthService);
  toast = inject(ToastService);
  sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);

  tasks = this.ds.tasks;
  loading = this.ds.tasksLoading;
  search = signal('');
  filter = signal('All');
  showForm = signal(false);
  form = signal<Partial<Task>>({ priority: 'Medium', status: 'To Do' });
  submitted = signal(false);

  // Task edit form states
  showEditTaskForm = signal(false);
  editTaskForm = signal<Partial<Task>>({});
  editTaskSubmitted = signal(false);

  // Document preview helpers
  showPreviewModal = signal(false);
  selectedDocForPreview = signal<any>(null);

  getPreviewDetails = computed(() => {
    const doc = this.selectedDocForPreview();
    if (!doc) return null;

    const rawUrl = doc.s3Url && doc.s3Url.startsWith('http') ? doc.s3Url : null;
    const localUrl = rawUrl ? this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl) : null;

    let clientName = doc.clientName || 'No Client';
    let matterTitle = 'Unlinked Matter';

    if (doc.matterId) {
      const matter = this.matters().find(m => String(m.id) === String(doc.matterId));
      if (matter) {
        matterTitle = matter.title;
        clientName = matter.clientName || clientName;
      }
    }

    const ext = doc.name.split('.').pop()?.toLowerCase() || '';
    const isImage = ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext);
    const isPdf = ext === 'pdf';
    const isTemplateInstance = doc.source?.toLowerCase().includes('template') ||
      doc.docType?.toLowerCase().includes('template') ||
      (!isImage && !isPdf);

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

  // View settings
  viewMode = signal<'kanban' | 'list'>('kanban');
  selectedTask = signal<Task | null>(null);
  showDetailModal = signal(false);

  // Pagination states
  currentPage = signal(1);
  pageSize = 8;

  // Attached files list & File Objects
  attachedFiles: string[] = [];
  selectedFilesToUpload: File[] = [];

  // Master data signals
  matters = this.ds.matters;
  users = this.ds.users;
  readonly filters = ['All', 'To Do', 'Under Review', 'Done', 'Overdue'];

  prios = computed(() => this.ds.masters()?.priorities || ['Urgent', 'High', 'Medium', 'Low']);
  taskTypes = computed(() => this.ds.masters()?.taskTypes || ['Draft Petition', 'Research', 'Court Appearance', 'Filing', 'Client Meeting']);

  constructor() {
    effect(() => {
      this.search();
      this.filter();
      untracked(() => this.currentPage.set(1));
    });
  }

  ngOnInit() {
    this.ds.loadTasks().subscribe();
    this.ds.loadMasters().subscribe();
    this.ds.loadMatters().subscribe();
    this.ds.loadUsers().subscribe();
    this.ds.loadFilings().subscribe();

    this.route.queryParams.subscribe((params: any) => {
      if (params['new'] === '1') {
        this.openNewTaskModal();
      }
    });
  }

  get filtered() {
    let list = this.tasks();
    // User with manage tasks perm can only review task and see all tasks unless individuals can only see task assigned to them
    if (!this.auth.hasPermission('manage_tasks')) {
      const currentUserName = this.auth.userName();
      if (currentUserName) {
        list = list.filter(t => t.assignedTo === currentUserName);
      } else {
        list = [];
      }
    }
    return list
      .filter(t => {
        if (this.filter() === 'All') return true;
        if (this.filter() === 'Done') return t.done || t.status === 'Done';
        if (this.filter() === 'To Do') return !t.done && (t.status === 'To Do' || t.status === 'Open') && !this.isTaskOverdue(t);
        if (this.filter() === 'Overdue') return this.isTaskOverdue(t);
        return !t.done && t.status === this.filter();
      })
      .filter(t => !this.search() || t.title?.toLowerCase().includes(this.search().toLowerCase()));
  }

  isTaskOverdue(t: Task): boolean {
    if (t.done || t.status === 'Done' || t.status === 'Under Review') return false;
    if (t.status === 'Overdue') return true;
    if (!t.dueDate) return false;
    const today = new Date().toISOString().split('T')[0];
    return t.dueDate < today;
  }

  // Kanban buckets
  get todoTasks() {
    return this.filtered.filter(t =>
      !t.done &&
      (t.status === 'To Do' || t.status === 'Open' || !t.status) &&
      !this.isTaskOverdue(t)
    );
  }

  get overdueTasks() {
    return this.filtered.filter(t => this.isTaskOverdue(t));
  }

  get underReviewTasks() {
    return this.filtered.filter(t => !t.done && t.status === 'Under Review');
  }

  get doneTasks() {
    return this.filtered.filter(t => t.done || t.status === 'Done');
  }

  // Pagination getters
  get totalPages() {
    return Math.ceil(this.filtered.length / this.pageSize) || 1;
  }

  get paginatedList() {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  openNewTaskModal() {
    const firstMatter = this.matters()[0];
    const firstUser = this.users()[0];
    const firstType = this.taskTypes()[0];
    const firstPriority = this.prios()[0] || 'Medium';

    this.form.set({
      title: '',
      matterId: firstMatter ? Number(firstMatter.id) : undefined,
      matterTitle: firstMatter ? firstMatter.title : '',
      priority: firstPriority as any,
      dueDate: new Date().toISOString().split('T')[0],
      assignedTo: firstUser ? firstUser.name : '',
      type: firstType || '',
      status: 'To Do',
      done: false,
      notes: '',
      createdBy: this.auth.userName() || 'System'
    });
    this.submitted.set(false);
    this.showForm.set(true);
  }

  onMatterChange(id: string) {
    const m = this.matters().find(x => String(x.id) === id);
    if (m) {
      this.form.update(f => ({ ...f, matterId: Number(m.id), matterTitle: m.title }));
    }
  }

  onEditMatterChange(id: string) {
    const m = this.matters().find(x => String(x.id) === id);
    if (m) {
      this.editTaskForm.update(f => ({ ...f, matterId: Number(m.id), matterTitle: m.title }));
    }
  }

  openDetail(task: Task) {
    this.selectedTask.set(task);
    this.attachedFiles = [];
    this.selectedFilesToUpload = [];
    this.showDetailModal.set(true);
  }

  onFileSelected(event: any) {
    const files = event.target.files;
    if (files) {
      for (let i = 0; i < files.length; i++) {
        this.attachedFiles.push(files[i].name);
        this.selectedFilesToUpload.push(files[i]);
      }
    }
  }

  removeAttachedFile(index: number) {
    this.attachedFiles.splice(index, 1);
    this.selectedFilesToUpload.splice(index, 1);
  }

  // Temp storage for task uploads prior to approval
  uploadedTaskFiles: Record<string, { name: string; s3Url: string }[]> = {};

  submitForReview(task: Task) {
    if (this.selectedFilesToUpload.length === 0) {
      this.ds.updateTask(task.id!, { status: 'Under Review' }).subscribe(() => {
        this.showDetailModal.set(false);
        this.toast.success('Task submitted for review.');
        this.ds.loadTasks().subscribe();
      });
      return;
    }

    let completedUploads = 0;
    const linkedMatter = task.matterId ? this.matters().find(m => String(m.id) === String(task.matterId)) : undefined;

    this.selectedFilesToUpload.forEach(file => {
      // 1. Upload file attachment
      this.ds.uploadFilingAttachment(file).subscribe({
        next: (uploaded: any) => {
          this.ds.storeLocalUrl(file.name, file);

          // 2. Create a Filing in Draft stage so the file immediately enters the filing workflow
          const filingPayload: Partial<Filing> = {
            title: `Task Submission: ${file.name}`,
            matterId: task.matterId ? Number(task.matterId) : undefined,
            matterTitle: task.matterTitle,
            court: linkedMatter?.court,
            dueDate: task.dueDate,
            filingType: 'Task Submission',
            stage: 'Draft',
            status: 'Draft',
            advocate: task.assignedTo,
            notes: `Submitted for task #${task.id}: ${task.title}`,
            s3Url: uploaded?.s3Url,
            source: 'task'
          } as any;

          this.ds.createFiling(filingPayload).subscribe(() => {
            completedUploads++;
            if (completedUploads === this.selectedFilesToUpload.length) {
              this.ds.updateTask(task.id!, { status: 'Under Review' }).subscribe(() => {
                this.showDetailModal.set(false);
                this.attachedFiles = [];
                this.selectedFilesToUpload = [];
                this.toast.success('Task submitted for review with attachments.');
                this.ds.loadTasks().subscribe();
                this.ds.loadFilings().subscribe();
              });
            }
          });
        },
        error: () => {
          completedUploads++;
          if (completedUploads === this.selectedFilesToUpload.length) {
            this.ds.updateTask(task.id!, { status: 'Under Review' }).subscribe(() => {
              this.showDetailModal.set(false);
              this.attachedFiles = [];
              this.selectedFilesToUpload = [];
              this.toast.success('Task submitted for review.');
              this.ds.loadTasks().subscribe();
            });
          }
        }
      });
    });
  }

  approveTask(task: Task) {
    this.ds.updateTask(task.id!, { status: 'Done', done: true }).subscribe(() => {
      this.showDetailModal.set(false);
      this.toast.success('Task approved and completed successfully!');
      this.ds.loadTasks().subscribe();
      this.ds.loadFilings().subscribe();
    });
  }

  rejectTask(task: Task) {
    this.ds.updateTask(task.id!, { status: 'To Do', done: false }).subscribe(() => {
      this.showDetailModal.set(false);
      this.toast.info('Task reopened and returned to To Do.');
      this.ds.loadTasks().subscribe();
    });
  }

  getTaskSubmissions(task: Task) {
    if (!task || !task.id) return [];

    // 1. Files uploaded during current review session for this specific task
    const pending = (this.uploadedTaskFiles[task.id] || []).map(f => ({
      id: 'pending_' + f.name,
      title: `Task Submission: ${f.name}`,
      s3Url: f.s3Url,
      filedDate: 'Pending Review',
      matterId: task.matterId,
      advocate: task.assignedTo
    }));

    // 2. Existing filings matching this specific taskId in notes
    const existing = this.ds.filings().filter(f =>
      (f as any).source === 'task' &&
      f.title?.startsWith('Task Submission:') &&
      f.notes?.includes(`task #${task.id}:`)
    );

    return [...pending, ...existing];
  }

  getFileIconClass(fileName: string): string {
    if (!fileName) return 'far fa-file-alt';
    const ext = fileName.toLowerCase().split('.').pop();
    if (['jpg', 'jpeg', 'png', 'gif', 'svg'].includes(ext!)) return 'far fa-file-image';
    if (['doc', 'docx'].includes(ext!)) return 'far fa-file-word';
    if (['xls', 'xlsx'].includes(ext!)) return 'far fa-file-excel';
    if (['pdf'].includes(ext!)) return 'far fa-file-pdf';
    return 'far fa-file-alt';
  }

  getFileViewerUrl(fileName: string): string {
    if (!fileName) return '#';
    const cleanName = fileName.replace('Task Submission: ', '');
    const base = environment.apiBaseUrl.replace('/api', '');
    return `${base}/uploads/${cleanName}`;
  }

  openFilingPreview(doc: any) {
    const base = environment.apiBaseUrl.replace('/api', '');
    const s3Url = doc.s3Url || `/uploads/${doc.title.replace('Task Submission: ', '')}`;
    const fullUrl = s3Url.startsWith('http') ? s3Url : `${base}${s3Url}`;
    const mockDoc = {
      id: doc.id,
      name: doc.title ? doc.title.replace('Task Submission: ', '') : 'Document',
      docType: 'Task Submission',
      matterId: doc.matterId,
      s3Url: fullUrl,
      source: 'Task Attachment',
      uploadedBy: doc.advocate || 'Advocate'
    };
    this.selectedDocForPreview.set(mockDoc);
    this.showPreviewModal.set(true);
  }

  deleteSubmission(doc: any, event: MouseEvent) {
    event.stopPropagation();
    const cleanName = doc.title ? doc.title.replace('Task Submission: ', '') : 'Document';
    if (!confirm(`Remove "${cleanName}" from this task's submitted documents? This cannot be undone.`)) return;

    if (String(doc.id).startsWith('pending_')) {
      // Remove from pending local uploads array if it hasn't been saved to DB yet
      for (const tId in this.uploadedTaskFiles) {
        this.uploadedTaskFiles[tId] = this.uploadedTaskFiles[tId].filter(f => 'pending_' + f.name !== doc.id);
      }
      this.toast.success('Submitted document removed.');
      return;
    }

    this.ds.deleteFiling(doc.id).subscribe(() => {
      this.ds.loadFilings().subscribe();
      this.toast.success('Submitted document removed.');
    });
  }

  get isFormValid(): boolean {
    const f = this.form();
    return !!(f.title && f.title.trim().length >= 3 && f.matterId && f.priority && f.dueDate && f.assignedTo && f.type);
  }

  toggle(id: string) {
    this.ds.toggleTaskDone(id).subscribe(() => {
      this.ds.loadTasks().subscribe();
    });
  }

  save() {
    this.submitted.set(true);
    if (!this.isFormValid) return;

    const payload = {
      ...this.form(),
      createdBy: this.auth.userName() || 'System'
    };

    this.ds.createTask(payload).subscribe(() => {
      this.showForm.set(false);
      this.submitted.set(false);
      this.ds.loadTasks().subscribe();
    });
  }

  get isEditTaskFormValid(): boolean {
    const f = this.editTaskForm();
    return !!(f.title && f.title.trim().length >= 3 && f.matterId && f.priority && f.dueDate && f.assignedTo && f.type);
  }

  openEditTaskModal(task: Task) {
    this.editTaskForm.set({ ...task });
    this.editTaskSubmitted.set(false);
    this.showEditTaskForm.set(true);
    this.showDetailModal.set(false);
  }

  updateTask() {
    this.editTaskSubmitted.set(true);
    if (!this.isEditTaskFormValid) return;

    const payload = {
      ...this.editTaskForm()
    };

    this.ds.updateTask(payload.id!, payload).subscribe(() => {
      this.showEditTaskForm.set(false);
      this.editTaskSubmitted.set(false);
      this.ds.loadTasks().subscribe();
      this.toast.success('Task updated successfully!');
    });
  }

  deleteTask(id: string) {
    if (confirm('Are you sure you want to delete this task?')) {
      this.ds.deleteTask(id).subscribe(() => {
        this.showDetailModal.set(false);
        this.toast.success('Task deleted successfully!');
        this.ds.loadTasks().subscribe();
      });
    }
  }
}