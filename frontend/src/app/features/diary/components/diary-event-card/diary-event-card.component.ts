import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-diary-event-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="diary-event-card" [class.is-urgent]="event.urgent" [class.is-conflict]="isConflict" (click)="onViewDetail.emit(event)">
      <div class="card-left">
        <div class="type-icon" [ngClass]="typeBadgeClass(event.type)">
          <i [class]="typeIcon(event.type)"></i>
        </div>
        <div class="card-info">
          <div class="header-line">
            <span class="event-time"><i class="far fa-clock"></i> {{ event.time || '09:00' }}</span>
            <span class="event-title">{{ event.title }}</span>
            <span class="type-badge" [ngClass]="typeBadgeClass(event.type)">{{ typeLabel(event.type) }}</span>
          </div>

          <!-- Structured Meta Grid Line: Court, Advocate, Matter, Client -->
          <div class="meta-chips-line">
            <a *ngIf="event.matterId; else matterText"
               [routerLink]="['/app/matters', event.matterId]"
               class="chip matter-chip"
               (click)="$event.stopPropagation()">
              <i class="fas fa-briefcase"></i> Matter: {{ event.matterTitle }}
            </a>
            <ng-template #matterText>
              <span class="chip matter-chip" *ngIf="event.matterTitle">
                <i class="fas fa-briefcase"></i> Matter: {{ event.matterTitle }}
              </span>
            </ng-template>

            <span class="chip client-chip" *ngIf="event.clientName">
              <i class="fas fa-building"></i> Client: {{ event.clientName }}
            </span>

            <span class="chip court-chip" *ngIf="event.court">
              <i class="fas fa-landmark"></i> Court: {{ event.court }}
            </span>

            <span class="chip advocate-chip" *ngIf="event.ownerName">
              <i class="fas fa-user-tie"></i> Advocate: {{ event.ownerName }}
            </span>
          </div>

          <!-- Notes / Bench Details -->
          <div class="notes-box" *ngIf="event.notes">
            <i class="fas fa-info-circle"></i>
            <span>{{ event.notes }}</span>
          </div>
        </div>
      </div>

      <div class="card-right">
        <span class="badge urgent-badge" *ngIf="event.urgent">
          <i class="fas fa-exclamation-circle"></i> Urgent
        </span>
        <span class="badge conflict-badge" *ngIf="isConflict">
          <i class="fas fa-exclamation-triangle"></i> Conflict
        </span>

        <div class="actions">
          <button class="action-btn view-btn" title="View Full Details" (click)="$event.stopPropagation(); onViewDetail.emit(event)">
            <i class="fas fa-eye"></i>
          </button>
          <button class="action-btn edit-btn" *ngIf="event.isLocal !== false" title="Edit Event" (click)="$event.stopPropagation(); onEdit.emit($event)">
            <i class="fas fa-pen"></i>
          </button>
          <button class="action-btn delete-btn" *ngIf="event.isLocal !== false" title="Delete Event" (click)="$event.stopPropagation(); onDelete.emit($event)">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .diary-event-card {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 0.75rem 0.9rem;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      margin-bottom: 0.6rem;
      transition: all 0.2s ease;
      box-shadow: 0 1px 3px rgba(0,0,0,0.03);
      width: 100%;
      box-sizing: border-box;

      &:hover {
        border-color: #cbd5e1;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      }

      &.is-urgent {
        border-left: 4px solid #ef4444;
      }
      &.is-conflict {
        background: #fff5f5;
        border-color: #fecaca;
      }
    }

    .card-left {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      flex: 1;
      min-width: 0;
    }

    .type-icon {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      flex-shrink: 0;
      margin-top: 2px;

      &.b-g { background: #e0e7ff; color: #4338ca; }
      &.b-o { background: #ffedd5; color: #c2410c; }
      &.b-a { background: #fef3c7; color: #b45309; }
      &.b-r { background: #fee2e2; color: #b91c1c; }
    }

    .card-info {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      min-width: 0;
      flex: 1;
    }

    .header-line {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;

      .event-time {
        font-size: 0.75rem;
        font-weight: 700;
        color: #475569;
        background: #f1f5f9;
        padding: 0.15rem 0.4rem;
        border-radius: 4px;
      }

      .event-title {
        font-size: 0.88rem;
        font-weight: 700;
        color: #0f172a;
        word-break: break-word;
        line-height: 1.3;
      }
    }

    .meta-chips-line {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.35rem;

      .chip {
        font-size: 0.72rem;
        padding: 0.15rem 0.45rem;
        border-radius: 12px;
        display: inline-flex;
        align-items: center;
        gap: 0.3rem;
        text-decoration: none;
        transition: background 0.15s ease;
        word-break: break-word;

        &.matter-chip {
          background: #eff6ff;
          color: #1d4ed8;
          font-weight: 600;
          &:hover { background: #dbeafe; }
        }
        &.client-chip {
          background: #f8fafc;
          color: #475569;
          border: 1px solid #e2e8f0;
        }
        &.court-chip {
          background: #f1f5f9;
          color: #64748b;
        }
      }
    }

    .owner-chip-line {
      display: flex;
      align-items: center;
      gap: 0.4rem;

      .owner-avatar {
        width: 18px;
        height: 18px;
        border-radius: 50%;
        color: #fff;
        font-size: 0.6rem;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .owner-name {
        font-size: 0.72rem;
        color: #64748b;
        font-weight: 500;
      }
    }

    .notes-text {
      font-size: 0.75rem;
      color: #64748b;
      line-height: 1.3;
    }

    .card-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.4rem;
      margin-left: 0.5rem;

      .badge {
        font-size: 0.7rem;
        padding: 0.15rem 0.45rem;
        border-radius: 6px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 0.25rem;
        white-space: nowrap;

        &.urgent-badge { background: #fee2e2; color: #dc2626; }
        &.conflict-badge { background: #fef2f2; color: #b91c1c; border: 1px solid #fca5a5; }
      }

      .actions {
        display: flex;
        gap: 0.2rem;

        .action-btn {
          background: transparent;
          border: none;
          color: #94a3b8;
          padding: 0.25rem;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.15s ease;

          &:hover {
            color: #0f172a;
            background: #f1f5f9;
          }
          &.delete-btn:hover {
            color: #ef4444;
            background: #fee2e2;
          }
        }
      }
    }
    .type-badge {
      font-size: 0.68rem;
      font-weight: 700;
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
      text-transform: uppercase;
      margin-left: 0.3rem;
      &.b-g { background: #e0e7ff; color: #4338ca; }
      &.b-o { background: #ffedd5; color: #c2410c; }
      &.b-a { background: #fef3c7; color: #b45309; }
      &.b-r { background: #fee2e2; color: #b91c1c; }
    }

    .notes-box {
      font-size: 0.76rem;
      color: #475569;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      padding: 0.3rem 0.55rem;
      border-radius: 6px;
      display: flex;
      align-items: center;
      gap: 0.4rem;
      margin-top: 0.2rem;
      i { color: #64748b; font-size: 0.72rem; }
    }

    .chip.advocate-chip {
      background: #fdf4ff;
      color: #86198f;
      border: 1px solid #f5d0fe;
    }
  `]
})
export class DiaryEventCardComponent {
  @Input() event: any;
  @Input() showOwner: boolean = false;
  @Input() isConflict: boolean = false;

  @Output() onEdit = new EventEmitter<MouseEvent>();
  @Output() onDelete = new EventEmitter<MouseEvent>();
  @Output() onViewDetail = new EventEmitter<any>();

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
      task: 'Task',
      deadline: 'Deadline',
    };
    return map[type || ''] || 'Event';
  }

  typeBadgeClass(type?: string): string {
    const map: Record<string, string> = {
      hearing: 'b-g',
      meeting: 'b-o',
      task: 'b-a',
      deadline: 'b-r',
    };
    return map[type || ''] || 'b-g';
  }

  getOwnerInitials(): string {
    if (!this.event?.ownerName) return 'U';
    const parts = this.event.ownerName.split(' ');
    return parts.length > 1
      ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      : this.event.ownerName.substring(0, 2).toUpperCase();
  }

  getOwnerGradient(): string {
    return 'linear-gradient(135deg,#b45309,#d97706)';
  }
}
