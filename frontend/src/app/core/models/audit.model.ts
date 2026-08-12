export interface AuditEntry {
  id: string | number;
  tenantId?: string;
  userId?: number;
  userEmail?: string;
  action: string;
  entity?: string;
  entityId?: string;
  details?: string;
  ipAddress?: string;
  risk?: string;
  createdAt: string;
}
